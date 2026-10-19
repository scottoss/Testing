package chat.stoat.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.user.Mutuals
import chat.stoat.api.routes.user.fetchMutuals
import chat.stoat.composables.sheets.SheetTile
import kotlinx.coroutines.CancellationException

@Composable
fun UserMutualsTile(userId: String) {
    if (userId == StoatAPI.selfId) return

    var mutuals by remember(userId) { mutableStateOf<Mutuals?>(null) }

    LaunchedEffect(userId) {
        try {
            mutuals = fetchMutuals(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    val data = mutuals ?: return
    if (data.users.isEmpty() && data.servers.isEmpty()) return

    val friendNames = data.users.map {
        StoatAPI.userCache[it]?.let { u -> u.displayName ?: u.username } ?: it
    }
    val serverNames = data.servers.map { StoatAPI.serverCache[it]?.name ?: it }

    SheetTile(
        header = { Text(stringResource(R.string.user_info_sheet_category_mutuals)) },
        contentPreview = {
            Text(
                text = pluralStringResource(
                    R.plurals.user_info_sheet_mutual_friends,
                    friendNames.size,
                    friendNames.size
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = pluralStringResource(
                    R.plurals.user_info_sheet_mutual_servers,
                    serverNames.size,
                    serverNames.size
                ),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            if (friendNames.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.user_info_sheet_mutual_friends_header),
                    style = MaterialTheme.typography.titleSmall
                )
                friendNames.forEach { Text(it ?: "") }
            }
            if (serverNames.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.user_info_sheet_mutual_servers_header),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                serverNames.forEach { Text(it ?: "") }
            }
        }
    }
}
