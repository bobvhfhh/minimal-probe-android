package com.minprobe.app.data.remote

import com.minprobe.app.data.remote.dto.NodeDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Parses both legacy array payloads and current wrapped monitor payloads. */
internal class MonitorPayloadParser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    fun nodes(payload: String): List<NodeDto> {
        val element = json.parseToJsonElement(payload)
        return when {
            element is kotlinx.serialization.json.JsonArray ->
                json.decodeFromJsonElement(ListSerializer(NodeDto.serializer()), element)
            element is kotlinx.serialization.json.JsonObject -> {
                val nodes = element["nodes"]?.jsonArray
                    ?: element["data"]?.let { data ->
                        when {
                            data is kotlinx.serialization.json.JsonArray -> data
                            data is kotlinx.serialization.json.JsonObject -> data["nodes"]?.jsonArray
                            else -> null
                        }
                    }
                    ?: error("节点响应缺少 nodes 字段")
                json.decodeFromJsonElement(ListSerializer(NodeDto.serializer()), nodes)
            }
            else -> error("节点响应格式不支持")
        }
    }
}
