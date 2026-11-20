package pe.rentamaq.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.rentamaq.core.data.CatalogRepository
import pe.rentamaq.core.data.local.Booking

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
}
