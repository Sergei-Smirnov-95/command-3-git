package testPackage

import database.deployDBVerticleAsync
import repoLoader.RepoInfo
import repoLoader.RepoLoaderVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import util.*
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.delay
//import util.GlobalLogging.log

fun main(args: Array<String>) = runBlocking<Unit> {
    if (args.size == 2)
        testDB(args[0], args[1])
    else
        testDB(args[0], args[1], args[2])
//    testDBCreate(args[0], args[1])
//    db_shenanigans()
//    val buildVerticle = deployBuildVerticleAsync()
//    var loadVerticle = deployLoadVerticleAsync()
//    try {
//        setUpBuildMessageListenerAsync(eb)
//        val repoInfo = RepoInfo("https://github.com/JetBrains/kotlin-examples.git", "master")
//        var loadResult = loadRepositoryAsync(repoInfo)
//        for (i in 1..5) {
//            doInCommandLineAsync("cp -r ${loadResult.body()} /tmp/$i")
//        }
//        for (i in 1..5) {
//            val builder = mvn {
//                test {
//                    path("/tmp/$i/maven/hello-world")
//                }
//            }
//            eb.publish("builder.build", JsonObject(mapOf("id" to "$i", "command" to builder.toString())))
//        }
//    } catch (e: Exception) {
//        println(e.message)
//    } finally {
//        vxu { vertx.undeploy(buildVerticle, it) }
//        vxu { vertx.undeploy(loadVerticle, it) }
//        vxu { vertx.close(it) }
//    }
}

suspend fun testDB(operation: String, key: String, path: String? = null){
    if (operation !in arrayListOf<String>("create", "read", "update", "delete"))
        return
    if (operation != "delete" && path == null)
        return
    setUpDBMessageListenerAsync(eb)
    deployDBVerticleAsync()
    eb.publish("database.$operation", JsonObject(mapOf(
            "id" to key,
            "artifact" to path)))
    delay(10000)
    vxu { vertx.close(it) }
}

suspend fun testDBCreate(key: String, path:String) {
    setUpDBMessageListenerAsync(eb)
    deployDBVerticleAsync()
    eb.publish("database.create", JsonObject(mapOf(
            "id" to key,
            "artifact" to path)))
    delay(1000)
    vxu { vertx.close(it) }
}

suspend fun testDBRead(key: String, path:String) {
    setUpDBMessageListenerAsync(eb)
    deployDBVerticleAsync()
    eb.publish("database.read", JsonObject(mapOf(
            "id" to "4",
            "artifact" to "/tmp/binary3")))
    delay(10000)
    vxu { vertx.close(it) }
}

suspend fun deployLoadVerticleAsync(): String {
    return vxa<String>({ callback ->
        vertx.deployVerticle(RepoLoaderVerticle(), callback)
    })
}

suspend fun loadRepositoryAsync(info: RepoInfo): Message<String> {
    return vxa<Message<String>> {
        eb.send("RepoLoader", Json.encode(info), it)
    }
}

suspend fun doSomethingUseful() {
    val job = launch(Unconfined) {
        var id: String? = null
        val rlVerticle = RepoLoaderVerticle()
        val deployRes = vxt<AsyncResult<String>> {
            println("Deploy verticle from coroutine")
            //log.info("Deploy verticle from coroutine")
            vertx.deployVerticle(rlVerticle, it)
        }

        if (deployRes.succeeded()) {
            val repos = Array<String>(3, { i -> "" })
            repos[0] = "https://github.com/json-iterator/java.git"
            repos[1] = "https://github.com/pavel-epanechkin/spbpu.git"
            repos[2] = "https://github.com/Sergei-Smirnov-95/team-3-git.git"
            val branches = Array<String>(3, { i -> "" })
            branches[0] = "master"
            branches[1] = "master"
            branches[2] = "build"
            for (i in 0..2) {
                val repoInfo = RepoInfo(repos[i], branches[i])
                val sender = vertx.eventBus().sender<String>("RepoLoader");
                val handler = Handler<AsyncResult<Message<String>>> { ar ->
                    if (ar.succeeded())
                        System.out.println("Repo has been loaded: " + ar.result().body() + "\n");
                }
                sender.send(Json.encode(repoInfo), handler);
            }
            id = deployRes.result()
            println("Deployment success")
            //log.info("Deployment success")
        } else
            println("Deployment failure")
            //log.info("Deployment failure")
    }
}

suspend fun setUpBuildMessageListenerAsync(eb: EventBus) {
    val consumer = eb.consumer<JsonObject>("results.build")
    consumer.handler { message ->
        val body = message.body()
        println("Response to ${body.getString("for")}: ${body.getString("result")}")
        //log.info("Response to ${body.getString("for")}: ${body.getString("result")}")
    }
    vxu {
        consumer.completionHandler(it)
    }
}

suspend fun setUpDBMessageListenerAsync(eb: EventBus) {
    val consumer = eb.consumer<JsonObject>("results.database")
    consumer.handler { message ->
        val body = message.body()
        println("Response to ${body.getString("for")}: ${body.getString("result") } (${body.getString("code")})")
    }
    vxu {
        consumer.completionHandler(it)
    }
}
