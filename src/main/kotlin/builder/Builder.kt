package builder

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.launch
import util.*
import java.io.File


class BuildVerticle() : AbstractVerticle(), Loggable {

    override fun start() {
        log.info("BuildVerticle start message")
        eb.consumer<JsonObject>("builder.build") { message ->
            if (checkMessage(message)) {
                val body = message.body()
                val command = body.getString("command")
                val cwd = body.getString("cwd")
                val commandId = body.getString("id")
                launch(VertxContext(vertx)) {
                    val res = doInCommandLineAsync(command, cwd)
                    eb.publish("results.build", JsonObject(
                            mapOf("for" to commandId, "result" to res.toString()))
                    )
                }
            }
            else
                message.fail(1, "Wrong format")
        }
    }

    override fun stop() {
        log.info("BuildVerticle stop message")
    }

    private fun checkMessage(message: Message<JsonObject>): Boolean {
        val body = message.body()
        if (body.containsKey("id") && body.containsKey("cwd") && body.containsKey("command")){
            val dir = File(body.getString("cwd"))
            if (dir.isDirectory)
                return true
        }
        return false
    }
}

suspend fun deployBuildVerticleAsync(): String {
    return vxa<String> ({ callback ->
        vertx.deployVerticle(BuildVerticle(), callback)
    })
}

suspend fun doBuilderTaskAsync(task: JsonObject, id: String): Message<JsonObject> {
    return vxa<Message<JsonObject>> {
        var command = task.getString("command")
        //doInCommandLineAsync("xcopy C:${loadResult.body().getString("loadPath")} C:\\tmp /s /e")
        // FIXME cwd теперь передается аргументом при выполнении команды, нужно настроить
        eb.send("builder.build", JsonObject(
                mapOf(
                        "id" to id,
                        "command" to command,
                        "cwd" to main.loadPath
                )
        ))
    }
}

