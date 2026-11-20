package pe.rentamaq.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.CatalogViewModel
import pe.rentamaq.feature.explore.CatalogCarousel
import pe.rentamaq.feature.explore.CatalogSearch
import pe.rentamaq.feature.explore.CatalogResults
import pe.rentamaq.feature.explore.EquipmentCard
import pe.rentamaq.feature.explore.EquipmentPhoto
import pe.rentamaq.feature.explore.EquipmentType
import pe.rentamaq.feature.explore.PhotoCredits
import pe.rentamaq.feature.explore.type
import java.text.Normalizer
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

private val background = Color(0xFFFAF9F5)
private val accent = Color(0xFFF6D45E)
private val ink = Color(0xFF232518)
private val zones = listOf("Norte", "Centro", "Sur")
private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)
private val lima = ZoneId.of("America/Lima")
private val combiningMarks = Regex("\\p{M}+")

private fun String.searchKey() = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(combiningMarks, "").lowercase()

@Composable
fun RentamaqApp(model: CatalogViewModel) {
    val equipment by model.equipment.collectAsState()
    val bookings by model.bookings.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var zone by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<EquipmentType?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selected = equipment.find { it.id == selectedId }
    val query = search.trim().searchKey()
    val filtered = query.isNotEmpty() || type != null || zone != null
    val matches = equipment.filter {
        (zone == null || it.zone == zone) && (type == null || it.type() == type) &&
            (it.name.searchKey().contains(query) || it.description.searchKey().contains(query))
    }

    BackHandler(tab == 0 && selectedId != null) { selectedId = null }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = ink, surface = Color.White, background = background,
        secondaryContainer = accent, onSecondaryContainer = ink, primaryContainer = accent,
        onPrimaryContainer = ink, surfaceVariant = Color(0xFFF4F1E7), outlineVariant = Color(0xFFE7E3D9)
    )) {
        Scaffold(
            containerColor = background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf(R.string.explore, R.string.bookings, R.string.account).forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { if (tab == index && index == 0) selectedId = null; tab = index },
                            icon = { Text(listOf("▦", "▤", "◉")[index], style = MaterialTheme.typography.titleLarge) },
                            label = { Text(stringResource(label)) }
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                when (tab) {
                    0 -> if (selected == null) {
                        item {
                            Text(stringResource(R.string.location), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(R.string.headline), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            CatalogSearch(search, { search = it }, type, zone, zones) { nextType, nextZone ->
                                type = nextType
                                zone = nextZone
                            }
                        }
                        if (filtered) {
                            item {
                                CatalogResults(matches.size) {
                                    search = ""
                                    type = null
                                    zone = null
                                }
                            }
                            if (matches.isEmpty()) item { Text(stringResource(R.string.no_equipment)) }
                            items(matches, key = { it.id }) { machine ->
                                EquipmentCard(machine) { selectedId = machine.id }
                            }
                        } else if (equipment.isNotEmpty()) {
                            item {
                                CatalogCarousel(R.string.featured_equipment, equipment.take(2), compact = false) { selectedId = it }
                            }
                            val recent = equipment.drop(2).asReversed()
                            if (recent.isNotEmpty()) item {
                                CatalogCarousel(R.string.recent_listings, recent, compact = true) { selectedId = it }
                            }
                        }
                    } else {
                        item(key = selected.id) {
                            var showZone by rememberSaveable { mutableStateOf(false) }
                            TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.cancel)) }
                            EquipmentPhoto(selected, Modifier.height(190.dp).clip(RoundedCornerShape(20.dp)))
                            Text(stringResource(R.string.photo_label), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Text(selected.name, style = MaterialTheme.typography.headlineSmall)
                            Text(selected.description)
                            Text(stringResource(R.string.zone_label, selected.zone))
                            TextButton(onClick = { showZone = !showZone }) {
                                Text(stringResource(if (showZone) R.string.hide_zone else R.string.show_zone))
                            }
                            if (showZone) ZoneMap(selected.zone)
                            Text(stringResource(R.string.rate, selected.hourlyRate, selected.dailyRate))
                            BookingForm(selected) { booking ->
                                model.reserve(booking) { id ->
                                    if (id != null) tab = 1
                                    scope.launch {
                                        val result = snackbar.showSnackbar(
                                            context.getString(if (id == null) R.string.save_failed else R.string.saved),
                                            actionLabel = id?.let { context.getString(R.string.undo) },
                                            duration = SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed && id != null) model.undo(id)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        item { Text(stringResource(R.string.bookings), style = MaterialTheme.typography.headlineLarge) }
                        if (bookings.isEmpty()) item { Text(stringResource(R.string.empty)) }
                        items(bookings, key = { it.id }) { booking ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(booking.equipmentName, style = MaterialTheme.typography.titleLarge)
                                    Text(stringResource(R.string.booking_period, booking.start, booking.end))
                                    Text(stringResource(R.string.total, booking.total))
                                    Text(stringResource(R.string.pending))
                                }
                            }
                        }
                    }
                    2 -> item {
                        Text(stringResource(R.string.account), style = MaterialTheme.typography.headlineLarge)
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.demo_message))
                        PhotoCredits()
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneMap(selected: String) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFE8EDDF), RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.map_hint), style = MaterialTheme.typography.labelLarge)
        zones.forEachIndexed { index, zone ->
            Surface(
                modifier = Modifier.fillMaxWidth(0.86f + 0.06f * index),
                shape = RoundedCornerShape(28.dp, 14.dp, 36.dp, 12.dp),
                color = if (zone == selected) accent else Color(0xFFD0DAC6)
            ) {
                Text(zone, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun BookingForm(machine: Equipment, onConfirm: (Booking) -> Unit) {
    var start by rememberSaveable { mutableStateOf("") }
    var end by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf(false) }
    var review by remember { mutableStateOf<Booking?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!daily, { daily = false }, { Text(stringResource(R.string.hourly)) })
            FilterChip(daily, { daily = true }, { Text(stringResource(R.string.daily)) })
        }
        OutlinedTextField(start, { start = it; error = false }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.start)) }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        OutlinedTextField(end, { end = it; error = false }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.end)) }, singleLine = true)
        if (error) Text(stringResource(R.string.invalid_dates), color = MaterialTheme.colorScheme.error)
        Button(onClick = {
            val first = runCatching { LocalDateTime.parse(start.trim(), dateFormat) }.getOrNull()
            val last = runCatching { LocalDateTime.parse(end.trim(), dateFormat) }.getOrNull()
            if (first == null || last == null || first <= LocalDateTime.now(lima) || last <= first) {
                error = true
            } else {
                val minutes = Duration.between(first, last).toMinutes()
                val unit = if (daily) 1440L else 60L
                val price = if (daily) machine.dailyRate else machine.hourlyRate
                review = Booking(equipmentName = machine.name, start = dateFormat.format(first),
                    end = dateFormat.format(last), total = ((minutes + unit - 1) / unit) * price)
            }
        }) { Text(stringResource(R.string.reserve)) }
    }

    review?.let { booking ->
        AlertDialog(
            onDismissRequest = { review = null },
            title = { Text(stringResource(R.string.review)) },
            text = {
                Column {
                    Text(booking.equipmentName)
                    Text(stringResource(R.string.booking_period, booking.start, booking.end))
                    Text(stringResource(R.string.total, booking.total))
                }
            },
            confirmButton = {
                TextButton(onClick = { review = null; onConfirm(booking) }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { review = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
