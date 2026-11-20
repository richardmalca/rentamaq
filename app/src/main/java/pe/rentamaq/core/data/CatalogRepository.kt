package pe.rentamaq.core.data

import androidx.room.withTransaction
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.BookingMessage
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.core.data.local.EquipmentBlock
import pe.rentamaq.core.data.local.ServiceRecord
import pe.rentamaq.core.data.local.RentamaqDatabase
import java.time.LocalDate
import java.time.LocalDateTime

class CatalogRepository(private val database: RentamaqDatabase) {
    private val dao = database.catalog()
    val equipment = dao.equipment()
    val bookings = dao.bookings()
    val blocks = dao.blocks()
    val services = dao.services()

    suspend fun seed() = database.withTransaction {
        dao.seed(demoEquipment)
        demoEquipment.take(4).forEach { dao.completeLegacyEquipment(it.id, it.category, it.zone) }
        val ids = demoEquipment.associate { it.name to it.id }
        dao.seedBookings(demoBookings().map { it.copy(equipmentId = ids[it.equipmentName]) })
    }

    suspend fun reserve(booking: Booking, accountId: String): Long? = database.withTransaction {
        val equipment = booking.equipmentId?.let { dao.equipmentById(it) } ?: return@withTransaction null
        if (equipment.ownerId == accountId) return@withTransaction null
        val start = booking.startTime() ?: return@withTransaction null
        val end = booking.endTime() ?: return@withTransaction null
        val now = LocalDateTime.now(bookingZone)
        if (start <= now || end <= start) return@withTransaction null
        if (hasConflict(booking, equipment)) return@withTransaction null
        val deadline = minOf(start, now.plusHours(24))
        dao.insert(booking.copy(expiresAt = deadline.toString(), customerId = accountId, providerId = equipment.ownerId))
    }

    suspend fun undo(id: Long, accountId: String) = database.withTransaction {
        if (dao.bookingSnapshot().none { it.id == id && it.customerId == accountId && it.status == pendingBooking }) return@withTransaction
        dao.removeMessages(id)
        dao.remove(id)
    }

    suspend fun publish(equipment: Equipment, accountId: String): Int? = database.withTransaction {
        if (equipment.id == 0) return@withTransaction dao.addPublication(equipment.copy(ownerId = accountId))
        val current = dao.equipmentById(equipment.id) ?: return@withTransaction null
        if (current.ownerId != accountId) return@withTransaction null
        equipment.id.takeIf { dao.updateEquipment(equipment.copy(owned = true, ownerId = accountId)) == 1 }
    }

    suspend fun undoPublication(id: Int, accountId: String) = dao.removePublication(id, accountId)

    suspend fun addBlock(block: EquipmentBlock, accountId: String): Boolean = database.withTransaction {
        if (dao.equipmentById(block.equipmentId)?.ownerId != accountId || block.firstDay > block.lastDay) return@withTransaction false
        if (dao.blockSnapshot(block.equipmentId).any { it.firstDay <= block.lastDay && it.lastDay >= block.firstDay }) return@withTransaction false
        if (dao.bookingSnapshot().any {
                it.equipmentId == block.equipmentId && it.status == confirmedBooking && overlapsBlock(it, block)
            }) return@withTransaction false
        dao.insertBlock(block)
        true
    }

    suspend fun removeBlock(block: EquipmentBlock, accountId: String): Boolean = database.withTransaction {
        if (dao.equipmentById(block.equipmentId)?.ownerId != accountId) return@withTransaction false
        val stored = dao.blockSnapshot(block.equipmentId).find { it.id == block.id } ?: return@withTransaction false
        dao.deleteBlock(stored) == 1
    }

    suspend fun saveService(record: ServiceRecord, accountId: String): Boolean = database.withTransaction {
        if (dao.equipmentById(record.equipmentId)?.ownerId != accountId) return@withTransaction false
        if (record.id != 0L && dao.serviceById(record.id)?.equipmentId != record.equipmentId) return@withTransaction false
        dao.saveService(record)
        true
    }

    suspend fun transition(booking: Booking, next: String, accountId: String): Boolean = database.withTransaction {
        val current = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction false
        if (current.status != booking.status) return@withTransaction false
        val customerAction = next == cancelledBooking || current.status == cancelledBooking
        if (accountId != if (customerAction) current.customerId else current.providerId) return@withTransaction false
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

    suspend fun pay(booking: Booking, accountId: String): Boolean = database.withTransaction {
        val current = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction false
        if (current.customerId != accountId) return@withTransaction false
        if (current.phase(LocalDateTime.now(bookingZone)) != BookingPhase.CONFIRMED) return@withTransaction false
        dao.pay(current.id) == 1
    }

    suspend fun advanceRefund(booking: Booking, accountId: String): Boolean = database.withTransaction {
        val stored = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction false
        if (stored.providerId != accountId) return@withTransaction false
        val current = stored.refundState ?: return@withTransaction false
        val next = when (current) {
            "REQUESTED" -> "REVIEW"
            "REVIEW" -> "PAID"
            else -> return@withTransaction false
        }
        dao.advanceRefund(booking.id, current, next) == 1
    }

    fun messages(bookingId: Long) = dao.messages(bookingId)

    suspend fun sendMessage(booking: Booking, text: String, accountId: String) = database.withTransaction {
        val stored = dao.bookingSnapshot().find { it.id == booking.id } ?: return@withTransaction
        if (accountId != stored.customerId && accountId != stored.providerId) return@withTransaction
        val content = text.trim().take(500)
        if (content.isBlank()) return@withTransaction
        dao.addMessage(BookingMessage(bookingId = booking.id, text = content, fromProvider = accountId == stored.providerId))
    }

    private suspend fun hasConflict(booking: Booking, machine: Equipment?): Boolean {
        if (machine != null && dao.blockSnapshot(machine.id).any { overlapsBlock(booking, it) }) return true
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

    private fun overlapsBlock(booking: Booking, block: EquipmentBlock): Boolean {
        val first = LocalDate.parse(block.firstDay).atStartOfDay()
        val last = LocalDate.parse(block.lastDay).plusDays(1).atStartOfDay()
        val start = booking.startTime() ?: return false
        val end = booking.endTime() ?: return false
        return start < last && end > first
    }
}
