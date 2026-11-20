package pe.rentamaq.feature.account

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.core.data.local.EquipmentBlock
import pe.rentamaq.core.data.local.ServiceRecord
import pe.rentamaq.feature.equipment.ServiceDocumentActions
import pe.rentamaq.feature.explore.CatalogViewModel

@Composable
fun EquipmentManagement(machine: Equipment, blocks: List<EquipmentBlock>, records: List<ServiceRecord>,
    model: CatalogViewModel, accountId: String) {
    var maintenance by rememberSaveable { mutableStateOf(false) }
    Text(machine.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(machine.zone, style = MaterialTheme.typography.bodyMedium)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(R.string.availability, R.string.maintenance).forEachIndexed { index, label ->
            SegmentedButton(maintenance == (index == 1), { maintenance = index == 1 },
                SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)) { Text(stringResource(label)) }
        }
    }
    if (maintenance) {
        MaintenanceManager(machine, records) { record, finished -> model.saveService(record, accountId, finished) }
    } else {
        AvailabilityManager(machine, blocks,
            onAdd = { block, finished -> model.addBlock(block, accountId, finished) },
            onRemove = { block, finished -> model.removeBlock(block, accountId, finished) })
    }
}

@Composable
private fun AvailabilityManager(machine: Equipment, blocks: List<EquipmentBlock>,
    onAdd: (EquipmentBlock, (Boolean) -> Unit) -> Unit, onRemove: (EquipmentBlock, (Boolean) -> Unit) -> Unit) {
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var removing by rememberSaveable { mutableStateOf<Long?>(null) }
    var saving by remember { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    Text(stringResource(R.string.block_dates_help))
    if (blocks.isEmpty()) Text(stringResource(R.string.no_blocks), style = MaterialTheme.typography.bodySmall)
    blocks.forEach { block ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.unavailable_dates, block.firstDay, block.lastDay), Modifier.weight(1f))
            TextButton(onClick = { removing = block.id }) { Text(stringResource(R.string.clear_block)) }
        }
        HorizontalDivider()
    }
    PublicationDate(R.string.block_start, first) { first = it; if (last < it) last = it }
    PublicationDate(R.string.block_end, last) { last = it }
    Button(
        onClick = {
            saving = true
            onAdd(EquipmentBlock(equipmentId = machine.id, firstDay = first, lastDay = last)) { success ->
                saving = false
                notice = if (success) R.string.block_saved else R.string.block_conflict
                if (success) { first = ""; last = "" }
            }
        },
        enabled = !saving && first.isNotBlank() && last.isNotBlank() && first <= last,
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.save_block)) }
    notice?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    blocks.find { it.id == removing }?.let { block ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.remove_block_title)) },
            text = { Text(stringResource(R.string.unavailable_dates, block.firstDay, block.lastDay)) },
            confirmButton = { TextButton(onClick = {
                removing = null
                onRemove(block) { success -> notice = if (success) R.string.block_removed else R.string.booking_update_unavailable }
            }) { Text(stringResource(R.string.clear_block)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } },
            containerColor = MaterialTheme.colorScheme.background
        )
    }
}

@Composable
private fun MaintenanceManager(machine: Equipment, records: List<ServiceRecord>, onSave: (ServiceRecord, (Boolean) -> Unit) -> Unit) {
    var editor by rememberSaveable { mutableStateOf<Long?>(null) }
    var notice by rememberSaveable { mutableStateOf<Int?>(null) }
    if (records.isEmpty()) Text(stringResource(R.string.no_maintenance), style = MaterialTheme.typography.bodySmall)
    records.forEach { record ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.maintenance_metadata, record.date, record.hours), fontWeight = FontWeight.Bold)
                Text(record.notes, style = MaterialTheme.typography.bodyMedium)
                record.responsible?.let { Text(stringResource(R.string.service_provider_summary, it), style = MaterialTheme.typography.bodySmall) }
                record.nextReview?.let { Text(stringResource(R.string.next_service_summary, it), style = MaterialTheme.typography.bodySmall) }
                FlowRow {
                    TextButton(onClick = { editor = record.id }) { Text(stringResource(R.string.edit_service)) }
                    ServiceDocumentActions(machine, record)
                }
            }
        }
    }
    TextButton(onClick = { editor = 0L }) { Text(stringResource(R.string.add_service)) }
    notice?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    editor?.let { id ->
        key(id) {
            ServiceForm(machine, records.find { it.id == id }) { record, finished ->
                onSave(record) { success ->
                    finished()
                    notice = if (success) R.string.service_saved else R.string.booking_update_unavailable
                    if (success) editor = null
                }
            }
            TextButton(onClick = { editor = null }) { Text(stringResource(R.string.cancel_action)) }
        }
    }
}

@Composable
private fun ServiceForm(machine: Equipment, original: ServiceRecord?, onSave: (ServiceRecord, () -> Unit) -> Unit) {
    var date by rememberSaveable { mutableStateOf(original?.date.orEmpty()) }
    var hours by rememberSaveable { mutableStateOf(original?.hours?.toString().orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(original?.notes.orEmpty()) }
    var responsible by rememberSaveable { mutableStateOf(original?.responsible.orEmpty()) }
    var nextReview by rememberSaveable { mutableStateOf(original?.nextReview.orEmpty()) }
    var document by rememberSaveable { mutableStateOf(original?.document.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    val resolver = LocalContext.current.contentResolver
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null && runCatching {
                resolver.takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.isSuccess) document = selected.toString()
    }
    PublicationDate(R.string.service_date, date) { date = it }
    OutlinedTextField(hours, { if (it.length <= 8 && it.all { c -> c in '0'..'9' }) hours = it },
        Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.service_hours)) },
        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    OutlinedTextField(notes, { if (it.length <= 350) notes = it }, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.service_notes)) }, minLines = 2)
    OutlinedTextField(responsible, { if (it.length <= 120) responsible = it }, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.service_provider)) }, singleLine = true)
    PublicationDate(R.string.next_service, nextReview) { nextReview = it }
    FlowRow {
        OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }, shape = MaterialTheme.shapes.small) {
            Text(stringResource(if (document.isBlank()) R.string.attach_service_document else R.string.replace_service_document))
        }
        if (document.isNotBlank()) TextButton(onClick = { document = "" }) { Text(stringResource(R.string.remove_service_document)) }
    }
    val meter = hours.toIntOrNull()
    Button(
        onClick = {
            val reading = meter ?: return@Button
            saving = true
            onSave(ServiceRecord(original?.id ?: 0L, machine.id, date, reading, notes.trim(),
                responsible.trim().ifBlank { null }, nextReview.ifBlank { null }, document.ifBlank { null })) {
                saving = false
            }
        },
        enabled = !saving && date.isNotBlank() && meter != null && notes.isNotBlank() && (nextReview.isBlank() || nextReview >= date),
        shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()
    ) { Text(stringResource(R.string.save_service)) }
}
