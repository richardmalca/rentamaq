package pe.rentamaq.core.data

import pe.rentamaq.core.data.local.Booking
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val bookingZone: ZoneId = ZoneId.of("America/Lima")
val bookingDateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

const val pendingBooking = "PENDING"
const val confirmedBooking = "CONFIRMED"
const val cancelledBooking = "CANCELLED"
const val rejectedBooking = "REJECTED"
const val finishedBooking = "FINISHED"

enum class BookingPhase {
    EXPIRING, IN_PROGRESS, CONFIRMED, PENDING, FINISHED, EXPIRED, CANCELLED, REJECTED
}

fun Booking.startTime(): LocalDateTime? =
    runCatching { LocalDateTime.parse(start, bookingDateTimeFormat) }.getOrNull()

fun Booking.endTime(): LocalDateTime? =
    runCatching { LocalDateTime.parse(end, bookingDateTimeFormat) }.getOrNull()

fun Booking.deadline(): LocalDateTime? =
    expiresAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() } ?: startTime()

fun Booking.phase(now: LocalDateTime): BookingPhase {
    val start = startTime()
    val end = endTime()
    val deadline = deadline()
    return when {
        status == cancelledBooking -> BookingPhase.CANCELLED
        status == rejectedBooking -> BookingPhase.REJECTED
        status == finishedBooking -> BookingPhase.FINISHED
        status == confirmedBooking && end != null && now >= end -> BookingPhase.FINISHED
        status == confirmedBooking && start != null && now >= start -> BookingPhase.IN_PROGRESS
        status == confirmedBooking -> BookingPhase.CONFIRMED
        deadline != null && now >= deadline -> BookingPhase.EXPIRED
        deadline != null && now.plusHours(2) >= deadline -> BookingPhase.EXPIRING
        else -> BookingPhase.PENDING
    }
}

fun BookingPhase.isInHistory() =
    this == BookingPhase.FINISHED || this == BookingPhase.EXPIRED || this == BookingPhase.CANCELLED || this == BookingPhase.REJECTED

fun BookingPhase.canCancel() =
    this == BookingPhase.PENDING || this == BookingPhase.EXPIRING || this == BookingPhase.CONFIRMED

internal fun demoBookings(): List<Booking> {
    val now = LocalDateTime.now(bookingZone).withSecond(0).withNano(0)
    val tomorrow = now.toLocalDate().plusDays(1).atTime(8, 0)
    return listOf(
        Booking(-1, "Excavadora CAT 320", now.plusHours(3).format(bookingDateTimeFormat),
            now.plusHours(5).format(bookingDateTimeFormat), 320, expiresAt = now.plusMinutes(45).toString()),
        Booking(-2, "Retroexcavadora JCB 3CX", tomorrow.format(bookingDateTimeFormat),
            tomorrow.plusHours(4).format(bookingDateTimeFormat), 460, confirmedBooking,
            paidAmount = 460, cancellationFee = 40, refundWindowDays = 5),
        Booking(-3, "Rodillo Bomag BW 211", tomorrow.plusDays(3).format(bookingDateTimeFormat),
            tomorrow.plusDays(4).format(bookingDateTimeFormat), 980),
        Booking(-4, "Minicargador Bobcat S650", tomorrow.minusDays(4).format(bookingDateTimeFormat),
            tomorrow.minusDays(4).plusHours(3).format(bookingDateTimeFormat), 285, confirmedBooking),
        Booking(-5, "Motoniveladora CAT 140M", tomorrow.minusDays(2).format(bookingDateTimeFormat),
            tomorrow.minusDays(2).plusHours(3).format(bookingDateTimeFormat), 585),
        Booking(-6, "Cargador frontal CAT 950K", tomorrow.minusDays(5).format(bookingDateTimeFormat),
            tomorrow.minusDays(5).plusHours(2).format(bookingDateTimeFormat), 350, cancelledBooking),
        Booking(-7, "Excavadora Komatsu PC200", now.minusHours(1).format(bookingDateTimeFormat),
            now.plusHours(2).format(bookingDateTimeFormat), 465, confirmedBooking),
        Booking(-8, "Motoniveladora CAT 120K", tomorrow.minusDays(1).format(bookingDateTimeFormat),
            tomorrow.minusDays(1).plusHours(3).format(bookingDateTimeFormat), 540, cancelledBooking,
            paidAmount = 540, cancellationFee = 40, refundWindowDays = 5,
            cancelledAt = now.minusDays(1).toString(), refundState = "REQUESTED")
    )
}
