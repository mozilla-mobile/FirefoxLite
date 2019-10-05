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

        fun <T, E> error(msg: String? = null, error: E): Result<T, E> {
            return Result(Status.Error, null, error, msg)
        }
    }

    sealed class Status {
        object Success : Status()
        object Error : Status()
    }
}

val <T, E> Result<T, E>.isSuccess: Boolean
    get() = status == Result.Status.Success

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

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> {
    return when (status) {
        is Result.Status.Success -> Result.success(transform(data!!))
        is Result.Status.Error -> Result.error(error = error!!)
    }
}

inline fun <T, E, R, W> Result<T, E>.map(transformResult: (T) -> R, transformError: (E) -> W): Result<R, W> {
    return when (status) {
        is Result.Status.Success -> Result.success(transformResult(data!!))
        is Result.Status.Error -> Result.error(error = transformError(error!!))
    }
}

inline fun <T, E> kotlin.Result<T>.toFxResult(transformError: (Throwable) -> E): Result<T, E> {
    return if (isSuccess) {
        Result.success(getOrNull()!!)
    } else {
        Result.error(error = transformError(exceptionOrNull()!!))
    }
}