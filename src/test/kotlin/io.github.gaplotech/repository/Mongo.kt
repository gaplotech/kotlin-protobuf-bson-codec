package io.github.gaplotech.repository

import com.google.protobuf.Message
import com.mongodb.async.client.MongoClient
import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import io.github.gaplotech.PBCodecProvider
import org.bson.codecs.configuration.CodecRegistries

abstract class MongoRepository<T>(databaseName: String) {

    private val client: MongoClient = MongoClients.create() // or `KMongo.createClient()` if using KMongo

    protected open val database: MongoDatabase = client.getDatabase(databaseName) // normal java driver usage

    protected abstract val collection: MongoCollection<T>
}

// directly read/write protobuf in a collection
abstract class MongoPBRepository<T : Message>(databaseName: String) : MongoRepository<T>(databaseName) {

    private val registry = CodecRegistries.fromProviders(PBCodecProvider())

    // register generic PBCodecProvider
    override val database: MongoDatabase = super.database.withCodecRegistry(registry)
}
