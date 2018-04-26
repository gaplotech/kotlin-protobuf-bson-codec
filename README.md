# Getting Started
kotlin-protobuf-bson-codec provide a google protocol buffer BSON codec for MongoDB and it is completely written by Kotlin.

# Gradle
TODO

# Maven
TODO

# Examples

1. Simple use case
```kotlin
  // use the PBCodec
  val codec: Codec<PBClass> = PBCodec(clazz = PBClass::class.java)

  // register the codec
  val registry = CodecRegistries.fromRegistries(
          CodecRegistries.fromCodecs(codec),
          MongoClients.getDefaultCodecRegistry(),
          CodecRegistries.fromProviders(DocumentCodecProvider(), IterableCodecProvider())
  )

  // use the registry for the database
  val collection = createClient()
          .getDatabase(...)
          .withCodecRegistry(registry)
          .getCollection(..., PBClass::class.java)
```

2. Creating generic protobuf repositories. (using `MyTestV3` as example)
```kotlin
import com.google.protobuf.Message
import org.bson.codecs.DocumentCodecProvider
import org.bson.codecs.IterableCodecProvider
import org.bson.codecs.configuration.CodecRegistries
import com.mongodb.async.client.MongoClients
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import io.github.gaplotech.PBCodec
import org.bson.codecs.Codec
import io.github.gaplotech.pb.Test.*

abstract class MongoRepository<T: Any>(databaseName: String) {
    private val client = MongoClients.create() //get com.mongodb.async.client.MongoClient new instance
    protected val database: MongoDatabase = client.getDatabase(databaseName)  //normal java driver usage
    protected abstract val collection: MongoCollection<T> //KMongo extension method

}

abstract class MongoPBRepository<T: Message>(databaseName: String): MongoRepository<T>(databaseName) {
    inline fun <reified T: Message> getCollectionWithCodec(collectionName: String): MongoCollection<T> {
        val codec: Codec<T> = PBCodec(clazz = T::class.java)
        val registry = CodecRegistries.fromRegistries(
                CodecRegistries.fromCodecs(codec),
                MongoClients.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(DocumentCodecProvider(), IterableCodecProvider())
        )
        return `access$database`.withCodecRegistry(registry).getCollection(collectionName, T::class.java)
    }

    @PublishedApi
    internal val `access$database`: MongoDatabase get() = database

}

class MyTestV3Repository: MongoPBRepository<MyTestV3>("protodb") {
    override val collection: MongoCollection<MyTestV3>
        get() = getCollectionWithCodec("mytestv3")
}

```
