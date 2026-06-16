package dev.jspade.mybriefcase.bookmarks

import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApp::class)
class SyncWorkerTest {

    @Before
    fun setup() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            RuntimeEnvironment.getApplication(),
            config,
        )
    }

    @Test
    fun `enqueue registers periodic sync work`() {
        val context = RuntimeEnvironment.getApplication()

        SyncWorker.enqueue(context)

        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork("mybriefcase_sync").get()

        assertTrue("SyncWorker should be enqueued", workInfos.isNotEmpty())
        assertTrue(
            "SyncWorker should be enqueued or running, was: ${workInfos[0].state}",
            workInfos[0].state == WorkInfo.State.ENQUEUED ||
                workInfos[0].state == WorkInfo.State.RUNNING,
        )
    }
}
