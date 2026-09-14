package com.bidding.khela.database

interface DataFetchListener {

    fun onDataFetchSuccess(jdo: Any)
    fun onLoginDataFetchSuccess(success : Boolean)
    fun onDataInsertSuccess(success : Boolean)
}