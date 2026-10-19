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

object AndroidQrScannerBridge {
    var customScannerContent: (@Composable (
        modifier: Modifier,
        onQrCodeScanned: (String) -> Unit,
        onClose: () -> Unit
    ) -> Unit)? = null
}

@Composable
actual fun PlatformQrScannerView(
    modifier: Modifier,
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val customRenderer = AndroidQrScannerBridge.customScannerContent
    if (customRenderer != null) {
        customRenderer(modifier, onQrCodeScanned, onClose)
        return
    }

    var manualInput by remember { mutableStateOf("") }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("android_qr_scanner_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "QR Code Scanner",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scan or Enter Friend QR Payload",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                label = { Text("Friend URL, ULID, or username#0000") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_manual_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("qr_close_button")
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            onQrCodeScanned(manualInput.trim())
                        }
                    },
                    enabled = manualInput.isNotBlank(),
                    modifier = Modifier.testTag("qr_submit_button")
                ) {
                    Text("Add Friend")
                }
            }
        }
    }
}
