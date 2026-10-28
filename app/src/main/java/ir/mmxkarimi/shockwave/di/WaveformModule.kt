package ir.mmxkarimi.shockwave.di

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import ir.mmxkarimi.shockwave.data.waveform.FileWaveformStore
import ir.mmxkarimi.shockwave.data.waveform.MediaFingerprint
import ir.mmxkarimi.shockwave.data.waveform.WaveformExtractor
import ir.mmxkarimi.shockwave.data.waveform.WaveformRepositoryImpl
import ir.mmxkarimi.shockwave.data.waveform.WaveformStore
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier

/**
 * Factory for the real waveform repository (wired into [AppContainer] by the integration task). Owns the
 * dedicated extraction thread: one thread, `THREAD_PRIORITY_BACKGROUND`, exits after 20 s idle.
 */
class WaveformModule private constructor() {
    companion object {
        /** With the simple file-backed store under `filesDir/waveforms`. */
        @JvmStatic fun create(context: Context): WaveformRepositoryImpl {
            return create(context, FileWaveformStore(File(context.filesDir, "waveforms")))
        }

        /** With the system's battery saver deciding whether background prefetch is skipped. */
        @JvmStatic fun create(context: Context, store: WaveformStore): WaveformRepositoryImpl {
            val power = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager?
            return create(context, store, power!!::isPowerSaveMode)
        }

        /** With a caller-provided store (e.g. the SQLite-backed one) and battery-saver state (called on the worker too). */
        @JvmStatic fun create(
            context: Context,
            store: WaveformStore,
            powerSave: BooleanSupplier,
        ): WaveformRepositoryImpl {
            val app = context.applicationContext
            val worker = ThreadPoolExecutor(
                1, 1, 20, TimeUnit.SECONDS, LinkedBlockingQueue(),
                ThreadFactory { runnable ->
                    Thread(
                        {
                            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                            runnable.run()
                        },
                        "waveform-extractor",
                    )
                },
            )
            worker.allowCoreThreadTimeOut(true)
            val mainHandler = Handler(Looper.getMainLooper())
            return WaveformRepositoryImpl(
                store, WaveformExtractor(app), MediaFingerprint(app),
                powerSave, worker, mainHandler::post,
            )
        }
    }
}
