package util

import org.apache.commons.exec.*
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

suspend fun doInCommandLine(instructions: String){
    val cmdLine = CommandLine.parse(instructions)
    for (value in cmdLine.arguments)
        println(value)
    val executor = DefaultExecutor()
    val exitValue = execResultAsync(executor, cmdLine)
    println(exitValue)
}