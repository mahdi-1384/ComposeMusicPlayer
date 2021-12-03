package ir.avesta.musicplayer.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

class EventMutableLiveData<T> : MutableLiveData<Event<T>> {

    private var initialValue: T? = null

    constructor() : super() {}

    constructor(initialValue: T?) : super(Event(initialValue)) {
        this.initialValue = initialValue
    }

    override fun setValue(value: Event<T>?) {

        if (this.value?.accessibleData == null && value?.accessibleData == null)
            return

        if (this.value == value)
            return

        super.setValue(value)
    }
}

class Event<T>(data: T?) {
    var isRead = false

    val accessibleData = data

    val data: T? = data
        get() {
            this.isRead = true

            return field
        }
}

class EventObserver<T>(private val block: (T?) -> Unit) : Observer<Event<T>> {
    override fun onChanged(event: Event<T>?) {
        if (event?.isRead == true)
            return

        block(event?.data)
    }
}