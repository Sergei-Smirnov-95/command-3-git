package testPackage

import builder.FirstVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import util.*

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
    doNothingUseful()
}


suspend fun doNothingUseful() {
    val job = launch(Unconfined) {
        var id: String? = null
//        val deployRes = vxa<String> {
        val deployRes = vxt<AsyncResult<String>> {
            println("Deploy verticle from coroutine")
            vertx.deployVerticle(FirstVerticle())
        }
        if (deployRes.succeeded()) {
            id = deployRes.result()
            println("Deployment success")
        } else
            println("Deployment failure")
        if (id != null){
            val undeployRes = vxt<AsyncResult<String>> {
                println("Undeploy verticle from coroutine")
                vertx.undeploy(id)
            }
            if (undeployRes.succeeded())
                println("Undeploy success")
            else
                println("Undeploy failure")
            val vxcloseRes = vxt<AsyncResult<String>> {
                println("Close vertx")
                vertx.close()
            }
            if (vxcloseRes.succeeded())
                println("Close success")
            else
                println("Close failure")
        }
    }
}
