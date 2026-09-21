package com.bidding.gstar.database

interface DataFetchListener {

    fun onDataFetchSuccess(jdo: Any)
    fun onLoginDataFetchSuccess(success: Boolean)
    fun onDataInsertSuccess(success: Boolean)
    fun onDataFetchFailure(error: Exception)
    fun onDataDeleteSuccess(success: Boolean) {}
}