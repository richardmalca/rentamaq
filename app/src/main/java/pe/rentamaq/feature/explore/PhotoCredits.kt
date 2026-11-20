package pe.rentamaq.feature.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R

private data class PhotoCredit(val image: Int, val author: String, val file: String, val licenseVersion: String, val shareAlike: Boolean = true)

private val credits = listOf(
    PhotoCredit(R.drawable.excavator, "Bene Riobó", "Caterpillar_320.jpg", "4.0"),
    PhotoCredit(R.drawable.excavator_side, "Justin Smith", "CAT_320.excavator.jpg", "3.0"),
    PhotoCredit(R.drawable.backhoe, "Andrew Butko", "JCB_3CX_backhoe_loader.jpg", "3.0"),
    PhotoCredit(R.drawable.backhoe_side, "hodihu", "JCB_3CX_backhoe_(2).jpg", "2.0", shareAlike = false),
    PhotoCredit(R.drawable.roller, "Reinhold Möller", "ICE-Baustelle-BOMAG-roadroller-P3209764.jpg", "4.0"),
    PhotoCredit(R.drawable.skid_steer, "Bob Adams", "Bobcat_S650_skid_steer_loader_(15084521732).jpg", "2.0"),
    PhotoCredit(R.drawable.grader, "Shinx990", "CAT_140_M_MOTORGRADER.jpg", "4.0"),
    PhotoCredit(R.drawable.loader, "Matthew T Rader / MatthewTRader.com", "Caterpillar_950k_Wheel_Loader.jpg", "4.0")
)

@Composable
fun PhotoCredits() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.photo_credits_intro), style = MaterialTheme.typography.bodyMedium)
        credits.forEach { credit ->
            val license = if (credit.shareAlike) "by-sa" else "by"
            val licenseName = if (credit.shareAlike) "CC BY-SA" else "CC BY"
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(credit.image), null, Modifier.size(66.dp, 52.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop)
                Column(Modifier.weight(1f)) {
                    Text(credit.author, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("$licenseName ${credit.licenseVersion}", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { uriHandler.openUri("https://commons.wikimedia.org/wiki/File:" + credit.file) },
                            contentPadding = PaddingValues(horizontal = 6.dp)) { Text(stringResource(R.string.photo_source)) }
                        TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/$license/${credit.licenseVersion}/") },
                            contentPadding = PaddingValues(horizontal = 6.dp)) { Text(stringResource(R.string.view_license)) }
                    }
                }
            }
        }
    }
}
