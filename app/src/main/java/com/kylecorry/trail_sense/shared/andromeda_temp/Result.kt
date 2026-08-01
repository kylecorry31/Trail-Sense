package com.kylecorry.trail_sense.shared.andromeda_temp

sealed interface Result<out T, out E> {
    data class Ok<T>(val value: T) : Result<T, Nothing>
    data class Err<E>(val error: E) : Result<Nothing, E>

    val isOk: Boolean
        get() = this is Ok

    val isErr: Boolean
        get() = this is Err
}

inline fun <T, E, R> Result<T, E>.map(
    transform: (T) -> R
): Result<R, E> = when (this) {
    is Result.Ok -> Result.Ok(transform(value))
    is Result.Err -> this
}

inline fun <T, E, F> Result<T, E>.mapError(
    transform: (E) -> F
): Result<T, F> = when (this) {
    is Result.Ok -> this
    is Result.Err -> Result.Err(transform(error))
}

inline fun <T, E, R> Result<T, E>.andThen(
    transform: (T) -> Result<R, E>
): Result<R, E> = when (this) {
    is Result.Ok -> transform(value)
    is Result.Err -> this
}

inline fun <T, E, R> Result<T, E>.fold(
    onOk: (T) -> R,
    onErr: (E) -> R
): R = when (this) {
    is Result.Ok -> onOk(value)
    is Result.Err -> onErr(error)
}

fun <T, E> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Ok -> value
    is Result.Err -> null
}

fun <T, E> Result<T, E>.errorOrNull(): E? = when (this) {
    is Result.Ok -> null
    is Result.Err -> error
}

fun <T, E> Result<T, E>.unwrap(): T = when (this) {
    is Result.Ok -> value
    is Result.Err -> error("Called unwrap() on Err($error)")
}

fun <T, E> Result<T, E>.unwrapError(): E = when (this) {
    is Result.Ok -> error("Called unwrap() on Ok($value)")
    is Result.Err -> error
}

fun <T, E> Result<T, E>.unwrapOr(default: T): T = when (this) {
    is Result.Ok -> value
    is Result.Err -> default
}

inline fun <T, E> Result<T, E>.unwrapOrElse(
    default: (E) -> T
): T = when (this) {
    is Result.Ok -> value
    is Result.Err -> default(error)
}
