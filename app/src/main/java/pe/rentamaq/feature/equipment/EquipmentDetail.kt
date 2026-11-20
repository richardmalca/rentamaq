package pe.rentamaq.feature.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.type

@Composable
fun EquipmentDetail(machine: Equipment) {
    var showMap by rememberSaveable { mutableStateOf(false) }
    val maintenance = when (machine.id) {
        1 -> listOf(
            MaintenanceEntry("12/11/2026", 4820, R.string.excavator_maintenance),
            MaintenanceEntry("03/09/2026", 4540, R.string.excavator_previous_service)
        )
        2 -> listOf(MaintenanceEntry("05/11/2026", 3250, R.string.backhoe_maintenance))
        else -> emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        EquipmentGallery(machine)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(machine.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.sector_name, machine.zone), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
            DetailRate(machine.hourlyRate, R.string.hourly, Modifier.weight(1f))
            DetailRate(machine.dailyRate, R.string.daily, Modifier.weight(1f))
        }
        DetailSection(R.string.about_machine) {
            Text(machine.description, style = MaterialTheme.typography.bodyLarge)
            machine.type()?.let { Text(stringResource(it.label), style = MaterialTheme.typography.labelLarge) }
        }
        DetailSection(R.string.maintenance) {
            if (maintenance.isEmpty()) {
                Text(stringResource(R.string.no_maintenance))
            } else {
                Text(stringResource(R.string.sample_maintenance), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                maintenance.forEach { entry ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.maintenance_metadata, entry.date, entry.hours), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(entry.service), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        DetailSection(R.string.reference_location) {
            Text(stringResource(R.string.sector_name, machine.zone))
            if (showMap) {
                ReferenceMap(machine.zone)
            }
            OutlinedButton(onClick = { showMap = !showMap }, shape = MaterialTheme.shapes.small) {
                Text(stringResource(if (showMap) R.string.hide_zone else R.string.show_zone))
            }
        }
    }
}

private data class MaintenanceEntry(val date: String, val hours: Int, val service: Int)

@Composable
private fun EquipmentGallery(machine: Equipment) {
    val primary = machine.type()?.photo ?: return
    val alternate = when (machine.id) {
        1 -> R.drawable.excavator_side
        2 -> R.drawable.backhoe_side
        else -> null
    }
    val photos = listOfNotNull(primary, alternate)
    val pager = rememberPagerState { photos.size }
    val scope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.55f).clip(MaterialTheme.shapes.medium)
        ) { index ->
            Image(painterResource(photos[index]), stringResource(R.string.reference_photo, machine.name),
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(if (photos.size == 1) R.string.photo_label else R.string.gallery_caption,
                pager.currentPage + 1, photos.size), style = MaterialTheme.typography.bodySmall)
            if (photos.size > 1) {
                Row {
                    IconButton(
                        enabled = pager.currentPage > 0,
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                    ) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.previous_photo)) }
                    IconButton(
                        enabled = pager.currentPage < photos.lastIndex,
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
                    ) { Icon(painterResource(R.drawable.ic_arrow), stringResource(R.string.next_photo)) }
                }
            }
        }
    }
}

@Composable
private fun DetailRate(rate: Int, period: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(period), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.price, rate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Spacer(Modifier.height(2.dp))
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
    }
}
