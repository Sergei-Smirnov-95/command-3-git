package util

import builder.BuildVerticle
import io.vertx.core.AsyncResult
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.launch

/*
Старая проверка запуска вертикалей
fun main(args: Array<String>) {
    var id: String? = null
    vertx.deployVerticle(FirstVerticle(), { res ->
        if (res.succeeded()) {
            id = res.result()
            println("Deployment success")
        } else {
            println("Deployment failure")
        }
    })
    Thread.sleep(2000)
    if (id != null)
        vertx.undeploy(id, { res ->
            if (res.succeeded())
                println("Undeploy success")
            else {
                println("Undeploy failure")
            }
            vertx.close({ res ->
                if (res.succeeded()) {
                    println("Close success")
                } else {
                    println("Close failure")
                }
            })
        })
    println("Finishing")
}
*/

/* Предыдущий пример через корутины
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
*/

/*
пожелания по виду DSL
fun foo() {
    val buildDescription = BuildDescription {
        Step (
            steps.CreateDirectory(dir = ...)
        )
    }
}
*/

/*
вариант примитивной работы с базой
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

/*
Для ручной проверки сборщика
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
*/

/*
Для ручной проверки БД
// Аргументы - операция, ключ и путь к файлу для всего, кроме удаления из базы
fun main(args: Array<String>) = runBlocking<Unit> {
    if (args.size == 2)
        testDB(args[0], args[1])
    else
        testDB(args[0], args[1], args[2])
    testDBCreate(args[0], args[1])
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
*/
