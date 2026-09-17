package com.t3code.explorer.domain.util

import kotlinx.coroutines.CancellationException

/** Never turn coroutine cancellation into a successful-looking operation failure. */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Result.failure(error)
}
