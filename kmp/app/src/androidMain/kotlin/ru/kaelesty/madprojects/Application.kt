package ru.kaelesty.madprojects

import android.app.Application
import org.koin.core.context.startKoin
import ru.kaelesty.madprojects.features.auth.sdk.authModule

class Application: Application() {
    init {
        startKoin {
            modules(
                authModule,
            )
        }
    }
}