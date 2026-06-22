package dev.jspade.mybriefcase.bookmarks

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withDataModifier
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class ArchitectureTest {
    private val appScope =
        Konsist.scopeFromPackage("dev.jspade.mybriefcase.bookmarks..")

    @Test
    fun `data classes should only have val properties`() {
        appScope
            .classes()
            .withDataModifier()
            .properties()
            .assertTrue { it.isVal }
    }

    @Test
    fun `ViewModels should reside in ui package`() {
        appScope
            .classes()
            .withParentOf(androidx.lifecycle.ViewModel::class)
            .assertTrue { it.resideInPackage("..ui..") }
    }

    @Test
    fun `ViewModels should have ViewModel suffix`() {
        appScope
            .classes()
            .withParentOf(androidx.lifecycle.ViewModel::class)
            .assertTrue { it.hasNameEndingWith("ViewModel") }
    }

    @Test
    fun `classes with ViewModel suffix should extend ViewModel`() {
        appScope
            .classes()
            .withNameEndingWith("ViewModel")
            .assertTrue {
                it.hasParentOf(androidx.lifecycle.ViewModel::class) ||
                    it.hasParentOf(androidx.lifecycle.AndroidViewModel::class)
            }
    }

    @Test
    fun `data layer should not depend on ui layer`() {
        appScope
            .files
            .filter { it.packagee?.name?.contains(".data") == true }
            .flatMap { it.imports }
            .assertTrue { !it.name.contains(".ui.") }
    }

    @Test
    fun `data layer should not import Android framework classes`() {
        appScope
            .files
            .filter { it.packagee?.name?.contains(".data") == true }
            .flatMap { it.imports }
            .assertTrue {
                !it.name.startsWith("android.") &&
                    !it.name.startsWith("androidx.")
            }
    }

    @Test
    fun `repository interfaces should reside in data package`() {
        appScope
            .interfaces()
            .withNameEndingWith("Repository")
            .assertTrue { it.resideInPackage("..data..") }
    }

    @Test
    fun `repository implementations should reside in data package`() {
        appScope
            .classes()
            .withNameEndingWith("RepositoryImpl")
            .assertTrue { it.resideInPackage("..data..") }
    }

    @Test
    fun `interfaces should not have I prefix`() {
        appScope
            .interfaces()
            .assertTrue { !it.name.startsWith("I") || it.name.length < 2 || !it.name[1].isUpperCase() }
    }

    @Test
    fun `ViewModel state classes should be data classes`() {
        appScope
            .classes()
            .withNameEndingWith("UiState")
            .assertTrue { it.hasDataModifier }
    }
}
