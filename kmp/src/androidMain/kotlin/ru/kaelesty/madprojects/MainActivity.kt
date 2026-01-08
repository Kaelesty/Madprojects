package ru.kaelesty.madprojects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowInsetsControllerCompat
import org.koin.core.context.GlobalContext
import ru.kaelesty.madprojects.features.auth.domain.AuthContext
import ru.kaelesty.madprojects.navigation.NavItem

class MainActivity : ComponentActivity() {

    private val navItemList: List<NavItem> by lazy {
        GlobalContext.get().getAll()
    }

    private val authContext: AuthContext by lazy {
        GlobalContext.get().get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.statusBarColor = 0xFFF3F6F8.toInt()
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            App(navItemList, authContext)
        }
    }
}

