package io.github.gaplotech.repository

import com.google.protobuf.*
import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import io.github.gaplotech.PBCodec
import org.bson.codecs.*
import org.bson.codecs.configuration.CodecRegistries


abstract class MongoPBRepository<T : Message>(databaseName: String) : MongoRepository<T>(databaseName) {
    inline fun <reified T : Message> getCollectionWithCodec(collectionName: String): MongoCollection<T> {
        val codec: Codec<T> = PBCodec(clazz = T::class.java)
        val registry = CodecRegistries.fromRegistries(
            CodecRegistries.fromCodecs(codec),
            MongoClients.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(DocumentCodecProvider(), IterableCodecProvider())
        )
        return `access$database`.withCodecRegistry(registry).getCollection(collectionName, T::class.java)
    }

    @PublishedApi
    internal val `access$database`: MongoDatabase
        get() = database

}

