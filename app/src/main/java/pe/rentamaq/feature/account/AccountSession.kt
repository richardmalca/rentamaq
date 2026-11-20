package pe.rentamaq.feature.account

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class Profile(val id: String, val name: String, val company: Boolean, val email: String)

class AccountSession(private val preferences: SharedPreferences) {
    private val catalogueProfile = Profile("catalog-provider", "Maquinaria Cajamarca", true, "operaciones@rentamaq.test")
    private val profiles = readProfiles()
    private var current by mutableStateOf(
        profiles.find { it.id == preferences.getString("active_profile", null) }
            ?: profiles.find { it.id == "initial" && preferences.getBoolean("signed_in", false) &&
                !preferences.contains("active_profile") }
    )
    val accountId get() = current?.id
    val name get() = current?.name.orEmpty()
    val company get() = current?.company ?: false
    val email get() = current?.email.orEmpty()
    val signedIn get() = current != null

    fun profileName(id: String): String? = profiles.find { it.id == id }?.name?.takeIf { it.isNotBlank() }

    fun save(name: String, company: Boolean, email: String): Boolean {
        val old = current
        if (profiles.any { it.id != old?.id && it.email.equals(email, ignoreCase = true) }) return false
        val id = old?.id ?: if (profiles.none { it.id == "initial" }) "initial" else UUID.randomUUID().toString()
        val profile = Profile(id, name, company, email)
        profiles.removeAll { it.id == id }
        profiles.add(profile)
        current = profile
        persist()
        return true
    }

    fun signIn(email: String): Boolean {
        current = profiles.find { it.email.equals(email.trim(), ignoreCase = true) } ?: return false
        persist()
        return true
    }

    fun signOut() {
        current = null
        persist()
    }

    private fun readProfiles(): MutableList<Profile> {
        val encoded = preferences.getString("profiles", null)
        if (encoded != null) {
            val parsed = runCatching {
                val list = JSONArray(encoded)
                MutableList(list.length()) { index ->
                    val item = list.getJSONObject(index)
                    Profile(item.getString("id"), item.getString("name"), item.getBoolean("company"), item.getString("email"))
                }
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return mutableListOf(catalogueProfile).apply {
            val legacyEmail = preferences.getString("email", "").orEmpty()
            if (legacyEmail.isNotBlank() && !legacyEmail.equals(catalogueProfile.email, ignoreCase = true)) {
                add(Profile("initial", preferences.getString("name", "").orEmpty(),
                    preferences.getBoolean("company", false), legacyEmail))
            }
        }
    }

    private fun persist() {
        val encoded = JSONArray()
        profiles.forEach {
            encoded.put(JSONObject().put("id", it.id).put("name", it.name).put("company", it.company).put("email", it.email))
        }
        preferences.edit().putString("profiles", encoded.toString()).putString("active_profile", accountId.orEmpty())
            .remove("signed_in").apply()
    }
}

@Composable
fun rememberAccountSession(): AccountSession {
    val context = LocalContext.current
    return remember(context) { AccountSession(context.getSharedPreferences("rentamaq_profile", 0)) }
}
