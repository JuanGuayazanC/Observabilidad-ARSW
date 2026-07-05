# Laboratorio de Observabilidad de Microservicios

Laboratorio de la asignatura Arquitecturas de Software: instrumentacion de una
app Spring Boot con Actuator y Micrometer, recoleccion de metricas con
Prometheus, centralizacion de logs con Loki y visualizacion con Grafana.

## Ejercicio 1: App Spring Boot instrumentada

### Punto 6 — Estructura del proyecto

Estructura creada segun la guia dentro de `arsw-observability-lab/`:
`docker-compose.yml`, `prometheus/prometheus.yml`, `loki/loki-config.yml`,
`promtail/promtail-config.yml` y `app/observability-demo`.

### Punto 7 — Creacion de la aplicacion Spring Boot

**Decision tecnica: Java 17 -> 21**

La guia (secciones 5 y 7) pide Java 17 como version minima, pero el codigo
del `OrderController` (punto 9) usa:

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

### Punto 9 — Controlador de prueba (OrderController)

**Decision tecnica: orders_created_total se expone como orders_total**

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

### Punto 11 — Prueba inicial de la aplicacion

**Concepto: metricas automaticas vs. metricas de negocio**

Al consultar `curl http://localhost:8081/actuator/prometheus` aparecen dos
tipos de metricas, con un origen distinto:

**Automaticas (las genera Spring Boot Actuator / Micrometer solas, sin
codigo adicional):**

- `http_server_requests_seconds_count{...uri="..."}` — contador por cada
  endpoint HTTP invocado (incluye method, status, uri como labels). Se
  autoincrementa incluso al llamar `/actuator/prometheus` (queda registrada
  su propia invocacion).
- `jvm_memory_used_bytes{area="heap|nonheap",id="..."}` — gauge de memoria
  usada por la JVM, desglosado por region del GC (G1 Eden/Old Gen/Survivor)
  y fuera de heap (Metaspace, CodeCache).
- `process_cpu_usage` — gauge entre 0 y 1 del uso de CPU del proceso Java en
  el momento del scrape.

**De negocio (las escribimos a mano en `OrderController` con
`Counter.builder(...).increment()`):**

- `orders_total` (nombre en codigo: `orders_created_total`, ver Punto 9) —
  cuenta pedidos creados exitosamente.
- `orders_failed_total` — cuenta errores simulados en `/orders/simulate-error`.

Estas dos ultimas arrancan en `0.0` porque solo se incrementan cuando ocurre
el evento de negocio correspondiente (`POST /orders` o
`GET /orders/simulate-error`), a diferencia de las automaticas que ya
reflejan actividad desde que la app arranca.

**Decision tecnica: adaptar los comandos curl de la guia a PowerShell**

Los comandos `curl` de las secciones 11 y 12 estan escritos para una shell
POSIX (bash/zsh). Ejecutados tal cual en PowerShell (Windows) fallan o se
comportan distinto por dos razones:

1. **`curl` es un alias de `Invoke-WebRequest` en PowerShell**, no el curl
   real. Devuelve un objeto de PowerShell (con `StatusCode`, `Headers`,
   `Content`, etc.) en vez de solo el cuerpo de la respuesta, y ademas
   muestra una advertencia de seguridad ("riesgo de ejecucion de script")
   antes de cada llamada porque intenta parsear la respuesta como HTML.
   Se soluciona invocando el curl real con `curl.exe` en vez de `curl`.
2. **El comillado de JSON en `-d` es distinto.** En bash, la guia usa
   comillas simples para envolver todo el JSON y comillas dobles adentro:
   ```bash
   -d '{"customerId":"CUS-01","total":120000}'
   ```
   En PowerShell, las comillas simples no funcionan igual para pasar el
   argumento a un ejecutable externo como `curl.exe`; hace falta escapar
   las comillas dobles internas con `\`:
   ```powershell
   -d '{\"customerId\":\"CUS-01\",\"total\":120000}'
   ```

### Punto 12 — Generacion de trafico

Comandos de la seccion 12 adaptados a PowerShell y usados en este laboratorio:

```powershell
curl.exe -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d '{\"customerId\":\"CUS-01\",\"total\":120000}'
curl.exe http://localhost:8081/orders/ORD-1001
curl.exe http://localhost:8081/orders/simulate-latency
curl.exe http://localhost:8081/orders/simulate-error
```
