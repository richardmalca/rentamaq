package pe.rentamaq.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.rentamaq.core.data.CatalogRepository
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.core.data.local.EquipmentBlock
import pe.rentamaq.core.data.local.ServiceRecord

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {
    val equipment = repository.equipment.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookings = repository.bookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val blocks = repository.blocks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val services = repository.services.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.seed() }
    }

    fun reserve(booking: Booking, accountId: String, onSaved: (Long?) -> Unit) {
        viewModelScope.launch {
            val id = try {
                repository.reserve(booking, accountId)
            } catch (_: android.database.sqlite.SQLiteException) {
                null
            }
            onSaved(id)
        }
    }

    fun undo(id: Long, accountId: String) {
        viewModelScope.launch { repository.undo(id, accountId) }
    }

    fun changeBookingStatus(booking: Booking, next: String, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch {
            val updated = try {
                repository.transition(booking, next, accountId)
            } catch (_: android.database.sqlite.SQLiteException) {
                false
            }
            onUpdated(updated)
        }
    }

    fun publish(equipment: Equipment, accountId: String, onSaved: (Int?) -> Unit) {
        viewModelScope.launch {
            val id = try {
                repository.publish(equipment, accountId)
            } catch (_: android.database.sqlite.SQLiteException) {
                null
            }
            onSaved(id)
        }
    }

    fun undoPublication(id: Int, accountId: String) {
        viewModelScope.launch { repository.undoPublication(id, accountId) }
    }

    fun pay(booking: Booking, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.pay(booking, accountId)) }
    }

    fun advanceRefund(booking: Booking, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.advanceRefund(booking, accountId)) }
    }

    fun messages(bookingId: Long) = repository.messages(bookingId)

    fun sendMessage(booking: Booking, text: String, accountId: String) {
        viewModelScope.launch { repository.sendMessage(booking, text, accountId) }
    }

    fun addBlock(block: EquipmentBlock, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.addBlock(block, accountId)) }
    }

    fun removeBlock(block: EquipmentBlock, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.removeBlock(block, accountId)) }
    }

    fun saveService(record: ServiceRecord, accountId: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.saveService(record, accountId)) }
    }
}
