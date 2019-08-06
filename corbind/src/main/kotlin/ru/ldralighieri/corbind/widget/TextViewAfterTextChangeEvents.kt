@file:Suppress("EXPERIMENTAL_API_USAGE")

package ru.ldralighieri.corbind.widget

import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.annotation.CheckResult
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
import ru.ldralighieri.corbind.internal.corbindReceiveChannel
import ru.ldralighieri.corbind.internal.safeOffer

// -----------------------------------------------------------------------------------------------

data class TextViewAfterTextChangeEvent(
        val view: TextView,
        val editable: Editable?
)

// -----------------------------------------------------------------------------------------------


/**
 * Perform an action after text change events for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
fun TextView.afterTextChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TextViewAfterTextChangeEvent) -> Unit
) {

    val events = scope.actor<TextViewAfterTextChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this))
    val listener = listener(scope = scope, textView = this, emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}

/**
 * Perform an action after text change events for [TextView] inside new [CoroutineScope].
 *
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 * @param action An action to perform
 */
suspend fun TextView.afterTextChangeEvents(
        capacity: Int = Channel.RENDEZVOUS,
        action: suspend (TextViewAfterTextChangeEvent) -> Unit
) = coroutineScope {

    val events = actor<TextViewAfterTextChangeEvent>(Dispatchers.Main, capacity) {
        for (event in channel) action(event)
    }

    events.offer(initialValue(this@afterTextChangeEvents))
    val listener = listener(scope = this, textView = this@afterTextChangeEvents,
            emitter = events::offer)
    addTextChangedListener(listener)
    events.invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------

/**
 * Create a channel of after text change events for [TextView].
 *
 * @param scope Root coroutine scope
 * @param capacity Capacity of the channel's buffer (no buffer by default)
 */
@CheckResult
fun TextView.afterTextChangeEvents(
        scope: CoroutineScope,
        capacity: Int = Channel.RENDEZVOUS
): ReceiveChannel<TextViewAfterTextChangeEvent> = corbindReceiveChannel(capacity) {
    safeOffer(initialValue(this@afterTextChangeEvents))
    val listener = listener(scope, this@afterTextChangeEvents, ::safeOffer)
    addTextChangedListener(listener)
    invokeOnClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


/**
 * Create a channel of after text change events for [TextView].
 *
 * *Note:* A value will be emitted immediately on collect.
 */
@CheckResult
private fun TextView.afterTextChangeEvents(): Flow<TextViewAfterTextChangeEvent> = channelFlow {
    offer(initialValue(this@afterTextChangeEvents))
    val listener = listener(this, this@afterTextChangeEvents, ::offer)
    awaitClose { removeTextChangedListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun initialValue(textView: TextView): TextViewAfterTextChangeEvent =
        TextViewAfterTextChangeEvent(textView, textView.editableText)


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        textView: TextView,
        emitter: (TextViewAfterTextChangeEvent) -> Boolean
) = object : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {  }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {  }

    override fun afterTextChanged(s: Editable) {
        if (scope.isActive) { emitter(TextViewAfterTextChangeEvent(textView, s)) }
    }

}
