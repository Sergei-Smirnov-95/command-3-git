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
import com.fasterxml.jackson.module.kotlin.*
import java.net.URI

data class RepoInfo (val url: String, val branch: String)

class RepoLoaderVerticle: AbstractVerticle(){

    override fun start() {
        val eb = vertx.eventBus()
        val consumer = eb.consumer<String>("RepoLoader")
        consumer.handler { message ->
            launch(Unconfined) {
                val repoInfo: RepoInfo = jacksonObjectMapper().readValue(message.body())
                val job = async(CommonPool) {
                    LoadRepo(repoInfo)
                }
                val status = job.await()
                message.reply(repoInfo.url)
            }

        }
    }

    override fun stop() {
        println("Verticle stop message")
    }

    suspend fun LoadRepo(repoInfo: RepoInfo): Unit {

        val loadRes = vxt<AsyncResult<String>> {
            System.out.println("Start loading repo: " + repoInfo.url)
            val git: Git;
            val loadToPath = Paths.get("/repos/" + repoInfo.url.hashCode())
            if (!Files.exists(loadToPath)) {
                git = org.eclipse.jgit.api.Git.cloneRepository()
                            .setURI(repoInfo.url)
                            .setDirectory(loadToPath.toFile())
                            .setBranch(repoInfo.branch)
                            .call()
                git.close()
            }
            else {
                val repo = FileRepositoryBuilder.create(File("/repos/" + repoInfo.url.hashCode() + "/.git"))
                git = Git(repo);
                git.remoteSetUrl().setUri( URIish(repoInfo.url))
                git.pull().setRemoteBranchName(repoInfo.branch).call()
                git.close()
            }
            it.handle(null)
        }
    }
}


