package testPackage

import builder.FirstVerticle
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



val vertx: Vertx = Vertx.vertx()

//fun main(args: Array<String>) {
//    var id: String? = null
//    vertx.deployVerticle(FirstVerticle(), { res ->
//        if (res.succeeded()) {
//            id = res.result()
//            println("Deployment success")
//        } else {
//            println("Deployment failure")
//        }
//    })
//    Thread.sleep(2000)
//    if (id != null)
//        vertx.undeploy(id, { res ->
//            if (res.succeeded())
//                println("Undeploy success")
//            else {
//                println("Undeploy failure")
//            }
//            vertx.close({ res ->
//                if (res.succeeded()) {
//                    println("Close success")
//                } else {
//                    println("Close failure")
//                }
//            })
//        })
//    println("Finishing")
//}


//fun main(args: Array<String>) = runBlocking <Unit> {
//    val jobs = List(100_000) { // launch a lot of coroutines and list their jobs
//        launch(Unconfined) {
//            delay(1000L)
//            print(".")
//        }
//    }
//    jobs.forEach { it.join() } // wait for all jobs to complete
//}

fun main(args: Array<String>) = runBlocking<Unit> {
    doSomethingUseful()
}

suspend fun doSomethingUseful() {
    // "Мнемоническое правило: если функция асинхронная, то должен быть callback, который когда-то дернут.
    // В данном примере, это делает it, в общем случае это плохая практика. Лучше писать async функции.
    // Даже в последнем случае нужно отслеживать асинхронные вызовы (разобраться, как именно)."
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
            repos[0] = "https://github.com/json-iterator/java"
            repos[1] = "https://github.com/pavel-epanechkin/spbpu.git"
            repos[2] = "https://github.com/Sergei-Smirnov-95/team-3-git.git"
            val branches = Array<String>(3, {i -> ""})
            branches[0] = "master"
            branches[1] = "master"
            branches[2] = "build"
            for (i in 0..2){
                val options = DeliveryOptions()
                options.addHeader("repoUrl", repos[i])
                options.addHeader("repoBranch", branches[i])
                val sender = vertx.eventBus().sender<String>("RepoLoader", options);
                val handler = Handler<AsyncResult<Message<String>>> { ar ->
                    if (ar.succeeded())
                        System.out.println("Repo has been loaded: " + ar.result().body() + "\n");
                }
                sender.send("Test message", handler);
            }
            id = deployRes.result()
            println("Deployment success")
        } else
            println("Deployment failure")

    }
}
