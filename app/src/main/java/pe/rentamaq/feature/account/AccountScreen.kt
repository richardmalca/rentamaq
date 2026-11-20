package pe.rentamaq.feature.account

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
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
import java.time.LocalDate

private enum class AccountPage { HOME, EDIT, PUBLISH, CREDITS, SIGN_IN }

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
    var email by remember { mutableStateOf(preferences.getString("email", "").orEmpty()) }
    var signedIn by remember { mutableStateOf(preferences.getBoolean("signed_in", name.isNotBlank())) }
    var page by rememberSaveable { mutableStateOf(AccountPage.HOME) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
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
                    AccountPage.EDIT -> if (signedIn) R.string.edit_profile else R.string.create_account
                    AccountPage.PUBLISH -> if (editingId == null) R.string.publish_equipment else R.string.edit_publication
                    AccountPage.CREDITS -> R.string.photo_credits
                    AccountPage.SIGN_IN -> R.string.sign_in
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
                        TextButton(onClick = { page = if (signedIn || email.isBlank()) AccountPage.EDIT else AccountPage.SIGN_IN }) {
                            Text(stringResource(if (signedIn) R.string.edit_schedule else if (email.isBlank()) R.string.create_account else R.string.sign_in))
                        }
                    }
                }
                AccountSection(R.string.reserve_equipment) {
                    AccountLink(R.string.browse_equipment, R.string.reserve_account_help, R.drawable.ic_search, onExplore)
                    AccountLink(R.string.bookings, R.string.bookings_account_help, R.drawable.ic_bookings, onBookings)
                }
                AccountSection(R.string.offer_equipment) {
                    AccountLink(R.string.publish_equipment, R.string.publish_account_help, R.drawable.ic_explore,
                        onOpen = { editingId = null; page = AccountPage.PUBLISH })
                    if (publications.isNotEmpty()) {
                        Text(stringResource(R.string.my_publications), style = MaterialTheme.typography.titleSmall)
                        publications.forEach { machine ->
                            EquipmentResult(machine) { onOpen(machine) }
                            TextButton(onClick = { editingId = machine.id; page = AccountPage.PUBLISH }) {
                                Text(stringResource(R.string.edit_publication))
                            }
                        }
                    }
                }
                HorizontalDivider()
                AccountLink(R.string.photo_credits, R.string.credits_account_help, R.drawable.ic_arrow,
                    onOpen = { page = AccountPage.CREDITS })
                if (signedIn) TextButton(onClick = {
                    preferences.edit().putBoolean("signed_in", false).apply()
                    signedIn = false
                }) { Text(stringResource(R.string.sign_out)) }
            }
            AccountPage.EDIT -> EditLocalProfile(name, company, email) { newName, isCompany, newEmail ->
                preferences.edit().putString("name", newName).putBoolean("company", isCompany)
                    .putString("email", newEmail).putBoolean("signed_in", true).apply()
                name = newName
                company = isCompany
                email = newEmail
                signedIn = true
                page = AccountPage.HOME
            }
            AccountPage.PUBLISH -> PublicationForm(sectors, saving, publications.find { it.id == editingId }) { machine, finished ->
                saving = true
                onPublish(machine) { id ->
                    saving = false
                    finished(id != null)
                    if (id != null) page = AccountPage.HOME
                }
            }
            AccountPage.CREDITS -> PhotoCredits()
            AccountPage.SIGN_IN -> {
                var enteredEmail by rememberSaveable { mutableStateOf("") }
                var mismatch by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(enteredEmail, { enteredEmail = it; mismatch = false }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.account_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                if (mismatch) Text(stringResource(R.string.account_not_found), color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    if (!enteredEmail.trim().equals(email, ignoreCase = true)) {
                        mismatch = true
                    } else {
                        preferences.edit().putBoolean("signed_in", true).apply()
                        signedIn = true
                        page = AccountPage.HOME
                    }
                }, enabled = enteredEmail.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.sign_in))
                }
            }
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
private fun EditLocalProfile(name: String, company: Boolean, email: String, onSave: (String, Boolean, String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf(name) }
    var isCompany by rememberSaveable { mutableStateOf(company) }
    var draftEmail by rememberSaveable { mutableStateOf(email) }
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
    OutlinedTextField(draftEmail, { if (it.length <= 120) draftEmail = it }, Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(R.string.account_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
    Button(onClick = { onSave(draft.trim(), isCompany, draftEmail.trim()) },
        enabled = draft.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(draftEmail.trim()).matches(),
        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.save_profile))
    }
}

@Composable
private fun PublicationForm(sectors: List<String>, saving: Boolean, original: Equipment?, onPublish: (Equipment, (Boolean) -> Unit) -> Unit) {
    var name by rememberSaveable { mutableStateOf(original?.name.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(original?.description.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(EquipmentType.entries.find { it.name == original?.category }) }
    var zone by rememberSaveable { mutableStateOf(original?.zone) }
    var hourly by rememberSaveable { mutableStateOf(original?.hourlyRate?.toString().orEmpty()) }
    var daily by rememberSaveable { mutableStateOf(original?.dailyRate?.toString().orEmpty()) }
    var photos by rememberSaveable { mutableStateOf(original?.photos.orEmpty()) }
    var blockStart by rememberSaveable { mutableStateOf(original?.blockedFrom.orEmpty()) }
    var blockEnd by rememberSaveable { mutableStateOf(original?.blockedUntil.orEmpty()) }
    var serviceDate by rememberSaveable { mutableStateOf(original?.serviceDate.orEmpty()) }
    var serviceHours by rememberSaveable { mutableStateOf(original?.serviceHours?.toString().orEmpty()) }
    var serviceNotes by rememberSaveable { mutableStateOf(original?.serviceNotes.orEmpty()) }
    var serviceProvider by rememberSaveable { mutableStateOf(original?.serviceProvider.orEmpty()) }
    var nextService by rememberSaveable { mutableStateOf(original?.nextServiceDate.orEmpty()) }
    var serviceDocument by rememberSaveable { mutableStateOf(original?.serviceDocument.orEmpty()) }
    val resolver = LocalContext.current.contentResolver
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        if (selected.isNotEmpty()) photos = selected.take(5).filter { uri ->
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess
        }.joinToString("\n")
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        if (selected != null && runCatching {
                resolver.takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }.isSuccess
        ) serviceDocument = selected.toString()
    }
    var failed by rememberSaveable { mutableStateOf(false) }
    val hourRate = hourly.toIntOrNull()?.takeIf { it > 0 }
    val dayRate = daily.toIntOrNull()?.takeIf { it > 0 }
    OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, shape = MaterialTheme.shapes.small) {
        Text(stringResource(R.string.choose_photos))
    }
    if (photos.isNotBlank()) Text(stringResource(R.string.selected_photos, photos.lines().size))
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
    Text(stringResource(R.string.availability), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.block_dates_help), style = MaterialTheme.typography.bodySmall)
    PublicationDate(R.string.block_start, blockStart) { blockStart = it }
    PublicationDate(R.string.block_end, blockEnd) { blockEnd = it }
    if (blockStart.isNotBlank() || blockEnd.isNotBlank()) TextButton(onClick = { blockStart = ""; blockEnd = "" }) {
        Text(stringResource(R.string.clear_block))
    }
    Text(stringResource(R.string.maintenance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    PublicationDate(R.string.service_date, serviceDate) { serviceDate = it }
    OutlinedTextField(serviceHours, { if (it.length <= 8 && it.all { c -> c in '0'..'9' }) serviceHours = it },
        Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.service_hours)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    OutlinedTextField(serviceNotes, { if (it.length <= 350) serviceNotes = it }, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.service_notes)) }, minLines = 2)
    OutlinedTextField(serviceProvider, { if (it.length <= 120) serviceProvider = it }, Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.service_provider)) }, singleLine = true)
    PublicationDate(R.string.next_service, nextService) { nextService = it }
    OutlinedButton(onClick = { documentPicker.launch(arrayOf("application/pdf")) }, shape = MaterialTheme.shapes.small) {
        Text(stringResource(if (serviceDocument.isBlank()) R.string.attach_service_document else R.string.replace_service_document))
    }
    if (serviceDocument.isNotBlank()) TextButton(onClick = { serviceDocument = "" }) { Text(stringResource(R.string.remove_service_document)) }
    if (failed) Text(stringResource(R.string.publication_failed), color = MaterialTheme.colorScheme.error)
    val validBlock = (blockStart.isBlank() && blockEnd.isBlank()) ||
        (blockStart.isNotBlank() && blockEnd.isNotBlank() && blockStart <= blockEnd)
    val hasService = listOf(serviceDate, serviceNotes, serviceHours, serviceProvider, nextService, serviceDocument).any { it.isNotBlank() }
    val validService = !hasService || (serviceDate.isNotBlank() && serviceNotes.isNotBlank() &&
        serviceHours.toIntOrNull() != null && (nextService.isBlank() || nextService >= serviceDate))
    if (!validBlock || !validService) Text(stringResource(R.string.publication_dates_invalid), color = MaterialTheme.colorScheme.error)
    val canPublish = !saving && name.isNotBlank() && description.isNotBlank() && type != null && zone != null &&
        hourRate != null && dayRate != null && validBlock && validService
    Button(
        onClick = {
            val category = type ?: return@Button
            val sector = zone ?: return@Button
            val hourlyRate = hourRate ?: return@Button
            val dailyRate = dayRate ?: return@Button
            failed = false
            onPublish(Equipment(original?.id ?: 0, name.trim(), description.trim(), sector, hourlyRate, dailyRate, category.name,
                owned = true, photos = photos.ifBlank { null }, blockedFrom = blockStart.ifBlank { null },
                blockedUntil = blockEnd.ifBlank { null }, serviceDate = serviceDate.ifBlank { null },
                serviceHours = serviceHours.toIntOrNull(), serviceNotes = serviceNotes.trim().ifBlank { null },
                serviceProvider = serviceProvider.trim().ifBlank { null }, nextServiceDate = nextService.ifBlank { null },
                serviceDocument = serviceDocument.ifBlank { null })) {
                failed = !it
            }
        },
        enabled = canPublish, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small
    ) { Text(stringResource(R.string.save_publication)) }
}

@Composable
private fun PublicationDate(label: Int, value: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        val date = value.takeIf { it.isNotBlank() }?.let(LocalDate::parse) ?: LocalDate.now()
        android.app.DatePickerDialog(context, { _, year, month, day ->
            onSelect(LocalDate.of(year, month + 1, day).toString())
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
        Text(stringResource(label))
        if (value.isNotBlank()) Text(" · $value")
    }
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
