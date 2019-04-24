package com.github.varhastra.epicenter.domain.model

class RemoteEvent(val event: Event, point: Coordinates? = null) {
    var distance: Double? = null
        private set

    init {
        point?.let {
            distance = event.coordinates.distanceTo(it)
        }
    }
}