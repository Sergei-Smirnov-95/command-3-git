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
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import util.vxt


class RepoLoaderVerticle: AbstractVerticle(){

    override fun start() {
        val eb = vertx.eventBus()
        val consumer = eb.consumer<String>("RepoLoader")
        consumer.handler { message ->
            launch(Unconfined) {
                val job = async(CommonPool) { LoadRepo(message) }
                val status = job.await()
                message.reply(message.headers().get("repoUrl"))
            }
        }
    }

    override fun stop() {
        println("Verticle stop message")
    }

    suspend fun LoadRepo(message: Message<String>): Unit {

        val loadRes = vxt<AsyncResult<String>> {
            val repoUrl = message.headers().get("repoUrl")
            val repoBranch = message.headers().get("repoBranch")
            System.out.println("Start loading repo: " + repoUrl)

            val git: Git;
            try {
                git = org.eclipse.jgit.api.Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(File("/repos/" + repoUrl.hashCode()))
                        .setBranch(repoBranch)
                        .call()
                git.close()
            }
            catch (err: Exception){
                try {
                    val repo = FileRepositoryBuilder.create(File("/repos/" + repoUrl.hashCode() +".git"));
                    val git = Git(repo);
                    git.remoteSetUrl()
                            .setUri( URIish(repoUrl));
                    git.pull().setRemoteBranchName(repoBranch).call();
                    git.close()
                }
                catch (err: Exception){
                    val test = "test";
                }
            }

            it.handle(null)
        }

    }
}


