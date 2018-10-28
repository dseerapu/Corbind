package ru.ldralighieri.corbind.viewpager

import androidx.annotation.CheckResult
import androidx.viewpager.widget.ViewPager
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun ViewPager.pageSelections(
        scope: CoroutineScope,
        action: suspend (Int) -> Unit
) {

    val events = scope.actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    events.offer(currentItem)
    val listener = listener(scope, events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}

suspend fun ViewPager.pageSelections(
        action: suspend (Int) -> Unit
) = coroutineScope {

    val events = actor<Int>(Dispatchers.Main, Channel.CONFLATED) {
        for (position in channel) action(position)
    }

    events.offer(currentItem)
    val listener = listener(this, events::offer)
    addOnPageChangeListener(listener)
    events.invokeOnClose { removeOnPageChangeListener(listener) }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
fun ViewPager.pageSelections(
        scope: CoroutineScope
): ReceiveChannel<Int> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    offer(currentItem)
    val listener = listener(this, ::offer)
    addOnPageChangeListener(listener)
    invokeOnClose { removeOnPageChangeListener(listener) }
}

@CheckResult
suspend fun ViewPager.pageSelections(): ReceiveChannel<Int> = coroutineScope {

    produce<Int>(Dispatchers.Main, Channel.CONFLATED) {
        offer(currentItem)
        val listener = listener(this, ::offer)
        addOnPageChangeListener(listener)
        invokeOnClose { removeOnPageChangeListener(listener) }
    }
}


// -----------------------------------------------------------------------------------------------


@CheckResult
private fun listener(
        scope: CoroutineScope,
        emitter: (Int) -> Boolean
) = object : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {  }
    override fun onPageSelected(position: Int) { emitter(position) }
    override fun onPageScrollStateChanged(state: Int) {  }
}