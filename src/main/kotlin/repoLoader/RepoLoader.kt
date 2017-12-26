package repoloader

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.nio.file.Files
import java.nio.file.Paths
import io.vertx.core.json.JsonObject
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import util.Loggable
import util.*
import io.vertx.core.eventbus.DeliveryOptions



abstract class ConsoleRepoloader {
    protected val executor = DefaultExecutor()
    abstract val cmd: String
    abstract fun pullCommand (url: String, branch: String) : CommandLine

    open fun load (repoInfo: JsonObject, id: String) : Unit {
        val url = repoInfo.getString("url");
        val branch = repoInfo.getString("branch");
        val loadToPath = Paths.get(main.loadPath + id)
        if (!Files.exists(loadToPath)) {
            Files.createDirectory(loadToPath);
            executor.setWorkingDirectory(loadToPath.toFile())
            val init = CommandLine.parse(
                    String.format("%s init", cmd)
            )
            executor.execute(init)
        }
        executor.setWorkingDirectory(loadToPath.toFile())
        executor.execute(pullCommand(url, branch))
    }
}
class GitLoader : ConsoleRepoloader () {

    override val cmd: String
        get() = "git"

    override fun pullCommand(url: String, branch: String): CommandLine {
        return CommandLine.parse(
                String.format(
                        "git pull \"%s\" %s",
                        url,
                        branch
                )
        )
    }
}
class HgLoader : ConsoleRepoloader () {

    override val cmd: String
        get() = "hg"

    override fun pullCommand(url: String, branch: String): CommandLine {
        return CommandLine.parse(
                String.format(
                        "hg pull \"%s\" -b %s",
                        url,
                        branch
                )
        )
    }
    override fun load(repoInfo: JsonObject, id: String) {
        super.load(repoInfo, id)
        val update = CommandLine.parse("hg update")
        executor.execute(update)
    }
}
object RepoloadersFactory {
    fun newLoader(type: String) : ConsoleRepoloader? {
        when (type) {
            "git"   -> return GitLoader()
            "hg"    -> return HgLoader()
            else    -> return null
        }
    }
}

data class RepoInfo(val url: String, val branch: String, val loadPath: String, val type: String)
class RepoLoaderVerticle : AbstractVerticle(),Loggable {

    override fun start() {
        log.info("Verticle RepoLoader start message")
        val eb = vertx.eventBus()
        val consumer = eb.consumer<JsonObject>("repoloader.load")
        consumer.handler { message ->
            launch(VertxContext(util.vertx)) {
                val job = async(CommonPool) {
                    LoadRepo(message.body(), message.headers().get("taskId"))
                }
                val status = job.await()
                message.reply(message.body())
            }
        }
    }

    override fun stop() {
        log.info("Verticle RepoLoader stop message")
    }

    suspend fun LoadRepo(repoInfo: JsonObject, id: String): Unit {


        val loadRes = vxt<AsyncResult<String>> {
            val type = repoInfo.getString("type");
            val loader = RepoloadersFactory.newLoader(type)
            if (loader != null)
                loader.load(repoInfo, id)
            else
                throw Exception("Wrong repos type")
            it.handle(null)
        }
    }
}

suspend fun deployLoadVerticleAsync(): String {
    return vxa<String>({ callback ->
        vertx.deployVerticle(RepoLoaderVerticle(), callback)
    })
}

suspend fun loadRepositoryAsync(info: JsonObject, taskId: String): Message<JsonObject> {
    return vxa<Message<JsonObject>> {
        val options = DeliveryOptions()
        options.addHeader("taskId", taskId)
        eb.send("repoloader.load", info, options, it)
    }
}