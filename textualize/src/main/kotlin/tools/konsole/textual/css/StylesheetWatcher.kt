package tools.konsole.textual.css

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.atomic.AtomicLong

/**
 * Watch a TCSS file and invoke [onChange] whenever it's modified.
 * Phase 9.16 hot-reload primitive.
 *
 * Uses [java.nio.file.WatchService] which polls the filesystem natively
 * on most platforms (inotify on Linux, FSEvents on macOS, ReadDirectoryChangesW
 * on Windows). The callback runs on the [scope]'s coroutine dispatcher,
 * not on the watch thread itself, so [onChange] can do CPU/IO work without
 * holding the watch event loop.
 *
 * Mirrors textual's `--dev` mode CSS hot-reload (textual implements it via
 * watchgod/watchfiles in Python; konsole uses the JVM-native equivalent).
 *
 * @param file the .tcss file to watch.
 * @param scope coroutine scope that owns the watcher; cancel the scope to stop.
 * @param onChange called once per coalesced change burst with the new file content.
 * @param debounceMs collapse rapid filesystem events (some editors save in
 *   multiple steps) into a single callback after this idle period.
 */
public class StylesheetWatcher(
    public val file: File,
    private val scope: CoroutineScope,
    private val onChange: suspend (newContent: String) -> Unit,
    private val debounceMs: Long = 100L,
) {

    @Volatile private var job: Job? = null

    /** Begin watching. Idempotent. */
    public fun start() {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            runWatchLoop()
        }
    }

    /** Stop watching. */
    public fun stop() {
        job?.cancel()
    }

    private suspend fun runWatchLoop() {
        val target: Path = file.absoluteFile.toPath()
        val parent = target.parent ?: return
        val watcher: WatchService = parent.fileSystem.newWatchService()
        parent.register(
            watcher,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_CREATE,
        )
        val lastFireMs = AtomicLong(0L)
        try {
            while (scope.isActive) {
                val key = watcher.poll() ?: run {
                    delay(50)
                    return@run null
                }
                if (key != null) {
                    val events = key.pollEvents()
                    val touched = events.any { event ->
                        val ctx = event.context() as? Path ?: return@any false
                        ctx.fileName == target.fileName
                    }
                    if (touched) {
                        // Debounce: coalesce rapid writes.
                        val now = System.currentTimeMillis()
                        lastFireMs.set(now)
                        delay(debounceMs)
                        if (lastFireMs.get() <= now) {
                            try {
                                val content = file.readText()
                                withContext(scope.coroutineContext) {
                                    onChange(content)
                                }
                            } catch (_: Throwable) {
                                // file may have been deleted/renamed mid-edit; swallow
                            }
                        }
                    }
                    if (!key.reset()) break
                }
            }
        } finally {
            try { watcher.close() } catch (_: Throwable) {}
        }
    }
}
