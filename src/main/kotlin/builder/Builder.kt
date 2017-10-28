package builder

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import util.*


class BuildVerticle() : AbstractVerticle() {

    override fun start() {
        println("BuildVerticle start message")
        eb.consumer<JsonObject>("builder.build") { message ->
            if (checkMessage(message)) {
                val body = message.body()
                val command = body.getString("command")
                val commandId = body.getString("id")
                launch(Unconfined) {
                    val res = doInCommandLineAsync(command)
                    eb.publish("results.build", JsonObject(
                            mapOf("for" to commandId, "result" to res.toString()))
                    )
                }
            }
        }
    }

    override fun stop() {
        println("BuildVerticle stop message")
    }


    private fun checkMessage(message: Message<JsonObject>): Boolean {
        return true
    }
}

suspend fun deployBuildVerticleAsync(): String {
    return vxa<String> ({ callback ->
        vertx.deployVerticle(BuildVerticle(), callback)
    })
}

