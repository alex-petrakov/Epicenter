package com.github.varhastra.epicenter.networking

/**
 * Defines common interface for all earthquake data providers.
 */
interface EventServiceProvider {

    fun getWeekFeed(responseCallback: ResponseCallback)

    fun getDayFeed(responseCallback: ResponseCallback)

    interface ResponseCallback {
        fun onResult(response: EventServiceResponse)

        fun onFailure(t: Throwable?)
    }
}