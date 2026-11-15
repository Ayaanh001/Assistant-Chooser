package com.hussain.assistantchooser.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomAppList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packages: List<String>
) {
    fun toJSONObject() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("packages", JSONArray(packages))
    }

    companion object {
        fun fromJSONObject(json: JSONObject) = CustomAppList(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", "Unnamed"),
            packages = json.optJSONArray("packages")?.let { arr ->
                List(arr.length()) { arr.optString(it) }
            } ?: emptyList()
        )
    }
}
