package pe.rentamaq.feature.equipment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.rentamaq.core.data.local.Equipment

fun Equipment.photoUris(): List<String> = photos.orEmpty().lineSequence().filter { it.isNotBlank() }.toList()

@Composable
fun StoredEquipmentPhoto(uri: String, description: String, modifier: Modifier, fallback: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, uri, resolver) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val source = Uri.parse(uri)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1400) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
    }
    val loaded = bitmap
    if (loaded == null) fallback() else Image(loaded.asImageBitmap(), description, modifier, contentScale = ContentScale.Crop)
}
