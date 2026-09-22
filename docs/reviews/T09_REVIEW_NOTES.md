<div align="center">

# T09 — Review Notes

**Implement capsule editing while `COLLECTING`**

</div>

<table>
  <tr>
    <td><strong>Tarea</strong></td>
    <td>T09</td>
  </tr>
  <tr>
    <td><strong>Área</strong></td>
    <td>Backend · Capsule editing</td>
  </tr>
  <tr>
    <td><strong>Dependencia</strong></td>
    <td>T07</td>
  </tr>
  <tr>
    <td><strong>Estado de revisión</strong></td>
    <td>Revisión completada · preflight y CI correctos</td>
  </tr>
</table>

---

## 1. Objetivo de la tarea

T09 debía permitir modificar la configuración de una cápsula **antes de su sellado**.

Los campos editables son:

```text
title
description
opensAt
timezone
```

El tipo de cápsula (`PERSONAL` / `SHARED`) debe permanecer inmutable.

---

## 2. Requisitos y criterios de aceptación

La actualización debe cumplir estas reglas:

- únicamente un `OWNER` puede editar;
- solo puede editarse una cápsula `COLLECTING`;
- `SEALED` y `OPENED` rechazan modificaciones;
- `opensAt` debe seguir siendo una fecha futura;
- `type` no puede modificarse;
- deben existir tests que cubran estos comportamientos.

---

## 3. Estado al iniciar la revisión

<table>
  <thead>
    <tr>
      <th>Requisito</th>
      <th>Estado</th>
      <th>Observación</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Editar <code>title</code></td>
      <td>✅ Cumplido</td>
      <td>Incluido en la actualización</td>
    </tr>
    <tr>
      <td>Editar <code>description</code></td>
      <td>🟡 Parcial</td>
      <td>Se añadió un límite de 255 caracteres no requerido</td>
    </tr>
    <tr>
      <td>Editar <code>opensAt</code></td>
      <td>✅ Cumplido</td>
      <td>Se validaba como fecha futura</td>
    </tr>
    <tr>
      <td>Editar <code>timezone</code></td>
      <td>✅ Cumplido</td>
      <td>Existía validación de zona horaria</td>
    </tr>
    <tr>
      <td><code>type</code> inmutable</td>
      <td>✅ Cumplido</td>
      <td>No estaba incluido en el DTO de actualización</td>
    </tr>
    <tr>
      <td>Solo OWNER</td>
      <td>🟡 Parcial</td>
      <td>La comprobación existía, pero el error HTTP no representaba correctamente la autorización</td>
    </tr>
    <tr>
      <td>Solo COLLECTING</td>
      <td>🟡 Parcial</td>
      <td>La regla estaba únicamente en la capa de servicio</td>
    </tr>
    <tr>
      <td>SEALED / OPENED no editables</td>
      <td>🟡 Parcial</td>
      <td>La condición existía, pero faltaban tests específicos</td>
    </tr>
    <tr>
      <td>Tests de T09</td>
      <td>🔴 Pendiente</td>
      <td>No se añadió la cobertura requerida</td>
    </tr>
    <tr>
      <td>Alcance limitado a T09</td>
      <td>🔵 Fuera de alcance</td>
      <td>La rama contenía también parte del frontend correspondiente a T10</td>
    </tr>
  </tbody>
</table>

---

## 4. Aspectos correctos de la implementación

Antes de entrar en las correcciones, había varias decisiones bien planteadas.

### DTO específico para actualización

Se creó `UpdateCapsuleRequest` separado del DTO de creación.

Esto permite controlar exactamente qué puede modificarse.

Además, `type` no forma parte del DTO, por lo que la regla de mantener `PERSONAL/SHARED` inmutable ya estaba correctamente reflejada.

### Endpoint específico de edición

La funcionalidad se expuso mediante:

```text
PUT /api/capsules/{capsuleId}
```

separando claramente creación, consulta y actualización.

### Comprobación de OWNER

La implementación ya verificaba que el miembro tuviera rol `OWNER` antes de permitir cambios.

La regla funcional era correcta; solo hubo que ajustar la forma de representar el acceso denegado.

### Restricción a COLLECTING

También existía una comprobación para impedir modificaciones cuando la cápsula no estaba en estado `COLLECTING`.

### Reutilización de validaciones

La actualización reutilizaba las comprobaciones de:

- fecha futura;
- timezone válida.

Esto mantiene un comportamiento coherente entre creación y edición.

---

## 5. Hallazgos y correcciones

### 5.1 Se adelantó parte de T10

La rama incluía componentes, rutas y cambios del frontend para editar cápsulas.

Esa interfaz pertenece a **T10 — Build capsule detail and editing UI**, que depende precisamente de T09.

**Corrección aplicada:** se retiró todo el frontend adelantado. T10 se desarrollará desde cero una vez T09 esté integrada.

> [!TIP]
> Una tarea dependiente puede estar muy relacionada con la actual sin formar parte de su alcance.

<details>
<summary><strong>¿Por qué separarlo?</strong></summary>

Mantener T09 y T10 separadas permite:

- revisar cada PR contra su propia issue;
- conservar claras las dependencias;
- evitar que una tarea posterior nazca parcialmente implementada dentro de otra;
- trabajar sobre el estado real de `main` una vez integrada la dependencia.

</details>

---

### 5.2 El endpoint utilizaba un principal distinto

La actualización recibía directamente un `User`, mientras que el resto de `CapsuleController` utiliza el JWT como principal.

**Corrección aplicada:** se alineó con el patrón existente del módulo.

<details>
<summary><strong>Ver diferencia técnica</strong></summary>

Antes:

```java
@AuthenticationPrincipal User user
```

Patrón utilizado por After:

```java
@AuthenticationPrincipal Jwt jwt
```

Después, el usuario se obtiene mediante `AuthService` utilizando el `subject` del token.

</details>

**Idea reutilizable:** antes de introducir una solución nueva, revisar cómo resuelven el mismo problema los endpoints vecinos.

---

### 5.3 CONTRIBUTOR devolvía 400 en lugar de 403

La comprobación del rol existía, pero utilizaba `InvalidCapsuleRequestException`.

Eso representa:

```text
400 Bad Request
```

El problema en este caso no está en los datos de la petición: el usuario está autenticado pero **no tiene permisos**.

**Corrección aplicada:** se añadió una excepción específica de acceso denegado asociada a:

```text
403 Forbidden
```

<table>
  <tr>
    <td><strong>400</strong></td>
    <td>La petición o sus datos son inválidos</td>
  </tr>
  <tr>
    <td><strong>403</strong></td>
    <td>El usuario está autenticado pero no puede realizar la operación</td>
  </tr>
</table>

---

### 5.4 `Capsule.update()` no protegía las reglas del dominio

El método actualizaba directamente los valores recibidos.

Eso hacía que reglas importantes dependieran exclusivamente de haber pasado antes por `CapsuleService`.

**Corrección aplicada:** la propia entidad protege ahora que:

- el estado sea `COLLECTING`;
- `title` sea válido;
- `opensAt` exista;
- `opensAt` sea futuro;
- `timezone` sea válida.

<details>
<summary><strong>Por qué mantener también validaciones en el servicio</strong></summary>

No cumplen exactamente la misma función.

El servicio permite transformar errores de una petición en respuestas controladas de la API.

La entidad evita que el objeto pueda quedar en un estado inválido aunque en el futuro sea utilizado desde otro flujo.

</details>

> [!TIP]
> Una regla esencial del objeto no debería depender de que todos sus consumidores recuerden validarla antes.

---

### 5.5 `description` tenía un límite adicional de 255 caracteres

El DTO incluía:

```java
@Size(max = 255)
```

para `description`.

Sin embargo:

- T09 no define ese límite;
- el modelo persistente utiliza `TEXT`.

**Corrección aplicada:** se eliminó esa restricción.

El límite de 255 se mantiene donde sí corresponde: `title`.

---

### 5.6 Faltaban los tests requeridos por la tarea

T09 indicaba explícitamente que debía incluir tests, pero la implementación no añadía cobertura específica para la edición.

**Corrección aplicada:** se ampliaron los tests de integración.

<table>
  <thead>
    <tr>
      <th>Escenario</th>
      <th>Qué comprobamos</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>OWNER + COLLECTING</td>
      <td>La edición funciona y los cambios se persisten</td>
    </tr>
    <tr>
      <td>CONTRIBUTOR</td>
      <td>La petición devuelve 403</td>
    </tr>
    <tr>
      <td>SEALED</td>
      <td>La edición se rechaza</td>
    </tr>
    <tr>
      <td>OPENED</td>
      <td>La edición se rechaza</td>
    </tr>
    <tr>
      <td><code>opensAt</code> pasado</td>
      <td>La petición se rechaza</td>
    </tr>
    <tr>
      <td>Descripción &gt; 255 caracteres</td>
      <td>La actualización continúa siendo válida</td>
    </tr>
  </tbody>
</table>

El caso correcto también comprueba que `type` permanece sin cambios.

<details>
<summary><strong>SEALED y OPENED en los tests</strong></summary>

El flujo real para sellar y abrir cápsulas pertenece a tareas posteriores.

Para poder verificar ahora las restricciones de T09, los tests preparan esos estados directamente en persistencia.

De esta forma no se añaden setters o métodos públicos al dominio únicamente para facilitar un test.

</details>

> [!TIP]
> Los criterios de aceptación suelen convertirse casi directamente en casos de test.

---

## 6. Resultado final

Después de la revisión, T09 queda preparada para cumplir su alcance:

<table>
  <tbody>
    <tr>
      <td>✅</td>
      <td>Solo <code>OWNER</code> puede editar</td>
    </tr>
    <tr>
      <td>✅</td>
      <td>Solo <code>COLLECTING</code> permite modificaciones</td>
    </tr>
    <tr>
      <td>✅</td>
      <td><code>SEALED</code> y <code>OPENED</code> rechazan la edición</td>
    </tr>
    <tr>
      <td>✅</td>
      <td><code>type</code> permanece inmutable</td>
    </tr>
    <tr>
      <td>✅</td>
      <td><code>opensAt</code> debe continuar siendo futuro</td>
    </tr>
    <tr>
      <td>✅</td>
      <td>Las invariantes principales están protegidas también en dominio</td>
    </tr>
    <tr>
      <td>✅</td>
      <td>Los escenarios principales cuentan con tests de integración</td>
    </tr>
    <tr>
      <td>✅</td>
      <td>El trabajo correspondiente a T10 queda fuera de esta PR</td>
    </tr>
  </tbody>
</table>

> [!IMPORTANT]
> La revisión de código está terminada, pero T09 todavía no debe considerarse cerrada.
>
> Queda pendiente ejecutar **preflight, CI y merge**.

---

## 7. Aprendizajes reutilizables

### Seguir los patrones existentes

Si un módulo ya tiene una forma establecida de resolver autenticación, errores o acceso al usuario actual, debería ser el primer punto de referencia.

### Traducir correctamente los errores a HTTP

```text
Datos inválidos      → 400
Sin permisos         → 403
Recurso no accesible → 404 cuando corresponda
```

El código HTTP forma parte del contrato de la API.

### Proteger las invariantes cerca del dominio

Las comprobaciones de la API ayudan al cliente.

Las comprobaciones del dominio ayudan a que el modelo siga siendo válido independientemente de quién lo utilice.

### Convertir los criterios de aceptación en tests

En T09:

```text
OWNER puede editar
CONTRIBUTOR no puede
SEALED no puede
OPENED no puede
opensAt debe ser futuro
```

prácticamente define por sí solo buena parte del plan de tests.

### Respetar el alcance entre tareas

Cuando una tarea posterior depende de la actual, cerrar primero la dependencia deja una base más clara y estable para continuar.

> [!NOTE]
> La implementación revisada ha superado el preflight local y los checks de CI.