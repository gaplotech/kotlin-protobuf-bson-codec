package io.github.gaplotech

import com.google.common.io.BaseEncoding
import com.google.protobuf.*
import com.google.protobuf.util.FieldMaskUtil
import com.google.protobuf.Any as PBAny
import org.bson.BsonBinary
import org.bson.BsonWriter
import java.util.*


private typealias WellKnownTypeWriter = (PBBsonWriter, MessageOrBuilder) -> Unit

internal class PBBsonWriter(
    private val includingDefaultValueFields: Boolean = true,
    private val preservingProtoFieldNames: Boolean = false,
    private val writer: BsonWriter
) {

    fun write(message: MessageOrBuilder) {
        val specialWriter = wellKnownTypeWriters[message.descriptorForType.fullName]
        if (specialWriter != null) {
            specialWriter.invoke(this, message)
            return
        }
        write(message, null)
    }

    /** Write google.protobuf.Any  */
    private fun writeAny(message: MessageOrBuilder) {
        if (PBAny.getDefaultInstance() == message) {
            writer.writeStartDocument()
            writer.writeEndDocument()
            return
        }
        val descriptor = message.descriptorForType
        val typeUrlField = descriptor.findFieldByName("type_url")
        val valueField = descriptor.findFieldByName("value")
        // Validates type of the message. Note that we can't just cast the message
        // to com.google.protobuf.Any because it might be a DynamicMessage.
        if (typeUrlField == null
            || valueField == null
            || typeUrlField.type != Descriptors.FieldDescriptor.Type.STRING
            || valueField.type != Descriptors.FieldDescriptor.Type.BYTES) {
            throw PBCodecEncodeException("Invalid Any type.")
        }
        val typeUrl = message.getField(typeUrlField) as String
        val value = message.getField(valueField) as ByteString
        writer.writeStartDocument()
        writer.writeString("typeUrl", typeUrl)
        writer.writeBinaryData("value", BsonBinary(value.toByteArray()))
        writer.writeEndDocument()
    }

    /** Write wrapper types (e.g., google.protobuf.Int32Value)  */
    private fun writeWrapper(message: MessageOrBuilder) {
        val descriptor = message.descriptorForType
        val valueField = descriptor.findFieldByName("value") ?: throw PBCodecEncodeException("Invalid Wrapper type.")
        // When formatting wrapper types, we just write its value field instead of
        // the whole message.
        writeSingleFieldValue(valueField, message.getField(valueField))
    }

    private fun toByteString(message: MessageOrBuilder): ByteString {
        return if (message is Message) {
            message.toByteString()
        } else {
            (message as Message.Builder).build().toByteString()
        }
    }

    /** Write google.protobuf.FieldMask  */
    private fun writeFieldMask(message: MessageOrBuilder) {
        val value = FieldMask.parseFrom(toByteString(message))
        writer.writeString(FieldMaskUtil.toJsonString(value))
    }

    /** Write google.protobuf.Struct  */
    private fun writeStruct(message: MessageOrBuilder) {
        val descriptor = message.descriptorForType
        val field = descriptor.findFieldByName("fields") ?: throw PBCodecEncodeException("Invalid Struct type.")
        // Struct is formatted as a map object.
        writeMapFieldValue(field, message.getField(field))
    }

    /** Write google.protobuf.Value  */
    private fun writeValue(message: MessageOrBuilder) {
        // For a Value message, only the value of the field is formatted.
        val fields = message.allFields
        if (fields.isEmpty()) {
            // No value set.
            writer.writeNull()
            return
        }
        // A Value message can only have at most one field set (it only contains
        // an oneof).
        if (fields.size != 1) {
            throw InvalidProtocolBufferException("Invalid Value type.")
        }
        for ((key, value) in fields) {
            writeSingleFieldValue(key, value)
        }
    }

    /** Write google.protobuf.ListValue  */
    private fun writeListValue(message: MessageOrBuilder) {
        val descriptor = message.descriptorForType
        val field = descriptor.findFieldByName("values") ?: throw PBCodecEncodeException("Invalid ListValue type.")
        writeRepeatedFieldValue(field, message.getField(field))
    }

    /** Write a regular message with an optional type URL.  */
    private fun write(message: MessageOrBuilder, typeUrl: String?) {
        writer.writeStartDocument()

        if (typeUrl != null) {
            writer.writeString("typeUrl", typeUrl)
        }

        val fieldsToWrite: MutableMap<Descriptors.FieldDescriptor, Any>

        if (includingDefaultValueFields) {
            fieldsToWrite = TreeMap(message.allFields)
            for (field in message.descriptorForType.fields) {
                if (field.isOptional) {
                    if (field.javaType == Descriptors.FieldDescriptor.JavaType.MESSAGE && !message.hasField(field)) {
                        // Always skip empty optional message fields. If not we will recurse indefinitely if
                        // a message has itself as a sub-field.
                        continue
                    }
                    val oneof = field.containingOneof
                    if (oneof != null && !message.hasField(field)) {
                        // Skip all oneof fields except the one that is actually set
                        continue
                    }
                }
                if (!fieldsToWrite.containsKey(field) && includingDefaultValueFields) {
                    fieldsToWrite.put(field, message.getField(field))
                }
            }
        } else {
            fieldsToWrite = message.allFields
        }
        for ((key, value) in fieldsToWrite) {
            writeField(key, value)
        }
        writer.writeEndDocument()
    }

    private fun writeField(field: Descriptors.FieldDescriptor, value: Any) {
        if (preservingProtoFieldNames) {
            writer.writeName(field.name)
        } else {
            writer.writeName(field.jsonName)
        }
        when {
            field.isMapField -> writeMapFieldValue(field, value)
            field.isRepeated -> writeRepeatedFieldValue(field, value)
            else -> writeSingleFieldValue(field, value)
        }
    }

    private fun writeRepeatedFieldValue(field: Descriptors.FieldDescriptor, value: Any) {
        writer.writeStartArray()
        @Suppress("unchecked_cast")
        for (element in value as List<Any>) {
            writeSingleFieldValue(field, element)
        }
        writer.writeEndArray()
    }

    private fun writeMapFieldValue(field: Descriptors.FieldDescriptor, value: Any) {
        val type = field.messageType
        val keyField = type.findFieldByName("key")
        val valueField = type.findFieldByName("value")
        if (keyField == null || valueField == null) {
            throw PBCodecEncodeException("Invalid map field.")
        }
        writer.writeStartDocument()

        for (element in value as List<*>) {
            val entry = element as Message
            val entryKey = entry.getField(keyField)
            val entryValue = entry.getField(valueField)

            // Key fields are always double-quoted.
            writeSingleFieldValue(keyField, entryKey, isWritingName = true)
            writeSingleFieldValue(valueField, entryValue)
        }

        writer.writeEndDocument()
    }

    /**
     * Prints a field's value in JSON format.
     *
     * @param isWritingName whether to always add double-quotes to primitive
     * types.
     */
    private fun writeSingleFieldValue(
        field: Descriptors.FieldDescriptor, value: Any, isWritingName: Boolean = false) {
        when (field.type!!) {
            Descriptors.FieldDescriptor.Type.INT32, Descriptors.FieldDescriptor.Type.SINT32,
            Descriptors.FieldDescriptor.Type.SFIXED32, Descriptors.FieldDescriptor.Type.UINT32,
            Descriptors.FieldDescriptor.Type.FIXED32 -> {
                val typedValue = value as Int
                if (isWritingName) {
                    writer.writeName(typedValue.toString())
                } else {
                    writer.writeInt32(typedValue)
                }
            }

            Descriptors.FieldDescriptor.Type.INT64, Descriptors.FieldDescriptor.Type.SINT64,
            Descriptors.FieldDescriptor.Type.SFIXED64, Descriptors.FieldDescriptor.Type.UINT64,
            Descriptors.FieldDescriptor.Type.FIXED64 -> {
                val typedValue = value as Long
                if (isWritingName) {
                    writer.writeName(typedValue.toString())
                } else {
                    writer.writeInt64(typedValue)
                }
            }

            Descriptors.FieldDescriptor.Type.BOOL -> {
                val typedValue = value as Boolean

                if (isWritingName) {
                    writer.writeName(typedValue.toString())
                } else {
                    writer.writeBoolean(typedValue)
                }
            }

            Descriptors.FieldDescriptor.Type.FLOAT -> {
                val typedValue = value as Float
                if (typedValue.isNaN()) {
                    writer.writeDouble(Double.NaN)
                } else if (typedValue.isInfinite()) {
                    if (typedValue < 0) {
                        writer.writeDouble(Double.NEGATIVE_INFINITY)
                    } else {
                        writer.writeDouble(Double.POSITIVE_INFINITY)
                    }
                } else {
                    if (isWritingName) {
                        writer.writeName(typedValue.toString())
                    } else {
                        writer.writeDouble(typedValue.toDouble())
                    }
                }
            }

            Descriptors.FieldDescriptor.Type.DOUBLE -> {
                val typedValue = value as Double
                if (typedValue.isNaN()) {
                    writer.writeDouble(Double.NaN)
                } else if (typedValue.isInfinite()) {
                    if (typedValue < 0) {
                        writer.writeDouble(Double.NEGATIVE_INFINITY)
                    } else {
                        writer.writeDouble(Double.POSITIVE_INFINITY)
                    }
                } else {
                    if (isWritingName) {
                        writer.writeName(typedValue.toString())
                    } else {
                        writer.writeDouble(typedValue)
                    }
                }
            }
            Descriptors.FieldDescriptor.Type.STRING -> {
                val typedValue = value as String
                if (isWritingName) {
                    writer.writeName(typedValue)
                } else {
                    writer.writeString(typedValue)
                }
            }

            Descriptors.FieldDescriptor.Type.BYTES -> {
                val typedValue = value as ByteString

                if (isWritingName) {
                    writer.writeName(BaseEncoding.base64().encode(typedValue.toByteArray()))
                } else {
                    writer.writeBinaryData(BsonBinary(typedValue.toByteArray()))
                }
            }

            Descriptors.FieldDescriptor.Type.ENUM -> {
                // Special-case google.protobuf.NullValue (it's an Enum).
                if (field.enumType.fullName == "google.protobuf.NullValue") {
                    // No matter what value it contains, we always write it as "null".
                    if (isWritingName) {
                        writer.writeName("null")
                    } else {
                        writer.writeNull()
                    }
                } else {
                    if ((value as Descriptors.EnumValueDescriptor).index == -1) {
                        if (isWritingName) {
                            writer.writeName(value.number.toString())
                        } else {
                            writer.writeInt32(value.number)
                        }
                    } else {
                        if (isWritingName) {
                            writer.writeName(value.name)
                        } else {
                            writer.writeString(value.name)
                        }
                    }
                }
            }

            Descriptors.FieldDescriptor.Type.MESSAGE, Descriptors.FieldDescriptor.Type.GROUP -> {
                write(value as Message)
            }
        }
    }

    companion object {

        private val wellKnownTypeWriters = buildWellKnownTypeWrites()

        private fun buildWellKnownTypeWrites(): Map<String, WellKnownTypeWriter> {
            val writers = mutableMapOf<String, WellKnownTypeWriter>()
            // Special-case Any.
            writers[PBAny.getDescriptor().fullName] = { writer, message ->
                writer.writeAny(message)
            }

            // Special-case wrapper types.
            val wrappersWriter = { writer: PBBsonWriter, message: MessageOrBuilder ->
                writer.writeWrapper(message)
            }

            writers[BoolValue.getDescriptor().fullName] = wrappersWriter
            writers[Int32Value.getDescriptor().fullName] = wrappersWriter
            writers[UInt32Value.getDescriptor().fullName] = wrappersWriter
            writers[Int64Value.getDescriptor().fullName] = wrappersWriter
            writers[UInt64Value.getDescriptor().fullName] = wrappersWriter
            writers[StringValue.getDescriptor().fullName] = wrappersWriter
            writers[BytesValue.getDescriptor().fullName] = wrappersWriter
            writers[FloatValue.getDescriptor().fullName] = wrappersWriter
            writers[DoubleValue.getDescriptor().fullName] = wrappersWriter

            writers[FieldMask.getDescriptor().fullName] = { writer, message ->
                writer.writeFieldMask(message)
            }

            // Special-case Struct.
            writers[Struct.getDescriptor().fullName] = { writer, message ->
                writer.writeStruct(message)
            }

            // Special-case Value.
            writers[Value.getDescriptor().fullName] = { writer, message ->
                writer.writeValue(message)
            }
            // Special-case ListValue.
            writers[ListValue.getDescriptor().fullName] = { writer, message ->
                writer.writeListValue(message)
            }
            return writers
        }
    }
}
