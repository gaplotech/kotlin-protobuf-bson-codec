package io.github.gaplotech

import com.google.protobuf.Message
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext

class PBCodec<T : Message>(
    private val clazz: Class<T>,
    private val includingDefaultValueFields: Boolean = true,
    private val preservingProtoFieldNames: Boolean = false
) : Codec<T> {
    override fun getEncoderClass(): Class<T> = clazz

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): T {
        return PBBsonReader(reader).read(clazz)
    }

    override fun encode(writer: BsonWriter, value: T, encoderContext: EncoderContext) {
        PBBsonWriter(
            writer = writer,
            includingDefaultValueFields = includingDefaultValueFields,
            preservingProtoFieldNames = preservingProtoFieldNames
        ).write(value)
    }
}
