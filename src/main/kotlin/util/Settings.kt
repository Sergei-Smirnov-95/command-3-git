package util

import java.io.File
import io.vertx.core.json.JsonObject

fun setUp(config: String) {
    val settingsFile = File(config)
    val settings = JsonObject(settingsFile.readText())
}
