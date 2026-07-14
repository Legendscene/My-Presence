package com.kyrx.mypresence.feature.auth

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val JS_SNIPPET = "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3Bdocument.body.appendChild(i)%3Balert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"
private const val MOTOROLA = "motorola"
private const val SAMSUNG_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginWebView(
    onLoginCompleted: (String) -> Unit,
) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String,
                ): Boolean {
                    if (url.endsWith("/app")) {
                        stopLoading()
                        loadUrl(JS_SNIPPET)
                        visibility = View.GONE
                    }
                    return false
                }
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            if (android.os.Build.MANUFACTURER.equals(MOTOROLA, ignoreCase = true)) {
                settings.userAgentString = SAMSUNG_USER_AGENT
            }
            webChromeClient = object : WebChromeClient() {
                override fun onJsAlert(
                    view: WebView,
                    url: String,
                    message: String,
                    result: JsResult,
                ): Boolean {
                    onLoginCompleted(message)
                    visibility = View.GONE
                    return true
                }
            }
            loadUrl(DISCORD_LOGIN_URL)
        }
    })
}
