package io.github.halilozel1903.openf1.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.doubleOrNull

/**
 * A gap between two cars. OpenF1 sends seconds as a number (`41.019`) and lapped cars as text (`"+1 LAP"`).
 */
@Serializable(with = GapSerializer::class)
public sealed interface Gap {
    /** A gap in seconds. */
    public data class Seconds(public val seconds: Double) : Gap {
        override fun toString(): String = "+" + String.format(java.util.Locale.US, "%.3f", seconds)
    }

    /** One or more laps behind. */
    public data class Laps(public val laps: Int) : Gap {
        override fun toString(): String = if (laps == 1) "+1 LAP" else "+$laps LAPS"
    }

    public companion object {
        private val lapsPattern = Regex("""^\+?\s*(\d+)\s*LAPS?$""", RegexOption.IGNORE_CASE)

        /** Reads `41.019`, `"+41.019"`, `"+1 LAP"` or `"+2 LAPS"`; `null` for anything else. */
        public fun parse(text: String): Gap? {
            val trimmed = text.trim()
            lapsPattern.matchEntire(trimmed)?.let { return Laps(it.groupValues[1].toInt()) }
            return trimmed.removePrefix("+").toDoubleOrNull()?.let { Seconds(it) }
        }
    }
}

/** Decodes a [Gap] from a JSON number or string. */
public object GapSerializer : KSerializer<Gap> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.github.halilozel1903.openf1.Gap", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Gap {
        val json = decoder as? JsonDecoder ?: return Gap.parse(decoder.decodeString())
            ?: throw SerializationException("Not a gap")
        val element = json.decodeJsonElement()
        return fromElement(element) ?: throw SerializationException("Not a gap: $element")
    }

    internal fun fromElement(element: JsonElement): Gap? {
        val primitive = element as? JsonPrimitive ?: return null
        if (primitive is JsonNull) return null
        if (!primitive.isString) primitive.doubleOrNull?.let { return Gap.Seconds(it) }
        return Gap.parse(primitive.content)
    }

    override fun serialize(encoder: Encoder, value: Gap) {
        when {
            encoder is JsonEncoder && value is Gap.Seconds -> encoder.encodeJsonElement(JsonPrimitive(value.seconds))
            else -> encoder.encodeString(value.toString())
        }
    }
}

/**
 * Reads a value that is a single item in race results and an array (Q1, Q2, Q3) in qualifying results, such as
 * `duration` or `gap_to_leader` of `/session_result`. Always decodes to a list; `null` becomes an empty list.
 */
internal object OneOrManyDurations : JsonTransformingSerializer<List<Double?>>(ListSerializer(Double.serializer().nullable)) {
    override fun transformDeserialize(element: JsonElement): JsonElement = oneOrMany(element)
}

internal object OneOrManyGaps : JsonTransformingSerializer<List<Gap?>>(ListSerializer(GapSerializer.nullable)) {
    override fun transformDeserialize(element: JsonElement): JsonElement = oneOrMany(element)
}

private fun oneOrMany(element: JsonElement): JsonElement = when (element) {
    is JsonArray -> element
    JsonNull -> JsonArray(emptyList())
    else -> JsonArray(listOf(element))
}
