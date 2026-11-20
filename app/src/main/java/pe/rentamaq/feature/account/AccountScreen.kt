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
import pe.rentamaq.core.data.local.EquipmentBlock
import pe.rentamaq.core.data.local.ServiceRecord
import pe.rentamaq.feature.explore.CatalogViewModel
import pe.rentamaq.feature.explore.EquipmentResult
import pe.rentamaq.feature.explore.EquipmentType
import pe.rentamaq.feature.explore.PhotoCredits
import java.time.LocalDate

private enum class AccountPage { HOME, ACCESS, REGISTER, EDIT, PUBLISH, MANAGE, CREDITS, SIGN_IN }

private fun AccountEntry.page() = when (this) {
    AccountEntry.OPTIONS -> AccountPage.ACCESS
    AccountEntry.SIGN_IN -> AccountPage.SIGN_IN
    AccountEntry.REGISTER -> AccountPage.REGISTER
}

@Composable
fun AccountScreen(
    modifier: Modifier,
    session: AccountSession,
    publications: List<Equipment>,
    blocks: List<EquipmentBlock>,
    services: List<ServiceRecord>,
    model: CatalogViewModel,
    sectors: List<String>,
    requiredMessage: Int?,
    entry: AccountEntry,
    onAuthenticated: () -> Unit,
    onAuthBack: () -> Unit,
    onExplore: () -> Unit,
    onBookings: () -> Unit,
    onOpen: (Equipment) -> Unit,
    onViewReservations: (Equipment) -> Unit,
    onPublish: (Equipment, (Int?) -> Unit) -> Unit
) {
    val signedIn = session.signedIn
    val name = if (signedIn) session.name else ""
    val company = signedIn && session.company
    var page by rememberSaveable { mutableStateOf(if (requiredMessage == null) AccountPage.HOME else entry.page()) }
    var editingId by rememberSaveable { mutableStateOf<Int?>(null) }
    var publishAfterSignIn by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val back = {
        if (!saving) {
            when {
                !signedIn && page in listOf(AccountPage.SIGN_IN, AccountPage.REGISTER) ->
                    page = if (requiredMessage != null || publishAfterSignIn) AccountPage.ACCESS else AccountPage.HOME
                requiredMessage != null && !signedIn -> onAuthBack()
                else -> {
                    publishAfterSignIn = false
                    page = AccountPage.HOME
                }
            }
        }
    }
    fun signedInSuccessfully() {
        page = if (publishAfterSignIn) AccountPage.PUBLISH else AccountPage.HOME
        publishAfterSignIn = false
        onAuthenticated()
    }
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
                    AccountPage.ACCESS -> R.string.account
                    AccountPage.REGISTER -> R.string.create_account
                    AccountPage.EDIT -> R.string.edit_profile
                    AccountPage.PUBLISH -> if (editingId == null) R.string.publish_equipment else R.string.edit_publication
                    AccountPage.MANAGE -> R.string.manage_equipment
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
                if (!signedIn) {
                    AccountAccessPanel(R.string.account_access, R.string.account_access_description) { page = it.page() }
                } else Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
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
                        TextButton(onClick = { page = AccountPage.EDIT }) {
                            Text(stringResource(R.string.edit_schedule))
                        }
                    }
                }
                if (signedIn) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = session::signOut) { Text(stringResource(R.string.sign_out)) }
                    }
                }
                AccountSection(R.string.reserve_equipment) {
                    AccountLink(R.string.browse_equipment, R.string.reserve_account_help, R.drawable.ic_search, onExplore)
                    AccountLink(R.string.bookings, R.string.bookings_account_help, R.drawable.ic_bookings, onBookings)
                }
                AccountSection(R.string.offer_equipment) {
                    AccountLink(R.string.publish_equipment, R.string.publish_account_help, R.drawable.ic_explore,
                        onOpen = {
                            editingId = null
                            publishAfterSignIn = !signedIn
                            page = if (signedIn) AccountPage.PUBLISH else AccountPage.ACCESS
                        })
                }
                HorizontalDivider()
                AccountLink(R.string.photo_credits, R.string.credits_account_help, R.drawable.ic_arrow,
                    onOpen = { page = AccountPage.CREDITS })
                if (signedIn && publications.isNotEmpty()) {
                    AccountSection(R.string.my_publications) {
                        publications.forEach { machine ->
                            EquipmentResult(machine) { onOpen(machine) }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { onViewReservations(machine) }) { Text(stringResource(R.string.view_machine_bookings)) }
                                TextButton(onClick = { editingId = machine.id; page = AccountPage.MANAGE }) { Text(stringResource(R.string.manage_equipment)) }
                                TextButton(onClick = { editingId = machine.id; page = AccountPage.PUBLISH }) { Text(stringResource(R.string.edit_publication)) }
                            }
                        }
                    }
                }
            }
            AccountPage.ACCESS -> {
                val message = if (publishAfterSignIn) R.string.account_required_publish
                    else requiredMessage ?: R.string.account_access_description
                AccountAccessPanel(R.string.account_access, message) { page = it.page() }
            }
            AccountPage.EDIT -> if (signedIn) {
                var duplicate by rememberSaveable { mutableStateOf(false) }
                EditLocalProfile(name, company, session.email, R.string.save_profile) { newName, isCompany, newEmail ->
                    duplicate = !session.save(newName, isCompany, newEmail)
                    if (!duplicate) signedInSuccessfully()
                }
                if (duplicate) Text(stringResource(R.string.existing_device_account), color = MaterialTheme.colorScheme.error)
            }
            AccountPage.REGISTER -> if (!signedIn) {
                AuthExplanation(requiredMessage, publishAfterSignIn)
                var duplicate by rememberSaveable { mutableStateOf(false) }
                EditLocalProfile("", false, "", R.string.create_account) { newName, isCompany, newEmail ->
                    duplicate = !session.save(newName, isCompany, newEmail)
                    if (!duplicate) signedInSuccessfully()
                }
                if (duplicate) Text(stringResource(R.string.existing_device_account), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { page = AccountPage.SIGN_IN }) { Text(stringResource(R.string.already_have_account)) }
            }
            AccountPage.PUBLISH -> if (signedIn) PublicationForm(sectors, saving, publications.find { it.id == editingId }) { machine, finished ->
                saving = true
                onPublish(machine) { id ->
                    saving = false
                    finished(id != null)
                    if (id != null) page = AccountPage.HOME
                }
            }
            AccountPage.CREDITS -> PhotoCredits()
            AccountPage.MANAGE -> {
                val machine = publications.find { it.id == editingId }
                val accountId = session.accountId
                if (machine != null && accountId != null) {
                    EquipmentManagement(machine, blocks.filter { it.equipmentId == machine.id },
                        services.filter { it.equipmentId == machine.id }, model, accountId)
                }
            }
            AccountPage.SIGN_IN -> {
                AuthExplanation(requiredMessage, publishAfterSignIn)
                Text(stringResource(R.string.sign_in_description), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                var enteredEmail by rememberSaveable { mutableStateOf("") }
                var mismatch by rememberSaveable { mutableStateOf(false) }
                OutlinedTextField(enteredEmail, { enteredEmail = it; mismatch = false }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.account_email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                if (mismatch) Text(stringResource(R.string.account_not_found), color = MaterialTheme.colorScheme.error)
                Button(onClick = {
                    if (!session.signIn(enteredEmail)) {
                        mismatch = true
                    } else {
                        signedInSuccessfully()
                    }
                }, enabled = enteredEmail.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                    Text(stringResource(R.string.sign_in))
                }
                TextButton(onClick = { page = AccountPage.REGISTER }) { Text(stringResource(R.string.no_account_yet)) }
            }
        }
    }
}

@Composable
private fun AuthExplanation(requiredMessage: Int?, publishAfterSignIn: Boolean) {
    val message = if (publishAfterSignIn) R.string.account_required_publish else requiredMessage
    if (message != null) Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
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
private fun EditLocalProfile(name: String, company: Boolean, email: String, action: Int, onSave: (String, Boolean, String) -> Unit) {
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
        Text(stringResource(action))
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
    val resolver = LocalContext.current.contentResolver
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { selected ->
        if (selected.isNotEmpty()) photos = selected.take(5).filter { uri ->
            runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }.isSuccess
        }.joinToString("\n")
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
            onPublish(Equipment(original?.id ?: 0, name.trim(), description.trim(), sector, hourlyRate, dailyRate, category.name,
                owned = true, photos = photos.ifBlank { null })) {
                failed = !it
            }
        },
        enabled = canPublish, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small
    ) { Text(stringResource(R.string.save_publication)) }
}

@Composable
internal fun PublicationDate(label: Int, value: String, onSelect: (String) -> Unit) {
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
