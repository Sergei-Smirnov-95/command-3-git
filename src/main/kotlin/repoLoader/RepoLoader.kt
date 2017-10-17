package builder

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.URIish
import java.io.File
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.eventbus.MessageConsumer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import util.vxt
import java.nio.file.Files
import java.nio.file.Paths
import java.net.URI

data class RepoInfo (val url: String, val branch: String)

class RepoLoaderVerticle: AbstractVerticle(){

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
            val url = repoInfo.getString("repoUrl");
            val branch = repoInfo.getString("repoBranch");
            System.out.println("Start loading repo: " + url)
            val git: Git;
            val loadToPath = Paths.get("/repos/" + url.hashCode())
            if (!Files.exists(loadToPath)) {
                git = org.eclipse.jgit.api.Git.cloneRepository()
                            .setURI(url)
                            .setDirectory(loadToPath.toFile())
                            .setBranch(branch)
                            .call()
                git.close()
            }
            else {
                val repo = FileRepositoryBuilder.create(File("/repos/" + url.hashCode() + "/.git"))
                git = Git(repo);
                git.remoteSetUrl().setUri( URIish(url))
                git.pull().setRemoteBranchName(branch).call()
                git.close()
            }
            it.handle(null)
        }

    }
}


