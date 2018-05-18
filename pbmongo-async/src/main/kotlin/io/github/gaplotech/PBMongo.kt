package io.github.gaplotech

import com.google.protobuf.Message
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries

object PBMongo {
    @JvmStatic
    fun MongoDatabase.addPBCodec(pbCodecProvider: PBCodecProvider) =
        this.withCodecRegistry(CodecRegistries.fromRegistries(
            CodecRegistries.fromProviders(pbCodecProvider),
            this.codecRegistry
        ))

    @JvmStatic
    fun <T : Message> getPbCollection(db: MongoDatabase, name: String, clazz: Class<T>): MongoCollection<T> {
        val codec = PBCodec(clazz)
        val registry = CodecRegistries.fromCodecs(codec)
        return db.getCollection(name).withCodecRegistry(registry).withDocumentClass(clazz)
    }

    inline fun <reified T : Message> MongoDatabase.getPbCollection(name: String): MongoCollection<T> =
        PBMongo.getPbCollection(this, name, T::class.java)

}
