package pe.rentamaq.feature.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.EquipmentPhoto
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lima = ZoneId.of("America/Lima")
private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
private val bookingFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingWizard(machine: Equipment, onClose: () -> Unit, onSave: (Booking, (Boolean) -> Unit) -> Unit) {
    val defaultStart = remember { LocalDateTime.now(lima).toLocalDate().plusDays(1).atTime(8, 0) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var daily by rememberSaveable { mutableStateOf(false) }
    var startValue by rememberSaveable { mutableStateOf(defaultStart.toString()) }
    var endValue by rememberSaveable { mutableStateOf(defaultStart.plusHours(2).toString()) }
    var days by rememberSaveable { mutableIntStateOf(1) }
    var invalid by rememberSaveable { mutableStateOf(false) }
    var abandon by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf(false) }

    val start = LocalDateTime.parse(startValue)
    val end = if (daily) start.plusDays(days.toLong()) else LocalDateTime.parse(endValue)
    val units = if (daily) days.toLong() else (Duration.between(start, end).toMinutes() + 59) / 60
    val unitPrice = if (daily) machine.dailyRate else machine.hourlyRate
    val total = units * unitPrice
    val back = {
        if (!saving) {
            if (step > 0) step-- else abandon = true
        }
    }

    fun validSchedule(): Boolean {
        invalid = start <= LocalDateTime.now(lima) || end <= start
        if (invalid) step = 1
        return !invalid
    }

    BackHandler(onBack = back)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reserve), style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = back, enabled = !saving) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    HorizontalDivider()
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step == 1 && start < end) {
                            Text(stringResource(R.string.total, total), style = MaterialTheme.typography.titleSmall)
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = MaterialTheme.shapes.small,
                            enabled = !saving,
                            onClick = {
                                when (step) {
                                    0 -> step = 1
                                    1 -> if (validSchedule()) step = 2
                                    2 -> if (validSchedule()) {
                                        saving = true
                                        saveError = false
                                        onSave(Booking(equipmentName = machine.name, start = start.format(bookingFormat),
                                            end = end.format(bookingFormat), total = total)) { success ->
                                            saving = false
                                            saveError = !success
                                        }
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(listOf(R.string.choose_dates, R.string.review_booking, R.string.confirm_request)[step]), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(painterResource(if (step == 2) R.drawable.ic_check else R.drawable.ic_arrow), null, Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        val scroll = key(step) { rememberScrollState() }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BookingProgress(step)
            val heading = listOf(R.string.rental_mode, R.string.booking_schedule, R.string.booking_summary)[step]
            Text(stringResource(heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            when (step) {
                0 -> {
                    BookingMachine(machine, expanded = true)
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RentalOption(R.string.hourly, R.string.hourly_help, machine.hourlyRate, !daily) { daily = false }
                        RentalOption(R.string.daily, R.string.daily_help, machine.dailyRate, daily) { daily = true }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.billing_rule), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(if (daily) R.string.daily_rule else R.string.hourly_rule),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                1 -> {
                    BookingMachine(machine)
                    Text(stringResource(R.string.schedule_help), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DateTimeFields(R.string.start_label, start) { next ->
                        startValue = next.toString()
                        if (next >= LocalDateTime.parse(endValue)) endValue = next.plusHours(2).toString()
                        invalid = false
                    }
                    if (daily) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.rental_days), style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { days-- }, enabled = days > 1, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.remove_day)) }
                                    Text(pluralStringResource(R.plurals.day_count, days, days), Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    OutlinedButton(onClick = { days++ }, enabled = days < 365, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.add_day)) }
                                }
                                Text(stringResource(R.string.end_preview, end.format(bookingFormat)), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        DateTimeFields(R.string.end_label, end) { endValue = it.toString(); invalid = false }
                    }
                    if (invalid) Text(stringResource(R.string.invalid_dates), color = MaterialTheme.colorScheme.error)
                }
                2 -> {
                    BookingMachine(machine)
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SummaryLine(R.string.start_label, start.format(bookingFormat), Modifier.weight(1f))
                                SummaryLine(R.string.end_label, end.format(bookingFormat), Modifier.weight(1f))
                            }
                            HorizontalDivider()
                            SummaryLine(R.string.rental_mode, stringResource(if (daily) R.string.daily else R.string.hourly))
                        }
                    }
                    val count = units.toInt()
                    val billed = pluralStringResource(if (daily) R.plurals.day_count else R.plurals.hour_count, count, count)
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SummaryLine(R.string.billing, stringResource(R.string.unit_calculation, billed, unitPrice))
                            Text(stringResource(R.string.total, total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(stringResource(R.string.local_request_notice), style = MaterialTheme.typography.bodySmall)
                    if (saveError) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (abandon) {
        AlertDialog(
            onDismissRequest = { abandon = false },
            title = { Text(stringResource(R.string.exit_request)) },
            confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.leave)) } },
            dismissButton = { TextButton(onClick = { abandon = false }) { Text(stringResource(R.string.keep_editing)) } }
        )
    }
}

@Composable
private fun SummaryLine(label: Int, value: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BookingProgress(step: Int) {
    val labels = listOf(R.string.step_mode, R.string.step_schedule, R.string.step_review)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(
                    when {
                        index == step -> MaterialTheme.colorScheme.secondaryContainer
                        index < step -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                ))
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == step) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun BookingMachine(machine: Equipment, expanded: Boolean = false) {
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EquipmentPhoto(machine, Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium))
            MachineLabel(machine)
        }
    } else {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            EquipmentPhoto(machine, Modifier.size(width = 68.dp, height = 52.dp).clip(MaterialTheme.shapes.small))
            MachineLabel(machine)
        }
    }
}

@Composable
private fun MachineLabel(machine: Equipment) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(machine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.sector_name, machine.zone), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RentalOption(label: Int, description: Int, rate: Int, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onSelect).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(selected, onClick = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.price, rate), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DateTimeFields(label: Int, dateTime: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        onChange(LocalDateTime.of(year, month + 1, day, dateTime.hour, dateTime.minute))
                    }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show()
                },
                modifier = Modifier.weight(1f).heightIn(min = 62.dp),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { PickerValue(R.string.booking_date, dateTime.format(dateFormat)) }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(context, { _, hour, minute -> onChange(dateTime.withHour(hour).withMinute(minute)) },
                        dateTime.hour, dateTime.minute, true).show()
                },
                modifier = Modifier.weight(1f).heightIn(min = 62.dp),
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) { PickerValue(R.string.booking_time, dateTime.format(timeFormat)) }
        }
    }
}

@Composable
private fun PickerValue(label: Int, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}
