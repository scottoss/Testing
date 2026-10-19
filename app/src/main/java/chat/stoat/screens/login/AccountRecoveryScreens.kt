package chat.stoat.screens.login

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import chat.stoat.DawnChatApplication
import chat.stoat.R
import chat.stoat.api.routes.account.confirmAccountDeletion
import chat.stoat.api.routes.account.confirmPasswordReset
import chat.stoat.api.routes.account.verifyEmailToken
import chat.stoat.api.routes.account.requestPasswordReset
import chat.stoat.api.routes.account.resendVerification
import chat.stoat.api.routes.misc.getRootRoute
import chat.stoat.composables.generic.FormTextField
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.HCaptchaTheme
import kotlinx.coroutines.launch

class AccountRecoveryViewModel : ViewModel() {
    var email by mutableStateOf("")
    var error by mutableStateOf<String?>(null)
    var sent by mutableStateOf(false)
    var busy by mutableStateOf(false)

    private fun captcha(context: Context, onToken: (String) -> Unit) {
        viewModelScope.launch {
            val root = try {
                getRootRoute()
            } catch (e: Exception) {
                error = if (e.message?.startsWith("Expected response body of the type") == true) {
                    DawnChatApplication.instance.getString(R.string.service_health_alert_body_default)
                } else e.message
                busy = false
                return@launch
            }

            if (!root.features.captcha.enabled) {
                onToken("")
                return@launch
            }

            val config = HCaptchaConfig.builder().apply {
                siteKey(root.features.captcha.key)
                theme(HCaptchaTheme.DARK)
                size(HCaptchaSize.INVISIBLE)
            }.build()

            HCaptcha.getClient(context).apply {
                addOnSuccessListener { onToken(it.tokenResult) }
                addOnFailureListener {
                    error = it.message
                    busy = false
                }
                setup(config)
                verifyWithHCaptcha()
            }
        }
    }

    fun submit(context: Context, resend: Boolean) {
        error = null
        busy = true
        captcha(context) { token ->
            viewModelScope.launch {
                try {
                    if (resend) resendVerification(email.trim(), token)
                    else requestPasswordReset(email.trim(), token)
                    sent = true
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                } finally {
                    busy = false
                }
            }
        }
    }
}

@Composable
fun AccountRecoveryScreen(
    navController: NavController,
    resend: Boolean,
    viewModel: AccountRecoveryViewModel = viewModel()
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .imePadding()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (resend) R.string.resend_verification else R.string.password_forgot_heading
                ),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    when {
                        viewModel.sent -> R.string.recovery_email_sent
                        resend -> R.string.resend_verification_instructions
                        else -> R.string.password_forgot_instructions
                    }
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (!viewModel.sent) {
                FormTextField(
                    value = viewModel.email,
                    onChange = { viewModel.email = it },
                    label = stringResource(R.string.register_email),
                    type = KeyboardType.Email,
                    action = ImeAction.Done,
                    enabled = !viewModel.busy
                )
            }

            if (!viewModel.error.isNullOrBlank()) {
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (viewModel.sent && !resend) {
                TextButton(onClick = { navController.navigate("login/reset/confirm") }) {
                    Text(text = stringResource(R.string.reset_have_token))
                }
            }
        }

        Row {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(text = stringResource(R.string.back))
            }

            if (!viewModel.sent) {
                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = { viewModel.submit(context, resend) },
                    enabled = viewModel.email.isNotBlank() && !viewModel.busy
                ) {
                    Text(text = stringResource(R.string.send_email))
                }
            }
        }
    }
}

class ConfirmPasswordResetViewModel : ViewModel() {
    var token by mutableStateOf("")
    var password by mutableStateOf("")
    var removeSessions by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var done by mutableStateOf(false)
    var busy by mutableStateOf(false)

    fun submit() {
        error = null
        busy = true
        viewModelScope.launch {
            try {
                confirmPasswordReset(token.trim(), password, removeSessions)
                done = true
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun ConfirmPasswordResetScreen(
    navController: NavController,
    initialToken: String = "",
    viewModel: ConfirmPasswordResetViewModel = viewModel()
) {
    if (viewModel.token.isEmpty() && initialToken.isNotEmpty()) viewModel.token = initialToken

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .imePadding()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reset_new_password_heading),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(
                    if (viewModel.done) R.string.reset_success else R.string.reset_new_password_instructions
                ),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                ),
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (!viewModel.done) {
                FormTextField(
                    value = viewModel.token,
                    onChange = { viewModel.token = it },
                    label = stringResource(R.string.reset_token_label),
                    action = ImeAction.Next,
                    enabled = !viewModel.busy
                )

                Spacer(modifier = Modifier.height(10.dp))

                FormTextField(
                    value = viewModel.password,
                    onChange = { viewModel.password = it },
                    label = stringResource(R.string.register_password),
                    type = KeyboardType.Password,
                    action = ImeAction.Done,
                    enabled = !viewModel.busy
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = viewModel.removeSessions,
                        onCheckedChange = { viewModel.removeSessions = it }
                    )
                    Text(text = stringResource(R.string.reset_log_out_sessions))
                }
            }

            if (!viewModel.error.isNullOrBlank()) {
                Text(
                    text = viewModel.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 40.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        Row {
            TextButton(onClick = { navController.popBackStack() }) {
                Text(text = stringResource(R.string.back))
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (viewModel.done) {
                Button(onClick = {
                    navController.navigate("login/login") {
                        popUpTo("login/greeting")
                    }
                }) {
                    Text(text = stringResource(R.string.login))
                }
            } else {
                Button(
                    onClick = { viewModel.submit() },
                    enabled = viewModel.token.isNotBlank() &&
                            viewModel.password.isNotBlank() && !viewModel.busy
                ) {
                    Text(text = stringResource(R.string.reset_password_action))
                }
            }
        }
    }
}

class TokenActionViewModel : ViewModel() {
    var state by mutableStateOf<Boolean?>(null)
    private var started = false

    fun run(delete: Boolean, token: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            state = try {
                if (delete) confirmAccountDeletion(token) else verifyEmailToken(token)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}

@Composable
fun TokenActionScreen(
    navController: NavController,
    delete: Boolean,
    token: String,
    viewModel: TokenActionViewModel = viewModel()
) {
    androidx.compose.runtime.LaunchedEffect(token) { viewModel.run(delete, token) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (delete) R.string.auth_delete_heading else R.string.auth_verify_heading
            ),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(
                when {
                    viewModel.state == null -> R.string.auth_token_working
                    viewModel.state == false -> R.string.auth_token_error
                    delete -> R.string.auth_delete_success
                    else -> R.string.auth_verify_success
                }
            ),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = {
            navController.navigate("default") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }) {
            Text(text = stringResource(R.string.auth_token_continue))
        }
    }
}
