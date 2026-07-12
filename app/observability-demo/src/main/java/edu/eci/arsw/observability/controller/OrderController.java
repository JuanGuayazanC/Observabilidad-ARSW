package edu.eci.arsw.observability.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * REST controller for orders, used as the source of business and technical
 * metrics (counters, logs, and simulated latency) for the observability lab.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Random random = new Random();

    /**
     * Creates the controller, registering the created and failed order
     * counters in the Micrometer registry.
     *
     * @param meterRegistry Micrometer registry where the counters are registered
     */
    public OrderController(MeterRegistry meterRegistry) {
        this.orderCreatedCounter = Counter.builder("orders_created_total")
                .description("Total de pedidos creados correctamente")
                .register(meterRegistry);

        this.orderFailedCounter = Counter.builder("orders_failed_total")
                .description("Total de pedidos fallidos")
                .register(meterRegistry);
    }

    /**
     * Creates an order from the received data and increments the
     * created-orders counter.
     *
     * @param request data for the order to create
     * @return map with the generated id, customer, total, and order status
     */
    @PostMapping
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        logger.info("Solicitud recibida para crear pedido. customerId={}, total={}",
                request.customerId(), request.total());

        String orderId = "ORD-" + UUID.randomUUID();

        orderCreatedCounter.increment();

        logger.info("Pedido creado correctamente. orderId={}", orderId);

        return Map.of(
                "orderId", orderId,
                "customerId", request.customerId(),
                "total", request.total(),
                "status", "CREATED"
        );
    }

    /**
     * Looks up an order by its identifier.
     *
     * @param id identifier of the order to look up
     * @return map with the queried id and its status
     */
    @GetMapping("/{id}")
    public Map<String, Object> getOrder(@PathVariable String id) {
        logger.debug("Consultando pedido con id={}", id);

        return Map.of(
                "orderId", id,
                "status", "CREATED"
        );
    }

    /**
     * Simulates artificial latency between 500 and 3000 ms to observe its
     * impact on latency metrics and logs.
     *
     * @return map with the response message and the applied delay in milliseconds
     * @throws InterruptedException if the thread is interrupted during the simulated wait
     */
    @GetMapping("/simulate-latency")
    public Map<String, Object> simulateLatency() throws InterruptedException {
        int delay = 500 + random.nextInt(2500);

        logger.warn("Simulando latencia artificial de {} ms", delay);

        Thread.sleep(Duration.ofMillis(delay));

        return Map.of(
                "message", "Respuesta con latencia simulada",
                "delayMs", delay
        );
    }

    /**
     * Simulates an internal error in the order service and increments the
     * failed-orders counter.
     *
     * @return never returns: always throws an exception
     * @throws IllegalStateException always, to simulate a business error
     */
    @GetMapping("/simulate-error")
    public Map<String, Object> simulateError() {
        logger.error("Error simulado en el servicio de pedidos");

        orderFailedCounter.increment();

        throw new IllegalStateException("Error simulado para análisis de observabilidad");
    }

    /**
     * Input data for creating an order.
     *
     * @param customerId customer identifier, must not be blank
     * @param total total order value, must be greater than or equal to 1
     */
    public record CreateOrderRequest(
            @NotBlank String customerId,
            @Min(1) double total
    ) {
    }
}
