package pe.rentamaq.feature.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import pe.rentamaq.core.data.local.EquipmentBlock
import pe.rentamaq.core.data.local.ServiceRecord
import pe.rentamaq.feature.explore.type

@Composable
fun EquipmentDetail(machine: Equipment, blocks: List<EquipmentBlock>, records: List<ServiceRecord>, canViewDocuments: Boolean) {
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
            if (records.isNotEmpty()) {
                records.forEach { record ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.maintenance_metadata, record.date, record.hours),
                                style = MaterialTheme.typography.labelLarge)
                            Text(record.notes, style = MaterialTheme.typography.bodyMedium)
                            record.responsible?.let { Text(stringResource(R.string.service_provider_summary, it)) }
                            record.nextReview?.let { Text(stringResource(R.string.next_service_summary, it)) }
                            if (canViewDocuments) FlowRow { ServiceDocumentActions(machine, record) }
                            else if (record.document != null) {
                                Text(stringResource(R.string.service_document_after_booking), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else if (maintenance.isEmpty()) {
                Text(stringResource(R.string.no_maintenance))
            } else {
                maintenance.forEach { entry ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.maintenance_metadata, entry.date, entry.hours), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(entry.service), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if (blocks.isNotEmpty()) {
            DetailSection(R.string.availability) {
                blocks.forEach { block -> Text(stringResource(R.string.unavailable_dates, block.firstDay, block.lastDay)) }
            }
        }
    }
}

@Composable
fun EquipmentLocation(sector: String) {
    DetailSection(R.string.reference_location) {
        Text(stringResource(R.string.sector_name, sector), style = MaterialTheme.typography.labelLarge)
        ReferenceMap()
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
    val uploaded = machine.photoUris()
    val photoCount = uploaded.size.takeIf { it > 0 } ?: photos.size
    val pager = rememberPagerState { photoCount }
    val scope = rememberCoroutineScope()

    Column {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.55f).clip(MaterialTheme.shapes.medium)
        ) { index ->
            val uri = uploaded.getOrNull(index)
            val fallback: @Composable () -> Unit = {
                Image(painterResource(photos.getOrElse(index) { primary }), stringResource(R.string.reference_photo, machine.name),
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (uri == null) fallback() else StoredEquipmentPhoto(uri, machine.name, Modifier.fillMaxSize(), fallback)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            val caption = if (uploaded.isEmpty()) {
                if (photoCount == 1) R.string.photo_label else R.string.gallery_caption
            } else R.string.uploaded_gallery_caption
            Text(stringResource(caption, pager.currentPage + 1, photoCount), style = MaterialTheme.typography.bodySmall)
            if (photoCount > 1) {
                Row {
                    IconButton(
                        enabled = pager.currentPage > 0,
                        onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
                    ) { Icon(painterResource(R.drawable.ic_back), stringResource(R.string.previous_photo)) }
                    IconButton(
                        enabled = pager.currentPage < photoCount - 1,
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
