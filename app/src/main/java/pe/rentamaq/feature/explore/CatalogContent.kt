package pe.rentamaq.feature.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
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
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
            singleLine = true,
            shape = MaterialTheme.shapes.small
        )
        FilledTonalButton(
            onClick = { open = true },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.heightIn(min = 56.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Icon(painterResource(R.drawable.ic_filters), null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (filterCount == 0) R.string.filters else R.string.active_filters, filterCount))
        }
    }
    if (open) {
        var draftType by rememberSaveable { mutableStateOf(type) }
        var draftZone by rememberSaveable { mutableStateOf(zone) }
        ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.equipment_type), style = MaterialTheme.typography.titleSmall)
                    Column(Modifier.selectableGroup().padding(top = 6.dp)) {
                        (listOf(null) + EquipmentType.entries).forEach { option ->
                            FilterOption(stringResource(option?.label ?: R.string.all_equipment), draftType == option) {
                                draftType = option
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.area), style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth().selectableGroup().padding(top = 6.dp)) {
                        (listOf(null) + zones).forEach { option ->
                            FilterOption(option ?: stringResource(R.string.all_zones), draftZone == option, Modifier.weight(1f), showMark = false) {
                                draftZone = option
                            }
                        }
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
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) { Text(stringResource(R.string.apply_filters)) }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showMark: Boolean = true,
    onSelect: () -> Unit
) {
    Column(modifier.selectable(selected, role = Role.RadioButton, onClick = onSelect)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            if (selected && showMark) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
            }
        }
        HorizontalDivider()
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
                    FeaturedEquipment(machine, onOpen = { onOpen(machine.id) })
                }
            }
        }
    }
}

@Composable
private fun CompactEquipmentCard(machine: Equipment, onOpen: () -> Unit) {
    Column(Modifier.width(164.dp).clickable(role = Role.Button, onClick = onOpen)) {
        EquipmentPhoto(machine, Modifier.height(110.dp).clip(MaterialTheme.shapes.small))
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(machine.name, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2)
            Text(stringResource(R.string.zone_label, machine.zone), style = MaterialTheme.typography.labelMedium)
            RateLabel(machine.hourlyRate, R.string.per_hour)
        }
        HorizontalDivider()
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
private fun FeaturedEquipment(machine: Equipment, onOpen: () -> Unit) {
    Column(Modifier.width(282.dp).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button, onClick = onOpen)) {
        EquipmentPhoto(machine, Modifier.height(152.dp))
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(stringResource(R.string.zone_label, machine.zone), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondaryContainer)
            Text(machine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                minLines = 2, maxLines = 2, color = MaterialTheme.colorScheme.onPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.rate, machine.hourlyRate, machine.dailyRate),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f))
                Icon(painterResource(R.drawable.ic_arrow), null, tint = MaterialTheme.colorScheme.secondaryContainer)
            }
        }
    }
}

@Composable
fun EquipmentResult(machine: Equipment, onOpen: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            EquipmentPhoto(machine, Modifier.size(width = 96.dp, height = 84.dp).clip(MaterialTheme.shapes.small))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.zone_label, machine.zone), style = MaterialTheme.typography.labelMedium)
                Text(machine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.rate, machine.hourlyRate, machine.dailyRate), style = MaterialTheme.typography.bodySmall)
            }
            Icon(painterResource(R.drawable.ic_arrow), null)
        }
        HorizontalDivider()
    }
}

@Composable
private fun RateLabel(amount: Int, unit: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.price, amount), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(unit), style = MaterialTheme.typography.labelMedium)
    }
}
