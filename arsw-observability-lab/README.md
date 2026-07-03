# Laboratorio de Observabilidad de Microservicios

Laboratorio de la asignatura Arquitecturas de Software: instrumentacion de una
app Spring Boot con Actuator y Micrometer, recoleccion de metricas con
Prometheus, centralizacion de logs con Loki y visualizacion con Grafana.

## Ejercicio 1: App Spring Boot instrumentada

### Decision tecnica: Java 17 -> 21

La guia (secciones 5 y 7) pide Java 17 como version minima, y el codigo del
`OrderController` (seccion 9) usa:

```java
Thread.sleep(Duration.ofMillis(delay));
```

El overload `Thread.sleep(Duration)` fue agregado en el JDK 19, no existe en
Java 17. Al compilar con `<java.version>17</java.version>`, Maven usa
`--release 17`, que restringe el compilador a la API de esa version aunque el
JDK instalado sea mas nuevo, por lo que el codigo tal como aparece en la guia
no compila en Java 17.

Se decidio actualizar `java.version` a **21** (LTS) en `pom.xml` para poder
usar el codigo del PDF sin modificarlo, en vez de reescribir esa linea como
`Thread.sleep(delay)`.

### Decision tecnica: orders_created_total se expone como orders_total

El proyecto resuelve `micrometer-registry-prometheus 1.15.1`, que usa
internamente el cliente oficial nuevo de Prometheus
(`io.prometheus:prometheus-metrics-core`), en vez del cliente legacy
(`simpleclient`) con el que probablemente se escribio la guia.

Ese cliente nuevo sigue estrictamente la especificacion **OpenMetrics**, donde
`_created` es un sufijo reservado (usado para una serie companera que indica
el timestamp de creacion de un counter). Como el contador se llama
`orders_created_total`, la libreria reconoce `_created` como reservado y
reescribe el nombre quitandolo: la metrica se expone como `orders_total`, no
como `orders_created_total`.

Verificacion (`curl http://localhost:8081/actuator/prometheus | grep -i orders`):

```
# HELP orders_total Total de pedidos creados correctamente
# TYPE orders_total counter
orders_total{application="observability-demo"} 0.0
# HELP orders_failed_total Total de pedidos fallidos
# TYPE orders_failed_total counter
orders_failed_total{application="observability-demo"} 0.0
```

El `HELP` conserva la descripcion original (`.description(...)` en el codigo),
pero el nombre de la serie cambia. `orders_failed_total` no se ve afectado
porque `failed` no es un sufijo reservado por OpenMetrics.

Se decidio dejar el codigo Java identico al de la guia (no renombrar el
contador) y usar el nombre real expuesto, **`orders_total`**, en todas las
consultas PromQL de este laboratorio (Prometheus, paneles de Grafana y
alertas), en lugar de `orders_created_total` como aparece literalmente en el
PDF.
