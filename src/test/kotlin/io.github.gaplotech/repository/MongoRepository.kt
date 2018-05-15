package io.github.gaplotech.repository

import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import org.litote.kmongo.async.KMongo

abstract class MongoRepository<T : Any>(databaseName: String) {
    private val client = KMongo.createClient() //get com.mongodb.async.client.MongoClient new instance
    protected val database: MongoDatabase = client.getDatabase(databaseName)  //normal java driver usage
    protected abstract val collection: MongoCollection<T> //KMongo extension method

}
