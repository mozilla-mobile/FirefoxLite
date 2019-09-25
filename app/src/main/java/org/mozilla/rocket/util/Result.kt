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

inline fun <T, E> Result<T, E>.get(fallback: (E) -> Unit): T? {
    return when (status) {
        is Result.Status.Success -> data ?: error("Result status 'success' with data: null")
        is Result.Status.Error -> {
            val nonNullError = error ?: error("Result status 'Error' with error: null")
            fallback(nonNullError)
            return null
        }
    }
}

inline fun <T, E> Result<T, E>.getNotNull(fallback: (E) -> Unit): T {
    return get(fallback) ?: error("You must return out before the end of the fallback")
}