package ru.kaelesty.madprojects.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun nowMillis() = Clock.System.now().toEpochMilliseconds()