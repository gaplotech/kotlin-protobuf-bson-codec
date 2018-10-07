# Introduction
`kotlin-protobuf-bson-codec` provide a `PBCodecProvider` to encode/decode from google protocol buffer & BSON genericly.
It is 100% written by Kotlin.

The underlying implementation (`PBBsonReader` & `PBBsonWriter`) are mainly inspired by
the official google protocol buffer json serialization (`JsonFormat.Parser` & `JsonFormat.Printer`).

# Compatibility
`PBCodecProvider` is compatible to all mongodb driver using `org.bson.*`, for example
* [Mongo Java driver](https://mongodb.github.io/mongo-java-driver/)
* [KMongo](https://github.com/Litote/kmongo)

*Note: Only Proto3 is supported*

# Gradle
```
compile 'io.github.gaplotech:kotlin-protobuf-bson-codec:0.3.0'
```

# Maven
```xml
<dependency>
  <groupId>io.github.gaplotech</groupId>
  <artifactId>kotlin-protobuf-bson-codec</artifactId>
  <version>0.3.0</version>
</dependency>
```

# Example

```kotlin
// register the codec provider
val registry = CodecRegistries.fromRegistries(
                   CodecRegistries.fromProviders(PBCodecProvider()),
                   MongoClients.getDefaultCodecRegistry()
               )

// use the registry for the database
val collection = createClient()
    .getDatabase(...)
    .withCodecRegistry(registry)
```
