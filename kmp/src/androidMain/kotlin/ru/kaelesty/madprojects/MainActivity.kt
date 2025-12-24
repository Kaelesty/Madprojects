package ru.kaelesty.madprojects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.core.context.GlobalContext
import ru.kaelesty.madprojects.navigation.NavItem

class MainActivity : ComponentActivity() {

    private val navItemList: List<NavItem> by lazy {
        GlobalContext.get().getAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        setContent {
            App(navItemList)
        }
    }
}

