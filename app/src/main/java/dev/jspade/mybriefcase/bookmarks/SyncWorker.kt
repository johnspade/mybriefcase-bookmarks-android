package dev.jspade.mybriefcase.bookmarks

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        try {
            val app = applicationContext as MyBriefcaseApp
            app.repository.triggerFullMerge()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }

    companion object {
        private const val WORK_NAME = "mybriefcase_sync"
        private const val SYNC_INTERVAL_MINUTES = 15L

        fun enqueue(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
                    .build()
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }
    }
}
