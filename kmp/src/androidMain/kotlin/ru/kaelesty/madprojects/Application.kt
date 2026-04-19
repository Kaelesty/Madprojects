package ru.kaelesty.madprojects

import android.app.Application
import org.koin.android.ext.koin.androidContext
import ru.kaelesty.madprojects.di.initKoin

class Application: Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin(
            appDeclaration = {
                androidContext(this@Application)
            },
            platformModules = listOf(androidModule),
        )
    }
}

