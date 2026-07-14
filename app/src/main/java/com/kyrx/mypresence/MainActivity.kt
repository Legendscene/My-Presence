package com.kyrx.mypresence

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.kyrx.mypresence.core.utils.CallbackResult
import com.kyrx.mypresence.core.utils.Constants
import com.kyrx.mypresence.core.utils.OAuthCallbackHandler
import com.kyrx.mypresence.data.repository.AuthRepositoryImpl
import com.kyrx.mypresence.data.repository.PreferencesRepositoryImpl
import com.kyrx.mypresence.ui.navigation.MainApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepositoryImpl

    @Inject
    lateinit var authRepository: AuthRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainApp(
                    preferencesRepository = preferencesRepository,
                    authRepository = authRepository
                )
            }
        }
        handleOAuthCallback(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) {
            Log.d("OAuthCallback", "Ignoring intent: action=${intent?.action}")
            return
        }
        val uri = intent.data
        Log.d("OAuthCallback", "Received intent URI (scheme=${uri?.scheme} host=${uri?.host} path=${uri?.path})")
        if (uri == null || uri.scheme !in setOf(Constants.REDIRECT_SCHEME, "mypresence")) {
            Log.d("OAuthCallback", "URI not an auth callback scheme, ignoring")
            return
        }
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")
        Log.d("OAuthCallback", "OAuth callback received: error=$error")
        if ((code != null && state != null) || error != null) {
            OAuthCallbackHandler.handleCallback(
                CallbackResult(
                    code = code,
                    state = state,
                    error = error,
                    errorDescription = errorDescription
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
