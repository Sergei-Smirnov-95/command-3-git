package testPackage

import builder.deployBuildVerticleAsync
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.runBlocking
import util.*


fun main(args: Array<String>) = runBlocking<Unit> {
    val depRes = deployBuildVerticleAsync()
    setUpBuildMessageListenerAsync(eb)
    eb.publish("builder.build", JsonObject(mapOf("id" to "1", "command" to "mkdir /home/pavel/fromMessage1")))
    eb.publish("builder.build", JsonObject(mapOf("id" to "2", "command" to "mkdir /home/pavel/fromMessage2")))
    eb.publish("builder.build", JsonObject(mapOf("id" to "3", "command" to "mkdir /home/pavel/fromMessage3")))
    vxu { vertx.undeploy(depRes, it) }
    vxu { vertx.close(it) }
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
