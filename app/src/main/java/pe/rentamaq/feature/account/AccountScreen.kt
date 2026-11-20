package pe.rentamaq.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Equipment
import pe.rentamaq.feature.explore.EquipmentResult
import pe.rentamaq.feature.explore.EquipmentType
import pe.rentamaq.feature.explore.PhotoCredits

private enum class AccountPage { HOME, EDIT, PUBLISH, CREDITS }

@Composable
fun AccountScreen(
    modifier: Modifier,
    publications: List<Equipment>,
    sectors: List<String>,
    onExplore: () -> Unit,
    onBookings: () -> Unit,
    onOpen: (Equipment) -> Unit,
    onPublish: (Equipment, (Int?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("rentamaq_profile", 0) }
    var name by remember { mutableStateOf(preferences.getString("name", "").orEmpty()) }
    var company by remember { mutableStateOf(preferences.getBoolean("company", false)) }
    var page by rememberSaveable { mutableStateOf(AccountPage.HOME) }
    var saving by remember { mutableStateOf(false) }
    val back = { if (!saving) page = AccountPage.HOME }
    BackHandler(page != AccountPage.HOME, onBack = back)
    val scrollState = key(page) { rememberScrollState() }

    Column(
        modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        if (page != AccountPage.HOME) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back, enabled = !saving) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.cancel))
                }
                Text(stringResource(when (page) {
                    AccountPage.EDIT -> R.string.edit_profile
                    AccountPage.PUBLISH -> R.string.publish_equipment
                    AccountPage.CREDITS -> R.string.photo_credits
                    AccountPage.HOME -> R.string.account
                }), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
        when (page) {
            AccountPage.HOME -> {
                Text(stringResource(R.string.account), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }
                        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center) {
                            if (initials.isEmpty()) Icon(painterResource(R.drawable.ic_account), null, Modifier.size(20.dp))
                            else Text(initials, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(name.ifBlank { stringResource(R.string.local_profile) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(if (company) R.string.company_profile else R.string.personal_profile), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { page = AccountPage.EDIT }) { Text(stringResource(R.string.edit_schedule)) }
                    }
                }
                AccountSection(R.string.reserve_equipment) {
                    AccountLink(R.string.browse_equipment, R.string.reserve_account_help, R.drawable.ic_search, onExplore)
                    AccountLink(R.string.bookings, R.string.bookings_account_help, R.drawable.ic_bookings, onBookings)
                }
                AccountSection(R.string.offer_equipment) {
                    AccountLink(R.string.publish_equipment, R.string.publish_account_help, R.drawable.ic_explore,
                        onOpen = { page = AccountPage.PUBLISH })
                    if (publications.isNotEmpty()) {
                        Text(stringResource(R.string.my_publications), style = MaterialTheme.typography.titleSmall)
                        publications.forEach { machine -> EquipmentResult(machine) { onOpen(machine) } }
                    }
                }
                HorizontalDivider()
                AccountLink(R.string.photo_credits, R.string.credits_account_help, R.drawable.ic_arrow,
                    onOpen = { page = AccountPage.CREDITS })
            }
            AccountPage.EDIT -> EditLocalProfile(name, company) { newName, isCompany ->
                preferences.edit().putString("name", newName).putBoolean("company", isCompany).apply()
                name = newName
                company = isCompany
                page = AccountPage.HOME
            }
            AccountPage.PUBLISH -> PublicationForm(sectors, saving) { machine, finished ->
                saving = true
                onPublish(machine) { id ->
                    saving = false
                    finished(id != null)
                    if (id != null) page = AccountPage.HOME
                }
            }
            AccountPage.CREDITS -> PhotoCredits()
        }
    }
}

@Composable
private fun AccountSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        content()
    }
}

@Composable
private fun AccountLink(title: Int, description: Int, icon: Int, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painterResource(icon), null, Modifier.size(20.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(painterResource(R.drawable.ic_arrow), null, Modifier.size(16.dp))
    }
}

@Composable
private fun EditLocalProfile(name: String, company: Boolean, onSave: (String, Boolean) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(name) }
    var isCompany by rememberSaveable { mutableStateOf(company) }
    Text(stringResource(R.string.local_profile_notice), style = MaterialTheme.typography.bodyMedium)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf(R.string.personal_profile, R.string.company_profile).forEachIndexed { index, label ->
            SegmentedButton(
                selected = isCompany == (index == 1), onClick = { isCompany = index == 1 },
                shape = SegmentedButtonDefaults.itemShape(index, 2, MaterialTheme.shapes.small)
            ) { Text(stringResource(label)) }
        }
    }
    OutlinedTextField(draft, { if (it.length <= 70) draft = it }, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(if (isCompany) R.string.company_name else R.string.profile_name)) })
    Button(onClick = { onSave(draft.trim(), isCompany) }, enabled = draft.isNotBlank(),
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.save_profile))
    }
}

@Composable
private fun PublicationForm(sectors: List<String>, saving: Boolean, onPublish: (Equipment, (Boolean) -> Unit) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf<EquipmentType?>(null) }
    var zone by rememberSaveable { mutableStateOf<String?>(null) }
    var hourly by rememberSaveable { mutableStateOf("") }
    var daily by rememberSaveable { mutableStateOf("") }
    var failed by rememberSaveable { mutableStateOf(false) }
    val hourRate = hourly.toIntOrNull()?.takeIf { it > 0 }
    val dayRate = daily.toIntOrNull()?.takeIf { it > 0 }
    Text(stringResource(R.string.local_publication_notice), style = MaterialTheme.typography.bodyMedium)
    OutlinedTextField(name, { if (it.length <= 80) name = it }, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(R.string.equipment_name)) })
    OutlinedTextField(description, { if (it.length <= 350) description = it }, Modifier.fillMaxWidth(), minLines = 2,
        label = { Text(stringResource(R.string.equipment_description)) })
    PublicationChoice(R.string.equipment_type, type?.let { stringResource(it.label) },
        EquipmentType.entries.map { it to stringResource(it.label) }) { type = it }
    PublicationChoice(R.string.area, zone, sectors.map { it to it }) { zone = it }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Triple(R.string.hour_rate, hourly, { value: String -> hourly = value }),
            Triple(R.string.day_rate, daily, { value: String -> daily = value })).forEach { (label, value, update) ->
            OutlinedTextField(value, { if (it.length <= 6 && it.all { character -> character in '0'..'9' }) update(it) },
                Modifier.weight(1f), singleLine = true, label = { Text(stringResource(label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }
    if (failed) Text(stringResource(R.string.publication_failed), color = MaterialTheme.colorScheme.error)
    val canPublish = !saving && name.isNotBlank() && description.isNotBlank() && type != null && zone != null &&
        hourRate != null && dayRate != null
    Button(
        onClick = {
            val category = type ?: return@Button
            val sector = zone ?: return@Button
            val hourlyRate = hourRate ?: return@Button
            val dailyRate = dayRate ?: return@Button
            failed = false
            onPublish(Equipment(0, name.trim(), description.trim(), sector, hourlyRate, dailyRate, category.name)) {
                failed = !it
            }
        },
        enabled = canPublish, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small
    ) { Text(stringResource(R.string.save_publication)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PublicationChoice(label: Int, value: String?, choices: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }) {
        OutlinedTextField(value.orEmpty(), {}, Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true, label = { Text(stringResource(label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
        ExposedDropdownMenu(expanded, { expanded = false }) {
            choices.forEach { (choice, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(choice) })
            }
        }
    }
}
