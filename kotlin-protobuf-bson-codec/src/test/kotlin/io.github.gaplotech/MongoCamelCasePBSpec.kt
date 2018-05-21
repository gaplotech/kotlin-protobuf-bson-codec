import com.mongodb.async.client.MongoCollection
import io.github.gaplotech.pb.Test.CamelCase
import io.github.gaplotech.repository.MongoPBRepository
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.experimental.runBlocking
import org.litote.kmongo.coroutine.findOne
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.coroutine.singleResult

class MongoCamelCasePBSpec : FeatureSpec() {

    class CamelCaseRepository : MongoPBRepository<CamelCase>("test") {
        override val collection: MongoCollection<CamelCase> = database.getCollectionOfName("prototest.camel")
        suspend fun insertOne(test: CamelCase) {
            collection.insertOne(test)
        }

        suspend fun findOne(): CamelCase? {
            return collection.findOne()
        }

        suspend fun drop() {
            singleResult<Void> { collection.drop(it) }
        }
    }

    init {
        val repo = CamelCaseRepository()

        feature("mongodb with wrong naming convention protobuf message") {
            scenario("drop collection") {
                runBlocking {
                    repo.drop()
                }
            }
            scenario("save proto ") {
                runBlocking {
                    val proto = CamelCase.newBuilder().setCamelCase("hello").build()
                    repo.insertOne(proto)
                }
            }

            scenario("read proto from db") {
                runBlocking {
                    val proto = repo.findOne()!!
                    proto.camelCase.shouldBe("hello")
                }
            }
        }
    }
}
