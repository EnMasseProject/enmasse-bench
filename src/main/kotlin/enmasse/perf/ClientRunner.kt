package enmasse.perf

import org.apache.qpid.proton.Proton
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.CoreHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
open class ClientRunner(val hostname: String, val port: Int, val clientHandler: CoreHandler, val duration: Int): BaseHandler(), Runnable {
    val reactor = Proton.reactor(this)
    val thr = Thread(this)
    var startTime = 0L
    var endTime = 0L
    @Volatile var running = false

    fun start() {
        running = true
        thr.start()
    }

    override fun onReactorInit(e: Event) {
        startTime = System.currentTimeMillis()
        e.reactor.connectionToHost(hostname, port, clientHandler)
        e.reactor.schedule(TimeUnit.SECONDS.toMillis(duration.toLong()).toInt(), this)
    }

    override fun onTimerTask(e: Event) {
        running = false
    }

    override fun onReactorFinal(e: Event) {
        endTime = System.currentTimeMillis()
    }

    override fun run() {
        reactor.timeout = 3141
        reactor.start()
        while (reactor.process() && running) { }
        reactor.stop()
    }

    fun stop() {
        thr.join()
    }
}

class LoggingHandler(val id:String): BaseHandler() {
    override fun onUnhandled(event: Event) {
        println("${id}: ${event}")
    }
}