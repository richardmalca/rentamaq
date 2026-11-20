package pe.rentamaq.feature.bookings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
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
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
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
                    ) { Text(stringResource(if (step == 2) R.string.confirm_request else R.string.continue_booking)) }
                }
            }
        }
    ) { padding ->
        val scroll = key(step) { rememberScrollState() }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.booking_progress, step + 1), style = MaterialTheme.typography.labelLarge)
            LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.secondaryContainer)
            val heading = listOf(R.string.rental_mode, R.string.booking_schedule, R.string.booking_summary)[step]
            Text(stringResource(heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(machine.name, style = MaterialTheme.typography.titleMedium)

            when (step) {
                0 -> SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(R.string.hourly, R.string.daily).forEachIndexed { index, label ->
                        SegmentedButton(daily == (index == 1), { daily = index == 1 },
                            SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)) {
                            Text(stringResource(label))
                        }
                    }
                }
                1 -> {
                    DateTimeFields(R.string.start_label, start) { next ->
                        startValue = next.toString()
                        if (next >= LocalDateTime.parse(endValue)) endValue = next.plusHours(2).toString()
                        invalid = false
                    }
                    if (daily) {
                        Text(stringResource(R.string.rental_days), style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { days-- }, enabled = days > 1) { Text(stringResource(R.string.remove_day)) }
                            Text(pluralStringResource(R.plurals.day_count, days, days))
                            OutlinedButton(onClick = { days++ }, enabled = days < 365) { Text(stringResource(R.string.add_day)) }
                        }
                        Text(stringResource(R.string.end_preview, end.format(bookingFormat)), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        DateTimeFields(R.string.end_label, end) { endValue = it.toString(); invalid = false }
                    }
                    if (invalid) Text(stringResource(R.string.invalid_dates), color = MaterialTheme.colorScheme.error)
                }
                2 -> {
                    SummaryLine(R.string.reference_location, stringResource(R.string.sector_name, machine.zone))
                    SummaryLine(R.string.rental_mode, stringResource(if (daily) R.string.daily else R.string.hourly))
                    SummaryLine(R.string.start_label, start.format(bookingFormat))
                    SummaryLine(R.string.end_label, end.format(bookingFormat))
                    val count = units.toInt()
                    val billed = pluralStringResource(if (daily) R.plurals.day_count else R.plurals.hour_count, count, count)
                    SummaryLine(R.string.billing, stringResource(R.string.unit_calculation, billed, unitPrice))
                    Text(stringResource(R.string.total, total),
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(14.dp),
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.local_request_notice), style = MaterialTheme.typography.bodySmall)
                    if (saveError) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                }
            }
            if (step == 0) Text(stringResource(R.string.rate, machine.hourlyRate, machine.dailyRate))
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
private fun SummaryLine(label: Int, value: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun DateTimeFields(label: Int, dateTime: LocalDateTime, onChange: (LocalDateTime) -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        onChange(LocalDateTime.of(year, month + 1, day, dateTime.hour, dateTime.minute))
                    }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) { Text(dateTime.format(dateFormat)) }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(context, { _, hour, minute -> onChange(dateTime.withHour(hour).withMinute(minute)) },
                        dateTime.hour, dateTime.minute, true).show()
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.small
            ) { Text(dateTime.format(timeFormat)) }
        }
    }
}
