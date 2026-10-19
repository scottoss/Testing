package chat.stoat.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import chat.stoat.R
import chat.stoat.composables.generic.ListHeader
import chat.stoat.composables.generic.RadioItem
import chat.stoat.voice.ScreenShareQuality
import chat.stoat.voice.VoiceCallManager
import chat.stoat.voice.VoiceOutputRoute
import chat.stoat.voice.VoiceSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(navController: NavController) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { VoiceSettings.ensureLoaded() }

    fun save(change: () -> Unit) {
        change()
        scope.launch { VoiceSettings.persist() }
    }

    @Composable
    fun SwitchItem(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
        ListItem(
            headlineContent = { Text(stringResource(label)) },
            trailingContent = { Switch(checked = checked, onCheckedChange = null) },
            modifier = Modifier.clickable { onChange(!checked) }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings_voice),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
            )
        },
    ) { pv ->
        Column(
            Modifier
                .padding(pv)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ListHeader { Text(stringResource(R.string.settings_voice_processing)) }
            SwitchItem(R.string.settings_voice_noise_suppression, VoiceSettings.noiseSuppression) {
                save { VoiceSettings.noiseSuppression = it }
            }
            SwitchItem(R.string.settings_voice_echo_cancellation, VoiceSettings.echoCancellation) {
                save { VoiceSettings.echoCancellation = it }
            }
            SwitchItem(R.string.settings_voice_auto_gain_control, VoiceSettings.autoGainControl) {
                save { VoiceSettings.autoGainControl = it }
            }
            Text(
                stringResource(R.string.settings_voice_processing_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListHeader { Text(stringResource(R.string.settings_voice_output)) }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_voice_output_volume)) },
                supportingContent = {
                    Slider(
                        value = VoiceSettings.outputVolume,
                        onValueChange = { VoiceSettings.outputVolume = it },
                        onValueChangeFinished = {
                            VoiceCallManager.applyOutputSettings()
                            scope.launch { VoiceSettings.persist() }
                        },
                        valueRange = 0f..1f,
                    )
                }
            )
            Column(Modifier.selectableGroup()) {
                for ((route, label) in listOf(
                    VoiceOutputRoute.Auto to R.string.settings_voice_route_auto,
                    VoiceOutputRoute.Speaker to R.string.settings_voice_route_speaker,
                    VoiceOutputRoute.Earpiece to R.string.settings_voice_route_earpiece,
                    VoiceOutputRoute.Bluetooth to R.string.settings_voice_route_bluetooth,
                )) {
                    RadioItem(
                        selected = VoiceSettings.outputRoute == route,
                        onClick = {
                            save { VoiceSettings.outputRoute = route }
                            VoiceCallManager.applyOutputSettings()
                        },
                        label = { Text(stringResource(label)) }
                    )
                }
            }

            ListHeader { Text(stringResource(R.string.settings_voice_camera)) }
            SwitchItem(R.string.settings_voice_camera_front, VoiceSettings.defaultCameraFront) {
                save { VoiceSettings.defaultCameraFront = it }
            }

            ListHeader { Text(stringResource(R.string.settings_voice_screen_share)) }
            Column(Modifier.selectableGroup()) {
                for ((quality, label) in listOf(
                    ScreenShareQuality.Low to R.string.settings_voice_screen_share_low,
                    ScreenShareQuality.High to R.string.settings_voice_screen_share_high,
                    ScreenShareQuality.Text to R.string.settings_voice_screen_share_text,
                )) {
                    RadioItem(
                        selected = VoiceSettings.screenShareQuality == quality,
                        onClick = { save { VoiceSettings.screenShareQuality = quality } },
                        label = { Text(stringResource(label)) }
                    )
                }
            }
        }
    }
}
