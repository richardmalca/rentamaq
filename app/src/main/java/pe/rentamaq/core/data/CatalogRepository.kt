package pe.rentamaq.core.data

import pe.rentamaq.core.data.local.*

class CatalogRepository(private val dao: CatalogDao) {
    val equipment = dao.equipment()
    val bookings = dao.bookings()

    suspend fun seed() {
        dao.seed(demoEquipment)
        dao.seedBookings(demoBookings())
    }

    suspend fun reserve(booking: Booking) = dao.insert(booking)
    suspend fun undo(id: Long) = dao.remove(id)

    suspend fun transition(booking: Booking, next: String) =
        dao.transitionBooking(booking.id, booking.status, next) == 1
}
