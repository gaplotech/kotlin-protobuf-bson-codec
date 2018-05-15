import io.github.gaplotech.pb.Test.*
import com.google.protobuf.ByteString
import com.mongodb.async.client.MongoCollection
import io.github.gaplotech.repository.MongoPBRepository
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.experimental.runBlocking
import org.litote.kmongo.coroutine.findOne
import org.litote.kmongo.coroutine.insertOne
import java.util.*

class MongoMyTestPBSepc : FeatureSpec() {
    companion object {
        private val bytes = ByteArray(10).also { Random().nextBytes(it) }

        const val t_hello = "world"
        const val t_foobar = Int.MAX_VALUE
        const val t_bazinga = Long.MAX_VALUE
        val t_optEnum = MyEnumV3.V2
        val t_optBs = ByteString.copyFrom(bytes)!!
        const val t_optBool = true
        val t_optDouble = Double.MAX_VALUE
        val t_optFloat = Float.MAX_VALUE
        val t_primitiveSequenceList = listOf("a", "b", "c")
        val t_repMessageList = listOf(MyTestV3.getDefaultInstance())
        val t_stringToInt32Map = mapOf("test" to 12345)
        val t_intToMytestMap = mapOf(1 to MyTestV3.getDefaultInstance())
        val t_repEnumList = listOf(MyEnumV3.V1, MyEnumV3.V2)
        val t_intToEnumMap = mapOf(1 to MyEnumV3.V2)
        val t_boolToStringMap = mapOf(true to "test")
        val t_stringToBoolMap = mapOf("test" to true)
        val t_fixed64ToBytesMap = mapOf(Long.MIN_VALUE to ByteString.copyFrom(bytes))
    }

    init {
        val repo = object : MongoPBRepository<MyTestV3>("test") {
            override val collection: MongoCollection<MyTestV3> = getCollectionWithCodec("prototest")
            suspend fun insertOne(test: MyTestV3) {
                collection.insertOne(test)
            }

            suspend fun findOne(): MyTestV3? {
                return collection.findOne()
            }

            fun drop() {
                collection.drop { _, _ -> }
            }
        }

        feature("mongodb with protobuf") {
            scenario("drop collection") {
                runBlocking {
                    repo.drop()
                }
            }
            scenario("save proto ") {
                runBlocking {
                    val proto = MyTestV3.newBuilder().apply {
                        hello = t_hello
                        foobar = t_foobar
                        bazinga = t_bazinga
                        optEnum = t_optEnum
                        optBs = t_optBs
                        optBool = t_optBool
                        optDouble = t_optDouble
                        optFloat = t_optFloat

                        addAllPrimitiveSequence(t_primitiveSequenceList)
                        addAllRepMessage(t_repMessageList)
                        putAllStringToInt32(t_stringToInt32Map)
                        putAllIntToMytest(t_intToMytestMap)
                        addAllRepEnum(t_repEnumList)
                        putAllIntToEnum(t_intToEnumMap)
                        putAllBoolToString(t_boolToStringMap)
                        putAllStringToBool(t_stringToBoolMap)
                        putAllFixed64ToBytes(t_fixed64ToBytesMap)

                    }.build()
                    repo.insertOne(proto)
                }
            }

            scenario("read proto from db") {
                runBlocking {
                    val proto = repo.findOne()!!

                    proto.hello shouldBe t_hello
                    proto.foobar shouldBe t_foobar
                    proto.bazinga shouldBe t_bazinga
                    proto.optEnum shouldBe t_optEnum
                    proto.optBs shouldBe t_optBs
                    proto.optBool shouldBe t_optBool
                    proto.optDouble shouldBe t_optDouble
                    proto.optFloat shouldBe t_optFloat
                    proto.primitiveSequenceList shouldBe t_primitiveSequenceList
                    proto.repMessageList shouldBe t_repMessageList
                    proto.stringToInt32Map shouldBe t_stringToInt32Map
                    proto.intToMytestMap shouldBe t_intToMytestMap
                    proto.repEnumList shouldBe t_repEnumList
                    proto.intToEnumMap shouldBe t_intToEnumMap
                    proto.boolToStringMap shouldBe t_boolToStringMap
                    proto.stringToBoolMap shouldBe t_stringToBoolMap
                    proto.fixed64ToBytesMap shouldBe t_fixed64ToBytesMap

                }
            }
        }
    }
}
