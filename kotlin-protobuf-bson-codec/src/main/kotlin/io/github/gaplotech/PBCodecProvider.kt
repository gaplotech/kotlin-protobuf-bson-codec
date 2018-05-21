package io.github.gaplotech

import com.google.protobuf.Message
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry

class PBCodecProvider(
    private val includingDefaultValueFields: Boolean = true,
    private val preservingProtoFieldNames: Boolean = false
) : CodecProvider {
    private val messageClass = Message::class.java
    override fun <T> get(clazz: Class<T>, registry: CodecRegistry): Codec<T>? {
        return if (!messageClass.isAssignableFrom(clazz)) null else {
            @Suppress("unchecked_cast")
            PBCodec(clazz as Class<out Message>, includingDefaultValueFields, preservingProtoFieldNames) as Codec<T>
        }
    }

}
