package pe.rentamaq.feature.equipment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.rentamaq.R
import pe.rentamaq.core.data.local.Equipment
import java.io.OutputStream

@Composable
fun ServiceDocumentActions(machine: Equipment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var feedback by remember { mutableStateOf<Int?>(null) }
    val title = stringResource(R.string.maintenance)
    val details = listOfNotNull(
        stringResource(R.string.maintenance_metadata, machine.serviceDate.orEmpty(), machine.serviceHours ?: 0),
        machine.serviceProvider?.let { stringResource(R.string.service_provider_summary, it) },
        machine.serviceNotes,
        machine.nextServiceDate?.let { stringResource(R.string.next_service_summary, it) }
    )
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { result ->
        if (result != null) scope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(result)?.use { writeServicePdf(it, title, machine.name, details) }
                        ?: error("Unavailable document destination")
                }.isSuccess
            }
            feedback = if (success) R.string.service_pdf_saved else R.string.service_pdf_failed
        }
    }
    machine.serviceDocument?.let { document ->
        TextButton(onClick = {
            val intent = Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(document), "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            feedback = if (runCatching { context.startActivity(intent) }.isSuccess) null else R.string.service_document_unavailable
        }) { Text(stringResource(R.string.open_service_document)) }
    }
    TextButton(onClick = { export.launch("rentamaq-mantenimiento.pdf") }) {
        Text(stringResource(R.string.export_service_document))
    }
    feedback?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
}

private fun writeServicePdf(output: OutputStream, title: String, name: String, details: List<String>) {
    val document = PdfDocument()
    try {
        val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var top = 44f
        (listOf(title, name) + details).forEachIndexed { index, text ->
            val paint = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(35, 37, 24)
                textSize = if (index == 0) 24f else 14f
                typeface = if (index < 2) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, 507)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            page.canvas.save()
            page.canvas.translate(44f, top)
            layout.draw(page.canvas)
            page.canvas.restore()
            top += layout.height + 14f
        }
        document.finishPage(page)
        document.writeTo(output)
    } finally {
        document.close()
    }
}
