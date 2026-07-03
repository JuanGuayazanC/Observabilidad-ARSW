package edu.eci.arsw.observability.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Manejador global de excepciones que registra en logs cualquier error no
 * controlado y devuelve una respuesta HTTP 500 estandarizada al cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura cualquier excepcion no manejada por los controladores, la
     * registra en el log de errores y construye la respuesta enviada al cliente.
     *
     * @param ex excepcion capturada
     * @return mapa con timestamp, status, tipo de error y mensaje de la excepcion
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception ex) {
        logger.error("Excepción controlada por el manejador global", ex);

        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 500,
                "error", "Internal Server Error",
                "message", ex.getMessage()
        );
    }
}
