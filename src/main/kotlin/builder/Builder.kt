package builder

import io.vertx.core.AbstractVerticle

class FirstVerticle: AbstractVerticle(){

    override fun start() {
        println("Verticle start message")
    }

    override fun stop() {
        println("Verticle stop message")
    }
}


