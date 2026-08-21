package cn.hkim.addon.utils.waypoints

import com.google.gson.*
import java.lang.reflect.Type

class ActionAdapter : JsonSerializer<FarmingWaypoints.Action>, JsonDeserializer<FarmingWaypoints.Action> {
    override fun serialize(src: FarmingWaypoints.Action, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()

        if (src.forward) jsonObject.addProperty("forward", true)
        if (src.back) jsonObject.addProperty("back", true)
        if (src.left) jsonObject.addProperty("left", true)
        if (src.right) jsonObject.addProperty("right", true)
        if (src.leftClick) jsonObject.addProperty("leftClick", true)

        return jsonObject
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): FarmingWaypoints.Action {
        val obj = json.asJsonObject

        return FarmingWaypoints.Action(
            forward = obj.get("forward")?.asBoolean ?: false,
            back = obj.get("back")?.asBoolean ?: false,
            left = obj.get("left")?.asBoolean ?: false,
            right = obj.get("right")?.asBoolean ?: false,
            leftClick = obj.get("leftClick")?.asBoolean ?: false
        )
    }
}
