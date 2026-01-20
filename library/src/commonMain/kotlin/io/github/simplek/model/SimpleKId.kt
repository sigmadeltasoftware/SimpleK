package io.github.simplek.model

import com.benasher44.uuid.uuid4
import io.github.simplek.serialization.SimpleKIdSerializer
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Unique identifier for Kanban items.
 * Value class provides type safety with zero runtime overhead.
 */
@Serializable(with = SimpleKIdSerializer::class)
@JvmInline
value class SimpleKId(val value: String) {
    companion object {
        fun generate(): SimpleKId = SimpleKId(uuid4().toString())
    }
}
