package database

import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.PostgreSQLClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import kotlinx.coroutines.experimental.launch
import util.*
import java.io.*
//import util.GlobalLogging.log


class DataBaseVerticle() : AbstractVerticle(), Loggable {
    private var connection: SQLConnection? = null
    val settings = File("src/settings/db-settings.json")
    val postgreSQLClientConfig = JsonObject(settings.readText())
    val client = PostgreSQLClient.createShared(util.vertx, postgreSQLClientConfig)

    override fun start() {
        for (operation in arrayListOf<String>("create", "read", "update", "delete"))
            registerConsumer(operation)
        log.info("Database verticle started")
    }

    override fun stop() {
        log.info("Database verticle stopped")
    }

    private fun requestOK(type: String, request: JsonObject): Boolean{
        when (type) {
            "create" -> return request.containsKey("id") && request.containsKey("artifact")
            "read"   -> return request.containsKey("id") && request.containsKey("artifact")
            "update" -> return request.containsKey("id") && request.containsKey("artifact")
            "delete" -> return request.containsKey("id")
            else     -> return false
        }
    }

    private suspend fun SQLConnection.queryAsync(query: String): ResultSet = vxa <ResultSet> { this.query(query, it) }

    private suspend fun SQLConnection.execAsync( exec: String) = vxu { this.execute(exec, it) }

    private suspend fun SQLConnection?.getConnectionAsync(client: AsyncSQLClient): SQLConnection {
        if (this == null)
            return vxa<SQLConnection> { client.getConnection(it) }
        else
            return this
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
        val data = File(path).readBytes().toHex()
        launch(VertxContext(vertx)) {
            connection = connection.getConnectionAsync(client)
            if (!keyInDatabase(key, connection!!)) {
                connection!!.execAsync(
                        "INSERT INTO artifacts (key, data) VALUES ('$key', decode('$data', 'hex'))")
                publishResults(key, "create", "OK", 0)
            }
            else{
                publishResults(key, "create", "Wrong key", 1)
            }
        }
    }

    private fun handleRead(request: JsonObject) {
        val key = request.getString("id")
        val path = request.getString("artifact")
        launch(VertxContext(vertx)) {
            connection = connection.getConnectionAsync(client)
            if (keyInDatabase(key, connection!!)) {
                val fetchResult = connection!!.queryAsync(
                        "SELECT encode(data, 'hex') FROM artifacts WHERE key='$key';")
                val file = File(path)
                if (file.createNewFile()) {
                    for (item in fetchResult.results)
                        file.writeBytes(item.getString(0).toByteArray())
                    publishResults(key, "read", "OK", 0)

                } else {
                    publishResults(key, "read", "File exists", 2)
                }
            }
            else
                publishResults(key, "read", "Wrong key", 1)
        }
    }

    private fun handleUpdate(request: JsonObject) {
        val key = request.getString("id")
        val path = request.getString("artifact")
        val data = File(path).readBytes().toHex()
        launch(VertxContext(vertx)) {
            connection = connection.getConnectionAsync(client)
            if (keyInDatabase(key, connection!!)) {
                connection!!.execAsync(
                        "UPDATE artifacts SET data = '$data' where key = '$key';")
                publishResults(key, "update", "OK", 0)
            }
            else{
                publishResults(key, "update", "Wrong key", 1)
            }
        }
    }

    private fun handleDelete(request: JsonObject) {
        val key = request.getString("id")
        launch(VertxContext(vertx)) {
            connection = connection.getConnectionAsync(client)
            if (keyInDatabase(key, connection!!)) {
                connection!!.execAsync(
                        "DELETE FROM artifacts where key = '$key';")
                publishResults(key, "delete", "OK", 0)
            }
            else{
                publishResults(key, "delete", "Wrong key", 1)
            }
        }
    }

    private fun publishResults(key: String, op: String, result: String, code: Int){
        eb.publish("results.database", JsonObject(
                mapOf("for" to key,
                        "result" to result,
                        "operation" to op,
                        "code" to code.toString()))
        )
    }

    private suspend fun keyInDatabase(key: String, connection: SQLConnection): Boolean{
        val countQR = connection.queryAsync("SELECT count(*) FROM artifacts WHERE key = '$key';")
        return countQR.getResults().get(0).getInteger(0) == 1
    }

    private val HEX_CHARS_ARRAY = "0123456789ABCDEF".toCharArray()

    fun ByteArray.toHex() : String{
        val result = StringBuffer()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS_ARRAY[firstIndex])
            result.append(HEX_CHARS_ARRAY[secondIndex])
        }

        return result.toString()
    }

    private val HEX_CHARS = "0123456789abcdef"

    fun String.toByteArray() : ByteArray {

        val result = ByteArray(this.length / 2)

        for (i in 0 until this.length step 2) {
            val firstIndex = HEX_CHARS.indexOf(this[i]);
            val secondIndex = HEX_CHARS.indexOf(this[i + 1]);

            val octet = firstIndex.shl(4).or(secondIndex)
            result.set(i.shr(1), octet.toByte())
        }

        return result
    }
}

suspend fun deployDBVerticleAsync(): String = vxa<String> { vertx.deployVerticle(DataBaseVerticle(), it)}
