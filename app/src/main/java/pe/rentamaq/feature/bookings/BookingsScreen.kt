package pe.rentamaq.feature.bookings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import pe.rentamaq.R
import pe.rentamaq.core.data.*
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.EquipmentPhoto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDate = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.forLanguageTag("es-PE"))

@Composable
fun BookingsScreen(
    bookings: List<Booking>,
    equipment: List<Equipment>,
    state: LazyListState,
    modifier: Modifier,
    onExplore: () -> Unit,
    onCancel: (Booking) -> Unit
) {
    var history by rememberSaveable { mutableStateOf(false) }
    var cancelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var contactId by rememberSaveable { mutableStateOf<Long?>(null) }
    var now by remember { mutableStateOf(LocalDateTime.now(bookingZone)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now(bookingZone)
        }
    }
    val list = bookings.filter { it.phase(now).isInHistory() == history }
    val ordered = if (history) list.sortedByDescending { it.endTime() } else {
        list.sortedWith(compareBy<Booking> { it.phase(now).ordinal }.thenBy { it.deadline() })
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(stringResource(R.string.bookings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.bookings_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.upcoming_bookings, R.string.booking_history).forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = history == (index == 1),
                        onClick = { history = index == 1 },
                        shape = SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)
                    ) { Text(stringResource(label)) }
                }
            }
        }
        if (ordered.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(if (history) R.string.empty_booking_history else R.string.empty_active_bookings))
                if (!history) Button(onClick = onExplore, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.browse_equipment))
                }
            }
        }
        items(ordered, key = { it.id }) { booking ->
            val phase = booking.phase(now)
            BookingRecord(booking, equipment.find { it.name == booking.equipmentName }, phase,
                onContact = { contactId = booking.id }, onCancel = { cancelId = booking.id })
        }
    }

    bookings.find { it.id == cancelId }?.let { booking ->
        CancelBookingDialog(booking, onDismiss = { cancelId = null }, onConfirm = {
            cancelId = null
            onCancel(booking)
        })
    }
    bookings.find { it.id == contactId }?.let { booking ->
        BookingContactDialog(booking) { contactId = null }
    }
}

@Composable
private fun BookingRecord(booking: Booking, machine: Equipment?, phase: BookingPhase, onContact: () -> Unit, onCancel: () -> Unit) {
    val theme = MaterialTheme.colorScheme
    val accent = phase == BookingPhase.EXPIRING
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = theme.surface,
        border = BorderStroke(1.dp, if (accent) theme.secondaryContainer else theme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Surface(color = if (accent) theme.secondaryContainer else theme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Text(stringResource(phase.label()), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                if (booking.id < 0) Text(stringResource(R.string.demo_booking), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (machine != null) EquipmentPhoto(machine, Modifier.size(64.dp, 50.dp).clip(MaterialTheme.shapes.small))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(booking.equipmentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (machine != null) Text(machine.zone, style = MaterialTheme.typography.bodySmall, color = theme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            BookingLine(R.string.start_label, booking.startTime()?.format(displayDate) ?: booking.start)
            BookingLine(R.string.end_label, booking.endTime()?.format(displayDate) ?: booking.end)
            if (accent) {
                booking.deadline()?.let { deadline -> BookingLine(R.string.respond_before, deadline.format(displayDate)) }
            }
            BookingLine(if (booking.paidAmount == null) R.string.estimated_amount else R.string.amount_paid,
                stringResource(R.string.price, booking.paidAmount ?: booking.total))
            if (!phase.isInHistory() || (phase == BookingPhase.FINISHED && booking.providerPhone != null)) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onContact, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.contact_provider))
                    }
                    if (phase.canCancel()) TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel_booking), color = theme.error)
                    }
                }
            }
        }
    }
}

private fun BookingPhase.label() = when (this) {
    BookingPhase.EXPIRING -> R.string.booking_expiring
    BookingPhase.IN_PROGRESS -> R.string.booking_in_progress
    BookingPhase.CONFIRMED -> R.string.booking_confirmed
    BookingPhase.PENDING -> R.string.booking_pending
    BookingPhase.FINISHED -> R.string.booking_finished
    BookingPhase.EXPIRED -> R.string.booking_expired
    BookingPhase.CANCELLED -> R.string.booking_cancelled
}

@Composable
private fun BookingLine(label: Int, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CancelBookingDialog(booking: Booking, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    BookingActionDialog(R.string.cancel_booking_title, booking.equipmentName, onDismiss) {
        val paid = booking.paidAmount
        val fee = booking.cancellationFee
        val window = booking.refundWindowDays
        if (paid == null) {
            Text(stringResource(R.string.cancel_unpaid), style = MaterialTheme.typography.bodyMedium)
        } else if (fee != null && window != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BookingLine(R.string.amount_paid, stringResource(R.string.price, paid))
                    BookingLine(R.string.cancellation_fee, stringResource(R.string.price, fee))
                    HorizontalDivider()
                    BookingLine(R.string.refund_estimate, stringResource(R.string.price, paid - fee))
                    Text(stringResource(R.string.refund_window, window), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (booking.id < 0) Text(stringResource(R.string.demo_refund), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.refund_policy_pending), style = MaterialTheme.typography.bodyMedium)
        }
        Button(onClick = onDismiss, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.keep_booking))
        }
        TextButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.confirm_cancel_booking), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun BookingContactDialog(booking: Booking, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val message = stringResource(R.string.booking_contact_message, booking.equipmentName, booking.start)
    var launchFailed by remember { mutableStateOf(false) }
    val phone = booking.providerPhone?.takeIf { booking.status == confirmedBooking }
    BookingActionDialog(R.string.contact_provider, booking.equipmentName, onDismiss) {
        if (phone == null) {
            Text(stringResource(R.string.contact_missing), style = MaterialTheme.typography.bodyMedium)
        } else {
            Button(
                onClick = {
                    launchFailed = runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))) }.isFailure
                },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.call_provider)) }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Text(message, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = {
                val intent = if (phone == null) {
                    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message), null)
                } else {
                    Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", phone, null)).putExtra("sms_body", message)
                }
                launchFailed = runCatching { context.startActivity(intent) }.isFailure
            },
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small
        ) { Text(stringResource(if (phone == null) R.string.share_contact_message else R.string.message_provider)) }
        if (launchFailed) Text(stringResource(R.string.contact_app_unavailable), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_dialog)) }
    }
}

@Composable
private fun BookingActionDialog(title: Int, subtitle: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.titleSmall)
                content()
            }
        }
    }
}
