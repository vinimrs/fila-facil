package br.com.filafacil.logistics

import br.com.filafacil.logistics.config.AppConfig
import br.com.filafacil.logistics.db.Database
import br.com.filafacil.logistics.http.Router
import br.com.filafacil.logistics.http.handlers.CustomerHandler
import br.com.filafacil.logistics.http.handlers.DeliveryHandler
import br.com.filafacil.logistics.http.handlers.PickupHandler
import br.com.filafacil.logistics.http.handlers.PickupWindowHandler
import br.com.filafacil.logistics.http.handlers.RefundHandler
import br.com.filafacil.logistics.job.PickupExpirationJob
import br.com.filafacil.logistics.service.PickupImportService
import br.com.filafacil.logistics.service.PickupService
import br.com.filafacil.logistics.service.PickupWindowService
import br.com.filafacil.logistics.service.RefundService
import com.sun.net.httpserver.HttpServer
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.util.concurrent.Executors

private val log = LoggerFactory.getLogger("br.com.filafacil.logistics.Main")

fun main() {
    println("=== FilaFacil Logistics ===")
    log.info("Starting with config: {}", AppConfig.all())

    Database.init(
        AppConfig.get("db.url"),
        AppConfig.get("db.user"),
        AppConfig.get("db.password"),
        AppConfig.getInt("db.pool.size"),
    )
    Database.migrate()

    val router = Router(
        CustomerHandler(),
        PickupHandler(PickupService(), PickupImportService()),
        DeliveryHandler(),
        RefundHandler(RefundService()),
        PickupWindowHandler(PickupWindowService()),
    )

    val port = AppConfig.getInt("server.port")
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.createContext("/") { exchange -> router.handle(exchange) }
    server.executor = Executors.newFixedThreadPool(20)
    server.start()

    PickupExpirationJob().start()

    println("Server started on port $port")
}
