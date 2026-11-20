package pe.rentamaq.feature.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.rentamaq.R
import pe.rentamaq.core.data.*
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.EquipmentPhoto
import pe.rentamaq.feature.explore.CatalogViewModel
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
    model: CatalogViewModel,
    accountId: String,
    requestedEquipmentId: Int?,
    onClearEquipment: () -> Unit,
    accountName: (String) -> String?,
    onNotice: (Int) -> Unit,
    onExplore: () -> Unit,
    onCancel: (Booking) -> Unit
) {
    var history by rememberSaveable { mutableStateOf(false) }
    var cancelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var contactId by rememberSaveable { mutableStateOf<Long?>(null) }
    var refundId by rememberSaveable { mutableStateOf<Long?>(null) }
    var payId by rememberSaveable { mutableStateOf<Long?>(null) }
    var provider by rememberSaveable { mutableStateOf(requestedEquipmentId != null) }
    var phaseFilter by rememberSaveable { mutableStateOf<BookingPhase?>(null) }
    var refundFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var now by remember { mutableStateOf(LocalDateTime.now(bookingZone)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now(bookingZone)
        }
    }
    val accountBookings = bookings.filter {
        if (provider) it.providerId == accountId && (requestedEquipmentId == null || it.equipmentId == requestedEquipmentId)
        else it.customerId == accountId
    }
    val list = accountBookings.filter {
        val phase = it.phase(now)
        phase.isInHistory() == history && (phaseFilter == null || phaseFilter == phase) &&
            (refundFilter == null || refundFilter == it.refundState)
    }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!provider, {
                    provider = false; phaseFilter = null; refundFilter = null; history = false
                    if (requestedEquipmentId != null) onClearEquipment()
                }, { Text(stringResource(R.string.my_requests)) })
                FilterChip(provider, {
                    provider = true; phaseFilter = null; refundFilter = null; history = false
                }, { Text(stringResource(R.string.received_requests)) })
            }
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(R.string.upcoming_bookings, R.string.booking_history).forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = history == (index == 1),
                        onClick = { history = index == 1; phaseFilter = null; refundFilter = null },
                        shape = SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)
                    ) { Text(stringResource(label)) }
                }
            }
        }
        if (provider) item {
            ProviderSummary(
                machine = equipment.find { it.id == requestedEquipmentId },
                bookings = accountBookings,
                now = now,
                phaseFilter = phaseFilter,
                refundFilter = refundFilter,
                onClearMachine = onClearEquipment,
                onPhase = {
                    phaseFilter = if (phaseFilter == it) null else it
                    refundFilter = null
                    history = it.isInHistory()
                    scope.launch { state.animateScrollToItem(2) }
                },
                onRefund = {
                    refundFilter = if (refundFilter == it) null else it
                    phaseFilter = null
                    history = true
                    scope.launch { state.animateScrollToItem(2) }
                }
            )
        }
        if (provider && (phaseFilter != null || refundFilter != null)) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                val label = phaseFilter?.label() ?: when (refundFilter) {
                    "REQUESTED" -> R.string.refund_review_pending
                    "REVIEW" -> R.string.refund_under_review
                    else -> R.string.refund_paid_plural
                }
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { phaseFilter = null; refundFilter = null }) { Text(stringResource(R.string.clear_filters)) }
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
            val machine = if (booking.equipmentId != null) equipment.find { it.id == booking.equipmentId }
                else equipment.find { it.name == booking.equipmentName }
            BookingRecord(booking, machine, phase, provider,
                contactName = accountName(if (provider) booking.customerId else booking.providerId),
                onContact = { contactId = booking.id }, onCancel = { cancelId = booking.id },
                onRefund = { refundId = booking.id }, onPay = { payId = booking.id },
                onDecision = { next ->
                    model.changeBookingStatus(booking, next, accountId) { ok ->
                        onNotice(if (ok) R.string.booking_status_updated else R.string.booking_update_unavailable)
                    }
                })
        }
    }

    accountBookings.find { it.id == cancelId }?.let { booking ->
        CancelBookingDialog(booking, onDismiss = { cancelId = null }, onConfirm = {
            cancelId = null
            onCancel(booking)
        })
    }
    accountBookings.find { it.id == contactId }?.let { booking ->
        BookingConversation(booking, model, accountId, provider) { contactId = null }
    }
    accountBookings.find { it.id == refundId }?.let { booking ->
        RefundDialog(booking, provider, onAdvance = {
            model.advanceRefund(booking, accountId) { ok ->
                onNotice(if (ok) R.string.booking_status_updated else R.string.booking_update_unavailable)
            }
        }) { refundId = null }
    }
    accountBookings.find { it.id == payId }?.let { booking ->
        BookingActionDialog(R.string.booking_payment, booking.equipmentName, onDismiss = { payId = null }) {
            BookingLine(R.string.estimated_amount, stringResource(R.string.price, booking.total))
            BookingLine(R.string.cancellation_fee, stringResource(R.string.price, minOf(40L, booking.total)))
            Text(stringResource(R.string.refund_window, 5), style = MaterialTheme.typography.bodySmall)
            Button(onClick = {
                payId = null
                model.pay(booking, accountId) { ok -> onNotice(if (ok) R.string.payment_registered else R.string.booking_update_unavailable) }
            }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                Text(stringResource(R.string.confirm_payment))
            }
        }
    }
}

@Composable
private fun BookingRecord(booking: Booking, machine: Equipment?, phase: BookingPhase, provider: Boolean, contactName: String?,
    onContact: () -> Unit, onCancel: () -> Unit, onRefund: () -> Unit, onPay: () -> Unit, onDecision: (String) -> Unit) {
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (machine != null) EquipmentPhoto(machine, Modifier.size(64.dp, 50.dp).clip(MaterialTheme.shapes.small))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(booking.equipmentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (machine != null) Text(machine.zone, style = MaterialTheme.typography.bodySmall, color = theme.onSurfaceVariant)
                }
            }
            HorizontalDivider()
            if (contactName != null) BookingLine(if (provider) R.string.message_customer else R.string.message_provider_role, contactName)
            BookingLine(R.string.start_label, booking.startTime()?.format(displayDate) ?: booking.start)
            BookingLine(R.string.end_label, booking.endTime()?.format(displayDate) ?: booking.end)
            if (accent) {
                booking.deadline()?.let { deadline -> BookingLine(R.string.respond_before, deadline.format(displayDate)) }
            }
            BookingLine(if (booking.paidAmount == null) R.string.estimated_amount else R.string.amount_paid,
                stringResource(R.string.price, booking.paidAmount ?: booking.total))
            if (booking.paidAmount != null && (phase.canCancel() || phase == BookingPhase.CANCELLED)) {
                Surface(color = theme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp)) {
                        val refund = booking.refundableAmount()
                        val title = when {
                            phase == BookingPhase.CANCELLED -> R.string.refund_tracking
                            refund == null -> R.string.refund_terms
                            refund == 0L -> R.string.no_refund_amount
                            else -> R.string.refundable_label
                        }
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (refund != null) BookingLine(R.string.refund_estimate, stringResource(R.string.price, refund))
                        if (refund != null && refund > 0) booking.refundWindowDays?.let {
                            Text(stringResource(R.string.refund_window, it), style = MaterialTheme.typography.bodySmall)
                        }
                        if (phase == BookingPhase.CANCELLED && refund != 0L) {
                            BookingLine(R.string.refund_review_step, stringResource(booking.refundReviewLabel()))
                        }
                        TextButton(onClick = onRefund) {
                            Text(stringResource(if (phase == BookingPhase.CANCELLED) R.string.view_refund_tracking else R.string.view_refund_terms))
                        }
                    }
                }
            }
            if (phase !in listOf(BookingPhase.EXPIRED, BookingPhase.REJECTED)) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick = onContact, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(if (provider) R.string.contact_customer else R.string.contact_provider))
                    }
                    if (!provider && phase.canCancel()) TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel_booking), color = theme.error)
                    }
                }
            }
            if (!provider && phase == BookingPhase.CONFIRMED && booking.paidAmount == null) {
                Button(onClick = onPay, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.booking_payment))
                }
            }
            if (provider && phase in listOf(BookingPhase.PENDING, BookingPhase.EXPIRING)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onDecision(confirmedBooking) }, shape = MaterialTheme.shapes.small) {
                        Text(stringResource(R.string.accept_booking))
                    }
                    TextButton(onClick = { onDecision(rejectedBooking) }) { Text(stringResource(R.string.reject_booking)) }
                }
            }
            if (provider && phase == BookingPhase.IN_PROGRESS) {
                TextButton(onClick = { onDecision(finishedBooking) }) { Text(stringResource(R.string.finish_booking)) }
            }
        }
    }
}

internal fun BookingPhase.label() = when (this) {
    BookingPhase.EXPIRING -> R.string.booking_expiring
    BookingPhase.IN_PROGRESS -> R.string.booking_in_progress
    BookingPhase.CONFIRMED -> R.string.booking_confirmed
    BookingPhase.PENDING -> R.string.booking_pending
    BookingPhase.FINISHED -> R.string.booking_finished
    BookingPhase.EXPIRED -> R.string.booking_expired
    BookingPhase.CANCELLED -> R.string.booking_cancelled
    BookingPhase.REJECTED -> R.string.booking_rejected
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
        if (booking.paidAmount == null) {
            Text(stringResource(R.string.cancel_unpaid), style = MaterialTheme.typography.bodyMedium)
        } else {
            RefundTerms(booking)
        }
        Button(onClick = onDismiss, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.keep_booking))
        }
        TextButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.confirm_cancel_booking), color = MaterialTheme.colorScheme.error)
        }
    }
}

internal fun Booking.refundableAmount(): Long? {
    val paid = paidAmount ?: return null
    val fee = cancellationFee ?: return null
    return (paid - fee).coerceAtLeast(0L)
}

@Composable
private fun RefundTerms(booking: Booking) {
    val paid = booking.paidAmount ?: return
    val fee = booking.cancellationFee
    val refund = booking.refundableAmount()
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BookingLine(R.string.amount_paid, stringResource(R.string.price, paid))
            if (refund != null && fee != null) {
                BookingLine(R.string.cancellation_fee, stringResource(R.string.price, fee))
                HorizontalDivider()
                BookingLine(R.string.refund_estimate, stringResource(R.string.price, refund))
            } else {
                Text(stringResource(R.string.refund_policy_pending), style = MaterialTheme.typography.bodySmall)
            }
            if (refund != null && refund > 0) booking.refundWindowDays?.let {
                Text(stringResource(R.string.refund_window, it), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun Booking.refundReviewLabel() = when (refundState) {
    "PAID" -> R.string.refund_approved
    "REVIEW" -> R.string.refund_under_review
    else -> R.string.refund_review_pending
}

@Composable
private fun RefundDialog(booking: Booking, provider: Boolean, onAdvance: () -> Unit, onDismiss: () -> Unit) {
    val cancelled = booking.status == cancelledBooking
    BookingActionDialog(if (cancelled) R.string.refund_tracking else R.string.refund_terms, booking.equipmentName, onDismiss) {
        RefundTerms(booking)
        val cancelledAt = booking.cancelledAt?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
        if (cancelled && booking.refundableAmount() != 0L) {
            HorizontalDivider()
            BookingLine(R.string.refund_cancelled_step, cancelledAt?.format(displayDate) ?: stringResource(R.string.refund_recorded))
            BookingLine(R.string.refund_review_step, stringResource(booking.refundReviewLabel()))
            BookingLine(R.string.refund_transfer_step, stringResource(if (booking.refundState == "PAID") R.string.refund_paid else R.string.refund_not_started))
            if (provider && booking.refundState != "PAID") {
                TextButton(onClick = onAdvance) {
                    Text(stringResource(if (booking.refundState == "REVIEW") R.string.mark_refunded else R.string.begin_refund_review))
                }
            }
        } else if (!cancelled) {
            Text(stringResource(R.string.refund_process_explanation), style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.close_dialog))
        }
    }
}

@Composable
private fun BookingConversation(booking: Booking, model: CatalogViewModel, accountId: String, provider: Boolean, onDismiss: () -> Unit) {
    var message by rememberSaveable(booking.id) { mutableStateOf("") }
    val thread = remember(booking.id) { model.messages(booking.id) }
    val messages by thread.collectAsState(initial = emptyList())
    BookingActionDialog(R.string.conversation, booking.equipmentName, onDismiss) {
        if (messages.isEmpty()) Text(stringResource(R.string.empty_conversation), style = MaterialTheme.typography.bodyMedium)
        val threadScroll = rememberScrollState()
        LaunchedEffect(messages.size) { threadScroll.scrollTo(threadScroll.maxValue) }
        Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(threadScroll),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            messages.forEach { entry ->
                val own = entry.fromProvider == provider
                Surface(color = if (own) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small) {
                    Column(Modifier.fillMaxWidth().padding(10.dp)) {
                        Text(stringResource(if (own) R.string.message_you else if (provider) R.string.message_customer else R.string.message_provider_role),
                            style = MaterialTheme.typography.labelSmall)
                        Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        OutlinedTextField(message, { if (it.length <= 500) message = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.write_message)) }, maxLines = 3)
        Button(onClick = { model.sendMessage(booking, message, accountId); message = "" },
            enabled = message.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
            Text(stringResource(R.string.send_message))
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_dialog)) }
    }
}
@Composable
private fun BookingActionDialog(title: Int, subtitle: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.titleSmall)
                content()
            }
        }
    }
}
