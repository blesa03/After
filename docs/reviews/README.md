<div align="center">

# Review Notes

Notas de revisión técnica del proyecto **After**

</div>

---

## ¿Para qué sirven?

Los documentos de este directorio recogen revisiones realizadas sobre tareas ya implementadas.

La idea es dejar constancia de:

- qué pedía realmente la tarea;
- qué estaba correctamente implementado;
- qué aspectos necesitaron ajustes;
- qué correcciones se realizaron;
- qué podemos aprender y reutilizar en tareas posteriores.

> [!NOTE]
> Estas notas revisan **implementaciones y decisiones técnicas**, no el trabajo individual de una persona.  
> Su objetivo es facilitar el aprendizaje y mantener criterios consistentes dentro del proyecto.

## Convención

Cada revisión utiliza el número de la tarea:

```text
T09_REVIEW_NOTES.md
T10_REVIEW_NOTES.md
T11_REVIEW_NOTES.md
```

Estructura del directorio:

```text
docs/reviews/
├── README.md
├── T09_REVIEW_NOTES.md
├── T10_REVIEW_NOTES.md
└── ...
```

## Estructura de una review

<table>
  <thead>
    <tr>
      <th>Sección</th>
      <th>Contenido</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><strong>1. Objetivo</strong></td>
      <td>Qué debía resolver la tarea</td>
    </tr>
    <tr>
      <td><strong>2. Requisitos</strong></td>
      <td>Qué condiciones y criterios debía cumplir</td>
    </tr>
    <tr>
      <td><strong>3. Estado inicial</strong></td>
      <td>Qué estaba cumplido, parcial o pendiente al revisar</td>
    </tr>
    <tr>
      <td><strong>4. Aspectos correctos</strong></td>
      <td>Decisiones que ya estaban bien resueltas</td>
    </tr>
    <tr>
      <td><strong>5. Hallazgos y correcciones</strong></td>
      <td>Qué hubo que modificar y por qué</td>
    </tr>
    <tr>
      <td><strong>6. Resultado final</strong></td>
      <td>Cómo queda la tarea después de la revisión</td>
    </tr>
    <tr>
      <td><strong>7. Aprendizajes</strong></td>
      <td>Ideas reutilizables para próximas tareas</td>
    </tr>
  </tbody>
</table>

### Estados utilizados

- ✅ **Cumplido** — requisito correctamente implementado.
- 🟡 **Parcial** — existe, pero necesita algún ajuste.
- 🔴 **Pendiente** — requisito todavía no cubierto.
- 🔵 **Fuera de alcance** — implementación perteneciente a otra tarea.

## Nivel de detalle

El contenido importante debe poder entenderse **sin abrir ningún desplegable**.

Los bloques `<details>` se reservan para:

- pequeñas referencias de código;
- explicaciones técnicas secundarias;
- ejemplos;
- contexto adicional.

<details>
<summary><strong>Ejemplo</strong></summary>

Si una corrección afecta al principal utilizado por Spring Security, basta con mostrar la diferencia relevante:

```java
@AuthenticationPrincipal Jwt jwt
```

No es necesario copiar el controlador completo.

</details>

## Criterio para el código

Estas notas **no duplican el código fuente**.

Los fragmentos incluidos deben ser únicamente los necesarios para entender:

- el problema;
- el patrón correcto;
- la decisión tomada.

Lo mismo aplica a los tests: se documentan **los escenarios cubiertos y su finalidad**, no la implementación completa de cada test.

## Tono

Las revisiones deben describir hechos técnicos.

Preferimos:

> El endpoint utilizaba un mecanismo de autenticación diferente al empleado por el resto del controlador.

frente a:

> La autenticación estaba mal hecha.

Del mismo modo, no se identifica quién escribió originalmente cada fragmento. El foco se mantiene en **la implementación y en cómo mejorarla**.

---

Estas notas complementan las issues, Pull Requests, tests y CI. No sustituyen ninguno de ellos.