
<div align="center">

# T21 — Review Notes

**Implement Media Worker job claiming loop**

</div>

<table>
  <tr>
    <td><strong>Tarea</strong></td>
    <td>T21</td>
  </tr>
  <tr>
    <td><strong>Issue</strong></td>
    <td>#22</td>
  </tr>
  <tr>
    <td><strong>Área</strong></td>
    <td>Media Worker · PostgreSQL job queue</td>
  </tr>
  <tr>
    <td><strong>Dependencia</strong></td>
    <td>T19</td>
  </tr>
  <tr>
    <td><strong>Tipo de revisión</strong></td>
    <td><strong>Corrective review</strong></td>
  </tr>
  <tr>
    <td><strong>Estado de la implementación inicial</strong></td>
    <td>Reclamación concurrente implementada · polling pendiente · ciclo de procesamiento parcialmente operativo</td>
  </tr>
  <tr>
    <td><strong>Estado de la revisión</strong></td>
    <td>Correcciones incorporadas · validación final realizada</td>
  </tr>
</table>

---

## 1. Contexto de la revisión

T21 tenía como objetivo permitir que el proceso `media-worker` consumiese periódicamente trabajos multimedia almacenados en PostgreSQL.

La implementación original resolvía correctamente el núcleo de la reclamación de trabajos:

```text
PostgreSQL
    ↓
Buscar PENDING
    ↓
Reclamación exclusiva
    ↓
PROCESSING
    ↓
Incrementar attempts
    ↓
Invocar handler
    ↓
COMPLETED / FAILED
```

El uso de transacciones y `FOR UPDATE SKIP LOCKED` estaba bien planteado. También se añadieron pruebas que demostraban que dos workers concurrentes podían reclamar trabajos sin seleccionar simultáneamente el mismo registro.

Sin embargo, durante la revisión se identificaron dos problemas directamente relacionados con el alcance original:

1. **El polling periódico no estaba implementado.** Existían propiedades de configuración, pero ningún componente programado que ejecutase automáticamente el consumidor.
2. **El handler provisional podía generar falsos éxitos.** Su ejecución no realizaba procesamiento, pero el worker podía interpretar su retorno normal como un resultado satisfactorio y marcar el trabajo como `COMPLETED`.

Además, se detectaron oportunidades para reforzar la comprobación de las transiciones, la cobertura de pruebas y la limpieza del módulo.

Por decisión posterior se amplió también la revisión con recuperación de trabajos abandonados y una política de reintentos automáticos.

### 1.1 Clasificación de la revisión

After distingue dos tipos de revisión:

<table>
  <thead>
    <tr>
      <th>Tipo</th>
      <th>Cuándo corresponde</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>Corrective review</strong></td>
      <td>La implementación no cumple completamente el alcance o los criterios de aceptación originales.</td>
    </tr>
    <tr>
      <td><strong>Consistency review</strong></td>
      <td>La implementación cumple su contrato original, pero se identifican mejoras posteriores de consistencia, robustez, mantenibilidad o cobertura.</td>
    </tr>
  </tbody>
</table>

**T21 corresponde a una Corrective review.**

El motivo determinante es la ausencia de polling periódico, exigido expresamente por la issue.

No obstante, esta review también contiene mejoras de consistencia y funcionalidades adicionales. Es importante distinguirlas de los requisitos incumplidos.

> [!IMPORTANT]
> Esta revisión no considera defectuosa toda la implementación original.
>
> La reclamación transaccional, la estrategia de exclusión mutua y la separación mediante un handler estaban correctamente orientadas.
>
> Tampoco se atribuyen como incumplimientos de T21 las capacidades de recuperación y reintentos, porque no formaban parte de su contrato original.
>
> La revisión documenta decisiones técnicas y comportamiento del sistema, no el trabajo individual de un desarrollador.

---

## 2. Alcance original de T21

La issue T21 dependía de T19, responsable de la persistencia de trabajos de procesamiento multimedia.

### 2.1 Objetivo

Hacer que el proceso `media-worker` consuma la cola PostgreSQL.

### 2.2 Flujo requerido

El worker debía:

1. Buscar un trabajo en estado `PENDING`.
2. Reclamarlo de forma exclusiva.
3. Marcarlo como `PROCESSING`.
4. Incrementar `attempts`.
5. Ejecutar su procesamiento.
6. Completarlo o marcarlo como fallido.

### 2.3 Reglas

La issue establecía:

- polling periódico;
- sin Redis;
- sin RabbitMQ;
- sin Kafka;
- posibilidad de ejecutar varios workers concurrentes sin procesar el mismo trabajo.

### 2.4 Criterio de aceptación

> Dos workers concurrentes no reclaman el mismo trabajo.

El criterio de aceptación explícito se encontraba principalmente relacionado con la exclusión mutua.

Sin embargo, **el polling periódico también era un requisito de la tarea**, aunque no figurase repetido en el apartado de criterios de aceptación.

### 2.5 Responsabilidades no requeridas originalmente

T21 no exigía definir:

- procesamiento específico de `IMAGE`, `AUDIO` y `VIDEO`;
- transcodificación;
- thumbnails;
- recuperación automática después de una caída;
- duración de leases;
- renovación mediante heartbeat;
- límites de intentos;
- backoff entre reintentos;
- clasificación de errores permanentes y recuperables.

Los últimos cinco aspectos se incorporan por decisión expresa durante esta revisión.

El procesamiento específico de los tres formatos multimedia permanece fuera de alcance y se reserva a T22 y posteriores.

---

## 3. Estado de la implementación inicial

<table>
  <thead>
    <tr>
      <th>Aspecto</th>
      <th>Estado inicial</th>
      <th>Clasificación</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Cola PostgreSQL</td>
      <td>✅ Cumplido</td>
      <td>Implementación correcta</td>
    </tr>
    <tr>
      <td>Reclamación exclusiva</td>
      <td>✅ Cumplido</td>
      <td>Implementación correcta</td>
    </tr>
    <tr>
      <td>Transición a PROCESSING</td>
      <td>✅ Cumplido</td>
      <td>Implementación correcta</td>
    </tr>
    <tr>
      <td>Incremento de attempts</td>
      <td>✅ Cumplido</td>
      <td>Implementación correcta</td>
    </tr>
    <tr>
      <td>Invocación del handler</td>
      <td>🟡 Parcial</td>
      <td>Faltaba una ejecución funcional segura</td>
    </tr>
    <tr>
      <td>COMPLETED / FAILED</td>
      <td>🟡 Parcial</td>
      <td>Existían transiciones, pero podía producirse un falso COMPLETED</td>
    </tr>
    <tr>
      <td>Polling periódico</td>
      <td>🔴 Pendiente</td>
      <td>Requisito original incumplido</td>
    </tr>
    <tr>
      <td>Configuración enabled/disabled</td>
      <td>🟡 Parcial</td>
      <td>Las propiedades existían, pero no gobernaban una ejecución programada</td>
    </tr>
    <tr>
      <td>Tests de concurrencia</td>
      <td>✅ Cumplido</td>
      <td>Cobertura existente, susceptible de refuerzo</td>
    </tr>
    <tr>
      <td>Comprobación de transiciones persistidas</td>
      <td>🔧 Consistencia</td>
      <td>Refuerzo adicional de robustez</td>
    </tr>
    <tr>
      <td>Enum sin utilización</td>
      <td>🔧 Consistencia</td>
      <td>Limpieza de código innecesario</td>
    </tr>
    <tr>
      <td>Integración con migración real T19</td>
      <td>🔧 Cobertura adicional</td>
      <td>Los tests originales utilizaban un esquema simplificado</td>
    </tr>
    <tr>
      <td>Recuperación tras caída</td>
      <td>🔧 Ampliación</td>
      <td>No era requisito de T21</td>
    </tr>
    <tr>
      <td>Política de reintentos</td>
      <td>🔧 Ampliación</td>
      <td>No era requisito de T21</td>
    </tr>
    <tr>
      <td>Procesamiento IMAGE/AUDIO/VIDEO</td>
      <td>🔵 Fuera de alcance</td>
      <td>Responsabilidad de T22 y posteriores</td>
    </tr>
  </tbody>
</table>

Esta clasificación es importante porque una revisión correctiva no convierte automáticamente todas las mejoras encontradas en defectos de la tarea original.

---

## 4. Aspectos correctamente implementados

### 4.1 PostgreSQL como mecanismo de cola

La implementación reutiliza PostgreSQL para almacenar y reclamar trabajos.

No introduce servicios adicionales de mensajería.

Esto respeta la arquitectura definida en T21 y evita añadir infraestructura que la tarea no necesitaba.

---

### 4.2 Reclamación transaccional

La consulta original utiliza:

```sql
FOR UPDATE SKIP LOCKED
```

y actualiza el trabajo reclamado dentro de una operación transaccional.

Esta estrategia permite que varias instancias intenten consumir la cola sin reclamar simultáneamente una misma fila.

El mecanismo principal se conserva durante la revisión.

> [!NOTE]
> `SKIP LOCKED` protege la reclamación transaccional.
>
> No protege automáticamente toda la duración del procesamiento una vez que termina la transacción.
>
> Esa diferencia explica por qué las leases y los tokens de propiedad constituyen una ampliación de fiabilidad independiente.

---

### 4.3 Transición e incremento de intentos

La reclamación original actualizaba:

```text
PENDING → PROCESSING
attempts → attempts + 1
```

Esto ya proporcionaba una base correcta para registrar las ejecuciones.

La review amplía posteriormente el uso de `attempts` para determinar cuándo procede un nuevo intento y cuándo debe finalizar definitivamente el trabajo.

---

### 4.4 Separación mediante un handler

La interfaz:

```java
MediaProcessingTaskHandler
```

es una decisión adecuada.

Permite separar el consumo de la cola del procesamiento concreto de cada fichero.

Gracias a esta separación, T21 no necesita conocer los detalles de:

```text
IMAGE
AUDIO
VIDEO
```

El problema no era la interfaz, sino el comportamiento del handler provisional.

La review conserva la abstracción.

---

### 4.5 Pruebas de concurrencia originales

La implementación incluía pruebas con PostgreSQL real mediante Testcontainers.

Se comprobaban escenarios con uno y dos trabajos pendientes y con varios consumidores.

Esta cobertura aportaba evidencia útil para el criterio de aceptación de T21.

La revisión no parte de una ausencia total de tests, sino de la necesidad de verificar situaciones más exigentes y el comportamiento añadido posteriormente.

---

## 5. Hallazgos, correcciones y mejoras

La revisión agrupa los once puntos incluidos en siete bloques.

<table>
  <thead>
    <tr>
      <th>Bloque</th>
      <th>Objeto</th>
      <th>Naturaleza</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>A</td>
      <td>Polling y configuración</td>
      <td><strong>Corrección de requisito original</strong></td>
    </tr>
    <tr>
      <td>B</td>
      <td>Handler y falsos éxitos</td>
      <td><strong>Corrección funcional</strong></td>
    </tr>
    <tr>
      <td>C</td>
      <td>Consistencia de transiciones</td>
      <td>Mejora de robustez</td>
    </tr>
    <tr>
      <td>D</td>
      <td>Tests de concurrencia e integración</td>
      <td>Cobertura adicional</td>
    </tr>
    <tr>
      <td>E</td>
      <td>Limpieza de código</td>
      <td>Mejora de mantenibilidad</td>
    </tr>
    <tr>
      <td>F</td>
      <td>Recuperación ante caídas</td>
      <td>Funcionalidad adicional</td>
    </tr>
    <tr>
      <td>G</td>
      <td>Límite de intentos y reintentos</td>
      <td>Funcionalidad adicional</td>
    </tr>
  </tbody>
</table>

### 5.1 A — Implementar el polling periódico

**Clasificación: requisito original incumplido.**

T21 exigía polling periódico.

La implementación inicial definía propiedades como:

```properties
worker.polling.enabled
worker.polling.fixed-delay
```

pero no existía ningún planificador que utilizase estas propiedades para ejecutar periódicamente el consumidor.

Por tanto, aunque el mecanismo de reclamación funcionaba mediante invocación directa, faltaba el ciclo automático esperado de un worker.

**Corrección planteada:**

- incorporar Spring Scheduling;
- ejecutar periódicamente el consumidor;
- respetar el intervalo configurable;
- permitir deshabilitar el polling;
- separar el polling de otras tareas periódicas, como heartbeat.

El comportamiento esperado es:

```text
Polling enabled
      ↓
Ejecución periódica
      ↓
Buscar trabajo elegible
      ↓
Reclamar y ejecutar
```

Cuando `worker.polling.enabled=false`, no deben producirse reclamaciones mediante el planificador.

> [!IMPORTANT]
> La ausencia de polling es un defecto de T21 porque su implementación forma parte de las reglas originales de la issue.
>
> No debe clasificarse como una mera optimización o mejora posterior.

---

### 5.2 B — Ejecutar el handler sin producir falsos éxitos

**Clasificación: corrección funcional relacionada con el ciclo requerido.**

La interfaz del handler estaba correctamente diseñada.

Sin embargo, la implementación provisional no realizaba procesamiento real y podía retornar sin errores.

El worker interpretaba esa finalización como éxito:

```text
Handler vacío
      ↓
Retorno normal
      ↓
COMPLETED
```

Esto podía provocar que un trabajo apareciese como completado sin haberse ejecutado ninguna operación útil.

**Corrección planteada:**

- eliminar el handler provisional;
- conservar `MediaProcessingTaskHandler`;
- permitir que el worker detecte la ausencia de un handler operativo;
- impedir la reclamación cuando todavía no existe procesamiento funcional;
- mantener los trabajos en `PENDING` hasta disponer de un procesador real.

El comportamiento esperado antes de T22 es:

```text
No existe handler real
         ↓
No reclamar el trabajo
         ↓
PENDING permanece intacto
```

Los tests pueden aportar handlers funcionales de prueba para verificar las transiciones a `COMPLETED` y `FAILED`.

Eso no implica que T21 deba implementar la transformación real de imágenes, audio o vídeo.

---

### 5.3 C — Comprobar las transiciones de estado

**Clasificación: mejora de consistencia y robustez.**

La implementación original disponía de operaciones para marcar trabajos como completados o fallidos.

Sin embargo, el worker no comprobaba suficientemente si esas actualizaciones habían modificado realmente el registro esperado.

Esto podía ocultar una transición no persistida o una ejecución cuya reclamación ya no fuese válida.

**Mejora planteada:**

- comprobar el resultado de las actualizaciones SQL;
- asociar cada reclamación a un identificador único;
- verificar que la reclamación continúa activa antes de finalizar;
- impedir transiciones realizadas por propietarios obsoletos.

Se incorpora el concepto:

```text
job_id + claim_token
```

La transición final se permite únicamente cuando el token coincide con la reclamación activa.

**Resultado esperado:**

```text
Token vigente    → puede finalizar
Token incorrecto → transición rechazada
Token caducado   → transición rechazada
```

Este refuerzo es especialmente relevante para las capacidades adicionales de recuperación introducidas en el bloque F.

No debe confundirse con la ausencia del mecanismo de reclamación exclusiva: ese mecanismo ya existía.

---

### 5.4 D — Reforzar los tests de concurrencia e integración

**Clasificación: cobertura adicional de verificación y regresión.**

T21 incluía un criterio explícito relacionado con concurrencia.

La implementación original ya contaba con pruebas que aportaban evidencia sobre él.

Por tanto, no corresponde afirmar que la tarea carecía de tests de concurrencia.

La revisión amplía la verificación porque el sistema incorpora nuevos comportamientos y porque algunas garantías requieren comprobar solapamiento real entre ejecuciones.

Se contemplan los siguientes escenarios:

<table>
  <thead>
    <tr>
      <th>Escenario</th>
      <th>Garantía buscada</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Dos workers y un job</td>
      <td>Una sola reclamación efectiva</td>
    </tr>
    <tr>
      <td>Dos workers y dos jobs</td>
      <td>Procesamiento paralelo sin solapamiento de propiedad</td>
    </tr>
    <tr>
      <td>Handler todavía activo</td>
      <td>Un segundo worker no reclama el mismo trabajo</td>
    </tr>
    <tr>
      <td>Cola vacía</td>
      <td>No se generan reclamaciones ficticias</td>
    </tr>
    <tr>
      <td>Sin handler</td>
      <td>El trabajo permanece PENDING</td>
    </tr>
    <tr>
      <td>Procesamiento correcto</td>
      <td>COMPLETED se persiste correctamente</td>
    </tr>
    <tr>
      <td>Procesamiento fallido</td>
      <td>FAILED o nuevo intento según la política</td>
    </tr>
    <tr>
      <td>Lease caducada</td>
      <td>El trabajo puede recuperarse</td>
    </tr>
    <tr>
      <td>Propietario antiguo</td>
      <td>No puede finalizar una reclamación posterior</td>
    </tr>
    <tr>
      <td>Polling habilitado</td>
      <td>El trabajo se consume automáticamente</td>
    </tr>
    <tr>
      <td>Polling deshabilitado</td>
      <td>No existe consumo programado</td>
    </tr>
    <tr>
      <td>Reintentos agotados</td>
      <td>El trabajo termina definitivamente</td>
    </tr>
  </tbody>
</table>

#### Compatibilidad con la migración real de T19

Los tests originales del worker creaban manualmente una versión simplificada de `media_processing_jobs`.

Esto era suficiente para comprobar ciertas operaciones de cola, pero no verificaba todas las restricciones y relaciones de la migración real.

La revisión contempla ejecutar las pruebas contra el esquema construido mediante Flyway.

```text
Flyway V1–V8
     ↓
PostgreSQL real
     ↓
MediaProcessingJobQueueRepository
     ↓
MediaProcessingWorker
```

Así se comprueba que las consultas del worker son compatibles con el esquema real de la aplicación.

> [!NOTE]
> Los tests adicionales no se documentan como una ausencia absoluta de cobertura en la implementación inicial.
>
> Se incorporan para reforzar garantías ya existentes y verificar los nuevos comportamientos introducidos por la review.

---

### 5.5 E — Eliminar código innecesario

**Clasificación: mejora de mantenibilidad.**

Durante la revisión se identificaron dos clases que ya no eran necesarias dentro del worker:

```text
MediaProcessingJobStatus.java
NoOpMediaProcessingTaskHandler.java
```

El enum de estados no tenía consumidores dentro del módulo.

El handler provisional, además de ser innecesario cuando no existe un procesamiento real, permitía los falsos éxitos descritos anteriormente.

**Limpieza planteada:**

- eliminar el enum no utilizado del worker;
- eliminar el handler vacío;
- mantener los tipos del backend;
- conservar la interfaz necesaria para implementar T22.

Esta limpieza es específica y no justifica una refactorización general.

---

### 5.6 F — Recuperar trabajos abandonados

**Clasificación: funcionalidad adicional acordada durante la review.**

T21 no exigía recuperar trabajos que permaneciesen en `PROCESSING` después de una caída del proceso.

La implementación original podía dejar trabajos en ese estado indefinidamente si el worker terminaba antes de ejecutar la transición final.

Esto representa una limitación operativa real, pero no un incumplimiento de la issue original.

Se decide incorporar un sistema de propiedad temporal mediante leases.

#### Nuevos campos

La migración V8 añade:

```text
claim_token
lease_expires_at
next_attempt_at
```

Cada campo tiene una responsabilidad:

<table>
  <tr>
    <td><code>claim_token</code></td>
    <td>Identifica la reclamación activa.</td>
  </tr>
  <tr>
    <td><code>lease_expires_at</code></td>
    <td>Indica hasta cuándo mantiene su validez.</td>
  </tr>
  <tr>
    <td><code>next_attempt_at</code></td>
    <td>Determina cuándo puede volver a reclamarse el trabajo.</td>
  </tr>
</table>

#### Ciclo de recuperación

```text
PENDING
   ↓
Claim exclusivo
   ↓
PROCESSING + claim_token
   ↓
Lease activa
   ↓
Heartbeat periódico
   ↓
¿Lease caducada?
   ├── No → Mantener propietario
   └── Sí
        ↓
     Recuperación
        ↓
     PENDING / FAILED
```

La recuperación utiliza nuevamente bloqueos transaccionales para evitar que varias instancias seleccionen simultáneamente el mismo trabajo abandonado.

El token impide que el worker antiguo confirme una ejecución después de que otro haya reclamado el job.

> [!IMPORTANT]
> Esta recuperación se incorpora como ampliación deliberada de T21.
>
> No debe presentarse como una responsabilidad que la implementación original estaba obligada a cumplir.

---

### 5.7 G — Incorporar reintentos acotados

**Clasificación: funcionalidad adicional acordada durante la review.**

El contador `attempts` existía desde T19 y se incrementaba correctamente durante la reclamación.

Sin embargo, T21 no definía una política de reintentos automáticos.

Incrementar `attempts` no implica que los trabajos deban reprogramarse automáticamente.

La revisión introduce una política configurable.

#### Política inicial propuesta

<table>
  <thead>
    <tr>
      <th>Parámetro</th>
      <th>Valor</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Intentos máximos</td>
      <td>3, incluido el inicial</td>
    </tr>
    <tr>
      <td>Backoff inicial</td>
      <td>10 segundos</td>
    </tr>
    <tr>
      <td>Backoff máximo</td>
      <td>60 segundos</td>
    </tr>
    <tr>
      <td>Duración de lease</td>
      <td>60 segundos</td>
    </tr>
    <tr>
      <td>Heartbeat</td>
      <td>10 segundos</td>
    </tr>
  </tbody>
</table>

Estos valores se consideran una configuración inicial, no requisitos establecidos por la issue original.

#### Comportamiento esperado

```text
Error recuperable
       ↓
¿Quedan intentos?
   ├── Sí
   │    ↓
   │  Calcular backoff
   │    ↓
   │  Programar PENDING
   │
   └── No
        ↓
      FAILED
```

Los errores permanentes no deben repetirse automáticamente.

Para diferenciarlos se introduce una excepción específica:

```java
NonRetryableMediaProcessingException
```

La política debe mantener configurables los intervalos y límites para poder ajustarlos posteriormente, cuando el procesamiento multimedia real permita identificar los errores recuperables de cada formato.

---

## 6. Resultado y validación

### 6.1 Resultado funcional previsto

La revisión conserva la arquitectura original y amplía el ciclo de procesamiento.

<table>
  <thead>
    <tr>
      <th>Aspecto</th>
      <th>Resultado de la revisión</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Reclamación PostgreSQL</td>
      <td>Se conserva el mecanismo original con SKIP LOCKED.</td>
    </tr>
    <tr>
      <td>Polling</td>
      <td>Se incorpora ejecución programada y configurable.</td>
    </tr>
    <tr>
      <td>Handler</td>
      <td>Se evita consumir trabajos sin implementación operativa.</td>
    </tr>
    <tr>
      <td>COMPLETED / FAILED</td>
      <td>Se refuerza la comprobación de propiedad y transición.</td>
    </tr>
    <tr>
      <td>Concurrencia</td>
      <td>Se mantienen las garantías originales y se amplían sus pruebas.</td>
    </tr>
    <tr>
      <td>Recuperación</td>
      <td>Se incorpora recuperación por expiración de leases.</td>
    </tr>
    <tr>
      <td>Reintentos</td>
      <td>Se añade un máximo configurable y backoff.</td>
    </tr>
    <tr>
      <td>Migraciones</td>
      <td>Se introduce V8 sin modificar V7.</td>
    </tr>
    <tr>
      <td>Procesamiento multimedia</td>
      <td>Permanece pendiente para T22 y posteriores.</td>
    </tr>
  </tbody>
</table>

### 6.2 Validación del backend

Durante el preflight del 22 de octubre de 2026 se observó:

```text
Tests run: 152
Failures: 0
Errors: 1
Skipped: 0
```

El único error procedía de:

```text
BackendApplicationTests.contextLoads
```

La causa fue un problema de autenticación con PostgreSQL local:

```text
SQL State: 28P01
Password authentication failed for user "after"
```

Este test utilizaba `@SpringBootTest` sin configurar un contenedor PostgreSQL propio.

El fallo corresponde al aislamiento de la infraestructura de pruebas y no demuestra un defecto de la cola de T21.

Se preparó una corrección para ejecutar también este test mediante PostgreSQL de Testcontainers.

No se considera verificada hasta superar una nueva ejecución.

### 6.3 Migraciones verificadas

En los tests de integración del backend se confirmó que Flyway aplicó correctamente las migraciones:

```text
V1
V2
V3
V4
V5
V6
V7
V8
```

La nueva migración:

```text
V8__add_media_processing_job_leases.sql
```

pudo ejecutarse correctamente sobre PostgreSQL 17 en esos entornos.

La migración V7 de T19 permanece intacta.

Esto confirma la compatibilidad de la evolución del esquema con las pruebas del backend, sin sustituir la verificación específica del worker.

### 6.4 Preflight pendiente de cierre

Antes del commit y de abrir la Pull Request deben quedar confirmadas las siguientes comprobaciones:

```text
Backend clean verify
Media Worker clean verify
Frontend tests
Frontend build
Docker Compose validation
git diff --check
git status
```

También deben revisarse los resultados específicos de concurrencia, recuperación y reintentos.

> [!IMPORTANT]
> Este documento no declara completados tests ni comprobaciones cuyos resultados todavía no se hayan confirmado.
>
> El estado de la review deberá actualizarse a **Completada · tests verdes** después de superar el preflight correspondiente.
>
> El CI de GitHub se verificará posteriormente en la Pull Request.

### 6.5 Consideraciones para T22

La recuperación introducida durante la revisión utiliza una semántica de ejecución *at-least-once*.

Un trabajo puede ejecutarse nuevamente si la lease anterior expira.

La comprobación del `claimToken` impide que una reclamación antigua modifique el estado de otra nueva, pero no garantiza que el proceso antiguo haya dejado de producir efectos externos.

Por tanto, el procesamiento multimedia de T22 deberá considerar operaciones idempotentes, especialmente durante la escritura de objetos procesados en storage.

Esto no implica adelantar su implementación dentro de esta review.

---

## 7. Aprendizajes reutilizables

### Diferenciar correcciones de requisitos y mejoras posteriores

La categoría de una review depende del estado de cumplimiento de la tarea original.

Si existe un requisito explícito incumplido, como el polling periódico en T21, corresponde una `Corrective review`.

Eso no significa que todos los cambios posteriores deban clasificarse como defectos.

Una misma revisión puede contener:

```text
Original scope corrections
Consistency improvements
Additional coverage
Additional functionality
```

Es importante documentar cada grupo de forma independiente.

---

### Verificar el comportamiento, no solo la configuración

Declarar propiedades de polling no demuestra que exista una ejecución periódica.

La verificación debe recorrer el flujo completo:

```text
Configuration
    ↓
Scheduler
    ↓
Worker
    ↓
Queue repository
    ↓
PostgreSQL
```

La configuración y el comportamiento observable son responsabilidades diferentes.

---

### Evitar que implementaciones provisionales generen estados falsos

Un handler vacío que termina normalmente puede parecer correcto desde el punto de vista de ejecución del método.

Sin embargo, no representa necesariamente una operación completada.

Cuando una funcionalidad posterior todavía no está implementada, es preferible impedir el consumo a persistir un estado de éxito que no corresponde con ningún trabajo real.

---

### Separar reclamación y propiedad durante el procesamiento

La reclamación transaccional protege la asignación inicial.

La propiedad durante una operación larga requiere otra garantía.

```text
FOR UPDATE SKIP LOCKED
        ↓
Exclusive claim
        ↓
claim_token + lease
        ↓
Ownership validation
```

No debe mantenerse una transacción abierta durante toda la transformación multimedia simplemente para conservar un bloqueo.

---

### Diseñar la recuperación junto con la protección frente a propietarios obsoletos

Recuperar un trabajo abandonado no consiste únicamente en devolverlo a `PENDING`.

Debe considerarse qué ocurre si el proceso anterior continúa ejecutándose y trata de confirmar el resultado.

Sin un mecanismo de propiedad, la recuperación puede introducir nuevas condiciones de carrera.

Las leases y los tokens deben diseñarse conjuntamente.

---

### No confundir contador de intentos con política de reintentos

Un campo `attempts` informa de cuántas reclamaciones se han producido.

Para implementar reintentos es necesario definir adicionalmente:

- errores recuperables;
- errores permanentes;
- límite máximo;
- intervalos;
- transición final cuando se agotan los intentos.

Estos elementos constituyen una política de ejecución distinta de la persistencia del contador.

---

### Los tests de concurrencia deben forzar condiciones concurrentes

Crear dos threads no garantiza que las operaciones relevantes coincidan.

Las barreras y los mecanismos de sincronización permiten mantener un primer handler activo mientras otro intenta reclamar trabajos.

Esto demuestra propiedades que una ejecución concurrente no coordinada podría no comprobar.

---

### Probar contra el esquema real cuando el contrato depende de PostgreSQL

Los esquemas simplificados pueden resultar útiles para pruebas aisladas.

Sin embargo, cuando existen enums, restricciones, índices y claves foráneas, conviene verificar también las consultas sobre las migraciones reales.

En After, el flujo esperado es:

```text
Flyway
   ↓
PostgreSQL
   ↓
Repository
   ↓
Worker
```

La compatibilidad del SQL debe comprobarse con el esquema que realmente utilizará la aplicación.

---

### Aislar los tests de la infraestructura local

Una suite de integración no debería depender accidentalmente de las credenciales de PostgreSQL instaladas en la máquina del desarrollador.

Testcontainers permite proporcionar bases de datos temporales y reproducibles.

Este principio también se aplica a los tests sencillos de carga de contexto cuando su inicialización activa Flyway y JPA.

---

### No confundir implementación terminada con revisión validada

Una review puede tener todos sus cambios incorporados y todavía necesitar pruebas.

Conviene distinguir:

```text
Changes implemented
        ↓
Local preflight passed
        ↓
Pull Request opened
        ↓
CI passed
        ↓
Review approved
        ↓
Merged
```

Registrar estos estados de forma precisa permite que las notas sigan siendo fiables para futuras revisiones.

---

## Conclusión

T21 introdujo una base adecuada para consumir trabajos mediante PostgreSQL, con reclamación transaccional y exclusión mutua entre workers.

La revisión se clasifica como **Corrective review** porque el polling periódico, exigido expresamente por la tarea, no estaba implementado.

También se corrige el comportamiento del handler provisional para impedir que trabajos sin procesamiento real puedan quedar registrados como `COMPLETED`.

Sobre esa base se incorporan mejoras adicionales de consistencia, pruebas, mantenibilidad, recuperación ante caídas y política de reintentos.

Estas ampliaciones están documentadas como decisiones posteriores y no como incumplimientos de la implementación original.

El procesamiento específico de `IMAGE`, `AUDIO` y `VIDEO` permanece fuera del alcance de esta revisión.