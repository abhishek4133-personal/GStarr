package com.bidding.gstar.database

interface EntryRepository {
    fun saveDay(entry: EntryJDO)
    fun deleteDay(dateString: String)
}
