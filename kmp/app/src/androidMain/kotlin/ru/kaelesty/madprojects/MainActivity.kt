package ru.kaelesty.madprojects

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.core.context.GlobalContext
import ru.kaelesty.madprojects.navigation.FeatureNavigation

class MainActivity : ComponentActivity() {

    private val featureNavigationList: List<FeatureNavigation> by lazy {
        GlobalContext.get().getAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(featureNavigationList)
        }
    }
}

