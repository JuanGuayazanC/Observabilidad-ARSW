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
