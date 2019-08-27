package org.mozilla.rocket.msrp.domain

abstract class UseCase<in P, R> {
    abstract suspend fun execute(parameters: P): R
}
