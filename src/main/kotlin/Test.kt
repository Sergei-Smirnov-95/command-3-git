package testPackage

import repoloader.*
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import util.*
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject


val vertx: Vertx = Vertx.vertx()

fun main(args: Array<String>) = runBlocking<Unit> {
    doSomethingUseful()
}

suspend fun doSomethingUseful() {
    val job = launch(Unconfined) {
        var id: String? = null
        val rlVerticle = RepoLoaderVerticle()
//        val deployRes = vxa<String> {
        val deployRes = vxt<AsyncResult<String>> {
            println("Deploy verticle from coroutine")
            vertx.deployVerticle(rlVerticle, it)
        }

        if (deployRes.succeeded()) {
            val repos = Array<String>(4, {i -> ""})
            repos[0] = "https://github.com/json-iterator/java.git"
            repos[1] = "https://bitbucket.org/PavelEpanechkin/testhgrepo"
            repos[2] = "https://github.com/pavel-epanechkin/spbpu.git"
            repos[3] = "https://github.com/Sergei-Smirnov-95/team-3-git.git"


            val branches = Array<String>(4, {i -> ""})
            branches[0] = "master"
            branches[1] = "default"
            branches[2] = "master"
            branches[3] = "build"

            val types = Array<String>(4, {i -> ""})
            types[0] = "git"
            types[1] = "hg"
            types[2] = "git"
            types[3] = "git"

            for (i in 0..3){
                val repoInfo = RepoInfo(
                        repos[i],
                        branches[i],
                        "/repos/ " + repos[i].hashCode(),
                        types[i]
                )
                val sender = vertx.eventBus().sender<JsonObject>("RepoLoader");
                val handler = Handler<AsyncResult<Message<JsonObject>>> { ar ->
                    if (ar.succeeded())
                        System.out.println("Repo has been loaded: " + ar.result().body() + "\n");
                }
                sender.send(JsonObject(
                        Json.encode(repoInfo)
                ), handler);
            }
            id = deployRes.result()
            println("Deployment success")
        } else
            println("Deployment failure")

    }
}
