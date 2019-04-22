package com.github.varhastra.epicenter.domain

import com.github.varhastra.epicenter.domain.model.FeedFilter

interface FeedStateDataSource {

    fun saveSelectedPlaceId(id: Int)

    fun getSelectedPlaceId(): Int

    fun saveCurrentFilter(filter: FeedFilter)

    fun getCurrentFilter(): FeedFilter
}