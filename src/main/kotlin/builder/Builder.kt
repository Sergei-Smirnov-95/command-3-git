package builder

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import util.vertx
import io.vertx.ext.shell.ShellService
import io.vertx.ext.shell.ShellServiceOptions
import io.vertx.ext.shell.term.TelnetTermOptions
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecuteResultHandler
import util.vxu
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.Executor
import util.execResult


class BuildVerticle() : AbstractVerticle() {

    private val eb: EventBus = vertx.eventBus()
//    private val shellOptions: ShellServiceOptions = getOptions()
//    private val shell: ShellService = createShell(shellOptions)

    override fun start() {
        println("BuildVerticle start message")
    }

    override fun stop() {
        println("BuildVerticle stop message")
    }
    /*
    private fun getOptions(): ShellServiceOptions {
        return ShellServiceOptions().setTelnetOptions(
                TelnetTermOptions().
                        setHost("localhost").
                        setPort(1337)
        )
    }

    private fun createShell(options: ShellServiceOptions? = null): ShellService {
        return if (options != null) ShellService.create(vertx, options) else ShellService.create(vertx)
    }


    private suspend fun startShellAsync() {
        vxu { callback ->
            shell.start(callback)
        }
        println("Shell started")
    }

    private suspend fun stopShellAsync() {
        vxu { callback ->
            shell.stop(callback)
        }
        println("Shell stopped")
    }
    */



}


