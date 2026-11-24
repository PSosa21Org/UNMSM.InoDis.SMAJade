# VAR Peruano - JADE + Maven + Docker

Proyecto académico que simula el flujo de arbitraje usando cuatro agentes JADE:

1. **Árbitro:** recibe los equipos, genera 20 faltas aleatorias y registra las decisiones finales.
2. **Operador:** revisa las decisiones y coordina las consultas.
3. **Federación (FPF):** valida los equipos e informa al operador cuáles son amigos.
4. **VAR:** determina el equipo contra el que se cobra y devuelve la decisión al operador.

La lista de equipos amigos es `Universitario`, `Juan Pablo II` y `Boys`. Se pueden ingresar cualesquiera dos equipos distintos, aunque no estén en esa lista. Si solo uno es amigo, las decisiones finales se cobran contra el otro equipo. Si ambos o ninguno son amigos, el VAR elige aleatoriamente contra cuál cobrar. El equipo que comete cada falta también se elige aleatoriamente; hay una pausa de un segundo entre faltas. Esta regla deliberadamente sesgada es la solicitada para el ejercicio; no representa el funcionamiento de un VAR neutral.

## Arquitectura distribuida

Docker Compose inicia dos contenedores en la misma red:

- `referee` crea la plataforma JADE principal y ejecuta al árbitro.
- `workers` se conecta como contenedor remoto a esa plataforma y ejecuta al operador, la FPF y el VAR.

Es una sola plataforma JADE con dos contenedores JADE distribuidos en dos contenedores Docker. Los agentes se anuncian y buscan en las páginas amarillas (DF) por tipo de servicio; no dependen de nombres de agente codificados para encontrarse.

## Requisitos

- JDK 11 o superior.
- Apache Maven.
- Docker con Docker Compose.
- Acceso a Internet durante la primera compilación para obtener JADE y las dependencias de Maven desde Maven Central.

## Ejecutar con Docker

Desde la carpeta `AgentProject`, construir y levantar ambos contenedores:

```powershell
docker compose up --build
```

Para ver la salida de los agentes en otra terminal:

```powershell
docker compose logs -f
```

Para detener los contenedores:

```powershell
docker compose down
```

Los equipos del ejemplo se configuran en `docker-compose.yml`, en el comando del servicio `referee`. Se deben indicar dos nombres no vacíos y distintos.

## Ver la GUI de JADE

Los contenedores Docker ejecutan JADE sin interfaz gráfica. Para tomar capturas de la GUI, detén Docker Compose y ejecuta la demostración local en Windows desde `AgentProject`:

```powershell
docker compose down
mvn clean test
mvn exec:java "-Dexec.args=gui Universitario Alianza"
```

Este modo abre la GUI de JADE y ejecuta los cuatro agentes en una plataforma local para la demostración visual. Se puede capturar la ventana de JADE y la consola donde aparecen las decisiones. La GUI local es una modalidad de demostración; el despliegue distribuido de dos contenedores se ejecuta por separado con Docker Compose.

## Ejecutar los agentes localmente sin GUI

Desde la carpeta `AgentProject`, inicia primero la plataforma y el árbitro:

```powershell
mvn clean test
mvn exec:java "-Dexec.args=referee Universitario Alianza"
```

En una segunda terminal, inicia los demás agentes como contenedor JADE remoto:

```powershell
$env:JADE_MAIN_HOST = "localhost"
mvn exec:java "-Dexec.args=workers"
```

## Estructura

```text
AgentProject/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/java/pe/edu/fisi/AgentProject/
    │   ├── AgentServices.java
    │   ├── App.java
    │   ├── FederationAgent.java
    │   ├── MatchProtocol.java
    │   ├── MatchRules.java
    │   ├── OperatorAgent.java
    │   ├── RefereeAgent.java
    │   └── VarAgent.java
    └── test/java/pe/edu/fisi/AgentProject/
        ├── MatchRulesTest.java
        └── TeamRegistryTest.java
```
