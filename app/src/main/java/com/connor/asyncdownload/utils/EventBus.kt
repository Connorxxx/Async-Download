package com.connor.asyncdownload.utils

import com.connor.asyncdownload.type.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

@PublishedApi
internal val eventBus = MutableSharedFlow<Event>()

inline fun <reified T : Event> CoroutineScope.subscribe(crossinline block: (T) -> Unit) {
    launch {
        eventBus.collect {
            if (it is T) block(it)
        }
    }
}

fun CoroutineScope.post(event: Event) = launch { eventBus.emit(event) }
