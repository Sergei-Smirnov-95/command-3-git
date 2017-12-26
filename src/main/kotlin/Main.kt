/**
 * Created by User on 07.11.2017.
 */
package main

import builder.*
import io.vertx.core.AbstractVerticle
import repoloader.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import util.*
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.*
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import java.lang.Math.abs
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Timestamp

val sep = "/"
val root = "/tmp/"
// FIXME не работает для рекурсивных папок
val loadPath = root + "repos${sep}"

class BuilderTask {

    val subtasks = JsonArray()
    var id = ""
    var resListener = ""

    init {
        val timestamp = Timestamp(System.currentTimeMillis())
        id += Math.random()
        id = abs(id.hashCode()).toString()
    }
    fun repo(addParam: RepoParams.() -> Unit): Unit {
        var subtask = repoloader.repo(addParam)
        subtasks.add(JsonObject(mapOf(
                "type" to "repo",
                "taskId" to id,
                "subtask" to subtask
        )))
    }
    fun mvn(init: Mvn.() -> Unit): Unit {
        var subtask = builder.mvn(init)
        subtasks.add(JsonObject(mapOf(
                "type" to "mvn",
                "taskId" to id,
                "subtask" to subtask
        )))
    }
    fun resultsListener(channel: String): Unit {
        resListener = channel
    }
}

object EmbeddedBuilder {
    init {
        launch(VertxContext(util.vertx)) {
            deployMainVerticleAsync()
            deployBuildVerticleAsync()
            deployLoadVerticleAsync()
            setUpBuildMessageListenerAsync(eb)
        }
    }

    fun run(action: BuilderTask.() -> Unit): Unit {
        launch(VertxContext(vertx)) {
            val bt = BuilderTask()
            bt.action()
            val sender = vertx.eventBus().sender<JsonObject>("main.runtask");
            val handler = Handler<AsyncResult<Message<JsonObject>>> { ar ->
                if (ar.succeeded()){
                    eb.send(bt.resListener, JsonObject(mapOf(
                            "id" to bt.id,
                            "status" to "ok"
                    )))
                }
            }
            sender.send(JsonObject(mapOf(
                    "subtasks" to bt.subtasks
            )), handler);
        }
    }
    suspend fun setUpBuildMessageListenerAsync(eb: EventBus) {
        val consumer = eb.consumer<JsonObject>("results.build")
        consumer.handler { message ->
            val body = message.body()
            println("Response to ${body.getString("for")}: ${body.getString("result")}")
        }
        vxu {
            consumer.completionHandler(it)
        }
    }
}


class MainVerticle : AbstractVerticle(), Loggable {
    override fun start() {
        val consumer = eb.consumer<JsonObject>("main.runtask")
        consumer.handler { message ->
            launch(VertxContext(util.vertx)) {
                var subtasks = message.body().getValue("subtasks") as JsonArray;
                for (i in 0..subtasks.count() - 1){
                    var jsonObj = subtasks.getJsonObject(i);
                    var subtaskType = jsonObj.getString("type");
                    var subtask = jsonObj.getJsonObject("subtask")
                    var taskId = jsonObj.getString("taskId")
                    // TODO: Добавить обработку статуса возвращаемого результата
                    when (subtaskType) {
                        "repo" -> loadRepositoryAsync(subtask, taskId)
                        "mvn"  -> doBuilderTaskAsync(subtask, taskId)

                    }
                }
                // TODO: Если все подзадачи завершились успешно, отправить соответствующее сообщение
                message.reply(message.body())
            }
        }
    }
}

suspend fun deployMainVerticleAsync(): String {
    return vxa<String>({ callback ->
        vertx.deployVerticle(MainVerticle(), callback)
    })
}





