package util

import builder.BuildVerticle
import io.vertx.core.AsyncResult
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch

//val vertx: Vertx = Vertx.vertx()

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

suspend fun doNothingUseful() {
    // "Мнемоническое правило: если функция асинхронная, то должен быть callback, который когда-то дернут.
    // В данном примере, это делает it, в общем случае это плохая практика. Лучше писать async функции.
    // Даже в последнем случае нужно отслеживать асинхронные вызовы (разобраться, как именно)."
    // Eventbus.consumer
    val job = launch(Unconfined) {
        var id: String? = null
        val deployRes = vxt<AsyncResult<String>> {
            println("Deploy verticle from coroutine")
            vertx.deployVerticle(BuildVerticle(), it)
        }
        if (deployRes.succeeded()) {
            id = deployRes.result()
            println("Deployment success")
        } else
            println("Deployment failure")
        if (id != null) {
            val undeployRes = vxu {
                println("Undeploy verticle from coroutine")
                vertx.undeploy(id, it)
            }
            val vxcloseRes = vxt<AsyncResult<Void>> {
                println("Close vertx")
                vertx.close(it)
            }
            if (vxcloseRes.succeeded())
                println("Close success")
            else
                println("Close failure")
        }
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

/*
suspend fun db_shenanigans() {
    val settings = File("src/settings/db-settings.json")
    val postgreSQLClientConfig = JsonObject(settings.readText())
    val client = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig)
    val connection = vxa<SQLConnection> { client.getConnection(it) }
    val result = vxa<ResultSet> { connection.query("select * from test;", it) }
    for (res in result.results) {
        println(res)
    }
    vxu { connection.close(it) }
    vxu { client.close(it) }
}
*/
