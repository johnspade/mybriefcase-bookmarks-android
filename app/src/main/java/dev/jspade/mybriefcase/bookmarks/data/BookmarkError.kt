package dev.jspade.mybriefcase.bookmarks.data

import uniffi.mybriefcase_bookmarks_ffi.FfiException

sealed interface BookmarkError {
    val message: String

    data class NotFound(
        override val message: String,
    ) : BookmarkError

    data class InvalidInput(
        override val message: String,
    ) : BookmarkError

    data class IoError(
        override val message: String,
    ) : BookmarkError

    data class NotInitialized(
        override val message: String,
    ) : BookmarkError

    data class Internal(
        override val message: String,
    ) : BookmarkError

    companion object {
        fun from(exception: FfiException): BookmarkError =
            when (exception) {
                is FfiException.NotFound -> NotFound(exception.msg)
                is FfiException.InvalidInput -> InvalidInput(exception.msg)
                is FfiException.IoException -> IoError(exception.msg)
                is FfiException.NotInitialized -> NotInitialized(exception.msg)
                is FfiException.Internal -> Internal(exception.msg)
            }
    }
}
