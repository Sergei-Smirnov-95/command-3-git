package database

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import kotlinx.coroutines.experimental.launch
import util.*
import java.io.*
//import util.GlobalLogging.log

suspend fun db_shenanigans() {
    val settings = File("src/settings/db-settings.json")
    val postgreSQLClientConfig = JsonObject(settings.readText())
    val client = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig)
    val connection = vxa<SQLConnection> { client.getConnection(it) }
    val result = vxa<ResultSet> { connection.query("select * from test;", it) }
    for (res in result.results) {
        println(res)
        //log.info({res})
    }
    vxu { connection.close(it) }
    vxu { client.close(it) }
}

class DataBaseVerticle() : AbstractVerticle(), Loggable {
    var connection: SQLConnection? = null

    override fun start() {
            val settings = File("src/settings/db-settings.json")
            val postgreSQLClientConfig = JsonObject(settings.readText())
            val client = PostgreSQLClient.createShared(util.vertx, postgreSQLClientConfig)
            eb.consumer<JsonObject>("database.create") { message ->
                log.info("Request received")
                val request = message.body()
                if (requestOK("create", request)) {
                    val id = request.getString("id")
                    val data = request.getBinary("artifact")
                    launch(VertxContext(vertx)) {
                        if (connection == null) {
                            connection = vxa<SQLConnection> { client.getConnection(it) }
                        }
                        vxu {
                            connection!!.execute("INSERT INTO artifacts (key, data) " +
                                    "values ('$id', '$data');", it)
                        }
                        message.reply(request.getString("id"))
                    }
                } else
                    message.reply("Not good")
            }
            log.info("Database verticle started")
    }

    override fun stop() {
        log.info("Database verticle stopped")
    }

    private fun requestOK(type: String, request: JsonObject): Boolean{
        when (type) {
            "create" -> return request.containsKey("id") && request.containsKey("artifact")
            "update" -> return request.containsKey("id") && request.containsKey("artifact")
            "delete" -> return request.containsKey("id")
            else     -> return false
        }
    }
}

suspend fun deployDBVerticleAsync(): String = vxa<String> { vertx.deployVerticle(DataBaseVerticle(), it)}
