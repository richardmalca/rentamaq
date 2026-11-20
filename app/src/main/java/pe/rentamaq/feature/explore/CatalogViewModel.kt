package pe.rentamaq.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.rentamaq.core.data.CatalogRepository
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment

class CatalogViewModel(private val repository: CatalogRepository) : ViewModel() {
    val equipment = repository.equipment.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookings = repository.bookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.seed() }
    }

    fun reserve(booking: Booking, onSaved: (Long?) -> Unit) {
        viewModelScope.launch {
            val id = try {
                repository.reserve(booking)
            } catch (_: android.database.sqlite.SQLiteException) {
                null
            }
            onSaved(id)
        }
    }

    fun undo(id: Long) {
        viewModelScope.launch { repository.undo(id) }
    }

    fun changeBookingStatus(booking: Booking, next: String, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch {
            val updated = try {
                repository.transition(booking, next)
            } catch (_: android.database.sqlite.SQLiteException) {
                false
            }
            onUpdated(updated)
        }
    }

    fun publish(equipment: Equipment, onSaved: (Int?) -> Unit) {
        viewModelScope.launch {
            val id = try {
                repository.publish(equipment)
            } catch (_: android.database.sqlite.SQLiteException) {
                null
            }
            onSaved(id)
        }
    }

    fun undoPublication(id: Int) {
        viewModelScope.launch { repository.undoPublication(id) }
    }

    fun pay(booking: Booking, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.pay(booking)) }
    }

    fun advanceRefund(booking: Booking, onUpdated: (Boolean) -> Unit) {
        viewModelScope.launch { onUpdated(repository.advanceRefund(booking)) }
    }

    fun messages(bookingId: Long) = repository.messages(bookingId)

    fun sendMessage(booking: Booking, text: String, fromProvider: Boolean) {
        viewModelScope.launch { repository.sendMessage(booking, text, fromProvider) }
    }
}
