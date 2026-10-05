<div align="center">

# T17 — Review Notes

**Implement media upload initiation API**

</div>

<table>
  <tr>
    <td><strong>Tarea</strong></td>
    <td>T17</td>
  </tr>
  <tr>
    <td><strong>Área</strong></td>
    <td>Backend · Media upload initiation</td>
  </tr>
  <tr>
    <td><strong>Tipo de revisión</strong></td>
    <td>Corrective review</td>
  </tr>
  <tr>
    <td><strong>Estado de la implementación inicial</strong></td>
    <td>Flujo principal implementado · revisión necesaria sobre consistencia y contrato</td>
  </tr>
  <tr>
    <td><strong>Estado de la revisión</strong></td>
    <td>Completada · tests verdes</td>
  </tr>
</table>

---

## 1. Contexto de la revisión

T17 tenía como objetivo permitir iniciar una contribución multimedia y obtener la información necesaria para subir el fichero directamente desde el cliente hacia object storage mediante un PUT presigned.

La implementación inicial resolvía correctamente el núcleo del flujo:

```text
authenticated user
→ validate capsule membership
→ validate COLLECTING
→ validate media metadata
→ create Contribution
→ create MediaObject
→ generate private object key
→ generate presigned PUT
→ return upload information
```

Durante la revisión se identificaron tres tipos de situaciones diferentes:

### Correcciones sobre la implementación

Algunos cambios introducidos durante T17 modificaban decisiones de dominio que ya habían sido revisadas y consolidadas en T16.

También se detectaron cambios de formato e infraestructura que no eran necesarios para implementar la tarea.

### Decisiones abiertas concretadas durante la review

T17 estaba definida deliberadamente con margen para que el desarrollador tuviese que tomar decisiones técnicas.

Algunas de esas decisiones no estaban fijadas previamente, especialmente:

- formatos multimedia concretos soportados;
- política exacta del contrato devuelto para el PUT presigned.

Estas decisiones se concretan durante la review y no deben interpretarse como requisitos que la implementación original hubiese ignorado.

### Cobertura adicional de verificación

La issue T17 no exigía expresamente tests automatizados específicos.

Por tanto, la ausencia de nuevos tests en la implementación inicial no se considera por sí misma un criterio de aceptación incumplido.

Sin embargo, varios comportamientos de T17 son difíciles de verificar con confianza únicamente mediante inspección de código, por lo que la review añade cobertura específica para demostrar el comportamiento esperado y protegerlo frente a regresiones.

---

## 2. Alcance original de T17

T17 debía permitir iniciar uploads para contribuciones multimedia de tipo:

```text
IMAGE
AUDIO
VIDEO
```

La entrada debía contener:

- `capsuleId`;
- tipo de contribución;
- `originalFilename`;
- `mimeType`;
- `sizeBytes`.

Debían comprobarse:

- pertenencia del usuario a la cápsula;
- cápsula en estado `COLLECTING`;
- tipo multimedia permitido;
- MIME permitido;
- tamaño dentro de límites configurados.

El flujo debía crear:

- una `Contribution`;
- un `MediaObject`;
- estado inicial `UPLOADING`;
- una object key privada generada por backend.

El backend debía devolver la información necesaria para que el cliente realizase un PUT presigned.

La arquitectura esperada era:

```text
client
   │
   │ metadata
   ▼
Spring Boot
   │
   │ presigned upload information
   ▼
client
   │
   │ binary PUT
   ▼
object storage
```

El fichero no debía atravesar Spring Boot.

T17 no incluía todavía:

- finalización del upload;
- comprobación posterior de existencia del objeto;
- transición `UPLOADING → PROCESSING`;
- validación del contenido real del fichero;
- creación de `MediaProcessingJob`;
- procesamiento mediante worker;
- FFmpeg o ffprobe;
- UI de upload.

Estas responsabilidades pertenecen a tareas posteriores.

---

## 3. Estado de la implementación inicial

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
      <td>Endpoint de inicio de upload</td>
      <td>✅ Cumplido</td>
      <td>Se añadió el flujo multimedia dentro de Contribution API</td>
    </tr>
    <tr>
      <td>Membership</td>
      <td>✅ Cumplido</td>
      <td>Se reutiliza la validación existente de pertenencia a cápsula</td>
    </tr>
    <tr>
      <td>Estado COLLECTING</td>
      <td>✅ Cumplido</td>
      <td>Se reutiliza la regla existente de modificación de contribuciones</td>
    </tr>
    <tr>
      <td>Tipos multimedia</td>
      <td>✅ Cumplido</td>
      <td>IMAGE, AUDIO y VIDEO se admiten; TEXT queda fuera del flujo</td>
    </tr>
    <tr>
      <td>Límites de tamaño</td>
      <td>✅ Cumplido</td>
      <td>Se propusieron límites configurables de 10 / 50 / 500 MiB que se mantienen tras la review</td>
    </tr>
    <tr>
      <td>Validación MIME</td>
      <td>🟡 Parcial</td>
      <td>Se comprobaba la categoría general del MIME, pero todavía no existía una política concreta de formatos soportados</td>
    </tr>
    <tr>
      <td>Contribution multimedia</td>
      <td>✅ Cumplido</td>
      <td>Se añadió una factory específica para contribuciones multimedia</td>
    </tr>
    <tr>
      <td>MediaObject UPLOADING</td>
      <td>✅ Cumplido</td>
      <td>Se crea y persiste asociado a la Contribution con el estado esperado</td>
    </tr>
    <tr>
      <td>Object key privada</td>
      <td>✅ Cumplido</td>
      <td>La key se genera en backend y no depende del filename original</td>
    </tr>
    <tr>
      <td>Presigned PUT</td>
      <td>🔧 Consistencia</td>
      <td>El flujo principal estaba implementado, pero parte del contrato necesario para ejecutar el PUT permanecía implícito</td>
    </tr>
    <tr>
      <td>Invariantes revisadas de T16</td>
      <td>🔴 Pendiente</td>
      <td>Se reintrodujeron decisiones anteriores que contradecían el estado revisado de T16</td>
    </tr>
    <tr>
      <td>Cobertura automatizada específica</td>
      <td>🔧 Cobertura adicional</td>
      <td>No era requisito explícito de T17; se añade para verificar de forma fiable el comportamiento</td>
    </tr>
  </tbody>
</table>

---

## 4. Aspectos correctamente implementados

### 4.1 Flujo directo hacia object storage

La implementación utiliza la abstracción existente:

```text
ObjectStorageService
```

para generar una URL temporal de upload.

Esto mantiene correctamente la separación entre responsabilidades:

```text
Spring Boot
→ autorización
→ validación de metadata
→ persistencia
→ generación del contrato presigned

Object storage
→ recepción del fichero
```

El binario no atraviesa Spring Boot.

---

### 4.2 Reutilización de reglas existentes de Contribution

Las comprobaciones de:

```text
membership
COLLECTING
```

reutilizan las reglas existentes del módulo de contribuciones.

Esto evita crear una segunda interpretación de permisos o estados únicamente para multimedia.

---

### 4.3 Creación de contribuciones multimedia

Se introduce:

```java
Contribution.media(...)
```

como factory específica para:

```text
IMAGE
AUDIO
VIDEO
```

sin alterar el comportamiento existente de:

```java
Contribution.text(...)
```

Esto permite construir correctamente una contribución multimedia antes de asociarle su `MediaObject`.

---

### 4.4 Política inicial de tamaños

La implementación propuso los siguientes límites:

```text
IMAGE → 10 MiB
AUDIO → 50 MiB
VIDEO → 500 MiB
```

mediante propiedades configurables.

T17 exigía que existiesen límites configurados, pero no fijaba valores concretos.

Después de revisarlos, estos valores se consideran adecuados como política inicial del proyecto y se mantienen sin cambios.

Por tanto, no forman parte de las correcciones de esta review.

---

## 5. Hallazgos y correcciones

### 5.1 Preservar las decisiones revisadas en T16

La revisión de T16 había establecido una única forma pública de construir la asociación:

```text
Contribution ↔ MediaObject
```

El constructor de `MediaObject` establece su `Contribution` y utiliza internamente:

```java
attachMediaObject(...)
```

para completar el lado inverso.

Ese helper había quedado deliberadamente con visibilidad package-private para evitar que el resto de la aplicación dispusiese de una segunda vía pública para modificar la asociación.

Durante T17 volvió a exponerse como método público.

La review restaura su visibilidad package-private.

También se había eliminado del mapping:

```java
orphanRemoval = true
```

La revisión restaura esta configuración para mantener el modelo coherente con el estado consolidado en T16.

Ninguno de estos cambios era necesario para implementar el flujo de T17.

> [!NOTE]
> La review documenta el cambio técnico observado.
>
> No presupone la causa por la que reapareció el estado anterior del modelo.

---

### 5.2 Mantener el diff dentro del alcance

T17 introdujo también reformateos y modificaciones sobre código existente que no eran necesarios para implementar el nuevo flujo.

Este tipo de cambios aumenta el tamaño del diff y puede ocultar modificaciones semánticas entre cambios puramente visuales.

Antes de integrar una tarea conviene poder responder fácilmente:

```text
¿Qué cambios existen realmente para implementar esta issue?
```

Esto es especialmente importante cuando una herramienta de IA genera o sustituye archivos completos.

Revisar el diff ayuda a detectar:

- código no relacionado;
- decisiones antiguas reintroducidas;
- cambios de infraestructura innecesarios;
- regresiones accidentales.

La review elimina los cambios ajenos que modificaban decisiones consolidadas y mantiene el código necesario para T17.

---

### 5.3 Centralizar la política de uploads

La implementación inicial mantenía dentro de `ContributionService`:

- validación del MIME;
- límite máximo de IMAGE;
- límite máximo de AUDIO;
- límite máximo de VIDEO.

La revisión extrae estas decisiones a:

```text
MediaUploadPolicy
```

La responsabilidad queda separada de forma más clara:

```text
ContributionService
→ orquesta el caso de uso

MediaUploadPolicy
→ define qué uploads acepta After
```

Esto evita que las reglas de aceptación de ficheros queden mezcladas con la lógica general de contribuciones.

Los límites originalmente propuestos se conservan:

```text
IMAGE → 10 MiB
AUDIO → 50 MiB
VIDEO → 500 MiB
```

---

### 5.4 Definir los formatos soportados en v0.1

T17 indicaba que debía validarse un MIME permitido, pero no definía una lista concreta de formatos soportados.

La implementación inicial comprobaba la categoría general mediante una regla equivalente a:

```text
IMAGE → image/*
AUDIO → audio/*
VIDEO → video/*
```

Esto permite comprobar que el MIME declarado corresponde al tipo de contribución, pero no establece qué formatos soporta oficialmente After.

Durante la review se define la política inicial.

#### IMAGE

```text
JPEG → image/jpeg
PNG  → image/png
WebP → image/webp
```

#### AUDIO

```text
MP3        → audio/mpeg
WAV        → audio/wav
WAV legacy → audio/x-wav
Ogg        → audio/ogg
M4A        → audio/mp4
WebM       → audio/webm
```

#### VIDEO

```text
MP4  → video/mp4
WebM → video/webm
MOV  → video/quicktime
```

Esta política queda centralizada en `MediaUploadPolicy`.

La extensión del fichero no se considera una prueba de su contenido real.

T17 valida únicamente la metadata declarada por el cliente.

La validación real del fichero corresponde posteriormente al pipeline multimedia.

---

### 5.5 Hacer explícito el contrato del PUT presigned

La respuesta inicial devolvía:

```text
contributionId
uploadUrl
httpMethod
```

La implementación conocía además:

- la expiración del upload;
- el `Content-Type` utilizado para generar el PUT.

Sin embargo, esa información permanecía implícita para el consumidor de la API.

La review amplía el contrato con:

```text
expiresAt
headers
```

De esta forma el cliente recibe directamente:

```text
upload URL
HTTP method
expiration
required headers
```

y no necesita reconstruir parte del contrato utilizando información conocida indirectamente.

Esta modificación se considera una mejora de consistencia del contrato.

La definición original de T17 no concretaba la estructura exacta que debía tener esta respuesta.

---

### 5.6 Alinear la validación HTTP con persistencia

La persistencia existente establece:

```text
original_filename VARCHAR(255)
original_mime_type VARCHAR(100)
```

La request multimedia comprobaba inicialmente que ambos valores no estuviesen vacíos, pero no limitaba su longitud.

Esto podía permitir que una petición superase Bean Validation y posteriormente fuese incompatible con las restricciones de PostgreSQL.

La review añade límites equivalentes en la frontera HTTP:

```text
originalFilename → 255
mimeType         → 100
```

De esta forma las restricciones de entrada quedan alineadas con el modelo de persistencia.

---

### 5.7 Añadir cobertura de verificación para T17

La issue original no establecía tests automatizados específicos como criterio obligatorio.

Por tanto:

```text
no dedicated T17 tests
≠
unmet acceptance criterion
```

Sin embargo, varios comportamientos de esta tarea son difíciles de demostrar con confianza únicamente leyendo la implementación:

```text
membership
capsule state
media type
MIME policy
size boundaries
Contribution persistence
MediaObject persistence
UPLOADING initial state
presigned PUT contract
direct upload to object storage
```

Durante la review se incorpora cobertura específica para estos comportamientos.

#### `MediaUploadPolicyTest`

Comprueba:

1. MIME admitidos para IMAGE.
2. MIME admitidos para AUDIO.
3. MIME admitidos para VIDEO.
4. Normalización del MIME.
5. Rechazo de formatos no soportados.
6. Rechazo de MIME perteneciente a otra categoría.
7. Rechazo de `TEXT`.
8. Aceptación del tamaño máximo exacto.
9. Rechazo al superar el tamaño máximo.
10. Rechazo de tamaños no positivos.

#### `MediaUploadApiIntegrationTest`

Comprueba utilizando PostgreSQL y MinIO reales:

1. El owner puede iniciar un upload.
2. Un contributor puede iniciar un upload.
3. Un usuario ajeno no puede iniciar el upload.
4. Una cápsula no modificable rechaza nuevos uploads.
5. `TEXT` no puede utilizar el flujo multimedia.
6. Un MIME no soportado es rechazado.
7. Un MIME de otra categoría es rechazado.
8. Los límites de tamaño se aplican.
9. Los límites de longitud de la request se aplican.
10. La `Contribution` se persiste con el tipo correcto.
11. El `MediaObject` queda asociado a la contribución.
12. El estado inicial es `UPLOADING`.
13. `originalObjectKey`, filename, MIME y size se almacenan correctamente.
14. Los campos correspondientes a procesamiento posterior permanecen `null`.
15. La respuesta expone URL, método, expiración y headers.
16. El contrato devuelto permite realizar un PUT real directamente contra MinIO.

El último escenario ejecuta literalmente:

```text
Spring Boot API
→ presigned upload contract
→ HTTP client
→ MinIO
```

El binario se envía directamente al storage y no atraviesa Spring Boot.

> [!NOTE]
> Estos tests se añaden como cobertura de verificación y regresión.
>
> No se documentan como tests que la issue original exigiese expresamente y que hubiesen sido omitidos.

---

## 6. Resultado final

Después de esta revisión:

- T17 mantiene el flujo directo cliente → object storage;
- owner y contributor pueden iniciar uploads;
- los usuarios ajenos quedan rechazados;
- solo se permiten uploads durante `COLLECTING`;
- `TEXT` queda fuera del flujo multimedia;
- los formatos soportados están definidos explícitamente;
- los límites de 10 / 50 / 500 MiB se conservan;
- `MediaUploadPolicy` centraliza la política de aceptación;
- las validaciones HTTP quedan alineadas con persistencia;
- el contrato presigned expone URL, método, expiración y headers;
- `Contribution` y `MediaObject` se persisten correctamente;
- `MediaObject` comienza en `UPLOADING`;
- los campos reservados para procesamiento posterior permanecen sin completar;
- las invariantes consolidadas durante T16 quedan restauradas;
- el PUT directo contra MinIO queda cubierto mediante integración;
- la suite backend permanece verde;
- no se introduce comportamiento perteneciente a T18 o tareas posteriores.

---

## 7. Aprendizajes reutilizables

### Diferenciar requisitos definidos de decisiones abiertas

Una issue puede dejar margen deliberadamente al desarrollador.

Antes de implementar conviene distinguir:

```text
defined requirement
existing project decision
open technical decision
out-of-scope behaviour
```

Una decisión abierta no es necesariamente un defecto de la tarea.

Sí requiere que el desarrollador identifique que existe una decisión que debe tomar conscientemente antes de dejar que forme parte del código.

---

### Revisar el contexto anterior antes de modificar una zona existente

Cuando una tarea depende de una funcionalidad ya revisada, el estado actual del código y sus review notes forman parte del contexto técnico relevante.

En este caso T17 construía directamente sobre T16.

Consultar:

```text
T16_REVIEW_NOTES.md
```

permite conocer qué decisiones del modelo fueron deliberadas y deben preservarse mientras la nueva tarea no requiera modificarlas.

---

### Revisar el diff además del resultado funcional

Que una tarea compile y funcione no significa que todos los cambios introducidos sean necesarios.

Un diff inesperadamente grande puede indicar:

- reformateo no relacionado;
- código generado desde una versión anterior;
- archivos sustituidos completamente;
- cambios de arquitectura accidentales;
- regresiones difíciles de detectar leyendo únicamente el resultado final.

La revisión del diff forma parte de validar el trabajo generado por una herramienta de IA.

---

### Convertir criterios de aceptación en preguntas verificables

Antes de considerar un comportamiento terminado conviene preguntarse:

```text
Requirement
    ↓
How do I know this actually works?
```

La respuesta puede ser:

```text
code inspection
manual verification
automated test
integration test
```

No todas las tareas necesitan el mismo tipo o cantidad de tests.

En cambio, cuando intervienen:

```text
permissions
state transitions
boundary values
persistence
external infrastructure
```

la cobertura automatizada puede ser la forma más fiable de demostrar que el comportamiento esperado existe realmente.

---

### La IA propone; el desarrollador conserva el contexto y toma las decisiones

Una herramienta con acceso al repositorio puede leer el código existente, pero no necesariamente distingue por sí sola:

- qué decisiones son deliberadas;
- qué decisiones son históricas;
- qué partes pueden modificarse;
- qué partes pertenecen a otra tarea;
- qué supuestos está introduciendo al rellenar huecos.

El desarrollador sigue siendo responsable de revisar esas propuestas y decidir conscientemente qué cambios deben formar parte de la implementación.

El objetivo no es únicamente obtener código que compile.

El objetivo es conseguir que el código generado siga respetando:

```text
issue
architecture
existing invariants
scope
acceptance criteria
```

de forma coherente.