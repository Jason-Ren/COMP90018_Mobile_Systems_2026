package com.comp90018.flashcards.testutil

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

// lets virtual time move on, then runs whatever became due
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.passMillis(millis: Long) {
    advanceTimeBy(millis)
    runCurrent()
}

fun TestScope.passSeconds(seconds: Int) = passMillis(seconds * 1_000L)

// state flows that only run while somebody collects them need a subscriber for the test's whole length
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> TestScope.keepCollecting(flow: Flow<T>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
}
