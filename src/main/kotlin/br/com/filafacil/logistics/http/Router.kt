package br.com.filafacil.logistics.http

import br.com.filafacil.logistics.http.handlers.CustomerHandler
import br.com.filafacil.logistics.http.handlers.DeliveryHandler
import br.com.filafacil.logistics.http.handlers.DockAppointmentHandler
import br.com.filafacil.logistics.http.handlers.PickupHandler
import br.com.filafacil.logistics.http.handlers.PickupWindowHandler
import br.com.filafacil.logistics.http.handlers.RefundHandler
import com.sun.net.httpserver.HttpExchange

class Router(
    private val customerHandler: CustomerHandler,
    private val pickupHandler: PickupHandler,
    private val deliveryHandler: DeliveryHandler,
    private val refundHandler: RefundHandler,
    private val pickupWindowHandler: PickupWindowHandler,
    private val dockAppointmentHandler: DockAppointmentHandler,
) {

    fun handle(exchange: HttpExchange) {
        val method = exchange.requestMethod
        val path = exchange.requestURI.path.trimEnd('/')

        try {
            when {
                method == "POST" && path == "/customers" -> customerHandler.create(exchange)
                method == "GET" && path == "/customers" -> customerHandler.list(exchange)
                method == "GET" && path.matches(Regex("/customers/\\d+")) -> customerHandler.get(exchange, HttpUtils.pathId(path, 2))
                method == "PATCH" && path.matches(Regex("/customers/\\d+/deactivate")) ->
                    customerHandler.deactivate(exchange, HttpUtils.pathId(path, 2))

                method == "POST" && path == "/pickups" -> pickupHandler.create(exchange)
                method == "POST" && path == "/pickups/batch" -> pickupHandler.importBatch(exchange)
                method == "GET" && path == "/pickups" -> pickupHandler.list(exchange)
                method == "GET" && path == "/pickups/summary" -> pickupHandler.summary(exchange)
                method == "GET" && path.matches(Regex("/pickups/\\d+")) -> pickupHandler.get(exchange, HttpUtils.pathId(path, 2))
                method == "PATCH" && path.matches(Regex("/pickups/\\d+/status")) ->
                    pickupHandler.updateStatus(exchange, HttpUtils.pathId(path, 2))
                method == "POST" && path.matches(Regex("/pickups/\\d+/reschedule")) ->
                    pickupHandler.reschedule(exchange, HttpUtils.pathId(path, 2))
                method == "DELETE" && path.matches(Regex("/pickups/\\d+")) -> pickupHandler.delete(exchange, HttpUtils.pathId(path, 2))
                method == "PUT" && path.matches(Regex("/pickups/\\d+/proof")) ->
                    pickupHandler.uploadProof(exchange, HttpUtils.pathId(path, 2))
                method == "GET" && path.matches(Regex("/pickups/\\d+/proof")) ->
                    pickupHandler.downloadProof(exchange, HttpUtils.pathId(path, 2))

                method == "GET" && path == "/deliveries" -> deliveryHandler.list(exchange)
                method == "GET" && path.matches(Regex("/deliveries/\\d+")) -> deliveryHandler.get(exchange, HttpUtils.pathId(path, 2))
                method == "POST" && path.matches(Regex("/deliveries/\\d+/occurrences")) ->
                    deliveryHandler.registerOccurrence(exchange, HttpUtils.pathId(path, 2))

                method == "POST" && path == "/refunds" -> refundHandler.request(exchange)
                method == "GET" && path == "/refunds" -> refundHandler.list(exchange)
                method == "GET" && path == "/refunds/report" -> refundHandler.report(exchange)
                method == "GET" && path.matches(Regex("/refunds/\\d+")) -> refundHandler.get(exchange, HttpUtils.pathId(path, 2))
                method == "POST" && path.matches(Regex("/refunds/\\d+/approve")) -> refundHandler.approve(exchange, HttpUtils.pathId(path, 2))
                method == "POST" && path.matches(Regex("/refunds/\\d+/reject")) -> refundHandler.reject(exchange, HttpUtils.pathId(path, 2))

                method == "GET" && path == "/pickup-windows" -> pickupWindowHandler.list(exchange)
                method == "GET" && path.matches(Regex("/pickup-windows/\\d+")) -> pickupWindowHandler.get(exchange, HttpUtils.pathId(path, 2))

                method == "POST" && path == "/dock-appointments" -> dockAppointmentHandler.create(exchange)
                method == "GET" && path == "/dock-appointments" -> dockAppointmentHandler.list(exchange)
                method == "PUT" && path.matches(Regex("/dock-appointments/\\d+/attachment")) ->
                    dockAppointmentHandler.uploadAttachment(exchange, HttpUtils.pathId(path, 2))
                method == "GET" && path.matches(Regex("/dock-appointments/\\d+/attachment")) ->
                    dockAppointmentHandler.downloadAttachment(exchange, HttpUtils.pathId(path, 2))

                else -> HttpUtils.send(exchange, 404, "{\"error\":\"route not found: $method $path\"}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            HttpUtils.send(exchange, 500, "{\"error\":\"${e.toString().replace("\"", "'")}\"}")
        }
    }
}
