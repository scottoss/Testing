package chat.stoat.shared.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
actual fun PlatformQrScannerView(
    modifier: Modifier,
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    var manualPayload by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("ios_qr_scanner_stub_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "iOS QR Scanner Stub",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "iOS Camera QR Scanner (Stub)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CameraX scanning is Android-specific. Paste a friend link, ULID, or username#discriminator below on iOS:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = manualPayload,
                onValueChange = { manualPayload = it },
                label = { Text("Friend QR link or username#0000") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ios_qr_manual_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("ios_qr_cancel_button")
                ) {
                    Text("Close")
                }
                Button(
                    onClick = {
                        if (manualPayload.isNotBlank()) {
                            onQrCodeScanned(manualPayload.trim())
                        }
                    },
                    enabled = manualPayload.isNotBlank(),
                    modifier = Modifier.testTag("ios_qr_submit_button")
                ) {
                    Text("Parse & Add")
                }
            }
        }
    }
}
