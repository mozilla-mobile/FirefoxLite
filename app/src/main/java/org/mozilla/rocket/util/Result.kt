package org.mozilla.rocket.util

data class Result<out T, E>(
    val status: Status,
    val data: T? = null,
    val error: E? = null,
    val message: String?
) {
    companion object {
        fun <T, E> success(data: T): Result<T, E> {
            return Result(Status.Success, data, null, null)
        }

        fun <T, E> error(msg: String? = null, error: E? = null): Result<T, E> {
            return Result(Status.Error, null, error, msg)
        }
    }

    sealed class Status {
        object Success : Status()
        object Error : Status()
    }
}
