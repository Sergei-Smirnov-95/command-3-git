package database

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult
import kotlinx.coroutines.experimental.launch
import util.*
import java.io.*
import util.GlobalLogging.log
import org.apache.commons.codec.binary.Hex


class DataBaseVerticle() : AbstractVerticle(), Loggable {
    val settings = File("src/settings/db-settings.json")
    val postgreSQLClientConfig = JsonObject(settings.readText())
    var client: AsyncSQLClient? = null

    override fun start() {
        client = PostgreSQLClient.createShared(this.getVertx(), postgreSQLClientConfig)
        for (operation in arrayListOf<String>("create", "read", "update", "delete")) {
            registerConsumer(operation)
        }
        log.info("Database verticle started")
    }

    override fun stop() {
        log.info("Database verticle stopped")
    }

    private fun requestOK(type: String, request: JsonObject): Boolean {
        return when (type) {
            "create",
            "update" -> request.containsKey("id") && request.containsKey("artifact")
            "read",
            "delete" -> return request.containsKey("id")
            else -> return false
        }
    }

    private suspend fun SQLConnection.queryAsync(query: String): ResultSet = vxa<ResultSet> { this.query(query, it) }

    private suspend fun SQLConnection.execAsync(exec: String) = vxu { this.execute(exec, it) }

    private suspend fun SQLConnection.updateAsync(update: String): UpdateResult {
       return vxa<UpdateResult> { this.update(update, it) }
    }

    private suspend fun getConnectionAsync(client: AsyncSQLClient): SQLConnection {
        return vxa<SQLConnection> { client.getConnection(it) }
    }

    private fun registerConsumer(operationType: String) {
        eb.consumer<JsonObject>("database.$operationType") { message ->
            log.info("$operationType request received")
            val request = message.body()
            if (requestOK(operationType, request)) {
                when (operationType) {
                    "create" -> handleCreate(request)
                    "read" -> handleRead(request)
                    "update" -> handleUpdate(request)
                    "delete" -> handleDelete(request)
                }
            } else
                message.fail(1, "Wrong format")
        }
    }

    private fun handleCreate(request: JsonObject) {
        val key = request.getString("id")
        val path = request.getString("artifact")
        val data = Hex.encodeHex(File(path).readBytes())
        launch(VertxContext(vertx)) {
            val connection = getConnectionAsync(client!!)
            connection.execAsync(
                    "INSERT INTO artifacts (key, data) VALUES ('$key', decode('$data', 'hex'))")
            publishResults(key, "create", "OK", 0)
        }
    }

    private fun handleRead(request: JsonObject) {
        val key = request.getString("id")
        val path = request.getString("artifact")
        launch(VertxContext(this.getVertx())) {
            val connection = getConnectionAsync(client!!)
            val fetchResult = connection.queryAsync(
                    "SELECT encode(data, 'hex') FROM artifacts WHERE key='$key';")
            if (fetchResult.results.count() != 1) {
                publishResults(key, "read", "${fetchResult.results.count()} records returned", 1)
            } else {
                var hexResult = fetchResult.results[0].getString(0)
                publishResults(key, "read", hexResult, 0)
            }
        }
    }

    private fun handleUpdate(request: JsonObject) {
        val key = request.getString("id")
        val path = request.getString("artifact")
        val data = Hex.encodeHex(File(path).readBytes())
        launch(VertxContext(vertx)) {
            val connection = getConnectionAsync(client!!)
            val result = connection.updateAsync(
                    "UPDATE artifacts SET data = '$data' where key = '$key';")
            if (result.updated == 1){
                publishResults(key, "update", "OK", 0)
            }
            else{
                publishResults(key, "update", "${result.updated} records updated", 1)
            }
        }
    }

    private fun handleDelete(request: JsonObject) {
        val key = request.getString("id")
        launch(VertxContext(this.vertx)) {
            val connection = getConnectionAsync(client!!)
            connection.execAsync(
                    "DELETE FROM artifacts where key = '$key';")
            publishResults(key, "delete", "OK", 0)
        }
    }

    private fun publishResults(key: String, op: String, result: String, code: Int) {
        eb.publish("results.database", JsonObject(
                mapOf("for" to key,
                        "result" to result,
                        "operation" to op,
                        "code" to code.toString()))
        )
    }
}

suspend fun deployDBVerticleAsync(): String = vxa<String> { vertx.deployVerticle(DataBaseVerticle(), it) }
