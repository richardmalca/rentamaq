package pe.rentamaq.feature.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Equipment

enum class EquipmentType(val label: Int, val photo: Int) {
    EXCAVATOR(R.string.excavators, R.drawable.excavator),
    BACKHOE(R.string.backhoes, R.drawable.backhoe),
    ROLLER(R.string.rollers, R.drawable.roller),
    SKID_STEER(R.string.skid_steers, R.drawable.skid_steer)
}

fun Equipment.type(): EquipmentType? = when (id) {
    1 -> EquipmentType.EXCAVATOR
    2 -> EquipmentType.BACKHOE
    3 -> EquipmentType.ROLLER
    4 -> EquipmentType.SKID_STEER
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSearch(
    search: String,
    onSearch: (String) -> Unit,
    type: EquipmentType?,
    zone: String?,
    zones: List<String>,
    onApply: (EquipmentType?, String?) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val filterCount = listOfNotNull(type, zone).size
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.search_equipment)) },
            leadingIcon = { Text("⌕", style = MaterialTheme.typography.headlineSmall) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        FilledTonalButton(
            onClick = { open = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.heightIn(min = 56.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Text(if (filterCount == 0) stringResource(R.string.filters) else stringResource(R.string.active_filters, filterCount))
        }
    }
    if (open) {
        var draftType by rememberSaveable { mutableStateOf(type) }
        var draftZone by rememberSaveable { mutableStateOf(zone) }
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.equipment_type), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(null) + EquipmentType.entries).forEach { option ->
                        FilterChip(draftType == option, { draftType = option }, { Text(stringResource(option?.label ?: R.string.all_equipment)) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.area), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(null) + zones).forEach { option ->
                        FilterChip(draftZone == option, { draftZone = option }, { Text(option ?: stringResource(R.string.all_zones)) })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { draftType = null; draftZone = null }) { Text(stringResource(R.string.clear_filters)) }
                    Button(
                        onClick = {
                            onApply(draftType, draftZone)
                            open = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(R.string.apply_filters)) }
                }
            }
        }
    }
}

@Composable
fun CatalogCarousel(title: Int, machines: List<Equipment>, compact: Boolean, onOpen: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(machines, key = { it.id }) { machine ->
                if (compact) {
                    CompactEquipmentCard(machine, onOpen = { onOpen(machine.id) })
                } else {
                    EquipmentCard(machine, Modifier.width(276.dp), onOpen = { onOpen(machine.id) })
                }
            }
        }
    }
}

@Composable
private fun CompactEquipmentCard(machine: Equipment, onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = Modifier.width(164.dp), shape = RoundedCornerShape(16.dp)) {
        EquipmentPhoto(machine, Modifier.height(106.dp))
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(machine.name, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2)
            Text(stringResource(R.string.zone_label, machine.zone), style = MaterialTheme.typography.labelMedium)
            RateLabel(machine.hourlyRate, R.string.per_hour)
        }
    }
}

@Composable
fun CatalogResults(count: Int, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pluralStringResource(R.plurals.equipment_results, count, count), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filters)) }
    }
}

@Composable
fun EquipmentPhoto(machine: Equipment, modifier: Modifier = Modifier) {
    val photo = machine.type()?.photo ?: return
    Image(
        painter = painterResource(photo),
        contentDescription = stringResource(R.string.reference_photo, machine.name),
        modifier = modifier.fillMaxWidth(),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun EquipmentCard(machine: Equipment, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    OutlinedCard(onClick = onOpen, modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surface
    )) {
        Box {
            EquipmentPhoto(machine, Modifier.height(160.dp))
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(stringResource(R.string.zone_label, machine.zone), Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(machine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RateLabel(machine.hourlyRate, R.string.per_hour)
                RateLabel(machine.dailyRate, R.string.per_day)
            }
        }
    }
}

@Composable
private fun RateLabel(amount: Int, unit: Int) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.price, amount), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(unit), style = MaterialTheme.typography.labelMedium)
    }
}
