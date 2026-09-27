<div align="center">

# T16 — Review Notes

**Extend Contribution model for media**

</div>

<table>
  <tr>
    <td><strong>Tarea</strong></td>
    <td>T16</td>
  </tr>
  <tr>
    <td><strong>Área</strong></td>
    <td>Backend · Media persistence</td>
  </tr>
  <tr>
    <td><strong>Tipo de revisión</strong></td>
    <td>Consistency review</td>
  </tr>
  <tr>
    <td><strong>Estado de la tarea original</strong></td>
    <td>Alcance y criterios de aceptación cumplidos</td>
  </tr>
  <tr>
    <td><strong>Estado de la revisión</strong></td>
    <td>Completada · tests verdes</td>
  </tr>
</table>

---

## 1. Contexto de la revisión

T16 tenía como objetivo añadir únicamente la persistencia necesaria para contribuciones multimedia de tipo:

```text
IMAGE
AUDIO
VIDEO
```

La implementación integrada en `main` cumplía el alcance y los criterios de aceptación definidos en la tarea.

Esta revisión no parte de requisitos funcionales incumplidos.

Después de integrar T16 se identificaron dos puntos donde el modelo podía mantener una mayor coherencia interna:

- alinear el tipo `MediaObjectStatus` entre Java y PostgreSQL;
- proteger mejor la asociación bidireccional entre `Contribution` y `MediaObject`.

Al modificar estas áreas se añadió también cobertura específica de regresión para el modelo y la persistencia multimedia.

> [!IMPORTANT]
> Los cambios documentados aquí no corrigen criterios de aceptación incumplidos de T16.
>
> Son mejoras de consistencia detectadas durante una revisión posterior a la implementación.

---

## 2. Alcance original de T16

T16 debía introducir la persistencia de `MediaObject` y relacionarla con `Contribution`.

El modelo debía almacenar:

- estado del objeto multimedia;
- claves de almacenamiento original, procesado y thumbnail;
- nombre y MIME types;
- tamaño;
- duración;
- dimensiones;
- timestamps.

Los estados definidos eran:

```text
UPLOADING
PROCESSING
READY
FAILED
```

Los criterios de aceptación eran:

- migración correcta;
- relación correcta entre `Contribution` y `MediaObject`;
- las contribuciones `TEXT` continúan funcionando sin `MediaObject`.

La tarea no requería todavía:

- endpoints de upload;
- generación de presigned URLs;
- procesamiento multimedia;
- creación del flujo completo de contribuciones multimedia;
- factories para contribuciones `IMAGE`, `AUDIO` o `VIDEO`;
- tests específicos como criterio de aceptación.

Estas responsabilidades pertenecen a tareas posteriores o, en el caso de los tests añadidos durante esta revisión, a cobertura adicional de regresión.

---

## 3. Estado de la implementación integrada

<table>
  <thead>
    <tr>
      <th>Aspecto</th>
      <th>Estado</th>
      <th>Observación</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Entidad <code>MediaObject</code></td>
      <td>✅ Cumplido</td>
      <td>Contiene los campos definidos por T16</td>
    </tr>
    <tr>
      <td>Estados multimedia</td>
      <td>✅ Cumplido</td>
      <td>UPLOADING, PROCESSING, READY y FAILED</td>
    </tr>
    <tr>
      <td>Relación 1:1</td>
      <td>✅ Cumplido</td>
      <td>Existe tanto en JPA como mediante UNIQUE en persistencia</td>
    </tr>
    <tr>
      <td>TEXT sin MediaObject</td>
      <td>✅ Cumplido</td>
      <td>La relación es opcional desde Contribution</td>
    </tr>
    <tr>
      <td>Campos específicos por formato</td>
      <td>✅ Cumplido</td>
      <td>Los campos no aplicables pueden permanecer null</td>
    </tr>
    <tr>
      <td>Representación de <code>status</code></td>
      <td>🔧 Consistencia</td>
      <td>Java utilizaba enum y PostgreSQL definía un enum que la columna todavía no utilizaba</td>
    </tr>
    <tr>
      <td>Sincronización de la relación</td>
      <td>🔧 Consistencia</td>
      <td>La API original permitía modificar únicamente el lado inverso de la asociación</td>
    </tr>
  </tbody>
</table>

---

## 4. Mejoras de consistencia

### 4.1 Alinear `MediaObjectStatus` con PostgreSQL

La migración original creó el tipo:

```sql
CREATE TYPE media_object_status AS ENUM (
    'UPLOADING',
    'PROCESSING',
    'READY',
    'FAILED'
);
```

Sin embargo, la columna quedó definida como:

```sql
status VARCHAR(50) NOT NULL DEFAULT 'UPLOADING'
```

Por tanto, existían simultáneamente dos representaciones del mismo concepto:

```text
Java       → MediaObjectStatus enum
PostgreSQL → media_object_status enum
Columna    → VARCHAR
```

Esto no impedía el funcionamiento de T16, pero dejaba el tipo PostgreSQL sin utilizar y permitía que la columna almacenase valores fuera del conjunto definido.

La revisión añade una nueva migración que convierte la columna:

```text
VARCHAR → media_object_status
```

sin modificar la migración original.

El mapping de Hibernate se alinea también con el named enum mediante:

```java
@Enumerated(EnumType.STRING)
@JdbcTypeCode(SqlTypes.NAMED_ENUM)
@Column(name = "status", nullable = false)
private MediaObjectStatus status = MediaObjectStatus.UPLOADING;
```

De esta forma, Java, Hibernate y PostgreSQL utilizan la misma representación conceptual del estado multimedia.

> [!NOTE]
> La migración original no se modifica.
>
> Al formar parte del historial de Flyway ya integrado, la evolución del schema se realiza mediante una nueva migración.

---

### 4.2 Mantener coherente `Contribution ↔ MediaObject`

`MediaObject` es el owning side de la relación porque contiene:

```text
contribution_id
```

mientras que `Contribution.mediaObject` utiliza `mappedBy`.

La implementación original exponía:

```java
contribution.setMediaObject(mediaObject);
```

Ese método modificaba únicamente el lado inverso de la asociación.

Esto permitía representar accidentalmente en memoria situaciones incoherentes:

```text
Contribution.mediaObject → MediaObject A
MediaObject.contribution → Contribution B
```

La revisión elimina ese setter público y establece un único flujo para construir la asociación.

`MediaObject` continúa requiriendo una `Contribution` en su constructor porque, de acuerdo con el modelo de persistencia:

```text
contribution_id NOT NULL
```

un `MediaObject` no representa un estado válido sin una contribución asociada.

Al construirlo:

```java
MediaObject mediaObject =
        new MediaObject(
                id,
                contribution
        );
```

el constructor establece su `Contribution` y delega en una operación package-private de `Contribution` para completar el otro lado:

```text
MediaObject.contribution      → Contribution
Contribution.mediaObject      → MediaObject
```

El resultado garantiza:

```java
mediaObject.getContribution() == contribution;
contribution.getMediaObject() == mediaObject;
```

La operación interna de asociación también impide sustituir silenciosamente un `MediaObject` ya asociado por otro distinto, reforzando la cardinalidad 1:1 también a nivel de dominio.

`attachMediaObject()` permanece package-private para evitar exponer dos formas públicas diferentes de construir la relación.

No se introduce una factory multimedia en `Contribution`.

El flujo para crear contribuciones `IMAGE`, `AUDIO` y `VIDEO` pertenece a T17 y se mantiene fuera del alcance de esta revisión.

---

## 5. Cobertura adicional de tests

T16 no incluía tests específicos entre sus criterios de aceptación.

Por tanto, la ausencia de nuevos tests en la implementación original **no se considera un requisito incumplido**.

La revisión incorpora:

```text
MediaObjectPersistenceIntegrationTest
```

con ocho escenarios de regresión:

1. La construcción de un `MediaObject` sincroniza ambos lados de la asociación.
2. Una `Contribution` no puede recibir silenciosamente un segundo `MediaObject`.
3. Un `MediaObject` no puede construirse sin identificador.
4. Un `MediaObject` no puede construirse sin `Contribution`.
5. El estado inicial de un `MediaObject` es `UPLOADING`.
6. Un `MediaObject` puede persistirse y recuperarse utilizando el enum PostgreSQL.
7. Estados distintos de `UPLOADING`, como `PROCESSING`, sobreviven correctamente al round-trip de persistencia.
8. Una contribución `TEXT` continúa persistiendo y recuperándose sin `MediaObject`.

Los tests de persistencia utilizan PostgreSQL real mediante Testcontainers.

Esto permite verificar conjuntamente:

```text
Flyway
   ↓
PostgreSQL media_object_status
   ↓
Hibernate named enum mapping
   ↓
MediaObjectStatus
```

y evita validar el comportamiento específico del enum contra una base de datos con semántica diferente.

> [!NOTE]
> Para probar la persistencia de `MediaObject` se utiliza temporalmente una `Contribution.text()` como entidad anfitriona.
>
> T16 todavía no define una factory pública para construir contribuciones `IMAGE`, `AUDIO` o `VIDEO`. Añadirla únicamente para facilitar estos tests adelantaría decisiones pertenecientes a T17.
>
> Estos tests verifican el mapping y la asociación de persistencia; no establecen que el flujo funcional de la aplicación deba permitir multimedia sobre una contribución `TEXT`.

La suite completa del backend permanece verde después de aplicar los cambios.

---

## 6. Resultado final

Después de esta revisión:

- T16 mantiene intacto su alcance funcional original;
- `MediaObjectStatus` utiliza una representación coherente entre Java, Hibernate y PostgreSQL;
- la asociación `Contribution ↔ MediaObject` se construye mediante un único flujo;
- un `MediaObject` no puede construirse sin `id` ni `Contribution`;
- una `Contribution` no puede sustituir silenciosamente un `MediaObject` ya asociado;
- el historial existente de Flyway permanece inmutable;
- la persistencia multimedia dispone de cobertura específica de regresión;
- las contribuciones `TEXT` continúan funcionando sin `MediaObject`;
- la suite del backend permanece verde;
- no se adelanta lógica correspondiente a T17.

---

## 7. Aprendizajes reutilizables

### Diferenciar requisitos incumplidos de mejoras posteriores

Una revisión puede encontrar oportunidades de mejora aunque una tarea haya cumplido correctamente su contrato.

Conviene documentar ambas situaciones de forma distinta para no convertir deuda técnica o inconsistencias internas en falsos incumplimientos funcionales.

### Mantener una única entrada para construir relaciones bidireccionales

En una relación JPA bidireccional no basta con modificar el lado inverso.

Cuando una entidad dependiente no puede existir válidamente sin su entidad principal, su construcción puede ser un buen punto para establecer la relación completa y preservar la invariante desde el primer momento.

### Restringir helpers internos del dominio

Una operación necesaria para sincronizar internamente una asociación no tiene por qué formar parte de la API pública del modelo.

La visibilidad package-private permite colaborar entre entidades del mismo dominio sin ofrecer caminos alternativos al resto de la aplicación.

### Evolucionar las migraciones en lugar de reescribirlas

Una migración ya integrada no debería modificarse para corregir decisiones posteriores.

El schema evoluciona mediante una nueva migración que transforma de forma explícita el estado anterior.

### Probar mappings específicos contra la base de datos real

Cuando el comportamiento depende de características concretas de PostgreSQL, como named enums, los tests de integración con Testcontainers ofrecen una garantía que una base de datos en memoria no puede reproducir necesariamente.

### Añadir cobertura cuando cambia una invariante

Los tests no solo proceden de criterios de aceptación.

Cuando una revisión endurece una relación de dominio o modifica el mapping de persistencia, añadir tests de regresión ayuda a conservar esa decisión en cambios posteriores.