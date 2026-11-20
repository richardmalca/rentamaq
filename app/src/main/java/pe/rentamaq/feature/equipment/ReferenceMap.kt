package pe.rentamaq.feature.equipment

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import pe.rentamaq.R

private const val mapPage = "file:///android_asset/leaflet-map/index.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReferenceMap() {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.userAgentString += " Rentamaq/0.1 (+https://github.com/richardmalca/rentamaq)"
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url
                            if (url.scheme == "https" && url.host == "www.openstreetmap.org") {
                                uriHandler.openUri(url.toString())
                            }
                            return true
                        }
                    }
                    loadUrl(mapPage)
                }
            },
            onRelease = { it.destroy() }
        )
        Text(stringResource(R.string.map_area_explanation), style = MaterialTheme.typography.bodySmall)
    }
}
