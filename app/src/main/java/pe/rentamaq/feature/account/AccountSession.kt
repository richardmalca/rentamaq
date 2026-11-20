package pe.rentamaq.feature.account

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class AccountSession(private val preferences: SharedPreferences) {
    var name by mutableStateOf(preferences.getString("name", "").orEmpty())
        private set
    var company by mutableStateOf(preferences.getBoolean("company", false))
        private set
    var email by mutableStateOf(preferences.getString("email", "").orEmpty())
        private set
    var signedIn by mutableStateOf(preferences.getBoolean("signed_in", false) && email.isNotBlank())
        private set

    fun save(name: String, company: Boolean, email: String) {
        preferences.edit().putString("name", name).putBoolean("company", company)
            .putString("email", email).putBoolean("signed_in", true).apply()
        this.name = name
        this.company = company
        this.email = email
        signedIn = true
    }

    fun signIn(email: String): Boolean {
        if (this.email.isBlank() || !this.email.equals(email.trim(), ignoreCase = true)) return false
        preferences.edit().putBoolean("signed_in", true).apply()
        signedIn = true
        return true
    }

    fun signOut() {
        preferences.edit().putBoolean("signed_in", false).apply()
        signedIn = false
    }
}

@Composable
fun rememberAccountSession(): AccountSession {
    val context = LocalContext.current
    return remember(context) { AccountSession(context.getSharedPreferences("rentamaq_profile", 0)) }
}
