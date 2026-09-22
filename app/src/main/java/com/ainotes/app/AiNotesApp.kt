package com.ainotes.app

import android.app.Application
import com.ainotes.app.ai.AiService
import com.ainotes.app.data.local.AiNotesDatabase
import com.ainotes.app.data.repository.NotesRepository
import com.ainotes.app.export.ExportManager
import com.ainotes.app.security.SecureStore
import com.ainotes.app.settings.SettingsRepository

/** Manual DI container - small, explicit, no framework overhead. */
class AppContainer(val app: Application) {
    val database = AiNotesDatabase.get(app)
    val secureStore = SecureStore(app)
    val repository = NotesRepository(database, secureStore)
    val settings = SettingsRepository(app)
    val aiService = AiService()
    val exporter = ExportManager(app)
}

class AiNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashLogger()
    }

    /**
     * Writes the last uncaught exception to filesDir/crash.log so the crash
     * trace can be read from inside the app on the next launch.
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                java.io.File(filesDir, "crash.log").writeText(
                    throwable.javaClass.name + ": " + throwable.message + "\n" +
                        throwable.stackTraceToString() + "\nthread: " + thread.name
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
