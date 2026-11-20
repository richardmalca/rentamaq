package pe.rentamaq.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import pe.rentamaq.R
import pe.rentamaq.feature.bookings.BookingWizard
import pe.rentamaq.feature.account.AccountScreen
import pe.rentamaq.feature.account.AccountAccessPanel
import pe.rentamaq.feature.account.AccountEntry
import pe.rentamaq.feature.account.rememberAccountSession
import pe.rentamaq.feature.bookings.BookingsScreen
import pe.rentamaq.core.data.bookingZone
import pe.rentamaq.core.data.cancelledBooking
import pe.rentamaq.core.data.confirmedBooking
import pe.rentamaq.core.data.canCancel
import pe.rentamaq.core.data.phase
import java.time.LocalDateTime
import pe.rentamaq.feature.equipment.EquipmentDetail
import pe.rentamaq.feature.equipment.EquipmentLocation
import pe.rentamaq.feature.explore.CatalogViewModel
import pe.rentamaq.feature.explore.CatalogCarousel
import pe.rentamaq.feature.explore.EquipmentCategories
import pe.rentamaq.feature.explore.CatalogSearch
import pe.rentamaq.feature.explore.CatalogResults
import pe.rentamaq.feature.explore.EquipmentResult
import pe.rentamaq.feature.explore.EquipmentType
import pe.rentamaq.feature.explore.type
import java.text.Normalizer

private val background = Color(0xFFFAF9F5)
private val accent = Color(0xFFF6D45E)
private val ink = Color(0xFF232518)
private val combiningMarks = Regex("\\p{M}+")
private enum class AccountDestination { RESERVATION, BOOKINGS }

private fun String.searchKey() = Normalizer.normalize(this, Normalizer.Form.NFD)
    .replace(combiningMarks, "").lowercase()

@Composable
fun RentamaqApp(model: CatalogViewModel) {
    val equipment by model.equipment.collectAsState()
    val bookings by model.bookings.collectAsState()
    val blocks by model.blocks.collectAsState()
    val services by model.services.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var zone by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<EquipmentType?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var showBooking by rememberSaveable { mutableStateOf(false) }
    var accountDestination by rememberSaveable { mutableStateOf<AccountDestination?>(null) }
    var accountEntry by rememberSaveable { mutableStateOf(AccountEntry.OPTIONS) }
    val session = rememberAccountSession()
    val accountId = session.accountId
    var bookingMachineId by rememberSaveable { mutableStateOf<Int?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selected = equipment.find { it.id == selectedId }
    val inDetail = tab == 0 && selected != null
    val query = search.trim().searchKey()
    val filtered = query.isNotEmpty() || type != null || zone != null || showAll
    val browseHome = tab == 0 && selected == null && !filtered
    val horizontalInset = if (browseHome) Modifier.padding(horizontal = 20.dp) else Modifier
    val zones = equipment.map { it.zone }.distinct().sorted()
    val homeScroll = rememberLazyListState()
    val resultsScroll = key(type, zone, showAll) { rememberLazyListState() }
    val detailScroll = key(selectedId) { rememberLazyListState() }
    val bookingsScroll = key(accountId) { rememberLazyListState() }
    val matches = equipment.filter {
        (zone == null || it.zone == zone) && (type == null || it.type() == type) &&
            (it.name.searchKey().contains(query) || it.description.searchKey().contains(query))
    }

    BackHandler(inDetail && !showBooking) { selectedId = null }
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
        if (showBooking && accountId != null && selected != null && selected.ownerId != accountId) {
            BookingWizard(selected, onClose = { showBooking = false }) { booking, finished ->
                model.reserve(booking, accountId) { id ->
                    finished(id != null)
                    if (id == null) return@reserve
                    showBooking = false
                    bookingMachineId = null
                    tab = 1
                    scope.launch {
                        val result = snackbar.showSnackbar(
                            context.getString(R.string.saved),
                            actionLabel = context.getString(R.string.undo),
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed && session.accountId == accountId) model.undo(id, accountId)
                    }
                }
            }
            return@MaterialTheme
        }
        Scaffold(
            containerColor = background,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (inDetail) {
                    Surface(color = background) {
                        Column(Modifier.statusBarsPadding()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedId = null }) {
                                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.cancel))
                                }
                                Text(stringResource(R.string.equipment_detail), style = MaterialTheme.typography.titleMedium)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    HorizontalDivider()
                    if (inDetail && selected.ownerId != accountId) {
                        Surface(color = MaterialTheme.colorScheme.surface) {
                            Button(
                                onClick = {
                                    if (session.signedIn) {
                                        showBooking = true
                                    } else {
                                        accountDestination = AccountDestination.RESERVATION
                                        accountEntry = AccountEntry.OPTIONS
                                        tab = 2
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.small
                            ) { Text(stringResource(R.string.reserve)) }
                        }
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        val icons = listOf(R.drawable.ic_explore, R.drawable.ic_bookings, R.drawable.ic_account)
                        listOf(R.string.explore, R.string.bookings, R.string.account).forEachIndexed { index, label ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = {
                                    if (tab == index && index == 0) selectedId = null
                                    if (index != 2) accountDestination = null
                                    if (index != 1 || tab != 1) bookingMachineId = null
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
            if (tab == 1) {
                if (accountId == null) {
                    Column(Modifier.padding(padding).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.bookings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        AccountAccessPanel(R.string.access_bookings_title, R.string.account_required_bookings) { entry ->
                            accountDestination = AccountDestination.BOOKINGS
                            accountEntry = entry
                            tab = 2
                        }
                        TextButton(onClick = { selectedId = null; tab = 0 }) { Text(stringResource(R.string.continue_exploring)) }
                    }
                    return@Scaffold
                }
                BookingsScreen(bookings, equipment, bookingsScroll, Modifier.padding(padding), model, accountId,
                    requestedEquipmentId = bookingMachineId, onClearEquipment = { bookingMachineId = null },
                    accountName = session::profileName,
                    onNotice = { notice -> scope.launch { snackbar.showSnackbar(context.getString(notice)) } },
                    onExplore = { selectedId = null; tab = 0 },
                    onCancel = { booking ->
                        if (!booking.phase(LocalDateTime.now(bookingZone)).canCancel()) {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.cancel_unavailable)) }
                        } else {
                            model.changeBookingStatus(booking, cancelledBooking, accountId) { updated ->
                                scope.launch {
                                    val result = snackbar.showSnackbar(
                                        context.getString(if (updated) R.string.booking_cancelled_notice else R.string.cancel_unavailable),
                                        actionLabel = if (updated && booking.paidAmount == null) context.getString(R.string.undo) else null,
                                        duration = SnackbarDuration.Long
                                    )
                                    if (result == SnackbarResult.ActionPerformed && session.accountId == accountId) {
                                        if (booking.phase(LocalDateTime.now(bookingZone)).canCancel()) {
                                            model.changeBookingStatus(booking.copy(status = cancelledBooking), booking.status, accountId) { restored ->
                                                scope.launch {
                                                    snackbar.showSnackbar(context.getString(
                                                        if (restored) R.string.booking_restored else R.string.restore_unavailable))
                                                }
                                            }
                                        } else {
                                            snackbar.showSnackbar(context.getString(R.string.restore_unavailable))
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
                return@Scaffold
            }
            if (tab == 2) {
                AccountScreen(
                    modifier = Modifier.padding(padding),
                    session = session,
                    entry = accountEntry,
                    publications = equipment.filter { it.ownerId == accountId }.asReversed(),
                    blocks = blocks,
                    services = services,
                    model = model,
                    sectors = zones,
                    requiredMessage = when (accountDestination) {
                        AccountDestination.RESERVATION -> R.string.account_required_reserve
                        AccountDestination.BOOKINGS -> R.string.account_required_bookings
                        null -> null
                    },
                    onAuthenticated = {
                        when (accountDestination) {
                            AccountDestination.RESERVATION -> {
                                tab = 0
                                showBooking = selected != null && selected.ownerId != session.accountId
                            }
                            AccountDestination.BOOKINGS -> tab = 1
                            null -> Unit
                        }
                        accountDestination = null
                    },
                    onAuthBack = {
                        tab = if (accountDestination == AccountDestination.RESERVATION) 0 else 1
                        accountDestination = null
                    },
                    onExplore = { selectedId = null; tab = 0 },
                    onBookings = { bookingMachineId = null; tab = 1 },
                    onOpen = { machine -> selectedId = machine.id; tab = 0 },
                    onViewReservations = { machine ->
                        bookingMachineId = machine.id
                        scope.launch { bookingsScroll.scrollToItem(0) }
                        tab = 1
                    },
                    onPublish = { machine, finished ->
                        if (accountId == null) finished(null)
                        else model.publish(machine, accountId) { id ->
                            finished(id)
                            if (id != null) scope.launch {
                                val result = snackbar.showSnackbar(
                                    context.getString(R.string.publication_saved),
                                    actionLabel = if (machine.id == 0) context.getString(R.string.undo) else null,
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed && session.accountId == accountId) model.undoPublication(id, accountId)
                            }
                        }
                    }
                )
                return@Scaffold
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                state = when {
                    selected != null -> detailScroll
                    filtered -> resultsScroll
                    else -> homeScroll
                },
                contentPadding = PaddingValues(horizontal = if (browseHome) 0.dp else 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!inDetail) item {
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
                        item(key = "catalog-heading") {
                            Text(stringResource(R.string.headline), horizontalInset, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        item(key = "catalog-search") {
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
                            EquipmentDetail(selected, blocks.filter {
                                it.equipmentId == selected.id && it.lastDay >= LocalDateTime.now(bookingZone).toLocalDate().toString()
                            },
                                services.filter { it.equipmentId == selected.id },
                                canViewDocuments = accountId != null && (selected.ownerId == accountId || bookings.any {
                                it.customerId == accountId && it.status == confirmedBooking && (it.equipmentId == selected.id ||
                                    (it.equipmentId == null && it.equipmentName == selected.name))
                            }))
                        }
                        item(key = "location-${selected.id}") { EquipmentLocation(selected.zone) }
                    }
                }
            }
        }
    }
}
