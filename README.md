# Introduction
`kotlin-protobuf-bson-codec` provide a `PBCodec<T>` to encode/decode from google protocol buffer & BSON genericly. It is 100% written by Kotlin.

from the [official mongo java driver introduction](https://mongodb.github.io/mongo-java-driver/)
> BSON Library - A standalone BSON library, with a new Codec infrastructure that you can use to build high-performance encoders and decoders without requiring an intermediate Map instance.

TLDR; `PBCodec<T>` is compatible to
* [Mongo Java driver](https://mongodb.github.io/mongo-java-driver/)
* [KMongo](https://github.com/Litote/kmongo)

# Gradle
```
compile 'io.github.gaplotech:kotlin-protobuf-bson-codec:0.1.1'
```

# Maven
```xml
<dependency>
  <groupId>io.github.gaplotech</groupId>
  <artifactId>kotlin-protobuf-bson-codec</artifactId>
  <version>0.1.1</version>
</dependency>
```

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

// base mongo repository
abstract class MongoRepository<T: Any>(databaseName: String) {
    private val client = MongoClients.create()
    protected val database: MongoDatabase = client.getDatabase(databaseName)
    protected abstract val collection: MongoCollection<T>

}

// generic for protobuf repository
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

// specific protobuf type to extends MongoPBRepository<T: Message>
class MyTestV3Repository: MongoPBRepository<MyTestV3>("protodb") {
    override val collection: MongoCollection<MyTestV3>
        get() = getCollectionWithCodec("mytestv3")
}

```


