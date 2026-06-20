package dev.jspade.mybriefcase.bookmarks.ui.wizard

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WizardUiState(
    val selectedPath: String? = null,
    val canFinish: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false,
)

@SuppressLint("StaticFieldLeak") // Always receives applicationContext from NavHost
class WizardViewModel(
    private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    fun onDirectorySelected(uri: Uri) {
        val path = SyncDirResolver.resolveTreeUri(uri)
        if (path != null) {
            _uiState.update { it.copy(selectedPath = path, canFinish = true, error = null) }
        } else {
            _uiState.update {
                it.copy(
                    selectedPath = null,
                    canFinish = false,
                    error = "Please select a folder on internal storage",
                )
            }
        }
    }

    fun finish() {
        val path = _uiState.value.selectedPath ?: return
        StartupDecision.persistSyncDir(context, path)
        _uiState.update { it.copy(isComplete = true) }
    }
}
