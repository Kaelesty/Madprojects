package app.plugins

import app.plugins.analytics.AnalyticsPlugin
import app.plugins.analytics.ExcelWizard
import app.plugins.analytics.PoiExcelWizard
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val pluginsModule = module {

    single<PluginContainer> {
        PluginContainer(plugins = getAll())
    }

    singleOf(::AnalyticsPlugin) bind Plugin::class
    singleOf(::BanHammer) bind Plugin::class
    singleOf(::Ping) bind Plugin::class

    single<ExcelWizard> {
        PoiExcelWizard()
    }
}