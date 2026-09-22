package com.bidding.gstar.database

import android.content.Context

interface EntryRepository {
    fun saveDay(entry: EntryJDO)
    fun deleteDay(dateString: String)
    fun fetchAllDays(context: Context)
}
