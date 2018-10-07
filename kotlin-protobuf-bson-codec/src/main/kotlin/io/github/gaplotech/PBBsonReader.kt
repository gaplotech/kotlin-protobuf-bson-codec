package io.github.gaplotech

import com.google.protobuf.*
import com.google.protobuf.util.FieldMaskUtil
import org.bson.BsonReader
import org.bson.BsonType
import java.util.HashMap

internal class PBBsonReader(
    private val reader: BsonReader
) {

    fun <T : Message> read(clazz: Class<T>): T {
        val builder = clazz.getDeclaredMethod("newBuilder").invoke(null) as Message.Builder

        @Suppress("unchecked_cast")
        return merge(builder) as T
    }

    private val fieldNameMaps = HashMap<Descriptors.Descriptor, Map<String, Descriptors.FieldDescriptor>>()

    private fun getFieldNameMap(descriptor: Descriptors.Descriptor): Map<String, Descriptors.FieldDescriptor> {
        return fieldNameMaps[descriptor] ?: HashMap<String, Descriptors.FieldDescriptor>().also { fieldNameMap ->
            descriptor.fields.forEach { field ->
                fieldNameMap[field.name] = field
                fieldNameMap[field.jsonName] = field
            }
            fieldNameMaps[descriptor] = fieldNameMap
        }
    }

    private fun merge(builder: Message.Builder): Any {
        when (reader.currentBsonType) {
            BsonType.DOCUMENT -> {
                reader.readStartDocument()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    mergeValue(builder)
                }
                reader.readEndDocument()
            }
            BsonType.ARRAY -> {
                reader.readStartArray()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    mergeValue(builder)
                }
                reader.readEndArray()
            }
            BsonType.END_OF_DOCUMENT -> {
            }
            else -> mergeValue(builder)

        }
        return builder.build()
    }

    private fun mergeValue(builder: Message.Builder) {
        val typeDes = builder.descriptorForType
        val name = reader.readName()
        val field: Descriptors.FieldDescriptor? = getFieldNameMap(typeDes)[name]

        if (field != null) {
            when {
                field.isMapField -> mergeMapField(field, builder)
                field.isRepeated -> mergeRepeatedField(field, builder)
                else -> mergeField(field, builder)
            }
        } else {
            reader.skipValue()
        }
    }

    private fun mergeMapField(field: Descriptors.FieldDescriptor, builder: Message.Builder) {
        when (reader.currentBsonType) {
            BsonType.DOCUMENT -> {
                val mapEntryDesc = field.messageType
                val keyDescriptor = mapEntryDesc.findFieldByNumber(1)
                val valueDescriptor = mapEntryDesc.findFieldByNumber(2)
                reader.readStartDocument()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    val key = reader.readName()
                    val keyObj: Any = when (keyDescriptor.javaType) {
                        Descriptors.FieldDescriptor.JavaType.INT -> key.toInt()
                        Descriptors.FieldDescriptor.JavaType.LONG -> key.toLong()
                        Descriptors.FieldDescriptor.JavaType.FLOAT -> key.toFloat()
                        Descriptors.FieldDescriptor.JavaType.DOUBLE -> key.toDouble()
                        Descriptors.FieldDescriptor.JavaType.BOOLEAN -> key.toBoolean()
                        Descriptors.FieldDescriptor.JavaType.STRING -> key
                        Descriptors.FieldDescriptor.JavaType.BYTE_STRING -> ByteString.copyFrom(key.toByteArray())
                        Descriptors.FieldDescriptor.JavaType.ENUM -> TODO()
                        Descriptors.FieldDescriptor.JavaType.MESSAGE -> TODO()
                        null -> TODO()
                    }

                    val value = parseFieldValue(valueDescriptor)

                    val entryBuilder = builder.newBuilderForField(field)
                    entryBuilder.setField(keyDescriptor, keyObj)
                    entryBuilder.setField(valueDescriptor, value)

                    builder.addRepeatedField(field, entryBuilder.build())
                }
                reader.readEndDocument()
            }
            else -> {
                throw PBCodecDecodeException("Expected an document for map field")
            }
        }

    }

    private fun mergeRepeatedField(field: Descriptors.FieldDescriptor, builder: Message.Builder) {
        when (reader.currentBsonType) {
            BsonType.ARRAY -> {
                reader.readStartArray()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                    val value = parseFieldValue(field)

                    builder.addRepeatedField(field, value)

                }
                reader.readEndArray()
            }
            else -> {
                throw PBCodecDecodeException("Expected an array for repeated field")
            }
        }
    }

    private fun mergeField(field: Descriptors.FieldDescriptor, builder: Message.Builder) {
        builder.setField(field, parseFieldValue(field))
    }

    private fun parseFieldValue(descriptor: Descriptors.FieldDescriptor): Any {
        return when (descriptor.javaType) {
            Descriptors.FieldDescriptor.JavaType.INT -> reader.readInt32()
            Descriptors.FieldDescriptor.JavaType.LONG -> reader.readInt64()
            Descriptors.FieldDescriptor.JavaType.FLOAT -> reader.readDouble().toFloat()
            Descriptors.FieldDescriptor.JavaType.DOUBLE -> reader.readDouble()
            Descriptors.FieldDescriptor.JavaType.BOOLEAN -> reader.readBoolean()
            Descriptors.FieldDescriptor.JavaType.STRING -> reader.readString()
            Descriptors.FieldDescriptor.JavaType.BYTE_STRING -> ByteString.copyFrom(reader.readBinaryData().data)
            Descriptors.FieldDescriptor.JavaType.ENUM -> descriptor.enumType.findValueByName(reader.readString())
            Descriptors.FieldDescriptor.JavaType.MESSAGE -> {
                val specialReader: WellKnownTypeReader? = wellKnownTypeReaders[descriptor.messageType.fullName]
                return if (specialReader != null) {
                    specialReader.parse(this)
                } else {
                    val subBuilder = DynamicMessage.newBuilder(descriptor.messageType)
                    merge(subBuilder)
                }

            }
            null -> throw PBCodecDecodeException("This should never happen")
        }
    }

    private fun parseWrapper(builder: Message.Builder): Message {
        val type = builder.descriptorForType
        val field = type.findFieldByName("value")
            ?: throw InvalidProtocolBufferException("Invalid wrapper type: " + type.fullName)
        builder.setField(field, parseFieldValue(field))
        return builder.build()
    }

    private fun parsePBValue(): Value {
        val builder = Value.newBuilder()
        val descriptor = builder.descriptorForType
        when (reader.currentBsonType) {
            BsonType.DOCUMENT -> {
                builder.setStructValue(parseStruct())
            }
            BsonType.ARRAY -> {
                builder.setListValue(parseListValue())
            }
            BsonType.DOUBLE -> builder.setNumberValue(reader.readDouble())
            BsonType.STRING -> builder.setStringValue(reader.readString())
            BsonType.BOOLEAN -> builder.setBoolValue(reader.readBoolean())
            BsonType.NULL -> {
                reader.readNull() // dummy read is needed
                builder.setNullValue(NullValue.NULL_VALUE)
            }
            BsonType.INT32 -> builder.setNumberValue(reader.readInt32().toDouble())
            BsonType.INT64 -> builder.setNumberValue(reader.readInt64().toDouble())
            BsonType.END_OF_DOCUMENT -> {}
            else -> throw PBCodecDecodeException("Unsupported java type in descriptor=$descriptor")
        }
        return builder.build()
    }

    private fun parseFieldMask(): FieldMask {
        return FieldMaskUtil.fromJsonString(reader.readString())
    }

    private fun parseStruct(): Struct {
        val builder = Struct.newBuilder()
        val descriptor = builder.descriptorForType
        val field = descriptor.findFieldByName("fields")
        mergeMapField(field, builder)
        return builder.build()
    }

    private fun parseListValue(): ListValue {
        val builder = ListValue.newBuilder()
        when (reader.currentBsonType) {
            BsonType.ARRAY -> {
                reader.readStartArray()
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                    val value = parsePBValue()
                    builder.addValues(value)
                }
                reader.readEndArray()
            }
            else -> {
                throw PBCodecDecodeException("Expected an array for ListValue")
            }
        }
        return builder.build()
    }

    companion object {

        private interface WellKnownTypeReader {
            companion object {
                fun create(f: (PBBsonReader) -> Any): WellKnownTypeReader = object : WellKnownTypeReader {
                    override fun parse(reader: PBBsonReader): Any {
                        return f(reader)
                    }
                }
            }

            fun parse(reader: PBBsonReader): Any
        }

        private val wellKnownTypeReaders = buildWellKnownTypeReaders()

        private fun buildWellKnownTypeReaders(): Map<String, WellKnownTypeReader> {
            val readers = mutableMapOf<String, WellKnownTypeReader>()

            readers[BoolValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(BoolValue.newBuilder())
            }
            readers[Int32Value.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(Int32Value.newBuilder())
            }
            readers[UInt32Value.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(UInt32Value.newBuilder())
            }
            readers[Int64Value.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(Int64Value.newBuilder())
            }
            readers[UInt64Value.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(UInt64Value.newBuilder())
            }
            readers[StringValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(StringValue.newBuilder())
            }
            readers[BytesValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(BytesValue.newBuilder())
            }
            readers[FloatValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(FloatValue.newBuilder())
            }
            readers[DoubleValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseWrapper(DoubleValue.newBuilder())
            }

            // Special-case Value.
            readers[Value.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parsePBValue()
            }

            // Special-case FieldMask.
            readers[FieldMask.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseFieldMask()
            }

            // Special-case Struct.
            readers[Struct.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseStruct()
            }

            // Special-case ListValue.
            readers[ListValue.getDescriptor().fullName] = WellKnownTypeReader.create {
                it.parseListValue()
            }
            return readers
        }

    }
}
