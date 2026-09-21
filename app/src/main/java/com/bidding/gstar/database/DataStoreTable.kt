package com.bidding.gstar.database

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class DataStoreTable(private val dataBaseListner: DataFetchListener) : EntryRepository {

    var database = FirebaseFirestore.getInstance()

    fun login(email: String, password: String, context: Context): Boolean {
        val intent = Intent("com.bidding.gstar.login")
        var response = false

        database.collection("loginData")
            .whereEqualTo("username", email)
            .get()
            .addOnSuccessListener { documentReference ->
                if (documentReference != null && !documentReference.documents.isEmpty()) {
                    for (document in documentReference) {
                        Log.e("TAG", " Login Data " + document.data)
                        val map: MutableMap<String, Any> = document.data
                        if (map["username"] == email && map["password"] == password) {
                            Log.e("TAG", "Data from server email login=> ${map["username"]}")
                            dataBaseListner.onLoginDataFetchSuccess(true)
                            response = true
                            intent.putExtra("login", true)
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            Log.e("TAG", "Data from server login => ${map["username"]}")
                        } else {
                            Log.e("TAG", "Login failed")
                            dataBaseListner.onLoginDataFetchSuccess(false)
                            intent.putExtra("login", false)
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                            response = false
                        }
                    }
                } else {
                    Log.e("TAG", " Data from server No such document")
                    dataBaseListner.onLoginDataFetchSuccess(false)
                    intent.putExtra("login", false)
                    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                    response = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", "Data from server Error adding document", e)
                dataBaseListner.onLoginDataFetchSuccess(false)
                response = false
                intent.putExtra("login", false)
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
            }

        return response
    }

    fun fetchAllLoginData() {
        database.collection("loginData")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    for (document in snapshot) {
                        Log.e("TAG", "All Login Data ${document.data}")
                    }
                    Log.e("TAG", "Fetched ${snapshot.size()} login documents")
                } else {
                    Log.e("TAG", "No loginData documents found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", "Error fetching loginData documents", e)
            }
    }

    fun updatePassword(passwordNew: String): Boolean {
        val dataInserted = false
        val user: MutableMap<String, Any> = HashMap()
        user["password"] = passwordNew
        database.collection("loginData").document("login")
            .update(user)
        Log.e("TAG", "passwordNew  $passwordNew")
        return dataInserted
    }

    fun insertEntryData(entryJDO: EntryJDO): Boolean {
        var dataInserted = false
        val entryData: MutableMap<String, Any> = HashMap()
        entryData["dateLong"] = entryJDO.dateLong
        entryData["dateString"] = entryJDO.dateString
        entryData["entry"] = entryJDO.entry
        entryData["dateEntry"] = entryJDO.dateEntry

        database.collection("entry").document(entryJDO.dateString)
            .set(entryData)
            .addOnSuccessListener { documentReference ->
                Log.d("TAG", "DocumentSnapshot added with ID: $documentReference")
                dataInserted = true
                dataBaseListner.onDataInsertSuccess(true)
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error adding document", e)
                dataInserted = false
                dataBaseListner.onDataInsertSuccess(false)
            }

        return dataInserted
    }

    override fun saveDay(entry: EntryJDO) {
        insertEntryData(entry)
    }

    override fun deleteDay(dateString: String) {
        if (dateString.isBlank()) {
            dataBaseListner.onDataDeleteSuccess(false)
            return
        }
        database.collection("entry").document(dateString)
            .delete()
            .addOnSuccessListener {
                Log.d("TAG", "Deleted entry document $dateString")
                dataBaseListner.onDataDeleteSuccess(true)
            }
            .addOnFailureListener { error ->
                Log.w("TAG", "Error deleting document $dateString", error)
                dataBaseListner.onDataDeleteSuccess(false)
            }
    }

    fun fetchCurrentDateEntry(dateString: String): EntryJDO {
        val entryJdo = EntryJDO()
        database.collection("entry")
            .whereEqualTo("dateString", dateString)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val mapped = mapToEntry(snapshot.documents.first().data.orEmpty())
                    Log.e("TAG", "Data from server email=> ${mapped.dateString}")
                    Log.e("TAG", "Data from server email=> ${mapped.entry}")
                    dataBaseListner.onDataFetchSuccess(mapped)
                } else {
                    Log.e("TAG", "No such document for date $dateString")
                    dataBaseListner.onDataFetchSuccess(entryJdo)
                }
            }
            .addOnFailureListener { e ->
                Log.e("TAG", "Error fetching current date document", e)
                dataBaseListner.onDataFetchFailure(e)
            }
        return entryJdo
    }

    fun fetchEntryList(context: Context) {
        database.collection("entry")
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                deliverEntryList(snapshot, context)
            }
            .addOnFailureListener { serverError ->
                if (isPermissionDenied(serverError)) {
                    Log.e(
                        "TAG",
                        "Firestore rejected entry reads. Publish firestore.rules on project gstarff-android.",
                        serverError
                    )
                } else {
                    Log.e("TAG", "Server fetch failed, trying cache", serverError)
                }
                database.collection("entry")
                    .get(Source.CACHE)
                    .addOnSuccessListener { cacheSnapshot ->
                        if (cacheSnapshot != null && !cacheSnapshot.isEmpty) {
                            deliverEntryList(cacheSnapshot, context)
                        } else {
                            notifyEntryListFailure(serverError, context)
                        }
                    }
                    .addOnFailureListener {
                        notifyEntryListFailure(serverError, context)
                    }
            }
    }

    private fun deliverEntryList(snapshot: QuerySnapshot, context: Context) {
        val intent = Intent("com.bidding.gstar.fetch")
        val entryListJdo = arrayListOf<EntryJDO>()
        for (document in snapshot) {
            val data = document.data ?: continue
            val entryJdo = mapToEntry(data)
            entryListJdo.add(entryJdo)
            Log.e("TAG", "Data from server => ${entryJdo.dateString} ${entryJdo.entry}")
        }
        Log.e("TAG", "Fetched ${entryListJdo.size} entry documents cache=${snapshot.metadata.isFromCache}")
        dataBaseListner.onDataFetchSuccess(entryListJdo)
        intent.putExtra("dataFetched", entryListJdo.isNotEmpty())
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun notifyEntryListFailure(error: Exception, context: Context) {
        Log.e("TAG", "Error fetching entry list", error)
        dataBaseListner.onDataFetchFailure(error)
        val intent = Intent("com.bidding.gstar.fetch")
        intent.putExtra("dataFetched", false)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun isPermissionDenied(error: Exception): Boolean {
        return error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }

    private fun mapToEntry(map: Map<String, Any?>): EntryJDO {
        return EntryJDO(
            dateLong = toLong(map["dateLong"]),
            dateString = map["dateString"] as? String ?: "",
            entry = toIntList(map["entry"]),
            dateEntry = toLongList(map["dateEntry"])
        )
    }

    private fun toLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> 0L
        }
    }

    private fun toIntList(value: Any?): List<Int> {
        val list = value as? List<*> ?: return ArrayList()
        return list.mapNotNull { item ->
            when (item) {
                is Number -> item.toInt()
                else -> null
            }
        }
    }

    private fun toLongList(value: Any?): List<Long> {
        val list = value as? List<*> ?: return ArrayList()
        return list.mapNotNull { item ->
            when (item) {
                is Number -> item.toLong()
                else -> null
            }
        }
    }
}
