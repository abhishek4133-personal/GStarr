package com.bidding.gstar.database

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.util.ArrayList

@JsonIgnoreProperties(ignoreUnknown = true)
data class EntryJDO(@JsonProperty("dateLong") var dateLong : Long = 0,
                    @JsonProperty("dateString") var dateString : String="",
                    @JsonProperty("dateEntry") var dateEntry: List<Long> =  ArrayList(),
                    @JsonProperty("entry") var entry: List<Int> =  ArrayList()) : Serializable {

    override fun toString(): String {
        return "EntryJDO(dateLong=$dateLong, dateString='$dateString', dateEntry=$dateEntry, entry=$entry)"
    }

    fun hasSlot(index: Int): Boolean {
        return index in entry.indices
    }

    fun withoutSlot(index: Int): EntryJDO {
        if (!hasSlot(index)) {
            return this
        }
        val values = entry.toMutableList()
        val times = dateEntry.toMutableList()
        values.removeAt(index)
        if (index in times.indices) {
            times.removeAt(index)
        }
        return copy(entry = values, dateEntry = times)
    }

    fun isEmptyDay(): Boolean {
        return entry.isEmpty()
    }

    fun filledCount(): Int {
        return entry.size
    }

    fun pattiPreview(): String {
        return entry.joinToString(" · ")
    }
}