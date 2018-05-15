package io.github.gaplotech.repository

import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import io.github.gaplotech.PBCodecProvider
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.IterableCodecProvider
import org.bson.codecs.configuration.CodecRegistries
import org.litote.kmongo.async.KMongo

abstract class MongoRepository<T : Any>(databaseName: String) {
    private val client = KMongo.createClient() //get com.mongodb.async.client.MongoClient new instance
    protected val database: MongoDatabase = client.getDatabase(databaseName)  //normal java driver usage
        .withCodecRegistry(CodecRegistries.fromRegistries(
            MongoClients.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(DocumentCodecProvider(), IterableCodecProvider(), PBCodecProvider())
        ))
    protected abstract val collection: MongoCollection<T> //KMongo extension method

}
