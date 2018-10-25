package ru.ldralighieri.corbind.material

import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.coroutineScope

// -----------------------------------------------------------------------------------------------


fun BottomNavigationView.itemSelections(
        scope: CoroutineScope,
        action: suspend (MenuItem) -> Unit
) {
    val events = scope.actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this, events::offer)
    setOnNavigationItemSelectedListener(listener(events::offer))
    events.invokeOnClose { setOnNavigationItemSelectedListener(null) }
}

suspend fun BottomNavigationView.itemSelections(
        action: suspend (MenuItem) -> Unit
) = coroutineScope {
    val events = actor<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        for (item in channel) action(item)
    }

    setInitialValue(this@itemSelections, events::offer)
    setOnNavigationItemSelectedListener(listener(events::offer))
    events.invokeOnClose { setOnNavigationItemSelectedListener(null) }
}


// -----------------------------------------------------------------------------------------------


fun BottomNavigationView.itemSelections(
        scope: CoroutineScope
): ReceiveChannel<MenuItem> = scope.produce(Dispatchers.Main, Channel.CONFLATED) {

    setInitialValue(this@itemSelections, ::offer)
    setOnNavigationItemSelectedListener(listener(::offer))
    invokeOnClose { setOnNavigationItemSelectedListener(null) }
}

suspend fun BottomNavigationView.itemSelections(): ReceiveChannel<MenuItem> = coroutineScope {

    produce<MenuItem>(Dispatchers.Main, Channel.CONFLATED) {
        setInitialValue(this@itemSelections, ::offer)
        setOnNavigationItemSelectedListener(listener(::offer))
        invokeOnClose { setOnNavigationItemSelectedListener(null) }
    }
}


// -----------------------------------------------------------------------------------------------


private fun setInitialValue(
        bottomNavigationView: BottomNavigationView,
        emitter: (MenuItem) -> Boolean
) {
    val menu = bottomNavigationView.menu
    for (i in 0 until menu.size()) {
        val item = menu.getItem(i)
        if (item.isChecked) {
            emitter(item)
            break
        }
    }
}


// -----------------------------------------------------------------------------------------------


private fun listener(
        emitter: (MenuItem) -> Boolean
) = BottomNavigationView.OnNavigationItemSelectedListener(emitter::invoke)