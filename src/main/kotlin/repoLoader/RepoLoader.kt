package repoloader

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import util.vxt
import java.nio.file.Files
import java.nio.file.Paths
import io.vertx.core.json.JsonObject
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor

data class RepoInfo(val url: String, val branch: String, val loadPath: String, val type: String)
class RepoLoaderVerticle : AbstractVerticle() {

    override fun start() {
        val eb = vertx.eventBus()
        val consumer = eb.consumer<JsonObject>("RepoLoader")
        consumer.handler { message ->
            launch(Unconfined) {
                val job = async(CommonPool) {
                    LoadRepo(message.body())
                }
                val status = job.await()
                message.reply(message.body())
            }
        }
    }

    override fun stop() {
        println("Verticle stop message")
    }

    suspend fun LoadRepo(repoInfo: JsonObject): Unit {

        val loadRes = vxt<AsyncResult<String>> {
            val url = repoInfo.getString("url");
            val branch = repoInfo.getString("branch");
            val type = repoInfo.getString("type");
            val loadPath = repoInfo.getString("loadPath")
            System.out.println("Start loading repo: " + url)
            val loadToPath = Paths.get(loadPath)
            val executor = DefaultExecutor()
            if (!Files.exists(loadToPath)) {
                Files.createDirectory(loadToPath);
                executor.setWorkingDirectory(loadToPath.toFile())
                val init = CommandLine.parse(
                        String.format("%s init", type)
                )
                executor.execute(init)
            }
            val pull = CommandLine.parse(
                    String.format(
                            "%s pull \"%s\" %s",
                            type,
                            url,
                            if (type == "hg") "-b " + branch else branch
                    )
            )
            executor.setWorkingDirectory(loadToPath.toFile())
            executor.execute(pull)
            if (type == "hg") {
                val update = CommandLine.parse("hg update")
                executor.execute(update)
            }
            it.handle(null)
        }
    }
}