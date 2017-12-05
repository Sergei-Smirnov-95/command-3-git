package util

import org.apache.commons.exec.*
import java.io.File
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


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

suspend fun doInCommandLineAsync(instructions: String): Int{
    val cmdLine = CommandLine.parse(instructions)
    val executor = DefaultExecutor()
    executor.setWorkingDirectory(File("C:/"))
    val exitValue = execResultAsync(executor, cmdLine)
    return exitValue
}