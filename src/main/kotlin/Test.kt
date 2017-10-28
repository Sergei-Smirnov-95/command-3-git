package testPackage

import builder.*
import builder.RepoInfo
import builder.RepoLoaderVerticle
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

fun main(args: Array<String>) = runBlocking<Unit> {
    doSomethingUseful()

/*
    val depRes = deployBuildVerticleAsync()
    setUpBuildMessageListenerAsync(eb)

    eb.publish("builder.build", JsonObject(mapOf("id" to "1", "command" to "mvn clean test -f /new_home/downloads/kotlin-examples-master/maven/hello-world-1")))
    eb.publish("builder.build", JsonObject(mapOf("id" to "2", "command" to "mvn clean test -f /new_home/downloads/kotlin-examples-master/maven/hello-world-2")))
    eb.publish("builder.build", JsonObject(mapOf("id" to "3", "command" to "mvn clean test -f /new_home/downloads/kotlin-examples-master/maven/hello-world-3")))
    vxu { vertx.undeploy(depRes, it) }
    vxu { vertx.close(it) }
*/
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
            val repos = Array<String>(3, {i -> ""})
            repos[0] = "https://github.com/json-iterator/java.git"
            repos[1] = "https://github.com/pavel-epanechkin/spbpu.git"
            repos[2] = "https://github.com/Sergei-Smirnov-95/team-3-git.git"
            val branches = Array<String>(3, {i -> ""})
            branches[0] = "master"
            branches[1] = "master"
            branches[2] = "build"
            for (i in 0..2){
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
        } else
            println("Deployment failure")

    }
}

suspend fun setUpBuildMessageListenerAsync(eb: EventBus){
    val consumer = eb.consumer<JsonObject>("results.build")
    consumer.handler{ message ->
        val body = message.body()
        println("Response to ${body.getString("for")}: ${body.getString("result")}")
    }
    vxu {
        consumer.completionHandler(it)
    }
}


/*
fun foo() {
    val buildDescription = BuildDescription {
        Step (
            steps.CreateDirectory(dir = ...)
        )
    }
}
*/