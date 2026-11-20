package pe.rentamaq.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.rentamaq.R

enum class AccountEntry { OPTIONS, SIGN_IN, REGISTER }

@Composable
fun AccountAccessPanel(title: Int, description: Int, onSelect: (AccountEntry) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_account), null, Modifier.size(23.dp))
            }
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(description), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { onSelect(AccountEntry.SIGN_IN) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp),
                shape = MaterialTheme.shapes.small
            ) { Text(stringResource(R.string.sign_in), fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { onSelect(AccountEntry.REGISTER) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 49.dp),
                shape = MaterialTheme.shapes.small
            ) { Text(stringResource(R.string.create_account), fontWeight = FontWeight.SemiBold) }
        }
    }
}
