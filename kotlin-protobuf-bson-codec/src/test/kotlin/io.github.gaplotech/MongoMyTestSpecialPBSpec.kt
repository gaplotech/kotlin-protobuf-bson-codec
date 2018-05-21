import com.google.protobuf.*
import com.google.protobuf.Any
import com.mongodb.async.client.MongoCollection
import io.github.gaplotech.pb.Test.MyTestSpecial
import io.github.gaplotech.pb.Test.MyTestV3
import io.github.gaplotech.repository.MongoPBRepository
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import kotlinx.coroutines.experimental.runBlocking
import org.litote.kmongo.coroutine.findOne
import org.litote.kmongo.coroutine.getCollectionOfName
import org.litote.kmongo.coroutine.insertOne
import org.litote.kmongo.coroutine.singleResult
import java.util.*

class MongoMyTestSpecialPBSpec : FeatureSpec() {

    class MyTestSpecialRepository : MongoPBRepository<MyTestSpecial>("test") {
        override val collection: MongoCollection<MyTestSpecial> = database.getCollectionOfName("prototest.special")
        suspend fun insertOne(test: MyTestSpecial) {
            collection.insertOne(test)
        }

        suspend fun findOne(): MyTestSpecial? {
            return collection.findOne()
        }

        suspend fun drop() {
            singleResult<Void> { collection.drop(it) }
        }
    }

    companion object {
        private val bytes = ByteArray(10).also { Random().nextBytes(it) }
        private const val t_seconds = 100L
        private const val t_nanos = 100
        private const val testString = "Testing"

        val t_any = Any.newBuilder().apply {
            typeUrl = "type.googleapis.com/${MyTestV3.getDefaultInstance().descriptorForType.fullName}"
            value = MyTestV3.getDefaultInstance().toByteString()
        }.build()!!

        val t_timestamp = Timestamp.newBuilder().apply {
            seconds = t_seconds
            nanos = t_nanos
        }.build()!!

        val t_duration = Duration.newBuilder().apply {
            seconds = t_seconds
            nanos = t_nanos
        }.build()!!

        val t_struct = Struct.newBuilder().build()!!

        val t_doubleVal = DoubleValue.newBuilder().setValue(1.0).build()!!

        val t_floatVal = FloatValue.newBuilder().setValue(1.0f).build()!!

        val t_int64Val = Int64Value.newBuilder().setValue(1L).build()!!
        val t_uint64Val = UInt64Value.newBuilder().setValue(1L).build()!!
        val t_int32Val = Int32Value.newBuilder().setValue(1).build()!!
        val t_uint32Val = UInt32Value.newBuilder().setValue(1).build()!!
        val t_boolVal = BoolValue.newBuilder().setValue(true).build()!!
        val t_stringVal = StringValue.newBuilder().setValue(testString).build()!!
        val t_bytesVal = BytesValue.newBuilder().setValue(ByteString.copyFrom(bytes)).build()!!
    }

    init {
        val repo = MyTestSpecialRepository()

        feature("mongodb with protobuf") {
            scenario("drop collection") {
                runBlocking {
                    repo.drop()
                }
            }
            scenario("save proto ") {
                runBlocking {
                    val proto = MyTestSpecial.newBuilder().apply {
                        emptyVal = Empty.newBuilder().build()
                        any = t_any
                        timestamp = t_timestamp
                        duration = t_duration
                        struct = t_struct
                        doubleVal = t_doubleVal
                        floatVal = t_floatVal
                        int64Val = t_int64Val
                        uint64Val = t_uint64Val
                        int32Val = t_int32Val
                        uint32Val = t_uint32Val
                        boolVal = t_boolVal
                        stringVal = t_stringVal
                        bytesVal = t_bytesVal
                    }.build()

                    repo.insertOne(proto)
                }
            }

            scenario("read proto from db") {
                runBlocking {
                    val proto = repo.findOne()!!

                    proto.emptyVal shouldBe Empty.getDefaultInstance()
                    proto.any shouldBe t_any
                    proto.timestamp shouldBe t_timestamp
                    proto.duration shouldBe t_duration
                    proto.struct shouldBe t_struct
                    proto.doubleVal shouldBe t_doubleVal
                    proto.floatVal shouldBe t_floatVal
                    proto.int64Val shouldBe t_int64Val
                    proto.uint64Val shouldBe t_uint64Val
                    proto.int32Val shouldBe t_int32Val
                    proto.uint32Val shouldBe t_uint32Val
                    proto.boolVal shouldBe t_boolVal
                    proto.stringVal shouldBe t_stringVal
                    proto.bytesVal shouldBe t_bytesVal

                }
            }
        }
    }
}
