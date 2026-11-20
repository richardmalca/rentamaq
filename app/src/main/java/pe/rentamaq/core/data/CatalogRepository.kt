package pe.rentamaq.core.data

import pe.rentamaq.core.data.local.*

class CatalogRepository(private val dao: CatalogDao) {
    val equipment = dao.equipment()
    val bookings = dao.bookings()

    suspend fun seed() = dao.seed(
        listOf(
            Equipment(1, "Excavadora CAT 320", "Excavación y movimiento de tierra. Incluye operador.", "Norte", 160, 1150),
            Equipment(2, "Retroexcavadora JCB 3CX", "Apertura de zanjas y carga de material.", "Centro", 115, 820),
            Equipment(3, "Rodillo Bomag BW 211", "Compactación de suelos y afirmado.", "Sur", 140, 980),
            Equipment(4, "Minicargador Bobcat S650", "Limpieza y nivelación en espacios reducidos.", "Centro", 95, 680)
        )
    )

    suspend fun reserve(booking: Booking) = dao.insert(booking)
    suspend fun undo(id: Long) = dao.remove(id)
}
