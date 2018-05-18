package io.github.gaplotech.examples

import com.mongodb.async.client.MongoDatabase
import io.github.gaplotech.PBCodecProvider
import io.github.gaplotech.PBMongo.addPBCodec
import io.github.gaplotech.pb.Test.HelloWorld
import io.kotlintest.Description
import io.kotlintest.TestResult
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.runBlocking
import org.litote.kmongo.async.KMongo
import org.litote.kmongo.coroutine.drop
import org.litote.kmongo.coroutine.findOne
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.save

class UseTransparently : StringSpec() {

    data class Wrapped(val helloWorld: HelloWorld, val i: Int)

    override fun isInstancePerTest() = false

    lateinit var db: MongoDatabase


    init {
        "You can add the codec provider to the DB" {
            db = KMongo.createClient().getDatabase("test")
                .addPBCodec(PBCodecProvider(
                    preservingProtoFieldNames = true
                ))
        }

        "Then use the db transparently" {
            runBlocking {
                val collection = db.getCollectionOfName<HelloWorld>("helloWorld")
                collection.save(helloWorld)
                collection.findOne() shouldBe helloWorld
            }
        }

        "Mix and match data class with Pb Class works too" {
            runBlocking {
                val wrapped = Wrapped(helloWorld, 1)
                val collection = db.getCollectionOfName<Wrapped>("wrapped")
                collection.save(wrapped)
                collection.findOne() shouldBe wrapped
            }
        }
    }

    override fun afterTest(description: Description, result: TestResult) {
        runBlocking { db.drop() }
        super.afterTest(description, result)
    }
}
