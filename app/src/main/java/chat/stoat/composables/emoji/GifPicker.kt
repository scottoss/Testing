package chat.stoat.composables.emoji

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.api.routes.microservices.gifs.GifCategory
import chat.stoat.api.routes.microservices.gifs.GifItem
import chat.stoat.api.routes.microservices.gifs.GifProvider
import chat.stoat.api.routes.microservices.gifs.GifService
import chat.stoat.composables.chat.LoopingMutedVideoPlayer
import chat.stoat.composables.generic.RemoteImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GifPickerContent(
    onSearchFocus: (Boolean) -> Unit = {},
    bottomInset: Dp = 0.dp,
    onGifSelected: (String) -> Unit
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var selectedProvider by rememberSaveable { mutableStateOf(GifProvider.GIFBOX) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    var categories by remember { mutableStateOf<List<GifCategory>>(emptyList()) }
    var gifResults by remember { mutableStateOf<List<GifItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            debouncedQuery = ""
        } else {
            delay(250)
            debouncedQuery = searchQuery.trim()
        }
    }

    LaunchedEffect(selectedProvider, debouncedQuery, reloadTrigger) {
        isLoading = true
        errorMessage = null
        try {
            if (debouncedQuery.isBlank()) {
                categories = GifService.fetchCategories(selectedProvider)
            } else {
                gifResults = GifService.fetchGifs(selectedProvider, debouncedQuery)
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to load GIFs"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gif_picker_content")
    ) {
        // Provider Switcher + Search Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        searchQuery = ""
                        debouncedQuery = ""
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("gif_picker_back_button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back_24dp),
                        contentDescription = stringResource(R.string.gif_picker_back_to_categories),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Search input box
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .onFocusChanged { onSearchFocus(it.isFocused) }
                    .testTag("gif_picker_search_input")
            ) { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(
                                if (selectedProvider == GifProvider.KLIPY) {
                                    R.string.gif_picker_search_klipy
                                } else {
                                    R.string.gif_picker_search_gifbox
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()

                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_24dp),
                            contentDescription = stringResource(R.string.emoji_picker_clear_search),
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(CircleShape)
                                .clickable {
                                    searchQuery = ""
                                    debouncedQuery = ""
                                    focusManager.clearFocus()
                                }
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            // Provider Toggle Pills (Gifbox | KLIPY)
            Row(
                modifier = Modifier
                    .height(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                GifProvider.entries.forEach { provider ->
                    val isSelected = selectedProvider == provider
                    Box(
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable {
                                if (selectedProvider != provider) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedProvider = provider
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("gif_provider_${provider.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Main Content Area
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.gif_picker_empty_results),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.gif_picker_empty_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { reloadTrigger++ }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                debouncedQuery.isBlank() -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gif_categories_grid")
                    ) {
                        items(
                            items = categories,
                            key = { "${selectedProvider.id}_cat_${it.query}_${it.title}" }
                        ) { category ->
                            GifCategoryTile(
                                category = category,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    focusManager.clearFocus()
                                    searchQuery = category.query
                                    debouncedQuery = category.query
                                }
                            )
                        }

                        item(span = { GridItemSpan(2) }) {
                            GifProviderAttributionFooter(selectedProvider = selectedProvider)
                        }

                        item(span = { GridItemSpan(2) }) {
                            Spacer(Modifier.height(bottomInset))
                        }
                    }
                }

                gifResults.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.gif_picker_empty_results),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.gif_picker_empty_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("gif_results_grid")
                    ) {
                        items(
                            items = gifResults,
                            key = { "${it.provider.id}_gif_${it.id}" }
                        ) { item ->
                            GifResultTile(
                                item = item,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    if (item.provider == GifProvider.KLIPY) {
                                        scope.launch {
                                            GifService.registerKlipyShare(item.slug, debouncedQuery)
                                        }
                                    }
                                    onGifSelected(item.sendUrl)
                                }
                            )
                        }

                        item(span = { GridItemSpan(2) }) {
                            GifProviderAttributionFooter(selectedProvider = selectedProvider)
                        }

                        item(span = { GridItemSpan(2) }) {
                            Spacer(Modifier.height(bottomInset))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GifCategoryTile(
    category: GifCategory,
    onClick: () -> Unit
) {
    val gradientColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (category.isTrendingTile || category.previewUrl.isNullOrBlank()) {
                    Brush.linearGradient(gradientColors)
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            )
            .clickable(onClick = onClick)
            .testTag("gif_category_${category.query}")
    ) {
        if (!category.previewUrl.isNullOrBlank()) {
            if (GifService.isVideoMediaUrl(category.previewUrl)) {
                LoopingMutedVideoPlayer(
                    videoUrl = category.previewUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                RemoteImage(
                    url = category.previewUrl,
                    description = category.title,
                    contentScale = ContentScale.Crop,
                    allowAnimation = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.2f),
                        1f to Color.Black.copy(alpha = 0.72f)
                    )
                )
                .padding(10.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = if (category.isTrendingTile) {
                    stringResource(R.string.gif_picker_trending)
                } else {
                    category.title
                },
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GifResultTile(
    item: GifItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .testTag("gif_item_${item.id}"),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = item.gifUrl ?: item.previewUrl.takeIf { !item.previewIsVideo }
        if (imageUrl != null) {
            RemoteImage(
                url = imageUrl,
                description = item.title,
                contentScale = ContentScale.Crop,
                allowAnimation = true,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LoopingMutedVideoPlayer(
                videoUrl = item.videoUrl ?: item.previewUrl,
                fallbackImageUrl = item.gifUrl,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GifProviderAttributionFooter(selectedProvider: GifProvider) {
    Text(
        text = stringResource(
            if (selectedProvider == GifProvider.KLIPY) {
                R.string.gif_picker_powered_by_klipy
            } else {
                R.string.gif_picker_powered_by_gifbox
            }
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
