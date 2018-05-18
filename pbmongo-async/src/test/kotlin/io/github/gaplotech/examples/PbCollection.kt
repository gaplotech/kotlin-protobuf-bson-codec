package io.github.gaplotech.examples

import com.mongodb.async.client.MongoCollection
import io.github.gaplotech.PBMongo.getPbCollection
import io.github.gaplotech.pb.Test.HelloWorld
import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.bson.Document
import org.bson.codecs.configuration.CodecConfigurationException
import org.litote.kmongo.async.KMongo
import org.litote.kmongo.coroutine.drop
import org.litote.kmongo.coroutine.findOne
import org.litote.kmongo.coroutine.save
import org.litote.kmongo.coroutine.withDocumentClass

class PbCollection : StringSpec() {

    override fun isInstancePerTest() = false

    val db = KMongo.createClient().getDatabase("test")
    lateinit var collection: MongoCollection<HelloWorld>

    init {
        "You can have a mongo collection of only one pb class." {
            collection = db.getPbCollection<HelloWorld>("helloWorld")
        }

        "Then use it" {
            runBlocking {
                collection.save(helloWorld)
                collection.findOne() shouldBe helloWorld
            }
        }

        "But no other types can be used with the collection." {
            runBlocking {
                val documents = collection.withDocumentClass<Document>()
                val t = shouldThrow<CodecConfigurationException> { documents.findOne() }
                t.message shouldBe "Can't find a codec for class org.bson.Document."
            }
        }
    }

    override fun afterTest(description: Description, result: TestResult) {
        runBlocking { db.drop() }
        super.afterTest(description, result)
    }
}
