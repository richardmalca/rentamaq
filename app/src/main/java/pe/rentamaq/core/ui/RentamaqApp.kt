package pe.rentamaq.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Booking
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.CatalogViewModel
import pe.rentamaq.feature.explore.CatalogCarousel
import pe.rentamaq.feature.explore.EquipmentCategories
import pe.rentamaq.feature.explore.CatalogSearch
import pe.rentamaq.feature.explore.CatalogResults
import pe.rentamaq.feature.explore.EquipmentResult
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
    var showAll by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selected = equipment.find { it.id == selectedId }
    val query = search.trim().searchKey()
    val filtered = query.isNotEmpty() || type != null || zone != null || showAll
    val browseHome = tab == 0 && selected == null && !filtered
    val horizontalInset = if (browseHome) Modifier.padding(horizontal = 20.dp) else Modifier
    val zones = equipment.map { it.zone }.distinct().sorted()
    val homeScroll = rememberLazyListState()
    val resultsScroll = key(search, type, zone, showAll) { rememberLazyListState() }
    val detailScroll = key(selectedId) { rememberLazyListState() }
    val bookingsScroll = rememberLazyListState()
    val accountScroll = rememberLazyListState()
    val matches = equipment.filter {
        (zone == null || it.zone == zone) && (type == null || it.type() == type) &&
            (it.name.searchKey().contains(query) || it.description.searchKey().contains(query))
    }

    BackHandler(tab == 0 && selectedId != null) { selectedId = null }
    BackHandler(tab == 0 && selectedId == null && filtered) {
        search = ""
        type = null
        zone = null
        showAll = false
    }

    MaterialTheme(shapes = Shapes(
        small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp)
    ), colorScheme = lightColorScheme(
        primary = ink, surface = Color.White, background = background,
        secondaryContainer = accent, onSecondaryContainer = ink, primaryContainer = accent,
        onPrimaryContainer = ink, surfaceVariant = Color(0xFFF4F1E7), outlineVariant = Color(0xFFE7E3D9),
        onSurface = ink, onSurfaceVariant = Color(0xFF535246), outline = Color(0xFF777568)
    )) {
        Scaffold(
            containerColor = background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    HorizontalDivider()
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val icons = listOf(R.drawable.ic_explore, R.drawable.ic_bookings, R.drawable.ic_account)
                        listOf(R.string.explore, R.string.bookings, R.string.account).forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = {
                                    if (tab == index && index == 0) selectedId = null
                                    tab = index
                                },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                                icon = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.width(18.dp).height(3.dp).background(if (tab == index) accent else Color.Transparent))
                                        Spacer(Modifier.height(5.dp))
                                        Icon(painterResource(icons[index]), null)
                                    }
                                },
                                label = { Text(stringResource(label), fontWeight = if (tab == index) FontWeight.Bold else FontWeight.Medium) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = when {
                    tab == 1 -> bookingsScroll
                    tab == 2 -> accountScroll
                    selected != null -> detailScroll
                    filtered -> resultsScroll
                    else -> homeScroll
                },
                contentPadding = PaddingValues(horizontal = if (browseHome) 0.dp else 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(horizontalInset) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            if (tab == 0 && selected == null) {
                                Text(stringResource(R.string.location), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider()
                    }
                }
                when (tab) {
                    0 -> if (selected == null) {
                        item {
                            Text(stringResource(R.string.headline), horizontalInset, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            Box(horizontalInset) {
                                CatalogSearch(search, { search = it }, type, zone, zones) { nextType, nextZone ->
                                    type = nextType
                                    zone = nextZone
                                }
                            }
                        }
                        if (filtered) {
                            item {
                                CatalogResults(matches.size) {
                                    search = ""
                                    type = null
                                    zone = null
                                    showAll = false
                                }
                            }
                            if (matches.isEmpty()) item { Text(stringResource(R.string.no_equipment)) }
                            items(matches, key = { it.id }) { machine ->
                                EquipmentResult(machine) { selectedId = machine.id }
                            }
                        } else if (equipment.isNotEmpty()) {
                            item {
                                CatalogCarousel(R.string.featured_equipment, equipment.take(6), compact = false) { selectedId = it }
                            }
                            item {
                                CatalogCarousel(R.string.recent_listings, equipment.takeLast(8).asReversed(), compact = true) { selectedId = it }
                            }
                            item { EquipmentCategories(equipment) { type = it } }
                            item {
                                Column(Modifier.padding(horizontal = 20.dp)) {
                                    Text(stringResource(R.string.lowest_hourly_rates), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    equipment.sortedBy { it.hourlyRate }.take(3).forEach { machine ->
                                        EquipmentResult(machine) { selectedId = machine.id }
                                    }
                                    TextButton(onClick = { showAll = true }) { Text(stringResource(R.string.see_all_equipment)) }
                                }
                            }
                        }
                    } else {
                        item(key = selected.id) {
                            var showZone by rememberSaveable { mutableStateOf(false) }
                            TextButton(onClick = { selectedId = null }) { Text(stringResource(R.string.cancel)) }
                            EquipmentPhoto(selected, Modifier.height(190.dp).clip(MaterialTheme.shapes.medium))
                            Text(stringResource(R.string.photo_label), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Text(selected.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(selected.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.sector_name, selected.zone), style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = { showZone = !showZone }) {
                                Text(stringResource(if (showZone) R.string.hide_zone else R.string.show_zone))
                            }
                            if (showZone) ZoneMap(selected.zone)
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
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
                        item { Text(stringResource(R.string.bookings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                        if (bookings.isEmpty()) item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.empty), style = MaterialTheme.typography.bodyLarge)
                                Button(onClick = { selectedId = null; tab = 0 }, shape = MaterialTheme.shapes.small) {
                                    Text(stringResource(R.string.browse_equipment))
                                }
                            }
                        }
                        items(bookings, key = { it.id }) { booking ->
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(booking.equipmentName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.pending), style = MaterialTheme.typography.labelMedium)
                                Text(stringResource(R.string.booking_period, booking.start, booking.end), style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(R.string.total, booking.total), style = MaterialTheme.typography.titleSmall)
                                HorizontalDivider(Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                    2 -> item {
                        Text(stringResource(R.string.account), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)) {
                            Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.demo_message), style = MaterialTheme.typography.bodyMedium)
                        }
                        PhotoCredits()
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneMap(selected: String) {
    val uriHandler = LocalUriHandler.current
    val query = android.net.Uri.encode("$selected, Cajamarca, Perú")
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.map_hint), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(R.string.sector_map_explanation, selected), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { uriHandler.openUri("https://www.openstreetmap.org/search?query=$query") }) {
            Text(stringResource(R.string.openstreetmap))
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

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.reserve), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(R.string.hourly, R.string.daily).forEachIndexed { index, label ->
                SegmentedButton(
                    selected = daily == (index == 1),
                    onClick = { daily = index == 1 },
                    shape = SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)
                ) { Text(stringResource(label)) }
            }
        }
        Text(stringResource(R.string.rate, machine.hourlyRate, machine.dailyRate), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(start, { start = it; error = false }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.start)) }, singleLine = true, isError = error,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        OutlinedTextField(end, { end = it; error = false }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.end)) }, singleLine = true, isError = error)
        if (error) Text(stringResource(R.string.invalid_dates), color = MaterialTheme.colorScheme.error)
        Button(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, onClick = {
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
        }) { Text(stringResource(R.string.review)) }
    }

    review?.let { booking ->
        AlertDialog(
            onDismissRequest = { review = null },
            containerColor = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.large,
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
