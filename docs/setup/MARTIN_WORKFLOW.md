# After — Workflow estándar por tarea

Este documento define el flujo que se sigue para resolver cada issue/tarea de After.

La resolución de una tarea se divide siempre en **tres bloques**:

1. actualización del repositorio local + creación de rama;
2. desarrollo;
3. commit, push, Pull Request y cierre.

La norma general es preparar desde el principio el recorrido completo de la tarea y asumir que los pasos funcionan. No se interrumpe el flujo para pedir confirmación después de cada comando.

Sólo se pausa cuando el resultado real de un comando, archivo o herramienta es necesario para decidir correctamente qué código hay que escribir a continuación.

---

# Bloque 1 — Actualización del repo local + creación de rama

## 1. Comprobar que la tarea puede empezar

Antes de tocar código:

1. revisar dependencias de la issue;
2. confirmar que todas las tareas bloqueantes están `DONE`;
3. comprobar que la tarea está en `READY`;
4. asignar la issue al desarrollador que la empieza;
5. cambiar en GitHub Project:

```text
READY → IN PROGRESS
```

No se crea la rama desde otra feature antigua.

## 2. Partir siempre del `main` actual

Desde la raíz del repositorio:

```powershell
git switch main
git pull --ff-only origin main
```

`git switch main` asegura que estamos sobre la rama base.

`git pull --ff-only origin main` actualiza el `main` local sin crear merges accidentales.

Esto es obligatorio incluso aunque creamos que `main` ya está actualizado, porque el otro desarrollador puede haber integrado cambios desde la tarea anterior.

## 3. Crear la rama de trabajo

Formato obligatorio:

```text
<type>/TXX-short-description
```

Ejemplo:

```powershell
git switch -c feat/T08-capsule-dashboard-ui
```

Tipos admitidos por el CI:

```text
feat
fix
chore
test
docs
refactor
```

El identificador de tarea utiliza `T` mayúscula:

```text
T08
```

No:

```text
t08
```

El CI valida actualmente un patrón equivalente a:

```regex
^(feat|fix|chore|test|docs|refactor)/T[0-9]{2,}-[a-z0-9]+(-[a-z0-9]+)*$
```

## 4. Verificar la rama

```powershell
git branch --show-current
```

Opcionalmente:

```powershell
git status
```

La salida debe indicar que estamos en la rama recién creada y que el working tree parte del `main` actualizado.

### Secuencia completa habitual

```powershell
git switch main
git pull --ff-only origin main
git switch -c feat/TXX-short-description
git branch --show-current
git status
```

A partir de aquí empieza el desarrollo.

---

# Bloque 2 — Desarrollo

## 1. Respetar estrictamente el alcance de la issue

Se implementa:

- lo indicado por el objetivo;
- los requisitos;
- los criterios de aceptación;
- las dependencias técnicas necesarias.

No se adelantan features posteriores sólo porque resulte cómodo.

Ejemplos:

- una tarea de persistencia no introduce endpoints si no los pide;
- una tarea de API no introduce UI si no la pide;
- una tarea de scaffolding no inventa lógica de negocio;
- una tarea de almacenamiento no acopla el dominio a MinIO.

Si para resolver bien la tarea fuese necesario cambiar materialmente la arquitectura o el alcance, se detiene el flujo y se decide antes de continuar.

## 2. Mantener las decisiones de arquitectura existentes

Antes de desarrollar se revisan las piezas ya existentes que puedan verse afectadas.

Entre las decisiones actuales:

- monorepo IDE-agnostic;
- Flyway controla el esquema;
- Hibernate usa `ddl-auto=validate`;
- PostgreSQL;
- paquetes backend organizados por feature/responsabilidad;
- access token en memoria;
- refresh token HttpOnly;
- storage abstraído del proveedor;
- Angular standalone;
- tests con infraestructura real cuando corresponde.

No se duplica funcionalidad ya existente.

## 3. Crear/modificar los archivos necesarios

Para backend se mantiene normalmente:

```text
feature/
├── api/
├── application/
├── domain/
├── exception/
├── infrastructure/
└── config/        # sólo cuando corresponda
```

No todas las tareas necesitan todas esas carpetas.

Para Angular se mantiene normalmente:

```text
feature/
├── pages/
├── services/
├── models/
├── guards/
├── interceptors/
├── validators/
└── utils/
```

Sólo se crean las piezas que aporte realmente la tarea.

## 4. Tests

Cada tarea añade o adapta los tests necesarios para demostrar sus criterios de aceptación.

### Backend

Como base:

```powershell
.\mvnw clean test
```

Preflight completo:

```powershell
.\mvnw clean verify
```

Cuando hay persistencia:

- PostgreSQL real mediante Testcontainers;
- Flyway;
- Hibernate `ddl-auto=validate`.

Cuando hay infraestructura S3:

- MinIO real mediante Testcontainers.

### Frontend

Instalación limpia:

```powershell
npm ci
```

Tests:

```powershell
npm test -- --no-watch --no-progress
```

Build:

```powershell
npm run build
```

Cuando la funcionalidad lo requiere también se prueba manualmente con:

```powershell
npm start
```

## 5. Comprobaciones finales locales

Antes del commit:

```powershell
git diff --check
git status
```

`git diff --check` detecta problemas de whitespace.

`git status` permite revisar exactamente qué archivos van a formar parte de la tarea y detectar archivos accidentales como configuración específica del IDE.

### Preflight backend habitual

```powershell
.\mvnw clean test
.\mvnw clean verify
git diff --check
git status
```

### Preflight frontend habitual

```powershell
npm ci
npm test -- --no-watch --no-progress
npm run build
git diff --check
git status
```

Si la tarea afecta a más de un módulo se ejecutan las validaciones correspondientes de cada uno.

---

# Bloque 3 — Commit, push, PR + cierre de tarea

Este bloque sólo empieza cuando el desarrollo y el preflight local están correctos.

## 1. Preparar el commit

Revisar cambios:

```powershell
git status
```

Añadirlos:

```powershell
git add .
```

Volver a revisar:

```powershell
git status
```

Crear un commit descriptivo:

```powershell
git commit -m "feat(scope): short description"
```

Ejemplo:

```powershell
git commit -m "feat(capsules): add capsule creation and listing API"
```

Norma práctica:

- mensaje en inglés;
- tipo Conventional Commit;
- scope cuando aporte contexto;
- una tarea suele producir un commit coherente, salvo que exista una razón real para varios.

## 2. Push de la rama

Primer push:

```powershell
git push -u origin feat/TXX-short-description
```

El `-u` configura el upstream.

Si después hay que corregir algo:

```powershell
git add .
git commit -m "fix(scope): short description"
git push
```

No se hace force push salvo que exista una razón explícita para reescribir historial.

## 3. Crear la Pull Request en github.com

La PR se crea manualmente desde GitHub.

Configuración:

```text
base: main
compare: feat/TXX-short-description
```

El título va en inglés.

Normalmente coincide con el commit principal:

```text
feat(scope): short description
```

La descripción también va en inglés.

Estructura recomendada:

```markdown
## Summary

Short explanation of what the task implements.

## Changes

- Change 1.
- Change 2.
- Change 3.

## Validation

- Relevant tests pass.
- Build passes.
- Acceptance criterion verified.

Closes #ISSUE_NUMBER
```

La línea:

```text
Closes #N
```

vincula la PR a la issue y permite que GitHub cierre la issue al integrar la PR.

## 4. Cambiar estado a REVIEW

Una vez creada la PR:

```text
IN PROGRESS → REVIEW
```

La issue/tarea permanece en `REVIEW` mientras:

- corre CI;
- se revisa la implementación;
- se resuelven posibles errores.

No se marca `DONE` antes del merge.

## 5. Esperar y revisar CI

Todos los checks requeridos deben estar verdes.

Actualmente el CI cubre:

- Frontend.
- Backend.
- Media Worker.
- Docker Compose.
- Branch Naming.

Si falla un check:

1. se reproduce/corrige el error;
2. se modifica la misma rama;
3. se crea commit de corrección;
4. se ejecuta `git push`;
5. GitHub vuelve a ejecutar CI.

## 6. Comentario final de PR

Antes de mergear, cuando la implementación está validada, se deja un comentario de cierre/revisión en la PR.

**Idioma: inglés**, porque forma parte del repositorio.

Formato orientativo:

```text
Implementation reviewed and validated successfully.

All acceptance criteria are covered and CI is green. Ready to merge.
```

El contenido se adapta a la funcionalidad real de la tarea.

## 7. Merge

El merge se hace manualmente en github.com.

No se considera una tarea completada sólo porque exista commit o PR.

La tarea se considera integrada cuando:

```text
PR merged into main
```

## 8. Comentario de cierre de la tarea

Después del merge se deja un comentario en la issue/proyecto resumiendo lo implementado y validado.

**Idioma: español**, porque este comentario pertenece al seguimiento interno del proyecto.

Debe incluir:

- qué se implementó;
- decisiones relevantes;
- tests/validaciones;
- confirmación de criterios de aceptación.

Ejemplo de estructura:

```text
TXX completada.

Se ha implementado ...

Incluye:

- ...
- ...
- ...

Todos los criterios de aceptación quedan cumplidos y el CI ha finalizado correctamente.
```

## 9. Estado final

Después del merge:

```text
REVIEW → DONE
```

Si una automatización del Project ya realiza el movimiento correctamente, no se duplica manualmente.

## 10. Revisar dependencias

Este paso es obligatorio al terminar cada tarea.

Se revisan las issues que dependían de la recién completada.

Si una issue ya tiene **todas** sus dependencias en `DONE`:

```text
BACKLOG → READY
```

Si todavía le queda alguna dependencia:

```text
permanece en BACKLOG
```

Al cerrar una tarea siempre se informa explícitamente qué tareas quedan desbloqueadas.

---

# Idiomas utilizados

## Repositorio GitHub

En inglés:

- commits;
- títulos de PR;
- descripción de PR;
- comentarios de PR;
- contenido técnico destinado al repositorio.

## Gestión interna del proyecto

En español:

- comentario de cierre de la issue/tarea;
- explicación de cambios de estado;
- seguimiento de dependencias.

---

# Regla de continuidad

Una vez entregado el plan completo de una tarea:

- se asume que cada paso funciona;
- se continúa siguiendo la lista sin pedir confirmación intermedia;
- si aparece un error, se resuelve sobre el punto concreto y después se retoma el workflow existente.

Sólo se pausa preventivamente cuando hace falta conocer un resultado real para generar correctamente el siguiente cambio.

Ejemplos válidos para pausar:

- conocer la estructura actual de un archivo antes de reescribirlo;
- ver una salida de compilación que determina qué API/version usar;
- conocer una migración existente antes de elegir el siguiente número Flyway;
- inspeccionar una configuración real antes de modificarla.

No se pausa simplemente para preguntar si un comando estándar salió bien.

---

# Chuleta rápida

## Inicio

```powershell
git switch main
git pull --ff-only origin main
git switch -c feat/TXX-short-description
git branch --show-current
```

Project:

```text
READY → IN PROGRESS
```

## Desarrollo

Backend:

```powershell
.\mvnw clean test
.\mvnw clean verify
git diff --check
git status
```

Frontend:

```powershell
npm ci
npm test -- --no-watch --no-progress
npm run build
git diff --check
git status
```

## Cierre local

```powershell
git add .
git status
git commit -m "feat(scope): short description"
git push -u origin feat/TXX-short-description
```

## PR

```text
base: main
compare: feat/TXX-short-description
```

Descripción:

```text
Summary
Changes
Validation
Closes #N
```

Project:

```text
IN PROGRESS → REVIEW
```

Después:

```text
CI green
→ comentario PR en inglés
→ merge
→ comentario issue en español
→ REVIEW → DONE
→ revisar BACKLOG → READY de dependencias desbloqueadas
```
