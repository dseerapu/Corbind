/*
 * Copyright 2019 Vladimir Raupov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.ldralighieri.corbind.material

import android.view.View
import androidx.annotation.CheckResult
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import ru.ldralighieri.corbind.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.AlwaysTrue
import ru.ldralighieri.corbind.safeOffer

/**
 * Perform an action on [TextInputLayout] end icon long click events.
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnLongClickListener] to emit long
 * clicks. Only one actor can be used for a view at a time.
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]
 * @param action An action to perform
 */
fun TextInputLayout.endIconLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend () -> Unit
) {
    val events = scope.actor<Unit>(Dispatchers.Main.immediate, capacity) {
        for (ignored in channel) action()
    }

    setEndIconOnLongClickListener(listener(scope, handled, events::offer))
    events.invokeOnClose { setEndIconOnLongClickListener(null) }
}

/**
 * Perform an action on [TextInputLayout] end icon long click events, inside new [CoroutineScope].
 *
 * *Warning:* The created actor uses [TextInputLayout.setEndIconOnLongClickListener]. Only one actor
 * can be used at a time.
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]
 * @param action An action to perform
 */
suspend fun TextInputLayout.endIconLongClicks(
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue,
    action: suspend () -> Unit
) = coroutineScope {
    endIconLongClicks(this, capacity, handled, action)
}

/**
 * Create a channel which emits on [TextInputLayout] end icon long click events.
 *
 * *Warning:* The created channel uses [TextInputLayout.setEndIconOnLongClickListener]. Only one
 * channel can be used at a time.
 *
 * Example:
 *
 * ```
 * launch {
 *      textInputLayout.endIconLongClicks(scope)
 *          .consumeEach { /* handle end icon long click */ }
 * }
 * ```
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]
 */
@CheckResult
fun TextInputLayout.endIconLongClicks(
    scope: CoroutineScope,
    capacity: Int = Channel.RENDEZVOUS,
    handled: () -> Boolean = AlwaysTrue
): ReceiveChannel<Unit> = corbindReceiveChannel(capacity) {
    setEndIconOnLongClickListener(listener(scope, handled, ::safeOffer))
    invokeOnClose { setEndIconOnLongClickListener(null) }
}

/**
 * Create a flow which emits on [TextInputLayout] end icon long click events.
 *
 * *Warning:* The created flow uses [TextInputLayout.setEndIconOnLongClickListener]. Only one flow
 * can be used at a time.
 *
 * Example:
 *
 * ```
 * textInputLayout.endIconLongClicks()
 *      .onEach { /* handle end icon long click */ }
 *      .launchIn(scope)
 * ```
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the underlying
 * [View.OnLongClickListener]
 */
@CheckResult
fun TextInputLayout.endIconLongClicks(
    handled: () -> Boolean = AlwaysTrue
): Flow<Unit> = channelFlow {
    setEndIconOnLongClickListener(listener(this, handled, ::offer))
    awaitClose { setEndIconOnLongClickListener(null) }
}

@CheckResult
private fun listener(
    scope: CoroutineScope,
    handled: () -> Boolean,
    emitter: (Unit) -> Boolean
) = View.OnLongClickListener {
    if (scope.isActive && handled()) {
        emitter(Unit)
        return@OnLongClickListener true
    }
    return@OnLongClickListener false
}