package testPackage

import builder.BuildVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import util.*


fun main(args: Array<String>) = runBlocking<Unit> {
    doInCommandLine("mkdir /home/pavel/testdir")
}

