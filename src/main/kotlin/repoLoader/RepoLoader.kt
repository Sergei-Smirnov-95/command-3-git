package repoLoader

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.URIish
import java.io.File
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import util.vxt
import util.Loggable
import java.nio.file.Files
import java.nio.file.Paths
import com.fasterxml.jackson.module.kotlin.*
import java.lang.Math.abs

data class RepoInfo(val url: String, val branch: String, val location: String = "/tmp/")

class RepoLoaderVerticle : AbstractVerticle(),Loggable {

    override fun start() {
        //println("Verticle RepoLoader start message")
        log.info("Verticle RepoLoader start message")
        val eb = vertx.eventBus()
        val consumer = eb.consumer<String>("RepoLoader")
        consumer.handler { message ->
            launch(Unconfined) {
                val repoInfo: RepoInfo = jacksonObjectMapper().readValue(message.body())
                val job = async(CommonPool) {
                    LoadRepo(repoInfo)
                }
                val status = job.await()
                message.reply(repoInfo.location + abs(repoInfo.url.hashCode()))
            }
        }
    }

    override fun stop() {
        //println("Verticle stop message")
        log.info("Verticle RepoLoader stop message")
    }

    suspend fun LoadRepo(repoInfo: RepoInfo): Unit {

        val loadRes = vxt<AsyncResult<String>> {
            System.out.println("Start loading repo: " + repoInfo.url)
            val git: Git;
            val loadToPath = Paths.get(repoInfo.location + abs(repoInfo.url.hashCode()))
            if (!Files.exists(loadToPath)) {
                git = org.eclipse.jgit.api.Git.cloneRepository()
                        .setURI(repoInfo.url)
                        .setDirectory(loadToPath.toFile())
                        .setBranch(repoInfo.branch)
                        .call()
                git.close()
            } else {
                val repo = FileRepositoryBuilder.create(File(repoInfo.location + abs(repoInfo.url.hashCode()) + "/.git"))
                git = Git(repo);
                git.remoteSetUrl().setUri(URIish(repoInfo.url))
                git.pull().setRemoteBranchName(repoInfo.branch).call()
                git.close()
            }
            it.handle(null)
        }
    }
}


