package dev.jspade.mybriefcase.bookmarks.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generateProfile() {
        rule.collect(
            packageName = "dev.jspade.mybriefcase.bookmarks",
        ) {
            pressHome()
            startActivityAndWait()

            // Wait for the bookmark list to render
            device.wait(Until.hasObject(By.scrollable(true)), 10_000)

            // Scroll the list to capture hot paths during fling
            val list = device.findObject(By.scrollable(true))
            list?.fling(Direction.DOWN)
            device.waitForIdle()
            list?.fling(Direction.UP)
            device.waitForIdle()
        }
    }
}
