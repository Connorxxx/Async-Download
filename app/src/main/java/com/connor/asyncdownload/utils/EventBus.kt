package com.connor.asyncdownload.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.connor.asyncdownload.type.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@PublishedApi
internal val eventBus = MutableSharedFlow<Event>()

inline fun <reified T : Event> CoroutineScope.subscribe(crossinline block: suspend (T) -> Unit) {
    launch {
        eventBus.collect {
            if (it is T) block(it)
        }
    }
}

suspend fun post(event: Event) {
    eventBus.emit(event)
}

fun LifecycleOwner.post(event: Event) = lifecycleScope.launch { eventBus.emit(event) }