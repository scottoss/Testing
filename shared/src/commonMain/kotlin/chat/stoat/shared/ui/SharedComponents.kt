package chat.stoat.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.stoat.core.model.data.DAWNCHAT_INVITE_SHORT
import chat.stoat.core.model.schemas.Channel
import chat.stoat.core.model.schemas.ContentReportReason
import chat.stoat.core.model.schemas.Message
import chat.stoat.core.model.schemas.Server
import chat.stoat.core.model.schemas.User
import chat.stoat.core.model.schemas.UserBadges
import chat.stoat.core.model.schemas.UserReportReason
import chat.stoat.core.model.schemas.has
import chat.stoat.shared.network.SharedStoatClient
import chat.stoat.shared.platform.PlatformInfo
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class SharedPermissionFlag(val label: String, val description: String, val bit: Long) {
    ManageChannel("Manage Channels", "Create, edit, and delete channels", 1L shl 0),
    ManageServer("Manage Server", "Edit server name, description, icon, and categories", 1L shl 1),
    ManagePermissions("Manage Permissions", "Edit channel and role permissions", 1L shl 2),
    ManageRole("Manage Roles", "Create, edit, reorder, and assign roles", 1L shl 3),
    ManageCustomisation("Manage Emojis", "Upload and remove custom server emojis", 1L shl 4),
    KickMembers("Kick Members", "Kick members from the server", 1L shl 6),
    BanMembers("Ban Members", "Ban and unban members from the server", 1L shl 7),
    TimeoutMembers("Timeout Members", "Temporarily mute members in chat and voice", 1L shl 8),
    AssignRoles("Assign Roles", "Assign roles below own highest role", 1L shl 9),
    ChangeNickname("Change Nickname", "Change own server nickname", 1L shl 10),
    ManageNicknames("Manage Nicknames", "Change other members' nicknames", 1L shl 11),
    ViewChannel("View Channel", "View channel and read messages", 1L shl 20),
    ReadMessageHistory("Read Message History", "Read previous messages in the channel", 1L shl 21),
    SendMessage("Send Messages", "Send messages in text channels", 1L shl 22),
    ManageMessages("Manage Messages", "Delete or pin messages from other users", 1L shl 23),
    ManageWebhooks("Manage Webhooks", "Create, edit, and delete channel webhooks", 1L shl 24),
    InviteOthers("Create Invites", "Invite new people to the server", 1L shl 25),
    SendEmbeds("Embed Links", "Show rich link previews on sent links", 1L shl 26),
    UploadFiles("Attach Files", "Upload images, videos, and files", 1L shl 27),
    React("Add Reactions", "React to messages with emojis", 1L shl 29),
    Connect("Connect to Voice", "Join voice channels", 1L shl 30),
    Speak("Speak in Voice", "Speak in voice channels", 1L shl 31),
    Video("Stream Video", "Share camera or screen in voice channels", 1L shl 32),
    MuteMembers("Mute Members", "Server-mute members in voice channels", 1L shl 33),
    DeafenMembers("Deafen Members", "Server-deafen members in voice channels", 1L shl 34),
    MoveMembers("Move Members", "Move members between voice channels", 1L shl 35)
}

fun parseHexColorOrDefault(raw: String?, defaultColor: Color): Color {
    if (raw.isNullOrBlank()) return defaultColor
    val cleaned = raw.trim().removePrefix("#")
    return runCatching {
        when (cleaned.length) {
            6 -> Color(("FF$cleaned").toLong(16))
            8 -> Color(cleaned.toLong(16))
            else -> defaultColor
        }
    }.getOrDefault(defaultColor)
}

private val avatarPalette = listOf(
    Color(0xFF5865F2),
    Color(0xFF00A8FC),
    Color(0xFF23A55A),
    Color(0xFFF0B232),
    Color(0xFFEB459E),
    Color(0xFFDA373C),
    Color(0xFF8B5CF6),
    Color(0xFF14B8A6)
)

fun deterministicAvatarColor(seed: String): Color {
    val hash = abs(seed.hashCode())
    return avatarPalette[hash % avatarPalette.size]
}

@Composable
fun SharedAvatarCircle(
    label: String,
    seed: String = label,
    size: Dp = 40.dp,
    online: Boolean? = null,
    presence: String? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val radiusPercent = if (isSelected) 32 else SharedStoatClient.avatarRadiusPercent
    val shape = RoundedCornerShape(percent = radiusPercent.coerceIn(10, 50))
    val bgColor = deterministicAvatarColor(seed)
    val initials = remember(label) {
        label.trim()
            .split(' ', '_', '-')
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "?" }
    }

    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                style = if (size >= 56.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (online != null) {
            val dotColor = when {
                !online || presence == "Invisible" -> Color(0xFF80848E)
                presence == "Busy" -> Color(0xFFF23F43)
                presence == "Idle" -> Color(0xFFF0B232)
                presence == "Focus" -> Color(0xFF8B5CF6)
                else -> Color(0xFF23A55A)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size.value * 0.30f).dp.coerceIn(10.dp, 18.dp))
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SharedUserBadgesRow(user: User) {
    val badges = user.badges ?: 0L
    val activeBadges = remember(badges, user.bot) {
        buildList {
            if (user.bot != null) add("BOT")
            if (badges has UserBadges.Founder) add("Founder")
            if (badges has UserBadges.Developer) add("Developer")
            if (badges has UserBadges.PlatformModeration) add("Mod")
            if (badges has UserBadges.Supporter || badges has UserBadges.ActiveSupporter) add("Supporter")
            if (badges has UserBadges.EarlyAdopter) add("Early Adopter")
            if (badges has UserBadges.Translator) add("Translator")
            if (badges has UserBadges.Paw) add("Paw")
        }
    }
    if (activeBadges.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        activeBadges.forEach { badge ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun SharedQrCodeCanvas(
    payload: String,
    modifier: Modifier = Modifier.size(210.dp)
) {
    val gridSize = 25
    val matrix = remember(payload) {
        val grid = Array(gridSize) { BooleanArray(gridSize) }
        fun drawFinder(r0: Int, c0: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val border = r == 0 || r == 6 || c == 0 || c == 6
                    val inner = r in 2..4 && c in 2..4
                    grid[r0 + r][c0 + c] = border || inner
                }
            }
        }
        drawFinder(0, 0)
        drawFinder(0, gridSize - 7)
        drawFinder(gridSize - 7, 0)

        val bytes = payload.encodeToByteArray()
        var bitIdx = 0
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inFinder = (r < 8 && c < 8) || (r < 8 && c >= gridSize - 8) || (r >= gridSize - 8 && c < 8)
                if (!inFinder) {
                    if (r == 6 || c == 6) {
                        grid[r][c] = (r + c) % 2 == 0
                    } else {
                        val b = bytes[bitIdx % bytes.size].toInt()
                        val shift = (bitIdx / bytes.size) % 8
                        grid[r][c] = ((b ushr shift) xor (r * 31 + c * 17)) and 1 == 1
                        bitIdx++
                    }
                }
            }
        }
        grid
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.padding(14.dp)) {
            val cellW = size.width / gridSize
            val cellH = size.height / gridSize
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW, cellH)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Dialogs & BottomSheets
// ============================================================================

@Composable
fun SharedInviteDialog(
    channelId: String,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(channelId) {
        isLoading = true
        errorText = null
        runCatching {
            SharedStoatClient.createChannelInvite(channelId)
        }.onSuccess { invite ->
            inviteCode = invite.effectiveCode
            isLoading = false
        }.onFailure { err ->
            errorText = err.message ?: "Failed to create invite"
            isLoading = false
        }
    }

    val fullInviteUrl = if (inviteCode.isNotBlank()) "$DAWNCHAT_INVITE_SHORT/$inviteCode" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite People") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Generating invite link...")
                    }
                } else if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Share this invite link so others can join the server:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fullInviteUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (fullInviteUrl.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            PlatformInfo.copyToClipboard("Invite Link", fullInviteUrl)
                            onStatusMessage("Invite link copied to clipboard")
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = {
                            PlatformInfo.shareText(fullInviteUrl)
                            onStatusMessage("Invite link ready to share")
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            errorText = null
                            runCatching { SharedStoatClient.createChannelInvite(channelId) }
                                .onSuccess {
                                    inviteCode = it.effectiveCode
                                    isLoading = false
                                }
                                .onFailure {
                                    errorText = it.message
                                    isLoading = false
                                }
                        }
                    }
                ) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SharedCreateOrJoinServerDialog(
    onDismiss: () -> Unit,
    onServerSelected: (String) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("create") } // "create" or "join"
    var serverName by remember { mutableStateOf("") }
    var serverDesc by remember { mutableStateOf("") }
    var isNsfw by remember { mutableStateOf(false) }
    var inviteInput by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text(if (mode == "create") "Create a Server" else "Join a Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "create",
                        onClick = { mode = "create"; errorMsg = null },
                        label = { Text("Create Server") }
                    )
                    FilterChip(
                        selected = mode == "join",
                        onClick = { mode = "join"; errorMsg = null },
                        label = { Text("Join via Invite") }
                    )
                }

                if (mode == "create") {
                    OutlinedTextField(
                        value = serverName,
                        onValueChange = { serverName = it },
                        label = { Text("Server Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serverDesc,
                        onValueChange = { serverDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Age-Restricted (NSFW)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isNsfw, onCheckedChange = { isNsfw = it })
                    }
                } else {
                    OutlinedTextField(
                        value = inviteInput,
                        onValueChange = { inviteInput = it },
                        label = { Text("Invite Code or Link") },
                        placeholder = { Text("e.g. https://csinvite.net/dc/abcd") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && (if (mode == "create") serverName.isNotBlank() else inviteInput.isNotBlank()),
                onClick = {
                    scope.launch {
                        isBusy = true
                        errorMsg = null
                        if (mode == "create") {
                            runCatching {
                                SharedStoatClient.createServer(serverName, serverDesc, isNsfw)
                            }.onSuccess { created ->
                                isBusy = false
                                created.id?.let(onServerSelected)
                                onStatusMessage("Created server ${created.name}")
                                onDismiss()
                            }.onFailure { err ->
                                isBusy = false
                                errorMsg = err.message ?: "Failed to create server"
                            }
                        } else {
                            runCatching {
                                SharedStoatClient.joinInviteByCode(inviteInput)
                            }.onSuccess { joined ->
                                isBusy = false
                                joined.server?.id?.let(onServerSelected)
                                onStatusMessage("Joined ${joined.server?.name ?: "server"}")
                                onDismiss()
                            }.onFailure { err ->
                                isBusy = false
                                errorMsg = err.message ?: "Failed to join invite"
                            }
                        }
                    }
                }
            ) {
                Text(if (isBusy) "Working..." else if (mode == "create") "Create" else "Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SharedReportMessageDialog(
    message: Message,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf(ContentReportReason.SpamAbuse) }
    var additionalContext by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val reasons = remember {
        listOf(
            ContentReportReason.SpamAbuse to "Spam or Abuse",
            ContentReportReason.Harassment to "Harassment or Bullying",
            ContentReportReason.UnsolicitedSpam to "Unsolicited Spam",
            ContentReportReason.ScamsFraud to "Scams or Fraud",
            ContentReportReason.Malware to "Malware or Malicious Links",
            ContentReportReason.PromotesHarm to "Promotes Harm",
            ContentReportReason.ExtremeViolence to "Extreme Violence",
            ContentReportReason.Illegal to "Illegal Content"
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Report Message") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select a reason for reporting this message to Trust & Safety:",
                    style = MaterialTheme.typography.bodySmall
                )
                reasons.forEach { (reason, label) ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = additionalContext,
                    onValueChange = { additionalContext = it },
                    label = { Text("Additional context (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && message.id != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    val mid = message.id ?: return@Button
                    scope.launch {
                        isSubmitting = true
                        errorMsg = null
                        runCatching {
                            SharedStoatClient.reportMessage(mid, selectedReason, additionalContext)
                        }.onSuccess {
                            isSubmitting = false
                            onStatusMessage("Message report submitted")
                            onDismiss()
                        }.onFailure { err ->
                            isSubmitting = false
                            errorMsg = err.message ?: "Failed to submit report"
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Reporting..." else "Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SharedReportUserDialog(
    user: User,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf(UserReportReason.SpamAbuse) }
    var additionalContext by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val reasons = remember {
        listOf(
            UserReportReason.SpamAbuse to "Spam or Abuse",
            UserReportReason.UnsolicitedSpam to "Unsolicited Spam",
            UserReportReason.InappropriateProfile to "Inappropriate Profile / Avatar",
            UserReportReason.Impersonation to "Impersonation",
            UserReportReason.BanEvasion to "Ban Evasion",
            UserReportReason.Underage to "Underage User"
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Report ${User.resolveDefaultName(user)}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { (reason, label) ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = additionalContext,
                    onValueChange = { additionalContext = it },
                    label = { Text("Additional context (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && user.id != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    val uid = user.id ?: return@Button
                    scope.launch {
                        isSubmitting = true
                        errorMsg = null
                        runCatching {
                            SharedStoatClient.reportUser(uid, selectedReason, additionalContext)
                        }.onSuccess {
                            isSubmitting = false
                            onStatusMessage("User report submitted")
                            onDismiss()
                        }.onFailure { err ->
                            isSubmitting = false
                            errorMsg = err.message ?: "Failed to submit report"
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Reporting..." else "Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SharedReportServerDialog(
    server: Server,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedReason by remember { mutableStateOf(ContentReportReason.SpamAbuse) }
    var additionalContext by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val reasons = remember {
        listOf(
            ContentReportReason.SpamAbuse to "Spam or Abuse",
            ContentReportReason.Raid to "Raid Organization",
            ContentReportReason.Harassment to "Harassment",
            ContentReportReason.ScamsFraud to "Scams or Fraud",
            ContentReportReason.Illegal to "Illegal Content",
            ContentReportReason.PromotesHarm to "Promotes Harm"
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Report ${server.name ?: "Server"}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { (reason, label) ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = additionalContext,
                    onValueChange = { additionalContext = it },
                    label = { Text("Additional context (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSubmitting && server.id != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    val sid = server.id ?: return@Button
                    scope.launch {
                        isSubmitting = true
                        errorMsg = null
                        runCatching {
                            SharedStoatClient.reportServer(sid, selectedReason, additionalContext)
                        }.onSuccess {
                            isSubmitting = false
                            onStatusMessage("Server report submitted")
                            onDismiss()
                        }.onFailure { err ->
                            isSubmitting = false
                            errorMsg = err.message ?: "Failed to submit report"
                        }
                    }
                }
            ) {
                Text(if (isSubmitting) "Reporting..." else "Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedMessageContextSheet(
    message: Message,
    onDismiss: () -> Unit,
    onReply: (Message) -> Unit,
    onEdit: (Message) -> Unit,
    onReact: (Message, String) -> Unit,
    onTogglePin: (Message) -> Unit,
    onReport: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val isOwnMessage = message.author == SharedStoatClient.selfId
    val quickEmojis = listOf("👍", "❤️", "😂", "🔥", "🎉", "👀")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                quickEmojis.forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable {
                                onReact(message, emoji)
                                onDismiss()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SheetActionRow(
                icon = Icons.AutoMirrored.Filled.Reply,
                title = "Reply",
                onClick = {
                    onReply(message)
                    onDismiss()
                }
            )

            if (isOwnMessage) {
                SheetActionRow(
                    icon = Icons.Default.Edit,
                    title = "Edit Message",
                    onClick = {
                        onEdit(message)
                        onDismiss()
                    }
                )
            }

            if (!message.content.isNullOrBlank()) {
                SheetActionRow(
                    icon = Icons.Default.ContentCopy,
                    title = "Copy Text",
                    onClick = {
                        PlatformInfo.copyToClipboard("Message", message.content!!)
                        onStatusMessage("Message copied to clipboard")
                        onDismiss()
                    }
                )
            }

            SheetActionRow(
                icon = Icons.Default.PushPin,
                title = if (message.pinned == true) "Unpin Message" else "Pin Message",
                onClick = {
                    onTogglePin(message)
                    onDismiss()
                }
            )

            SheetActionRow(
                icon = Icons.Default.Flag,
                title = "Report Message",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onReport(message)
                }
            )

            if (isOwnMessage) {
                SheetActionRow(
                    icon = Icons.Default.Delete,
                    title = "Delete Message",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDelete(message)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SharedUserProfileSheet(
    user: User,
    serverId: String? = null,
    onDismiss: () -> Unit,
    onOpenDm: (Channel) -> Unit,
    onOpenServerIdentity: (String, String) -> Unit,
    onReportUser: (User) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val userId = user.id ?: ""
    val profile = SharedStoatClient.profileByUser[userId] ?: user.profile
    val mutuals = SharedStoatClient.mutualsByUser[userId]
    val server = serverId?.let { SharedStoatClient.serverCache[it] }
    val member = serverId?.let { SharedStoatClient.memberCache[it]?.get(userId) }
    val isSelf = userId == SharedStoatClient.selfId
    val isServerOwner = server?.owner == SharedStoatClient.selfId

    var showRolePickerDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            runCatching { SharedStoatClient.fetchUserProfile(userId) }
            if (!isSelf) {
                runCatching { SharedStoatClient.fetchMutuals(userId) }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SharedAvatarCircle(
                    label = member?.nickname ?: User.resolveDefaultName(user),
                    seed = userId.ifBlank { user.username.orEmpty() },
                    size = 64.dp,
                    online = user.online,
                    presence = user.status?.presence
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member?.nickname ?: User.resolveDefaultName(user),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${user.username ?: "user"}#${user.discriminator ?: "0000"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val pronouns = member?.pronouns ?: user.pronouns
                    if (!pronouns.isNullOrBlank()) {
                        Text(
                            text = pronouns,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            SharedUserBadgesRow(user)

            user.status?.text?.takeIf { it.isNotBlank() }?.let { statusText ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            profile?.content?.takeIf { it.isNotBlank() }?.let { bio ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ABOUT ME",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (server != null && !member?.roles.isNullOrEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ROLES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        member?.roles.orEmpty().forEach { rid ->
                            val role = server.roles?.get(rid)
                            val roleColor = parseHexColorOrDefault(role?.colour, MaterialTheme.colorScheme.primary)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = roleColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = role?.name ?: rid,
                                    color = roleColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (mutuals != null && (mutuals.servers.isNotEmpty() || mutuals.users.isNotEmpty())) {
                Text(
                    text = "${mutuals.servers.size} Mutual Servers • ${mutuals.users.size} Mutual Friends",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (!isSelf && userId.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.openDirectMessage(userId) }
                                    .onSuccess { dm ->
                                        onDismiss()
                                        onOpenDm(dm)
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Could not open DM") }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }

                    val rel = user.relationship
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                runCatching {
                                    when (rel) {
                                        "Friend", "Outgoing" -> SharedStoatClient.removeOrDeclineFriend(userId)
                                        "Incoming" -> SharedStoatClient.acceptFriendRequest(userId)
                                        else -> SharedStoatClient.sendFriendRequest("${user.username}#${user.discriminator ?: "0000"}")
                                    }
                                }.onSuccess {
                                    onStatusMessage("Updated friend relationship")
                                }.onFailure {
                                    onStatusMessage(it.message ?: "Action failed")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (rel == "Friend") Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (rel) {
                                "Friend" -> "Unfriend"
                                "Outgoing" -> "Cancel Req"
                                "Incoming" -> "Accept"
                                else -> "Add Friend"
                            }
                        )
                    }
                }

                SheetActionRow(
                    icon = Icons.Default.Block,
                    title = if (user.relationship == "Blocked") "Unblock User" else "Block User",
                    onClick = {
                        scope.launch {
                            runCatching {
                                if (user.relationship == "Blocked") {
                                    SharedStoatClient.unblockUser(userId)
                                } else {
                                    SharedStoatClient.blockUser(userId)
                                }
                            }.onSuccess {
                                onStatusMessage("Updated block status")
                                onDismiss()
                            }.onFailure {
                                onStatusMessage(it.message ?: "Failed to update block status")
                            }
                        }
                    }
                )

                SheetActionRow(
                    icon = Icons.Default.Flag,
                    title = "Report User",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onReportUser(user)
                    }
                )
            }

            if (serverId != null && userId.isNotBlank()) {
                if (isSelf || isServerOwner) {
                    SheetActionRow(
                        icon = Icons.Default.Edit,
                        title = "Edit Server Identity",
                        onClick = {
                            onDismiss()
                            onOpenServerIdentity(serverId, userId)
                        }
                    )
                }

                if (isServerOwner && !server?.roles.isNullOrEmpty()) {
                    SheetActionRow(
                        icon = Icons.Default.ManageAccounts,
                        title = "Manage Member Roles",
                        onClick = { showRolePickerDialog = true }
                    )
                }

                if (isServerOwner && !isSelf) {
                    SheetActionRow(
                        icon = Icons.Default.PersonRemove,
                        title = "Kick from Server",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.kickServerMember(serverId, userId) }
                                    .onSuccess {
                                        onStatusMessage("Kicked ${User.resolveDefaultName(user)}")
                                        onDismiss()
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Kick failed") }
                            }
                        }
                    )
                    SheetActionRow(
                        icon = Icons.Default.Gavel,
                        title = "Ban from Server",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            scope.launch {
                                runCatching { SharedStoatClient.banServerMember(serverId, userId, "Banned via moderation sheet") }
                                    .onSuccess {
                                        onStatusMessage("Banned ${User.resolveDefaultName(user)}")
                                        onDismiss()
                                    }
                                    .onFailure { onStatusMessage(it.message ?: "Ban failed") }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showRolePickerDialog && server != null) {
        val allRoles = server.roles.orEmpty()
        var selectedRoleIds by remember(member) {
            mutableStateOf(member?.roles.orEmpty().toSet())
        }
        AlertDialog(
            onDismissRequest = { showRolePickerDialog = false },
            title = { Text("Manage Roles") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    allRoles.forEach { (rid, role) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRoleIds = if (rid in selectedRoleIds) {
                                        selectedRoleIds - rid
                                    } else {
                                        selectedRoleIds + rid
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rid in selectedRoleIds,
                                onCheckedChange = { checked ->
                                    selectedRoleIds = if (checked) selectedRoleIds + rid else selectedRoleIds - rid
                                }
                            )
                            Text(role.name ?: rid, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                SharedStoatClient.patchServerMember(
                                    serverId = serverId!!,
                                    userId = userId,
                                    roles = selectedRoleIds.toList()
                                )
                            }.onSuccess {
                                showRolePickerDialog = false
                                onStatusMessage("Updated member roles")
                            }.onFailure {
                                onStatusMessage(it.message ?: "Failed to update roles")
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRolePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedServerContextSheet(
    server: Server,
    onDismiss: () -> Unit,
    onInvite: () -> Unit,
    onOpenServerSettings: () -> Unit,
    onOpenServerIdentity: () -> Unit,
    onReportServer: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val serverId = server.id ?: return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SharedAvatarCircle(
                    label = server.name ?: "Server",
                    seed = serverId,
                    size = 48.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.name ?: "Server",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!server.description.isNullOrBlank()) {
                        Text(
                            text = server.description!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            SheetActionRow(
                icon = Icons.Default.GroupAdd,
                title = "Invite People",
                onClick = {
                    onDismiss()
                    onInvite()
                }
            )

            SheetActionRow(
                icon = Icons.Default.DoneAll,
                title = "Mark Server as Read",
                onClick = {
                    scope.launch {
                        SharedStoatClient.ackServer(serverId)
                        onStatusMessage("Marked ${server.name} as read")
                        onDismiss()
                    }
                }
            )

            SheetActionRow(
                icon = Icons.Default.Edit,
                title = "Edit Server Identity",
                onClick = {
                    onDismiss()
                    onOpenServerIdentity()
                }
            )

            SheetActionRow(
                icon = Icons.Default.Settings,
                title = "Server Settings",
                onClick = {
                    onDismiss()
                    onOpenServerSettings()
                }
            )

            SheetActionRow(
                icon = Icons.Default.Flag,
                title = "Report Server",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onReportServer()
                }
            )

            SheetActionRow(
                icon = Icons.Default.Delete,
                title = if (server.owner == SharedStoatClient.selfId) "Delete Server" else "Leave Server",
                tint = MaterialTheme.colorScheme.error,
                onClick = {
                    scope.launch {
                        runCatching { SharedStoatClient.leaveOrDeleteServer(serverId) }
                            .onSuccess {
                                onStatusMessage("Left ${server.name}")
                                onDismiss()
                            }
                            .onFailure {
                                onStatusMessage(it.message ?: "Failed to leave server")
                            }
                    }
                }
            )
        }
    }
}

@Composable
fun SheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, color = tint, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = title, tint = tint) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}
