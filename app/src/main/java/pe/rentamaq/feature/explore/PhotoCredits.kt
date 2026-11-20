package pe.rentamaq.feature.explore

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pe.rentamaq.R

private data class PhotoCredit(val author: String, val file: String, val licenseVersion: String)

private val credits = listOf(
    PhotoCredit("Bene Riobó", "Caterpillar_320.jpg", "4.0"),
    PhotoCredit("Andrew Butko", "JCB_3CX_backhoe_loader.jpg", "3.0"),
    PhotoCredit("Reinhold Möller", "ICE-Baustelle-BOMAG-roadroller-P3209764.jpg", "4.0"),
    PhotoCredit("Bob Adams", "Bobcat_S650_skid_steer_loader_(15084521732).jpg", "2.0"),
    PhotoCredit("Shinx990", "CAT_140_M_MOTORGRADER.jpg", "4.0"),
    PhotoCredit("Matthew T Rader / MatthewTRader.com", "Caterpillar_950k_Wheel_Loader.jpg", "4.0")
)

@Composable
fun PhotoCredits() {
    val uriHandler = LocalUriHandler.current
    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.photo_credits), style = MaterialTheme.typography.titleMedium)
    credits.forEach { credit ->
        Column {
            TextButton(onClick = { uriHandler.openUri("https://commons.wikimedia.org/wiki/File:" + credit.file) }) {
                Text(stringResource(R.string.photo_credit, credit.author, "CC BY-SA " + credit.licenseVersion))
            }
            TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/by-sa/" + credit.licenseVersion + "/") }) {
                Text(stringResource(R.string.view_license))
            }
        }
    }
}
