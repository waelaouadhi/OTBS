package com.example.onetechbs.util

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant
import java.time.format.DateTimeParseException

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant {
        if (json == null || json.isJsonNull) return Instant.EPOCH
        return try {
            Instant.parse(json.asString)
        } catch (e: DateTimeParseException) {
            Instant.EPOCH
        }
    }
}
