package ru.kaelesty.madprojects

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.core.context.GlobalContext
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.features.auth.domain.GithubOauthBridge
import ru.kaelesty.madprojects.navigation.NavItem
import ru.kaelesty.madprojects.utils.KLogger

class MainActivity : ComponentActivity() {

    private val navItemList: List<NavItem> by lazy {
        GlobalContext.get().getAll()
    }

    private val authContext: AuthContext by lazy {
        GlobalContext.get().get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOauthIntent(intent)
        actionBar?.hide()
        window.statusBarColor = 0xFFF3F6F8.toInt()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            App(navItemList, authContext)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOauthIntent(intent)
    }

    private fun handleOauthIntent(intent: Intent?) {
        val incomingUrl = intent?.dataString?.takeIf { it.isNotBlank() } ?: return
        val accepted = GithubOauthBridge.handleIncomingUrl(incomingUrl)
        KLogger.d(TAG) { "handleOauthIntent: accepted=$accepted url=$incomingUrl" }
    }

    private companion object {
        private const val TAG = "MainActivity"
    }
}

