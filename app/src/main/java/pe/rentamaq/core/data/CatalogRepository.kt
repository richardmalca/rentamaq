package pe.rentamaq.core.data

import androidx.room.withTransaction
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.BookingMessage
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.core.data.local.RentamaqDatabase
import java.time.LocalDate
import java.time.LocalDateTime

class CatalogRepository(private val database: RentamaqDatabase) {
    private val dao = database.catalog()
    val equipment = dao.equipment()
    val bookings = dao.bookings()

    suspend fun seed() = database.withTransaction {
        dao.seed(demoEquipment)
        dao.seedBookings(demoBookings())
    }

    suspend fun reserve(booking: Booking): Long? = database.withTransaction {
        val equipment = booking.equipmentId?.let { dao.equipmentById(it) } ?: return@withTransaction null
        if (hasConflict(booking, equipment)) return@withTransaction null
        val deadline = minOf(booking.startTime() ?: return@withTransaction null, LocalDateTime.now(bookingZone).plusHours(24))
        dao.insert(booking.copy(expiresAt = deadline.toString()))
    }

    suspend fun undo(id: Long) = database.withTransaction {
        dao.removeMessages(id)
        dao.remove(id)
    }

    suspend fun publish(equipment: Equipment): Int? = database.withTransaction {
        if (equipment.id == 0) return@withTransaction dao.addPublication(equipment)
        val current = dao.equipmentById(equipment.id) ?: return@withTransaction null
        if (!current.owned) return@withTransaction null
        val conflictsWithBooking = dao.bookingSnapshot().any {
            it.status == confirmedBooking && (it.equipmentId == equipment.id ||
                (it.equipmentId == null && it.equipmentName == current.name)) && overlapsBlock(it, equipment)
        }
        if (conflictsWithBooking) return@withTransaction null
        equipment.id.takeIf { dao.updateEquipment(equipment.copy(owned = true)) == 1 }
    }

    suspend fun undoPublication(id: Int) = dao.removePublication(id)

    suspend fun transition(booking: Booking, next: String): Boolean = database.withTransaction {
        val current = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction false
        if (current.status != booking.status) return@withTransaction false
        val now = LocalDateTime.now(bookingZone)
        when (next) {
            confirmedBooking -> {
                val request = current.copy(status = pendingBooking)
                if (request.phase(now) == BookingPhase.EXPIRED) return@withTransaction false
                val machine = current.equipmentId?.let { dao.equipmentById(it) }
                if (hasConflict(current, machine)) return@withTransaction false
            }
            cancelledBooking -> if (!current.phase(now).canCancel()) return@withTransaction false
            rejectedBooking -> if (current.phase(now) !in listOf(BookingPhase.PENDING, BookingPhase.EXPIRING)) return@withTransaction false
            finishedBooking -> if (current.phase(now) != BookingPhase.IN_PROGRESS) return@withTransaction false
            pendingBooking -> if (current.copy(status = pendingBooking).phase(now) == BookingPhase.EXPIRED) return@withTransaction false
            else -> return@withTransaction false
        }
        val cancelling = next == cancelledBooking
        val refund = if (cancelling && (current.paidAmount ?: 0L) > (current.cancellationFee ?: 0L)) "REQUESTED" else null
        dao.transitionBooking(current.id, current.status, next, now.toString().takeIf { cancelling }, refund) == 1
    }

    suspend fun pay(booking: Booking): Boolean = database.withTransaction {
        val current = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction false
        if (current.phase(LocalDateTime.now(bookingZone)) != BookingPhase.CONFIRMED) return@withTransaction false
        dao.pay(current.id) == 1
    }

    suspend fun advanceRefund(booking: Booking): Boolean {
        val current = booking.refundState ?: "REQUESTED"
        val next = when (current) {
            "REQUESTED" -> "REVIEW"
            "REVIEW" -> "PAID"
            else -> return false
        }
        return dao.advanceRefund(booking.id, current, next) == 1
    }

    fun messages(bookingId: Long) = dao.messages(bookingId)

    suspend fun sendMessage(booking: Booking, text: String, fromProvider: Boolean) = database.withTransaction {
        if (dao.bookingSnapshot().none { it.id == booking.id }) return@withTransaction
        dao.addMessage(BookingMessage(bookingId = booking.id, text = text.trim(), fromProvider = fromProvider))
    }

    private suspend fun hasConflict(booking: Booking, machine: Equipment?): Boolean {
        if (machine != null && overlapsBlock(booking, machine)) return true
        val start = booking.startTime() ?: return true
        val end = booking.endTime() ?: return true
        return dao.bookingSnapshot().any { existing ->
            if (existing.id == booking.id || existing.status != confirmedBooking) return@any false
            val sameMachine = if (booking.equipmentId != null && existing.equipmentId != null) {
                booking.equipmentId == existing.equipmentId
            } else booking.equipmentName == existing.equipmentName
            val existingStart = existing.startTime() ?: return@any false
            val existingEnd = existing.endTime() ?: return@any false
            sameMachine && start < existingEnd && end > existingStart
        }
    }

    private fun overlapsBlock(booking: Booking, equipment: Equipment): Boolean {
        val first = equipment.blockedFrom?.let(LocalDate::parse)?.atStartOfDay() ?: return false
        val last = equipment.blockedUntil?.let(LocalDate::parse)?.plusDays(1)?.atStartOfDay() ?: return false
        val start = booking.startTime() ?: return false
        val end = booking.endTime() ?: return false
        return start < last && end > first
    }
}
