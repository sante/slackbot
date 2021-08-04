package com.xmartlabs.slackbot.extensions

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalContracts::class, ExperimentalTime::class)
suspend inline fun <T> Mutex.withLockusingStabilizationDelay(delay: Duration, owner: Any? = null, action: () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }

    lock(owner)
    try {
        return action()
    } finally {
        GlobalScope.launch {
            delay(delay)
            unlock(owner)
        }
    }
}
