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
        // Handler FIRST: even a startup failure must leave crash.log behind.
        installCrashLogger()
        container = try {
            AppContainer(this)
        } catch (t: Throwable) {
            writeCrash(t, "Application.onCreate")
            throw t
        }
    }

    private fun writeCrash(throwable: Throwable, where: String) {
        runCatching {
            java.io.File(filesDir, "crash.log").writeText(
                throwable.javaClass.name + ": " + throwable.message + "\n" +
                    throwable.stackTraceToString() + "\nthread: " + where + "\nat: " +
                    java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                        .format(java.util.Date())
            )
        }
    }

    /**
     * Writes the last uncaught exception to filesDir/crash.log so the crash
     * trace can be read from inside the app (Home dialog + Settings -> Crash log).
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrash(throwable, thread.name)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
