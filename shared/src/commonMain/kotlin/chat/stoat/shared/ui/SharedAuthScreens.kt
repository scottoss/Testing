package chat.stoat.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.data.STOAT_BASE
import chat.stoat.core.model.data.STOAT_MARKETING
import chat.stoat.core.model.schemas.Alert
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.network.SharedStoatRealtime
import chat.stoat.shared.platform.PlatformInfo
import kotlinx.coroutines.launch

@Composable
fun SharedHealthAlertBanner(alert: Alert, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = alert.text ?: "Service Notice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                alert.actions.orEmpty().forEach { action ->
                    OutlinedButton(
                        onClick = {
                            action.href?.let { PlatformInfo.openExternalUrl(it) }
                        }
                    ) {
                        Text(action.text ?: "Open")
                    }
                }
                if (alert.dismissible != false) {
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}

@Composable
fun SharedDefaultDestinationScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isConnecting by remember { mutableStateOf(true) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var showCouldNotLoginDialog by remember { mutableStateOf(false) }
    var dismissedAlert by remember { mutableStateOf(false) }

    fun attemptStartup() {
        scope.launch {
            isConnecting = true
            connectionError = null
            runCatching {
                SharedStoatClient.fetchRootConfig(force = true)
                SharedStoatClient.fetchHealthNotice()
            }

            if (!SharedStoatClient.isLoggedIn) {
                isConnecting = false
                navigator.replaceRoot(SharedRoute.LoginInit)
                return@launch
            }

            runCatching {
                SharedStoatClient.fetchSelf()
            }.onSuccess {
                val needsOnboarding = runCatching {
                    SharedStoatClient.checkNeedsOnboarding()
                }.getOrDefault(false)

                if (needsOnboarding) {
                    isConnecting = false
                    navigator.replaceRoot(SharedRoute.Onboarding)
                } else {
                    SharedStoatRealtime.connect(SharedStoatClient.sessionToken)
                    isConnecting = false
                    if (SharedStoatClient.usePolarLayout) {
                        navigator.replaceRoot(SharedRoute.PolarMain)
                    } else {
                        navigator.replaceRoot(SharedRoute.ChatRouter)
                    }
                }
            }.onFailure { err ->
                isConnecting = false
                val msg = err.message ?: "Unable to connect to Stoat API"
                if (msg.contains("401") || msg.contains("Unauthorized", ignoreCase = true) || msg.contains("InvalidSession", ignoreCase = true)) {
                    showCouldNotLoginDialog = true
                } else {
                    connectionError = msg
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        attemptStartup()
    }

    val alert = SharedStoatClient.healthNotice?.alert

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (alert != null && !dismissedAlert) {
                SharedHealthAlertBanner(alert = alert, onDismiss = { dismissedAlert = true })
            }

            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(44.dp))
                Text(
                    text = "Connecting to DawnChat...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = SharedStoatClient.apiBaseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (connectionError != null) {
                SharedDisconnectedScreen(
                    errorMessage = connectionError!!,
                    onRetry = { attemptStartup() },
                    onOpenNetworkInspector = { navigator.navigate(SharedRoute.NetworkInspector) },
                    onLogout = {
                        SharedStoatClient.clearSession()
                        navigator.replaceRoot(SharedRoute.LoginInit)
                    }
                )
            }
        }
    }

    if (showCouldNotLoginDialog) {
        AlertDialog(
            onDismissRequest = { showCouldNotLoginDialog = false },
            title = { Text("Session Expired") },
            text = {
                Text("Your session is no longer valid or could not be authenticated. Please sign in again.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCouldNotLoginDialog = false
                        SharedStoatClient.clearSession()
                        navigator.replaceRoot(SharedRoute.LoginInit)
                    }
                ) {
                    Text("Sign In Again")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCouldNotLoginDialog = false
                        attemptStartup()
                    }
                ) {
                    Text("Retry")
                }
            }
        )
    }
}

@Composable
fun SharedDisconnectedScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenNetworkInspector: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Disconnected from Stoat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retry Connection")
            }
            OutlinedButton(
                onClick = onOpenNetworkInspector,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Network Inspector")
            }
            TextButton(onClick = onLogout) {
                Text("Log Out / Switch Account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedInitScreen(
    navigator: SharedNavigator
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DC",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Text(
                text = "Welcome to DawnChat",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Connect with communities, friends, and voice channels in real time.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { navigator.navigate(SharedRoute.Login) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Sign in to your account", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = { navigator.navigate(SharedRoute.RegisterGreeting) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Create a new account", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(onClick = { navigator.navigate(SharedRoute.LoginGreeting) }) {
                Text("Classic Login Greeting")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = { PlatformInfo.openExternalUrl("$STOAT_MARKETING/terms") }) {
                    Text("Terms of Service", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { PlatformInfo.openExternalUrl("$STOAT_MARKETING/privacy") }) {
                    Text("Privacy Policy", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { PlatformInfo.openExternalUrl("$STOAT_MARKETING/Acceptable-Use-Policy") }) {
                    Text("Community Guidelines", style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { navigator.navigate(SharedRoute.NetworkInspector) }
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Network Inspector")
                }
                OutlinedButton(
                    onClick = { navigator.navigate(SharedRoute.LabsRoot) }
                ) {
                    Text("Labs")
                }
            }
        }
    }
}

@Composable
fun SharedLoginGreetingScreen(navigator: SharedNavigator) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DawnChat",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your conversations, communities, and friends — everywhere.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { navigator.navigate(SharedRoute.Login) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(50.dp)
            ) {
                Text("Log In")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { navigator.navigate(SharedRoute.RegisterGreeting) },
                modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp).height(50.dp)
            ) {
                Text("Register")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { navigator.pop() }) {
                Text("Back")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLoginScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var customInstanceUrl by remember { mutableStateOf(SharedStoatClient.apiBaseUrl) }
    var showInstanceEditor by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Welcome back!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sign in with your DawnChat / Stoat credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { navigator.navigate(SharedRoute.AccountRecovery("password")) }) {
                        Text("Forgot Password?")
                    }
                    TextButton(onClick = { navigator.navigate(SharedRoute.AccountRecovery("verify")) }) {
                        Text("Resend Verification")
                    }
                }

                errorMessage?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            if (customInstanceUrl.isNotBlank() && customInstanceUrl != SharedStoatClient.apiBaseUrl) {
                                SharedStoatClient.updateApiBaseUrl(customInstanceUrl)
                                SharedStoatClient.fetchRootConfig(force = true)
                            }
                            val assessment = SharedStoatClient.login(email, password)
                            if (assessment.proceedMfa && assessment.mfaSpec != null) {
                                isLoading = false
                                navigator.navigate(
                                    SharedRoute.LoginMfa(
                                        mfaTicket = assessment.mfaSpec.ticket,
                                        allowedMethods = assessment.mfaSpec.allowedMethods
                                    )
                                )
                            } else if (assessment.success) {
                                isLoading = false
                                onStatusMessage("Signed in!")
                                navigator.replaceRoot(SharedRoute.DefaultDestination)
                            } else {
                                isLoading = false
                                errorMessage = assessment.error ?: "Login failed"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (isLoading) "Signing In..." else "Sign In")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showInstanceEditor = !showInstanceEditor }) {
                        Text(if (showInstanceEditor) "Hide Server Instance" else "Custom Server Instance")
                    }
                    TextButton(onClick = { navigator.navigate(SharedRoute.ConfirmPasswordReset("")) }) {
                        Text("Have a Reset Token?")
                    }
                }

                if (showInstanceEditor) {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Stoat API Base URL",
                                style = MaterialTheme.typography.labelLarge
                            )
                            OutlinedTextField(
                                value = customInstanceUrl,
                                onValueChange = { customInstanceUrl = it },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        customInstanceUrl = STOAT_BASE
                                        SharedStoatClient.updateApiBaseUrl(STOAT_BASE)
                                    }
                                ) {
                                    Text("Reset to Default")
                                }
                                Button(
                                    onClick = {
                                        SharedStoatClient.updateApiBaseUrl(customInstanceUrl)
                                        onStatusMessage("Updated API URL to ${SharedStoatClient.apiBaseUrl}")
                                    }
                                ) {
                                    Text("Apply")
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
fun SharedMfaScreen(
    mfaTicket: String,
    allowedMethods: List<String>,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val methods = remember(allowedMethods) {
        allowedMethods.ifEmpty { listOf("Totp", "Recovery") }
    }
    var selectedMethod by remember { mutableStateOf(methods.first()) }
    var codeInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Verify Your Identity",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select an authentication method and enter your verification code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { method ->
                        FilterChip(
                            selected = selectedMethod.equals(method, ignoreCase = true),
                            onClick = {
                                selectedMethod = method
                                codeInput = ""
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    when (method.lowercase()) {
                                        "totp" -> "Authenticator App"
                                        "recovery" -> "Recovery Code"
                                        "password" -> "Password"
                                        else -> method
                                    }
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it; errorMessage = null },
                    label = {
                        Text(
                            when (selectedMethod.lowercase()) {
                                "totp" -> "6-Digit Authenticator Code"
                                "recovery" -> "Recovery Code (e.g. xxxx-xxxx)"
                                else -> "Verification Secret"
                            }
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    enabled = !isSubmitting && codeInput.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorMessage = null
                            val result = when (selectedMethod.lowercase()) {
                                "recovery" -> SharedStoatClient.loginWithMfaRecovery(mfaTicket, codeInput)
                                "password" -> SharedStoatClient.loginWithMfaPassword(mfaTicket, codeInput)
                                else -> SharedStoatClient.loginWithMfaTotp(mfaTicket, codeInput)
                            }
                            isSubmitting = false
                            if (result.success) {
                                onStatusMessage("MFA verified!")
                                navigator.replaceRoot(SharedRoute.DefaultDestination)
                            } else {
                                errorMessage = result.error ?: "Invalid MFA code"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(if (isSubmitting) "Verifying..." else "Verify & Sign In")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedAccountRecoveryScreen(
    mode: String, // "password" or "verify"
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var successText by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isPasswordReset = mode == "password"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isPasswordReset) "Reset Password" else "Resend Verification") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isPasswordReset) {
                        "Enter the email address associated with your account and we'll send a password reset link."
                    } else {
                        "Enter your email address to receive a new verification email."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                successText?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    enabled = !isSubmitting && email.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            successText = null
                            runCatching {
                                if (isPasswordReset) {
                                    SharedStoatClient.requestPasswordReset(email)
                                } else {
                                    SharedStoatClient.resendVerificationEmail(email)
                                }
                            }.onSuccess {
                                isSubmitting = false
                                successText = if (isPasswordReset) {
                                    "Password reset email sent! If you have the reset token, tap below to confirm."
                                } else {
                                    "Verification email sent! Check your inbox."
                                }
                                onStatusMessage("Email request sent")
                            }.onFailure { err ->
                                isSubmitting = false
                                errorText = err.message ?: "Request failed"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (isSubmitting) "Sending..." else "Send Email")
                }

                if (isPasswordReset) {
                    OutlinedButton(
                        onClick = { navigator.navigate(SharedRoute.ConfirmPasswordReset("")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Password Reset Token")
                    }
                } else {
                    OutlinedButton(
                        onClick = { navigator.navigate(SharedRoute.TokenAction("verify", "")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Verification Token")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedConfirmPasswordResetScreen(
    initialToken: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(initialToken) }
    var newPassword by remember { mutableStateOf("") }
    var removeSessions by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Password Reset") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; errorText = null },
                    label = { Text("Reset Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorText = null },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { removeSessions = !removeSessions }
                ) {
                    Checkbox(checked = removeSessions, onCheckedChange = { removeSessions = it })
                    Text("Sign out of all existing sessions")
                }

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    enabled = !isSubmitting && token.isNotBlank() && newPassword.length >= 8,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            runCatching {
                                SharedStoatClient.confirmPasswordReset(token, newPassword, removeSessions)
                            }.onSuccess {
                                isSubmitting = false
                                onStatusMessage("Password reset complete! Please sign in.")
                                navigator.replaceRoot(SharedRoute.Login)
                            }.onFailure { err ->
                                isSubmitting = false
                                errorText = err.message ?: "Failed to reset password"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (isSubmitting) "Resetting..." else "Confirm New Password")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTokenActionScreen(
    mode: String, // "verify" or "delete"
    initialToken: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(initialToken) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val isVerify = mode == "verify"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVerify) "Verify Email Token" else "Confirm Account Deletion") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isVerify) {
                        "Paste the verification token from your email to activate your account."
                    } else {
                        "Paste the account deletion confirmation token from your email."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it; errorText = null },
                    label = { Text("Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = !isSubmitting && token.isNotBlank(),
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            runCatching {
                                if (isVerify) {
                                    SharedStoatClient.verifyEmailToken(token)
                                } else {
                                    SharedStoatClient.confirmAccountDeletion(token)
                                }
                            }.onSuccess {
                                isSubmitting = false
                                if (isVerify) {
                                    onStatusMessage("Email verified! You can now sign in.")
                                    navigator.replaceRoot(SharedRoute.Login)
                                } else {
                                    SharedStoatClient.clearSession()
                                    onStatusMessage("Account deletion confirmed.")
                                    navigator.replaceRoot(SharedRoute.LoginInit)
                                }
                            }.onFailure { err ->
                                isSubmitting = false
                                errorText = err.message ?: "Token action failed"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (isSubmitting) "Processing..." else if (isVerify) "Verify Account" else "Confirm Deletion")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedRegisterGreetingScreen(navigator: SharedNavigator) {
    var acceptedTerms by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Before You Join DawnChat",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "By creating an account, you agree to treat others with respect and follow the DawnChat Terms of Service, Privacy Policy, and Acceptable Use Policy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acceptedTerms = !acceptedTerms }
                        .padding(vertical = 8.dp)
                ) {
                    Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
                    Text(
                        text = "I have read and agree to the Terms of Service and Community Guidelines",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Button(
                    enabled = acceptedTerms,
                    onClick = { navigator.navigate(SharedRoute.RegisterDetails) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedRegisterDetailsScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val inviteOnly = SharedStoatClient.rootConfig?.features?.inviteOnly == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorText = null },
                    label = { Text("Email Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorText = null },
                    label = { Text("Password (min 8 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorText = null },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (inviteOnly) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it; errorText = null },
                        label = { Text("Server Registration Invite Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    enabled = !isSubmitting && email.isNotBlank() && password.length >= 8 && password == confirmPassword,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            runCatching {
                                SharedStoatClient.registerAccount(
                                    email = email,
                                    password = password,
                                    invite = inviteCode.takeIf { it.isNotBlank() }
                                )
                            }.onSuccess {
                                isSubmitting = false
                                onStatusMessage("Account created! Please check your email.")
                                navigator.navigate(SharedRoute.RegisterVerify(email.trim()))
                            }.onFailure { err ->
                                isSubmitting = false
                                errorText = err.message ?: "Registration failed"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isSubmitting) "Creating Account..." else "Create Account")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedRegisterVerifyScreen(
    email: String,
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var manualToken by remember { mutableStateOf("") }
    var isResending by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Your Email") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Check Your Inbox",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "We sent a verification link to $email. Once verified, you can sign in or paste the token below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = manualToken,
                    onValueChange = { manualToken = it },
                    label = { Text("Verification Token (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (manualToken.isNotBlank()) {
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.verifyEmailToken(manualToken) }
                                    .onSuccess {
                                        onStatusMessage("Email verified!")
                                        navigator.replaceRoot(SharedRoute.Login)
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Verification failed") }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Token")
                    }
                }

                Button(
                    onClick = { navigator.replaceRoot(SharedRoute.Login) },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Proceed to Sign In")
                }

                OutlinedButton(
                    enabled = !isResending,
                    onClick = {
                        scope.launch {
                            isResending = true
                            runCatching { SharedStoatClient.resendVerificationEmail(email) }
                                .onSuccess { onStatusMessage("Verification email resent") }
                                .onFailure { onStatusMessage(it.message ?: "Failed to resend") }
                            isResending = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isResending) "Resending..." else "Resend Verification Email")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedOnboardingScreen(
    navigator: SharedNavigator,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Username") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Welcome to DawnChat! Pick a username so your friends can find you.",
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it; errorText = null },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    enabled = !isSubmitting && username.trim().length >= 2,
                    onClick = {
                        scope.launch {
                            isSubmitting = true
                            errorText = null
                            runCatching {
                                SharedStoatClient.completeOnboarding(username)
                            }.onSuccess {
                                isSubmitting = false
                                onStatusMessage("Welcome, $username!")
                                navigator.replaceRoot(SharedRoute.DefaultDestination)
                            }.onFailure { err ->
                                isSubmitting = false
                                errorText = err.message ?: "Failed to set username"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isSubmitting) "Saving..." else "Complete Setup")
                }
            }
        }
    }
}
