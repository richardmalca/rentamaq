package pe.rentamaq.feature.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.BookingPhase
import pe.rentamaq.core.data.phase
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import java.time.LocalDateTime

@Composable
fun ProviderSummary(
    machine: Equipment?,
    bookings: List<Booking>,
    now: LocalDateTime,
    phaseFilter: BookingPhase?,
    refundFilter: String?,
    onClearMachine: () -> Unit,
    onPhase: (BookingPhase) -> Unit,
    onRefund: (String) -> Unit
) {
    var expanded by rememberSaveable(machine?.id) { mutableStateOf(machine != null) }
    val counts = bookings.groupingBy { it.phase(now) }.eachCount()
    val refunds = bookings.filter { it.refundState != null }.groupBy { it.refundState }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (machine != null) {
                    Text(machine.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearMachine) { Text(stringResource(R.string.all_my_machines)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCount(R.string.request_count, bookings.size, Modifier.weight(1f))
                    SummaryCount(R.string.customer_count, bookings.distinctBy { it.customerId }.size, Modifier.weight(1f))
                }
            }
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(stringResource(if (expanded) R.string.hide_booking_breakdown else R.string.show_booking_breakdown))
        }
        if (expanded) {
            Text(stringResource(R.string.reservation_statuses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            BookingPhase.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { phase ->
                        MetricButton(phase.label(), counts[phase] ?: 0, phaseFilter == phase, Modifier.weight(1f)) {
                            onPhase(phase)
                        }
                    }
                    if (pair.size < 2) Spacer(Modifier.weight(1f))
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.refunds), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            listOf("REQUESTED" to R.string.refund_review_pending, "REVIEW" to R.string.refund_under_review,
                "PAID" to R.string.refund_paid_plural).forEach { (state, label) ->
                val entries = refunds[state].orEmpty()
                val amount = entries.sumOf { it.refundableAmount() ?: 0L }
                OutlinedButton(
                    onClick = { onRefund(state) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (refundFilter == state) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(stringResource(label), Modifier.weight(1f))
                    Text(stringResource(R.string.refund_count_amount, entries.size, amount))
                }
            }
        }
    }
}

@Composable
private fun SummaryCount(label: Int, count: Int, modifier: Modifier) {
    Column(modifier) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MetricButton(label: Int, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick, modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
            Text(count.toString(), fontWeight = FontWeight.Bold)
        }
    }
}
