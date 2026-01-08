package ru.kaelesty.madprojects

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import ru.kaelesty.madprojects.di.commonModule
import ru.kaelesty.madprojects.features.auth.sdk.authModule

class Application: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@Application)
            modules(
                androidModule,
                commonModule,
            )
        }
    }
}

