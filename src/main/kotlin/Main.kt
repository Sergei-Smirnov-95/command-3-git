/**
 * Created by User on 07.11.2017.
 */
package testPackage

import builder.*
import io.vertx.core.AbstractVerticle
import repoloader.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import util.*
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.*
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import java.nio.file.Files
import java.nio.file.Paths

class BuilderTask {
    open var mvn = Mvn()
    open var repo = RepoParams()
    fun repo(addParam: RepoParams.() -> Unit): Unit {
        repo.addParam()
    }
    fun mvn(init: Mvn.() -> Unit): Unit {
        mvn.init()
    }
}

class EmbeddedBuilder {
    var mainVerticle = ""
    var isInitialized = false
    fun init() = runBlocking<Unit> {
        mainVerticle = deployMainVerticleAsync()
        isInitialized = true
    }
    fun deinit()= runBlocking<Unit> {
        vxu { util.vertx.undeploy(mainVerticle, it) }
        isInitialized = false;
    }

    fun run(action: BuilderTask.() -> Unit): String {
        launch(VertxContext(vertx)) {
            if (isInitialized) {
                val bt = BuilderTask()
                bt.action()
                val sender = vertx.eventBus().sender<JsonObject>("Main.RunTask");
                val handler = Handler<AsyncResult<Message<JsonObject>>> { ar ->
                    if (ar.succeeded())
                        System.out.println("Ok \n");
                }
                sender.send(JsonObject(mapOf(
                        "mvn" to bt.mvn.toString(),
                             "repo" to bt.repo.paramsObj
                )), handler);
            }
        }
        return "Err"
    }
}


class MainVerticle : AbstractVerticle(), Loggable {
    var buildVerticle = ""
    var loadVerticle = ""
    override fun start() {
        launch(VertxContext(util.vertx)) {
            buildVerticle = deployBuildVerticleAsync()
            loadVerticle = deployLoadVerticleAsync()
            setUpBuildMessageListenerAsync(eb)
        }
        val consumer = eb.consumer<JsonObject>("Main.RunTask")
        consumer.handler { message ->
            launch(VertxContext(util.vertx)) {
                try {
                    var mvn = message.body().getString("mvn")
                    var repo = message.body().getJsonObject("repo")
                    var loadResult = loadRepositoryAsync(repo)
                    var hash = loadResult.body().getString("url").hashCode()
                    //doInCommandLineAsync("xcopy C:${loadResult.body().getString("loadPath")} C:\\tmp /s /e")
                    // FIXME cwd передается аргументом при выполнении команды, нужно настроить вместо явного tmp
                    doInCommandLineAsync("cp -r ${loadResult.body().getString("loadPath")} /tmp/$hash", "/tmp")
                    eb.publish("builder.build", JsonObject(
                            mapOf(
                                    "id" to "$hash",
                                    "command" to mvn
                            )
                    ))
                    message.reply(message.body())
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }

    override fun stop() = runBlocking<Unit> {
        //println("Verticle stop message")
        vxu { util.vertx.undeploy(buildVerticle, it) }
        vxu { util.vertx.undeploy(loadVerticle, it) }
        vxu { util.vertx.close(it) }
        log.info("Verticle RepoLoader stop message")
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

suspend fun deployMainVerticleAsync(): String {
    return vxa<String>({ callback ->
        vertx.deployVerticle(MainVerticle(), callback)
    })
}





