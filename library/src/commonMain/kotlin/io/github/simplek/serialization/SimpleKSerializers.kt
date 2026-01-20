package io.github.simplek.serialization

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.simplek.model.SimpleKBoard
import io.github.simplek.model.SimpleKId
import io.github.simplek.model.SimpleKItem
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Custom serializer for [SimpleKId] value class.
 * Serializes the underlying String value directly.
 */
object SimpleKIdSerializer : KSerializer<SimpleKId> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SimpleKId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SimpleKId) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): SimpleKId {
        return SimpleKId(decoder.decodeString())
    }
}

/**
 * Custom serializer for Compose [Color].
 * Serializes color as ARGB integer value.
 */
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeInt(value.toArgb())
    }

    override fun deserialize(decoder: Decoder): Color {
        return Color(decoder.decodeInt())
    }
}

/**
 * Default JSON configuration for Kanban serialization.
 * - Ignores unknown keys for forward compatibility
 * - Pretty prints for readability
 * - Encodes defaults to ensure complete serialization
 */
val SimpleKJson = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}

/**
 * Serializes this [SimpleKBoard] to a JSON string.
 *
 * Note: The card type [T] must be annotated with @Serializable.
 *
 * @return JSON string representation of the board
 */
inline fun <reified T : SimpleKItem> SimpleKBoard<T>.toJson(): String {
    return SimpleKJson.encodeToString(serializer(), this)
}

/**
 * Deserializes a JSON string to a [SimpleKBoard].
 *
 * Note: The card type [T] must be annotated with @Serializable.
 *
 * @return Deserialized [SimpleKBoard] instance
 */
inline fun <reified T : SimpleKItem> String.toSimpleKBoard(): SimpleKBoard<T> {
    return SimpleKJson.decodeFromString(serializer(), this)
}
