package util

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

interface Loggable {
    val log: Logger
        get() = LoggerFactory.getLogger(javaClass)
}

/*
object GlobalLogging {
    val log: Logger
        inline get() = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
}

interface DelegateLoggable : Loggable {
    val loggingClass: Class<*>

    override val log: Logger
        get() = LoggerFactory.getLogger(loggingClass)
}

fun DelegateLoggable(loggingClass: Class<*>) = object : DelegateLoggable {
    override val loggingClass = loggingClass
}*/