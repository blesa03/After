# After — Flujo de trabajo

Esta guía explica cómo vamos a trabajar en **After** utilizando:

- GitHub Projects
- Git
- ramas
- commits
- Pull Requests
- Continuous Integration (CI)
- revisiones

> Esta documentación es temporal y está pensada para el desarrollo inicial del proyecto.

---

## ⚡ Flujo rápido

```text
GitHub Project
      ↓
Coger siguiente tarea READY
      ↓
Asignártela
      ↓
IN PROGRESS
      ↓
Actualizar main
      ↓
Crear rama
      ↓
Desarrollar + tests
      ↓
Commits
      ↓
Push
      ↓
Pull Request
      ↓
REVIEW
      ↓
CI
      ↓
Revisión
      ↓
Merge
      ↓
DONE
```

### Regla principal

> **Nunca trabajamos directamente sobre `main`.**

---

## 1. GitHub Project

Todo el trabajo pendiente estará en el **GitHub Project de After**.

Los estados principales son:

| Estado | Significado |
|---|---|
| `BACKLOG` | Tarea prevista, pero todavía no disponible |
| `READY` | Puede cogerse |
| `IN PROGRESS` | Alguien está trabajando en ella |
| `REVIEW` | Tiene una PR esperando CI/revisión/merge |
| `DONE` | Terminada e integrada en `main` |
| `BLOCKED` | No puede realizarse todavía |

### ¿Qué tarea cojo?

> Coge la tarea `READY` con el número más bajo.

Ejemplo:

```text
T07  DONE
T08  IN PROGRESS
T09  READY       ← coger esta
T10  READY
T11  BACKLOG
```

Si la siguiente está bloqueada:

```text
T09  BLOCKED
T10  READY       ← coger esta
```

---

## 2. Empezar una tarea

### En GitHub

1. Asígnate la tarea.
2. Muévela a `IN PROGRESS`.

### En local

Actualiza `main`:

```bash
git switch main
git pull
```

Crea una rama:

```bash
git switch -c tipo/TXX-descripcion
```

Ejemplos:

```text
feat/T07-login
feat/T12-create-capsule
fix/T18-upload-validation
chore/T05-docker-config
test/T22-capsule-service
docs/T30-update-documentation
```

---

<details>
<summary><strong>Tipos de ramas</strong></summary>

<br>

| Tipo | Uso |
|---|---|
| `feat/` | Nueva funcionalidad |
| `fix/` | Corrección |
| `chore/` | Configuración, infraestructura o mantenimiento |
| `test/` | Tests |
| `docs/` | Documentación |

Ejemplo:

```bash
git switch -c feat/T07-login
```

</details>

---

## 3. Trabajar y hacer commits

Trabaja únicamente sobre la rama de tu tarea.

```bash
git status
git add .
git commit -m "feat: add login endpoint"
```

Mensajes recomendados:

```text
feat: add capsule creation endpoint
fix: prevent contributions after sealing
test: add authentication service tests
chore: configure media worker container
docs: update setup instructions
```

Evita:

```text
fix
changes
cosas
prueba
asdf
```

---

## 4. Subir la rama

Primer push:

```bash
git push -u origin nombre-de-la-rama
```

Después:

```bash
git push
```

---

## 5. Pull Request

Cuando la tarea esté terminada:

1. Haz push.
2. Abre una Pull Request hacia `main`.
3. Relaciona la PR con la tarea.
4. Mueve la tarea a `REVIEW`.

Título recomendado:

```text
T07 — Implement login
```

Descripción breve:

```text
## Cambios
- ...

## Pruebas
- ...

## Notas
- ...
```

---

<details>
<summary><strong>¿Qué es una Pull Request?</strong></summary>

<br>

Una Pull Request es una propuesta para integrar:

```text
tu rama
   ↓
main
```

Permite:

- ver qué código cambia;
- ejecutar el CI;
- revisar el código;
- comentar problemas;
- hacer correcciones;
- mantener un historial claro.

No fusionaremos ramas directamente desde local.

</details>

---

## 6. CI — Continuous Integration

Cada Pull Request hacia `main` ejecuta automáticamente el CI definido en:

```text
.github/workflows/ci.yml
```

También se ejecuta cuando se hace push a `main`.

El objetivo es comprobar automáticamente que los cambios no han roto el proyecto.

### Actualmente comprueba

<table>
<tr>
<th>Frontend</th>
<th>Backend</th>
<th>Media Worker</th>
<th>Infraestructura</th>
</tr>

<tr>
<td>

`npm ci`

Tests

Build Angular

</td>

<td>

PostgreSQL

Tests Spring Boot

Maven

</td>

<td>

PostgreSQL

MinIO

Tests

Docker build

FFmpeg

FFprobe

</td>

<td>

Validación de

`docker-compose.yml`

</td>
</tr>
</table>

---

<details>
<summary><strong>¿Qué significa CI verde?</strong></summary>

<br>

Si todos los checks pasan:

```text
✅ Frontend
✅ Backend
✅ Media Worker
✅ Docker Compose
```

el CI está **verde**.

Significa que las comprobaciones automáticas no han detectado errores.

No significa que el código sea perfecto: todavía debe poder revisarse.

</details>

---

<details>
<summary><strong>¿Qué hago si el CI falla?</strong></summary>

<br>

Ejemplo:

```text
✅ Frontend
✅ Backend
❌ Media Worker
✅ Docker Compose
```

Entra en el check fallido desde GitHub y revisa los logs.

Corrige el problema **en la misma rama**:

```bash
git add .
git commit -m "fix: correct media worker test failure"
git push
```

No necesitas crear otra Pull Request.

Al hacer push, GitHub ejecutará el CI otra vez automáticamente.

### Regla

```text
CI GREEN → puede fusionarse

CI RED   → no se hace merge
```

</details>

---

## 7. Revisión

Cuando la tarea está en `REVIEW`, el otro desarrollador puede revisar la PR.

Se revisan especialmente:

- errores;
- comportamiento inesperado;
- código difícil de mantener;
- inconsistencias;
- tests;
- decisiones que afecten a otras partes del proyecto.

Si se solicitan cambios, se hacen **sobre la misma rama**:

```bash
git add .
git commit -m "fix: apply review changes"
git push
```

La PR y el CI se actualizarán automáticamente.

---

## 8. Merge

Cuando tengamos:

```text
CI        ✅
REVISIÓN  ✅
```

la PR puede integrarse en `main`.

Después:

```text
REVIEW
   ↓
DONE
```

La rama de la tarea puede eliminarse.

---

## 9. Siguiente tarea

Actualiza primero tu `main`:

```bash
git switch main
git pull
```

Después vuelve al GitHub Project y coge la siguiente tarea `READY` disponible.

---

## 10. No esperes mientras una PR está en REVIEW

Si tienes:

```text
T08 → REVIEW
```

puedes coger inmediatamente:

```text
T09 → IN PROGRESS
```

siempre que `T09` esté `READY` y no dependa de código todavía pendiente de integrar.

---

<details>
<summary><strong>¿Qué pasa si una tarea depende de una PR todavía abierta?</strong></summary>

<br>

No mezclaremos ramas sin necesidad.

Si una tarea necesita código que todavía no está en `main`, puede permanecer:

```text
BLOCKED
```

Cuando su dependencia llegue a `main`:

```text
BLOCKED
   ↓
READY
```

Entonces podrá cogerse.

</details>

---

<details>
<summary><strong>¿Qué hago si mi rama se queda desactualizada?</strong></summary>

<br>

Puede ocurrir que `main` cambie mientras trabajas.

Puedes actualizar tu rama:

```bash
git switch main
git pull

git switch nombre-de-tu-rama
git merge main
```

Si aparecen conflictos, Git indicará qué archivos necesitan resolverse.

Después:

```bash
git add .
git commit
git push
```

Si el conflicto no es evidente, lo hablamos antes de resolverlo a ciegas.

</details>

---

## 📌 Resumen

Cada tarea sigue siempre el mismo camino:

```text
READY
  ↓
ASIGNAR
  ↓
IN PROGRESS
  ↓
RAMA
  ↓
DESARROLLO
  ↓
COMMITS
  ↓
PUSH
  ↓
PULL REQUEST
  ↓
REVIEW
  ↓
CI
  ↓
REVISIÓN
  ↓
MERGE
  ↓
DONE
```

### Tres reglas

1. **No trabajar directamente en `main`.**
2. **No hacer merge con CI en rojo.**
3. **Coger siempre la siguiente tarea `READY` disponible.**

Si una tarea, dependencia o decisión técnica no está clara, se comenta antes de implementar algo que pueda afectar al trabajo del otro.