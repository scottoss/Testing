package chat.stoat.composables.screens.services

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import chat.stoat.activities.InviteActivity
import chat.stoat.api.StoatJson
import chat.stoat.api.buildUserAgent
import chat.stoat.api.internals.ThemeCompat
import chat.stoat.core.model.data.DAWNCHAT_DISCOVERY
import chat.stoat.core.model.data.DAWNCHAT_INVITE_SHORT
import chat.stoat.core.model.data.STOAT_BETA_WEB_APP
import chat.stoat.core.model.data.STOAT_WEB_APP
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ColumnScope.DiscoverView() {
    var showPlaceholder by remember { mutableStateOf(true) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (showPlaceholder) 1f else 0f,
        label = "discoverViewPlaceholderAlpha"
    )
    val themeMap = ThemeCompat.materialThemeAsDiscoverTheme(MaterialTheme)
    var webView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.userAgentString = buildUserAgent("DiscoverView")
                    settings.setSupportZoom(false)
                    settings.setSupportMultipleWindows(false)
                    loadUrl("$DAWNCHAT_DISCOVERY/servers?embedded=true")

                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val themeMessage = JsonObject(
                                mapOf(
                                    "source" to JsonPrimitive("revolt"),
                                    "type" to JsonPrimitive("theme"),
                                    "theme" to themeMap
                                )
                            )
                            val themeMessageString =
                                StoatJson.encodeToString(JsonObject.serializer(), themeMessage)

                            evaluateJavascript(
                                """
                                    (function() {
                                        const style = document.createElement('style');
                                        style.textContent = `
                                            html, body {
                                                width: 100% !important;
                                                min-height: 100% !important;
                                                margin: 0 !important;
                                                padding: 0 !important;
                                                box-sizing: border-box !important;
                                            }
                                            body {
                                                display: flex !important;
                                                flex-direction: column !important;
                                                align-items: center !important;
                                                justify-content: center !important;
                                            }
                                            body > main, body > #__next, body > #app, body > div {
                                                margin-left: auto !important;
                                                margin-right: auto !important;
                                                max-width: 100% !important;
                                                box-sizing: border-box !important;
                                            }
                                            .container {
                                                width: 100% !important;
                                                max-width: 100% !important;
                                                padding: 0 16px !important;
                                                box-sizing: border-box !important;
                                                text-align: center !important;
                                                display: flex !important;
                                                flex-direction: column !important;
                                                align-items: center !important;
                                                justify-content: center !important;
                                            }
                                            .glitch-text {
                                                padding-left: 0.2em !important;
                                                text-align: center !important;
                                                max-width: 100% !important;
                                                word-break: break-word !important;
                                            }
                                        `;
                                        document.head.appendChild(style);
                                    })();
                                    window.postMessage($themeMessageString, "*")
                                    window.addEventListener("message", event => {
                                        try {
                                            const data = JSON.parse(event.data)
                                            if (data.source === "discover") {
                                                switch (data.type) {
                                                    case "navigate":
                                                        // Cheap debounce
                                                        if (Date.now() - window.lastNavigateEvent < 500) {
                                                            return
                                                        }
                                                        window.lastNavigateEvent = Date.now()
                                                        window.location.href = data.url
                                                        break
                                                }
                                            }
                                        } catch(e) {}
                                    })
                                """.trimIndent(),
                                null
                            )

                            postDelayed({
                                showPlaceholder = false
                            }, 1000) // to prevent flickering
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val host = request?.url?.host
                            val isAppHost = host == Uri.parse(STOAT_WEB_APP).host ||
                                host == Uri.parse(STOAT_BETA_WEB_APP).host
                            val isInviteShort = host == Uri.parse(DAWNCHAT_INVITE_SHORT).host &&
                                request?.url?.pathSegments?.firstOrNull() == "dc"
                            if (isAppHost || isInviteShort) {
                                val target = request?.url
                                if (target?.pathSegments?.firstOrNull() == "bot") {
                                    // No native add-bot flow; hand off to the web app
                                    context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                    return true
                                }

                                val intent = Intent(
                                    context,
                                    InviteActivity::class.java
                                ).setAction(Intent.ACTION_VIEW)

                                intent.data = request?.url
                                context.startActivity(intent)

                                return true
                            }

                            if (host != Uri.parse(DAWNCHAT_DISCOVERY).host) {
                                return true
                            }

                            return false
                        }
                    }
                }
            },
            update = {
            },
            modifier = Modifier
                .fillMaxSize()
                .alpha(1f - animatedAlpha)
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .alpha(animatedAlpha)
        )
    }
}