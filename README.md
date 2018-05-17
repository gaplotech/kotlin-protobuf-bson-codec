# Introduction
`kotlin-protobuf-bson-codec` provide a `PBCodecProvider` to encode/decode from google protocol buffer & BSON genericly. It is 100% written by Kotlin.

from the [official mongo java driver introduction](https://mongodb.github.io/mongo-java-driver/)
> BSON Library - A standalone BSON library, with a new Codec infrastructure that you can use to build high-performance encoders and decoders without requiring an intermediate Map instance.

TLDR; `PBCodecProvider` is compatible to all mongodb driver using `org.bson.*`, for example
* [Mongo Java driver](https://mongodb.github.io/mongo-java-driver/)
* [KMongo](https://github.com/Litote/kmongo)

# Gradle
```
compile 'io.github.gaplotech:kotlin-protobuf-bson-codec:0.1.2'
```

# Maven
```xml
<dependency>
  <groupId>io.github.gaplotech</groupId>
  <artifactId>kotlin-protobuf-bson-codec</artifactId>
  <version>0.1.2</version>
</dependency>
```

# Example

```kotlin
// register the codec provider
val registry = CodecRegistries.fromRegistries(PBCodecProvider())

// use the registry for the database
val collection = createClient()
    .getDatabase(...)
    .withCodecRegistry(registry)
```
