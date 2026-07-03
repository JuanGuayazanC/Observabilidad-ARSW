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
 * Controlador REST de pedidos usado como fuente de metricas de negocio y
 * tecnicas (contadores, logs y latencia simulada) para el laboratorio de
 * observabilidad.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final Counter orderCreatedCounter;
    private final Counter orderFailedCounter;
    private final Random random = new Random();

    /**
     * Crea el controlador registrando los contadores de pedidos creados y
     * fallidos en el registry de Micrometer.
     *
     * @param meterRegistry registry de Micrometer donde se registran los contadores
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
     * Crea un pedido a partir de los datos recibidos e incrementa el contador
     * de pedidos creados.
     *
     * @param request datos del pedido a crear
     * @return mapa con el id generado, el cliente, el total y el estado del pedido
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
     * Consulta un pedido por su identificador.
     *
     * @param id identificador del pedido consultado
     * @return mapa con el id consultado y su estado
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
     * Simula una latencia artificial entre 500 y 3000 ms para observar el
     * impacto en las metricas y logs de latencia.
     *
     * @return mapa con el mensaje de respuesta y el retraso aplicado en milisegundos
     * @throws InterruptedException si el hilo es interrumpido durante la espera simulada
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
     * Simula un error interno en el servicio de pedidos e incrementa el
     * contador de pedidos fallidos.
     *
     * @return nunca retorna: siempre lanza una excepcion
     * @throws IllegalStateException siempre, para simular un error de negocio
     */
    @GetMapping("/simulate-error")
    public Map<String, Object> simulateError() {
        logger.error("Error simulado en el servicio de pedidos");

        orderFailedCounter.increment();

        throw new IllegalStateException("Error simulado para análisis de observabilidad");
    }

    /**
     * Datos de entrada para la creacion de un pedido.
     *
     * @param customerId identificador del cliente, no puede estar en blanco
     * @param total valor total del pedido, debe ser mayor o igual a 1
     */
    public record CreateOrderRequest(
            @NotBlank String customerId,
            @Min(1) double total
    ) {
    }
}
