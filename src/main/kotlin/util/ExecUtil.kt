package util

import org.apache.commons.exec.*
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import java.io.*


inline suspend fun execResult(crossinline cb: (ExecuteResultHandler) -> Unit) =
    suspendCoroutine { cont: Continuation<Int> ->
        cb( object: ExecuteResultHandler{
            override fun onProcessFailed(p0: ExecuteException?) {
                if (p0 != null)
                    cont.resumeWithException(p0)
            }
            override fun onProcessComplete(p0: Int) {
                cont.resume(p0)
            }

        })
    }

private suspend fun execResultAsync(executor: Executor, cmdLine: CommandLine): Int {
    return execResult {
        executor.execute(cmdLine, it)
    }
}

suspend fun doInCommandLineAsync(instructions: String, cwd: String? = null, logFilePath: String? = null): Int{
    val cmdLine = CommandLine.parse(instructions)
    val executor = DefaultExecutor()
    if (cwd != null)
        executor.setWorkingDirectory(File(cwd))
    val streamHandler: PumpStreamHandler
    if (logFilePath != null) {
        val out = FileOutputStream(logFilePath, true)
        streamHandler = PumpStreamHandler(out, out)
    }
    else {
        streamHandler = PumpStreamHandler()
    }
    executor.setStreamHandler(streamHandler)
    return execResultAsync(executor, cmdLine)
}
