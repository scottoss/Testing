package chat.stoat.shared.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.schemas.Status
import chat.stoat.core.model.schemas.User
import chat.stoat.shared.network.NetworkLogEntry
import chat.stoat.shared.network.SharedNetworkInspector
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.network.SharedStoatRealtime
import chat.stoat.shared.platform.PlatformInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSettingsRootScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val selfUser = SharedStoatClient.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Card(
                    onClick = { navigator.navigate(SharedRoute.ProfileSettings) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SharedAvatarCircle(
                            label = selfUser?.let { User.resolveDefaultName(it) } ?: "Me",
                            seed = selfUser?.id ?: "self",
                            size = 56.dp,
                            online = true,
                            presence = selfUser?.status?.presence
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selfUser?.let { User.resolveDefaultName(it) } ?: "My Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${selfUser?.username ?: "user"}#${selfUser?.discriminator ?: "0000"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            item { SettingsSectionHeader("USER SETTINGS") }
            item {
                SettingsMenuRow(Icons.Default.ManageAccounts, "My Account", "Email, username, password, 2FA") {
                    navigator.navigate(SharedRoute.AccountSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.AccountCircle, "User Profile", "Display name, status, pronouns, bio") {
                    navigator.navigate(SharedRoute.ProfileSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Devices, "Active Sessions", "Manage logged-in devices") {
                    navigator.navigate(SharedRoute.SessionsSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.SmartToy, "My Bots", "Create and manage Stoat bots") {
                    navigator.navigate(SharedRoute.BotsSettings)
                }
            }

            item { SettingsSectionHeader("APP SETTINGS") }
            item {
                SettingsMenuRow(Icons.Default.Palette, "Appearance", "Theme, dynamic colors, font, avatar shape") {
                    navigator.navigate(SharedRoute.AppearanceSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.AutoMirrored.Filled.Chat, "Chat & Media", "Reply gestures, embeds, compact view") {
                    navigator.navigate(SharedRoute.ChatSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.AutoMirrored.Filled.VolumeUp, "Voice & Audio", "Noise suppression, echo cancellation") {
                    navigator.navigate(SharedRoute.VoiceSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Notifications, "Notifications", "Push alerts & channel overrides") {
                    navigator.navigate(SharedRoute.NotificationsSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Language, "Language", "App display language (${SharedStoatClient.languageCode.uppercase()})") {
                    navigator.navigate(SharedRoute.LanguageSettings)
                }
            }

            item { SettingsSectionHeader("ADVANCED & DEVELOPER") }
            item {
                SettingsMenuRow(Icons.Default.Science, "Experiments", "Polar bottom-nav layout, beta features") {
                    navigator.navigate(SharedRoute.ExperimentsSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.BugReport, "Debug & Diagnostics", "WebSocket status, caches, API endpoint") {
                    navigator.navigate(SharedRoute.DebugSettings)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Security, "Network Inspector (Chucker)", "Live HTTP & WebSocket traffic log") {
                    navigator.navigate(SharedRoute.NetworkInspector)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Science, "Labs Sandboxes", "UI sandboxes, DSL, Gradient & Telecom") {
                    navigator.navigate(SharedRoute.LabsRoot)
                }
            }
            item {
                SettingsMenuRow(Icons.Default.Info, "About DawnChat", "Version ${PlatformInfo.appVersion}, Changelogs & OSS") {
                    navigator.navigate(SharedRoute.About)
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = {
                        Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    },
                    leadingContent = {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SharedStoatClient.clearSession()
                            onStatusMessage("Logged out")
                            navigator.replaceRoot(SharedRoute.LoginInit)
                        }
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp)
    )
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

// ============================================================================
// Account Settings & MFA Settings
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedAccountSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val selfUser = SharedStoatClient.currentUser
    var email by remember { mutableStateOf("Loading...") }
    var newUsername by remember(selfUser) { mutableStateOf(selfUser?.username.orEmpty()) }
    var usernamePassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var emailPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        runCatching { SharedStoatClient.fetchAccountEmail() }
            .onSuccess { email = it }
            .onFailure { email = "Unavailable" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account Email", style = MaterialTheme.typography.labelMedium)
                    Text(email, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { navigator.navigate(SharedRoute.MfaSettings) }) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Two-Factor Authentication (MFA)")
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Change Username", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("New Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernamePassword,
                        onValueChange = { usernamePassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = newUsername.isNotBlank() && usernamePassword.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.changeUsername(newUsername, usernamePassword) }
                                    .onSuccess {
                                        usernamePassword = ""
                                        onStatusMessage("Username updated!")
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Failed to update username") }
                            }
                        }
                    ) {
                        Text("Update Username")
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Change Email", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailPassword,
                        onValueChange = { emailPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = newEmail.isNotBlank() && emailPassword.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.changeEmail(newEmail, emailPassword) }
                                    .onSuccess {
                                        emailPassword = ""
                                        onStatusMessage("Email change requested! Check your inbox.")
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Failed to change email") }
                            }
                        }
                    ) {
                        Text("Update Email")
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Change Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = currentPassword.isNotBlank() && newPassword.length >= 8,
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.changePassword(newPassword, currentPassword) }
                                    .onSuccess {
                                        currentPassword = ""
                                        newPassword = ""
                                        onStatusMessage("Password changed!")
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Failed to change password") }
                            }
                        }
                    ) {
                        Text("Change Password")
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            onClick = {
                                scope.launch {
                                    runCatching { SharedStoatClient.requestAccountDeletion() }
                                        .onSuccess { onStatusMessage("Deletion confirmation email sent") }
                                        .onFailure { onStatusMessage(it.message ?: "Failed to request deletion") }
                                }
                            }
                        ) {
                            Text("Request Account Deletion")
                        }
                        OutlinedButton(onClick = { navigator.navigate(SharedRoute.TokenAction("delete", "")) }) {
                            Text("Enter Deletion Token")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMfaSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val methods = SharedStoatClient.mfaEnabledMethods
    var passwordForTicket by remember { mutableStateOf("") }
    var mfaTicketToken by remember { mutableStateOf("") }
    var totpSecret by remember { mutableStateOf<String?>(null) }
    var totpVerifyCode by remember { mutableStateOf("") }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { SharedStoatClient.fetchMfaMethods() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Two-Factor Authentication") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Enabled Methods: ${if (methods.isEmpty()) "None" else methods.joinToString(", ")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Step 1: Authorize MFA Changes", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = passwordForTicket,
                        onValueChange = { passwordForTicket = it },
                        label = { Text("Account Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = passwordForTicket.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.createMfaTicketWithPassword(passwordForTicket) }
                                    .onSuccess { ticket ->
                                        mfaTicketToken = ticket.token
                                        passwordForTicket = ""
                                        onStatusMessage("MFA Ticket Authorized")
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Failed to create MFA ticket") }
                            }
                        }
                    ) {
                        Text(if (mfaTicketToken.isNotBlank()) "Re-Authorize MFA Ticket" else "Authorize with Password")
                    }
                }
            }

            if (mfaTicketToken.isNotBlank()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Authenticator App (TOTP)", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.generateTotpSecret(mfaTicketToken) }
                                            .onSuccess { totpSecret = it }
                                            .onFailure { onStatusMessage(it.message ?: "Failed to generate secret") }
                                    }
                                }
                            ) {
                                Text("Generate TOTP Secret")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            SharedStoatClient.disableTotp(mfaTicketToken)
                                            SharedStoatClient.fetchMfaMethods()
                                        }.onSuccess { onStatusMessage("TOTP Disabled") }
                                            .onFailure { onStatusMessage(it.message ?: "Failed to disable TOTP") }
                                    }
                                }
                            ) {
                                Text("Disable TOTP")
                            }
                        }

                        totpSecret?.let { secret ->
                            Text("Secret: $secret", fontFamily = FontFamily.Monospace)
                            OutlinedTextField(
                                value = totpVerifyCode,
                                onValueChange = { totpVerifyCode = it },
                                label = { Text("Enter 6-digit code from Authenticator") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                enabled = totpVerifyCode.length >= 6,
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            SharedStoatClient.enableTotp(totpVerifyCode)
                                            SharedStoatClient.fetchMfaMethods()
                                        }.onSuccess {
                                            onStatusMessage("Authenticator TOTP enabled!")
                                        }.onFailure {
                                            onStatusMessage(it.message ?: "Failed to enable TOTP")
                                        }
                                    }
                                }
                            ) {
                                Text("Confirm & Enable TOTP")
                            }
                        }
                    }
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Recovery Codes", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.fetchRecoveryCodes(mfaTicketToken) }
                                            .onSuccess { recoveryCodes = it }
                                            .onFailure { onStatusMessage(it.message ?: "Failed to fetch recovery codes") }
                                    }
                                }
                            ) {
                                Text("View Recovery Codes")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.regenerateRecoveryCodes(mfaTicketToken) }
                                            .onSuccess {
                                                recoveryCodes = it
                                                onStatusMessage("Regenerated recovery codes")
                                            }
                                            .onFailure { onStatusMessage(it.message ?: "Failed to regenerate codes") }
                                    }
                                }
                            ) {
                                Text("Regenerate")
                            }
                        }
                        if (recoveryCodes.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = recoveryCodes.joinToString("\n"),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Profile, Sessions & Bots Settings
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedProfileSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val selfUser = SharedStoatClient.currentUser
    var displayName by remember(selfUser) { mutableStateOf(selfUser?.displayName.orEmpty()) }
    var pronouns by remember(selfUser) { mutableStateOf(selfUser?.pronouns.orEmpty()) }
    var statusText by remember(selfUser) { mutableStateOf(selfUser?.status?.text.orEmpty()) }
    var presence by remember(selfUser) { mutableStateOf(selfUser?.status?.presence ?: "Online") }
    var bio by remember(selfUser) { mutableStateOf(selfUser?.profile?.content.orEmpty()) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(selfUser?.id) {
        val uid = selfUser?.id ?: return@LaunchedEffect
        runCatching { SharedStoatClient.fetchUserProfile(uid) }
            .onSuccess { p -> if (bio.isBlank()) bio = p.content.orEmpty() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pronouns,
                onValueChange = { pronouns = it },
                label = { Text("Pronouns") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = statusText,
                onValueChange = { statusText = it },
                label = { Text("Custom Status Text") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Presence", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Online", "Idle", "Focus", "Busy", "Invisible").forEach { p ->
                    FilterChip(
                        selected = presence == p,
                        onClick = { presence = p },
                        label = { Text(p) }
                    )
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("About Me / Bio") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        val removeList = buildList {
                            if (displayName.isBlank()) add("DisplayName")
                            if (pronouns.isBlank()) add("Pronouns")
                        }
                        runCatching {
                            SharedStoatClient.patchSelf(
                                status = Status(text = statusText.takeIf { it.isNotBlank() }, presence = presence),
                                displayName = displayName.takeIf { it.isNotBlank() },
                                pronouns = pronouns.takeIf { it.isNotBlank() },
                                bio = bio,
                                remove = removeList
                            )
                        }.onSuccess {
                            isSaving = false
                            onStatusMessage("Profile saved!")
                        }.onFailure { err ->
                            isSaving = false
                            onStatusMessage(err.message ?: "Failed to save profile")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isSaving) "Saving..." else "Save Profile")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSessionsSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sessions = SharedStoatClient.sessionsList
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { SharedStoatClient.fetchAllSessions() }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Sessions") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (sessions.size > 1) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    runCatching { SharedStoatClient.revokeAllSessions(revokeSelf = false) }
                                        .onSuccess { onStatusMessage("Revoked all other sessions") }
                                }
                            }
                        ) {
                            Text("Revoke Others", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    val isCurrent = session.id == SharedStoatClient.sessionId
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.name, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (isCurrent) "Current Session (${session.id})" else session.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!isCurrent) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching { SharedStoatClient.revokeSession(session.id) }
                                                .onSuccess { onStatusMessage("Revoked session ${session.name}") }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedBotsSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val bots = SharedStoatClient.ownedBots
    var newBotName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { SharedStoatClient.fetchOwnedBots() }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Bots") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newBotName,
                    onValueChange = { newBotName = it },
                    label = { Text("New Bot Username") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    enabled = newBotName.trim().length >= 2,
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.createBot(newBotName) }
                                .onSuccess {
                                    newBotName = ""
                                    onStatusMessage("Created bot!")
                                }
                                .onFailure { onStatusMessage(it.message ?: "Failed to create bot") }
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Create")
                }
            }

            if (isLoading && bots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (bots.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You don't own any bots yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bots, key = { it.id }) { bot ->
                        val botUser = SharedStoatClient.ownedBotUsers[bot.id]
                        Card(
                            onClick = { navigator.navigate(SharedRoute.BotDetailSettings(bot.id)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SharedAvatarCircle(
                                    label = botUser?.username ?: "Bot",
                                    seed = bot.id,
                                    size = 42.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = botUser?.let { "${it.username}#${it.discriminator}" } ?: bot.id,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (bot.isPublic) "Public Bot" else "Private Bot",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedBotDetailSettingsScreen(
    botId: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val bot = SharedStoatClient.ownedBots.firstOrNull { it.id == botId }
    val botUser = SharedStoatClient.ownedBotUsers[botId]
    var username by remember(botUser) { mutableStateOf(botUser?.username.orEmpty()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(botUser?.username ?: "Bot Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (bot == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Bot not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Bot Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    enabled = username.isNotBlank(),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.renameBot(botId, username) }
                                .onSuccess { onStatusMessage("Renamed bot") }
                                .onFailure { onStatusMessage(it.message ?: "Rename failed") }
                        }
                    }
                ) {
                    Text("Save Username")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public Bot (anyone can invite)")
                    Switch(
                        checked = bot.isPublic,
                        onCheckedChange = { pub ->
                            scope.launch {
                                runCatching { SharedStoatClient.setBotPublic(botId, pub) }
                            }
                        }
                    )
                }

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("API Token", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    PlatformInfo.copyToClipboard("Bot Token", bot.token)
                                    onStatusMessage("Copied bot token")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Token")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { SharedStoatClient.resetBotToken(botId) }
                                            .onSuccess { onStatusMessage("Bot token regenerated") }
                                    }
                                }
                            ) {
                                Text("Reset Token")
                            }
                        }
                    }
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        scope.launch {
                            runCatching { SharedStoatClient.deleteBot(botId) }
                                .onSuccess {
                                    onStatusMessage("Deleted bot")
                                    navigator.pop()
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Bot")
                }
            }
        }
    }
}

// ============================================================================
// Appearance, Chat, Voice, Notifications, Language, Experiments, Debug & Inspector
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedAppearanceSettingsScreen(navigator: SharedNavigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Default", "Light", "Amoled", "M3Dynamic").forEach { mode ->
                    FilterChip(
                        selected = SharedStoatClient.themeMode == mode,
                        onClick = { SharedStoatClient.updateThemeMode(mode) },
                        label = { Text(mode) }
                    )
                }
            }

            Text("Font Family", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Default", "GoogleSansFlex", "Monospace").forEach { f ->
                    FilterChip(
                        selected = SharedStoatClient.fontName == f,
                        onClick = { SharedStoatClient.updateFontName(f) },
                        label = { Text(f) }
                    )
                }
            }

            Text(
                text = "Avatar Corner Radius: ${SharedStoatClient.avatarRadiusPercent}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = SharedStoatClient.avatarRadiusPercent.toFloat(),
                onValueChange = { SharedStoatClient.updateAvatarRadius(it.toInt()) },
                valueRange = 10f..50f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedChatSettingsScreen(navigator: SharedNavigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat & Media") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Message Reply Gesture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SwipeFromEnd", "DoubleTap", "None").forEach { style ->
                    FilterChip(
                        selected = SharedStoatClient.messageReplyStyle == style,
                        onClick = { SharedStoatClient.updateReplyStyle(style) },
                        label = { Text(style) }
                    )
                }
            }

            ToggleSettingRow("Compact Message List", SharedStoatClient.compactChatMode) {
                SharedStoatClient.updateCompactChatMode(it)
            }
            ToggleSettingRow("Show Message Timestamps", SharedStoatClient.showMessageTimestamps) {
                SharedStoatClient.updateShowMessageTimestamps(it)
            }
            ToggleSettingRow("Composer Backdrop Blur", SharedStoatClient.composerBlurEnabled) {
                SharedStoatClient.updateComposerBlur(it)
            }
            ToggleSettingRow("Interactive YouTube Embeds", SharedStoatClient.embedYouTube) {
                SharedStoatClient.updateEmbedYouTube(it)
            }
            ToggleSettingRow("Interactive Apple Music Embeds", SharedStoatClient.embedAppleMusic) {
                SharedStoatClient.updateEmbedAppleMusic(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVoiceSettingsScreen(navigator: SharedNavigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice & Audio") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleSettingRow("Noise Suppression", SharedStoatClient.voiceNoiseSuppression) {
                SharedStoatClient.updateVoiceNoiseSuppression(it)
            }
            ToggleSettingRow("Echo Cancellation", SharedStoatClient.voiceEchoCancellation) {
                SharedStoatClient.updateVoiceEchoCancellation(it)
            }
            ToggleSettingRow("Automatic Gain Control", SharedStoatClient.voiceAutoGain) {
                SharedStoatClient.updateVoiceAutoGain(it)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedNotificationsSettingsScreen(navigator: SharedNavigator) {
    val servers = SharedStoatClient.sortedServers()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ToggleSettingRow("Enable Push Notifications", SharedStoatClient.notificationsEnabled) {
                    SharedStoatClient.updateNotificationsEnabled(it)
                }
            }
            item {
                Text("Per-Server Notification Overrides", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(servers, key = { it.id ?: "" }) { srv ->
                val sid = srv.id ?: return@items
                val current = SharedStoatClient.serverNotificationOverrides[sid] ?: "All"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(srv.name ?: "Server", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All", "Mentions", "None").forEach { level ->
                                FilterChip(
                                    selected = current == level,
                                    onClick = { SharedStoatClient.serverNotificationOverrides[sid] = level },
                                    label = { Text(level) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLanguageSettingsScreen(navigator: SharedNavigator) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "pt" to "Português",
        "ja" to "日本語",
        "ko" to "한국어",
        "uk" to "Українська",
        "pl" to "Polski"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(languages, key = { it.first }) { (code, label) ->
                val selected = SharedStoatClient.languageCode == code
                Card(
                    onClick = { SharedStoatClient.updateLanguageCode(code) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text(code.uppercase(), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedExperimentsSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiments") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ToggleSettingRow("Use Polar Bottom-Tab Layout", SharedStoatClient.usePolarLayout) { enabled ->
                SharedStoatClient.updateUsePolarLayout(enabled)
                onStatusMessage(if (enabled) "Switched to Polar Layout" else "Switched to Classic Drawer Layout")
            }
            ToggleSettingRow("Enable Avatar & Media Pickers", SharedStoatClient.enableAvatarPickers) {
                SharedStoatClient.updateAvatarPickers(it)
            }
            ToggleSettingRow("Enable In-App Community Discover", SharedStoatClient.enableInAppDiscover) {
                SharedStoatClient.updateInAppDiscover(it)
            }
            ToggleSettingRow("Enable KLIPY GIF Provider", SharedStoatClient.enableKlipyGifs) {
                SharedStoatClient.updateKlipyGifs(it)
            }

            Button(
                onClick = {
                    navigator.replaceRoot(
                        if (SharedStoatClient.usePolarLayout) SharedRoute.PolarMain else SharedRoute.ChatRouter
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Layout Now")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedDebugSettingsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var apiInput by remember { mutableStateOf(SharedStoatClient.apiBaseUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Platform: ${PlatformInfo.platformName} (${PlatformInfo.osVersion})", fontWeight = FontWeight.Bold)
                    Text("WebSocket State: ${SharedStoatRealtime.state}")
                    Text("WebSocket URL: ${SharedStoatClient.wsUrl}")
                    Text("Cached Users: ${SharedStoatClient.userCache.size}")
                    Text("Cached Servers: ${SharedStoatClient.serverCache.size}")
                    Text("Cached Channels: ${SharedStoatClient.channelCache.size}")
                    Text("Cached Emojis: ${SharedStoatClient.emojiCache.size}")
                    SharedStoatRealtime.lastError?.let {
                        Text("Last WS Error: $it", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        SharedStoatRealtime.connect(SharedStoatClient.sessionToken)
                        onStatusMessage("Reconnecting WebSocket...")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reconnect WS")
                }
                OutlinedButton(
                    onClick = { navigator.navigate(SharedRoute.NetworkInspector) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Network Inspector")
                }
            }

            OutlinedTextField(
                value = apiInput,
                onValueChange = { apiInput = it },
                label = { Text("API Base URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        SharedStoatClient.updateApiBaseUrl(apiInput)
                        SharedStoatClient.fetchRootConfig(force = true)
                        onStatusMessage("Updated API Base URL")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Refresh Root Config")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedNetworkInspectorScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val entries = SharedNetworkInspector.entries
    var selectedEntry by remember { mutableStateOf<NetworkLogEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Inspector (${entries.size})") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { SharedNetworkInspector.clear() }) {
                        Text("Clear")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No HTTP or WebSocket events recorded yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    Card(
                        onClick = { selectedEntry = entry },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${entry.method} • ${entry.statusCode ?: "ERR"}",
                                    fontWeight = FontWeight.Bold,
                                    color = if ((entry.statusCode ?: 500) in 200..299) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                Text(
                                    text = "${entry.durationMs} ms",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = entry.url,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.method) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("URL: ${entry.url}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall)
                    Text("Status: ${entry.statusCode ?: "Error"} (${entry.durationMs} ms)")
                    entry.error?.let {
                        Text("Error: $it", color = MaterialTheme.colorScheme.error)
                    }
                    if (!entry.requestBody.isNullOrBlank()) {
                        Text("Request Body:", fontWeight = FontWeight.Bold)
                        Text(entry.requestBody, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!entry.responseBody.isNullOrBlank()) {
                        Text("Response / Frame Payload:", fontWeight = FontWeight.Bold)
                        Text(entry.responseBody, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        PlatformInfo.copyToClipboard("NetworkEntry", entry.responseBody ?: entry.requestBody ?: entry.url)
                        onStatusMessage("Copied payload")
                        selectedEntry = null
                    }
                ) {
                    Text("Copy Payload")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEntry = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
