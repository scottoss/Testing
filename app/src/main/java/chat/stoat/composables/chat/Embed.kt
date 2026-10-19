package chat.stoat.composables.chat

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import chat.stoat.R
import chat.stoat.api.internals.BrushCompat
import chat.stoat.api.internals.solidColor
import chat.stoat.api.routes.microservices.gifs.GifService
import chat.stoat.api.routes.microservices.gifs.ResolvedGifMedia
import chat.stoat.api.routes.microservices.january.asJanuaryProxyUrl
import chat.stoat.composables.chat.specialembeds.SpecialEmbedSwitch
import chat.stoat.composables.generic.RemoteImage
import chat.stoat.composables.markdown.prose.ChatMarkdown
import chat.stoat.core.model.schemas.Embed
import chat.stoat.core.model.schemas.Embed as EmbedSchema

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BorderlessGifEmbed(
    media: ResolvedGifMedia,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    val targetClickUrl = media.originalUrl ?: media.mediaUrl
    val preferAnimatedImage = !media.isVideo ||
        (media.fallbackImageUrl != null && GifService.isAnimatedImageMediaUrl(media.fallbackImageUrl))

    BoxWithConstraints(
        modifier = modifier
            .padding(vertical = 4.dp)
            .testTag("borderless_gif_embed")
    ) {
        val maxEmbedWidth = minOf(maxWidth, 320.dp)
        val desiredWidth = media.width.dp.coerceIn(140.dp, maxEmbedWidth)

        Box(
            modifier = Modifier
                .widthIn(min = 120.dp, max = desiredWidth)
                .aspectRatio(media.aspectRatio)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable {
                    onLinkClick(targetClickUrl)
                },
            contentAlignment = Alignment.Center
        ) {
            if (preferAnimatedImage) {
                val imageUrl = if (!media.isVideo) {
                    media.mediaUrl
                } else {
                    media.fallbackImageUrl ?: media.mediaUrl
                }
                RemoteImage(
                    url = imageUrl,
                    description = stringResource(R.string.gif_embed_alt),
                    contentScale = ContentScale.Crop,
                    allowAnimation = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LoopingMutedVideoPlayer(
                    videoUrl = media.mediaUrl,
                    fallbackImageUrl = media.fallbackImageUrl,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
fun LoopingMutedVideoPlayer(
    videoUrl: String,
    fallbackImageUrl: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasError by remember(videoUrl) { mutableStateOf(false) }

    if (hasError && fallbackImageUrl != null) {
        RemoteImage(
            url = fallbackImageUrl,
            description = stringResource(R.string.gif_embed_alt),
            contentScale = contentScale,
            allowAnimation = true,
            modifier = modifier
        )
        return
    }

    val player = remember(context, videoUrl) {
        ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                /* handleAudioFocus = */ false
            )
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ALL
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> {}
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    ContentFrame(
        player = player,
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
fun StandaloneGifEmbed(
    url: String,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {},
    onResolutionFailed: () -> Unit = {}
) {
    var resolvedMedia by remember(url) {
        mutableStateOf(GifService.getCachedMedia(url))
    }
    var isLoading by remember(url) {
        mutableStateOf(resolvedMedia == null)
    }

    LaunchedEffect(url) {
        if (resolvedMedia == null) {
            isLoading = true
            val fetched = GifService.resolveStandaloneGifUrl(url)
            resolvedMedia = fetched
            isLoading = false
            if (fetched == null) {
                onResolutionFailed()
            }
        }
    }

    when {
        resolvedMedia != null -> {
            BorderlessGifEmbed(
                media = resolvedMedia!!,
                modifier = modifier,
                onLinkClick = onLinkClick
            )
        }
        isLoading -> {
            Box(
                modifier = modifier
                    .padding(vertical = 4.dp)
                    .width(220.dp)
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun RegularEmbed(
    embed: EmbedSchema,
    serverId: String? = null,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit = {}
) {
    GifService.resolveGifMediaFromEmbed(embed)?.let { gifMedia ->
        BorderlessGifEmbed(
            media = gifMedia,
            modifier = modifier,
            onLinkClick = onLinkClick
        )
        return
    }

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .wrapContentWidth(Alignment.Start)
            .height(IntrinsicSize.Min)
    ) {
        // Stripe at the left side of the embed
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    embed.colour?.let { BrushCompat.parseColour(it) }
                        ?: Brush.solidColor(MaterialTheme.colorScheme.primary)
                )
        )

        Box(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Column {
                // Title row (icon + title)
                Row(
                    modifier = Modifier
                        .then(
                            if (embed.originalURL != null) {
                                Modifier
                                    .clickable {
                                        onLinkClick(embed.originalURL!!)
                                    }
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    embed.iconURL?.let {
                        if (it.endsWith(".svg")) return@let

                        RemoteImage(
                            url = asJanuaryProxyUrl(it),
                            width = 48,
                            height = 48,
                            contentScale = ContentScale.Crop,
                            description = null // decorative
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    embed.title?.let {
                        Text(
                            text = it,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Description
                embed.description?.let {
                    ChatMarkdown(
                        content = it,
                        serverId = serverId,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Image
                embed.image?.let {
                    if (it.url == null || it.url?.endsWith(".svg") == true) return@let

                    Spacer(modifier = Modifier.height(8.dp))
                    RemoteImage(
                        url = asJanuaryProxyUrl(it.url!!),
                        width = (it.width ?: 240).toInt(),
                        height = (it.height ?: 180).toInt(),
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .then(
                                if (embed.originalURL != null) {
                                    Modifier.clickable {
                                        onLinkClick(embed.originalURL!!)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .aspectRatio(
                                ((it.width?.toFloat() ?: 0f) / (it.height?.toFloat() ?: 0f)).let { ratio ->
                                    if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0f) 1f else ratio
                                }
                            ),
                        contentScale = ContentScale.Crop,
                        description = null // decorative
                    )
                }

                // Video inside non-GIF Website embed
                if (embed.image == null && embed.video?.url != null) {
                    val video = embed.video!!
                    val ratio = ((video.width?.toFloat() ?: 16f) / (video.height?.toFloat() ?: 9f)).let { r ->
                        if (r.isNaN() || r.isInfinite() || r <= 0f) 16f / 9f else r.coerceIn(0.35f, 3.5f)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .aspectRatio(ratio)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable {
                                onLinkClick(embed.originalURL ?: video.url!!)
                            }
                    ) {
                        LoopingMutedVideoPlayer(
                            videoUrl = video.url!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Special
                embed.special?.let {
                    SpecialEmbedSwitch(it)
                }
            }
        }
    }
}

@Composable
fun Embed(
    embed: Embed,
    serverId: String? = null,
    modifier: Modifier = Modifier,
    onLinkClick: (String) -> Unit
) {
    Column {
        val gifMedia = remember(embed) { GifService.resolveGifMediaFromEmbed(embed) }
        if (gifMedia != null) {
            BorderlessGifEmbed(
                media = gifMedia,
                modifier = modifier,
                onLinkClick = onLinkClick
            )
        } else {
            when (embed.type) {
                else -> RegularEmbed(
                    embed = embed,
                    serverId = serverId,
                    modifier = modifier,
                    onLinkClick = onLinkClick
                )
            }
        }
    }
}
