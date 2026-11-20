package pe.rentamaq.feature.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.EquipmentPhoto
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val lima = ZoneId.of("America/Lima")
private val dateFormat = DateTimeFormatter.ofPattern("EEE d 'de' MMM", Locale.forLanguageTag("es-PE"))
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
                actions = {
                    TextButton(onClick = { abandon = true }, enabled = !saving) {
                        Text(stringResource(R.string.cancel_action), fontWeight = FontWeight.SemiBold)
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
                                            end = end.format(bookingFormat), total = total, equipmentId = machine.id)) { success ->
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
                    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DateTimeFields(R.string.start_label, start) { next ->
                                startValue = next.toString()
                                if (next >= LocalDateTime.parse(endValue)) endValue = next.plusHours(2).toString()
                                invalid = false
                            }
                            HorizontalDivider()
                            if (daily) {
                                Text(stringResource(R.string.rental_days), style = MaterialTheme.typography.titleSmall)
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    OutlinedButton(onClick = { days-- }, enabled = days > 1, shape = MaterialTheme.shapes.small) {
                                        Text(stringResource(R.string.remove_day))
                                    }
                                    Text(pluralStringResource(R.plurals.day_count, days, days), style = MaterialTheme.typography.titleMedium)
                                    OutlinedButton(onClick = { days++ }, enabled = days < 365, shape = MaterialTheme.shapes.small) {
                                        Text(stringResource(R.string.add_day))
                                    }
                                }
                                Text(stringResource(R.string.end_preview, end.format(bookingFormat)), style = MaterialTheme.typography.bodyMedium)
                            } else {
                                DateTimeFields(R.string.end_label, end) { endValue = it.toString(); invalid = false }
                            }
                        }
                    }
                    ArrivalEstimate(start)
                    if (invalid) Text(stringResource(R.string.invalid_dates), color = MaterialTheme.colorScheme.error)
                }
                2 -> {
                    BookingMachine(machine)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.requested_schedule), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { step = 1 }, enabled = !saving) { Text(stringResource(R.string.edit_schedule)) }
                        }
                        ReviewTime(R.string.start_label, start)
                        HorizontalDivider()
                        ReviewTime(R.string.end_label, end)
                    }
                    ArrivalEstimate(start)
                    val count = units.toInt()
                    val billed = pluralStringResource(if (daily) R.plurals.day_count else R.plurals.hour_count, count, count)
                    Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stringResource(R.string.unit_calculation, billed, unitPrice),
                                color = MaterialTheme.colorScheme.secondaryContainer, style = MaterialTheme.typography.labelLarge)
                            Text(stringResource(R.string.total, total), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    if (saveError) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (abandon) {
        CancelRequestDialog(machine, onKeepEditing = { abandon = false }, onCancel = onClose)
    }
}

@Composable
private fun CancelRequestDialog(machine: Equipment, onKeepEditing: () -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onKeepEditing) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.exit_request), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.cancel_request_message), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                BookingMachine(machine)
                Button(
                    onClick = onKeepEditing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(R.string.keep_editing), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.discard_request), color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReviewTime(label: Int, dateTime: LocalDateTime) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
            Text(dateTime.format(dateFormat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(dateTime.format(timeFormat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArrivalEstimate(requestedStart: LocalDateTime) {
    val earliest = requestedStart.plusMinutes(15)
    val latest = requestedStart.plusMinutes(30)
    val format = if (latest.toLocalDate() == requestedStart.toLocalDate()) timeFormat else bookingFormat
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(R.drawable.ic_bookings), null, Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.estimated_arrival), style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.arrival_range, earliest.format(format), latest.format(format)),
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.arrival_explanation), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable(role = Role.Button) {
                    DatePickerDialog(context, { _, year, month, day ->
                        onChange(LocalDateTime.of(year, month + 1, day, dateTime.hour, dateTime.minute))
                    }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show()
                }.heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_bookings), stringResource(R.string.booking_date), Modifier.size(18.dp))
                Text(dateTime.format(dateFormat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = {
                    TimePickerDialog(context, { _, hour, minute -> onChange(dateTime.withHour(hour).withMinute(minute)) },
                        dateTime.hour, dateTime.minute, true).show()
                },
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) { Text(dateTime.format(timeFormat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        }
    }
}
