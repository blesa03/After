# After — Preparación del entorno de desarrollo

Esta guía sirve para preparar un equipo desde cero y dejarlo listo para trabajar en **After**.

> Esta documentación es temporal y está pensada para el onboarding inicial del equipo.

---

## ✅ Al terminar deberías poder

- Clonar el repositorio.
- Instalar las dependencias del frontend.
- Compilar y ejecutar el backend.
- Levantar PostgreSQL y MinIO.
- Ejecutar el Media Worker.
- Ejecutar los tests.
- Empezar a trabajar en una tarea del proyecto.

---

## ⚡ Resumen rápido

Necesitas:

| Herramienta | Versión |
|---|---:|
| Git | 2.x |
| Node.js | 22.x |
| npm | 10.x o compatible |
| Java JDK | 21 |
| Docker | versión reciente |
| Docker Compose | versión reciente |

No necesitas instalar manualmente:

- Maven
- PostgreSQL
- MinIO
- FFmpeg
- FFprobe

---

## 1. Comprobar qué tienes instalado

Ejecuta:

```powershell
git --version
node -v
npm -v
java -version
javac -version
docker --version
docker compose version
```

Si alguna herramienta falta, abre su sección correspondiente.

---

<details>
<summary><strong>2. Instalar Git</strong></summary>

<br>

En Windows puedes instalar Git con:

```powershell
winget install Git.Git
```

Después:

```powershell
git --version
```

También puedes instalarlo desde la web oficial de Git.

</details>

---

<details>
<summary><strong>3. Instalar Node.js 22</strong></summary>

<br>

After utiliza **Node.js 22**.

Después de instalarlo comprueba:

```powershell
node -v
npm -v
```

La versión de Node debería ser:

```text
22.x
```

### Angular CLI

No es obligatorio instalar Angular CLI globalmente.

El frontend utilizará la versión instalada en las dependencias del propio proyecto.

Si quieres disponer también de `ng` globalmente:

```powershell
npm install -g @angular/cli@22
```

</details>

---

<details>
<summary><strong>4. Instalar Java 21</strong></summary>

<br>

After utiliza **JDK 21**.

En Windows recomendamos Eclipse Temurin:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Cuando termine:

1. Cierra la terminal.
2. Abre una nueva.
3. Comprueba:

```powershell
java -version
javac -version
```

Ambos deberían mostrar Java 21.

### Maven

No necesitas instalar Maven globalmente.

Tanto `backend` como `media-worker` incluyen Maven Wrapper:

```text
mvnw
mvnw.cmd
.mvn/
```

En Windows:

```powershell
.\mvnw.cmd
```

En Linux/macOS:

```bash
./mvnw
```

</details>

---

<details>
<summary><strong>5. Instalar Docker</strong></summary>

<br>

Instala Docker Desktop.

En Windows debe funcionar utilizando WSL2.

Comprueba:

```powershell
docker --version
docker compose version
```

Para verificar que el motor de Docker funciona:

```powershell
docker run hello-world
```

</details>

---

<details>
<summary><strong>6. VSCode y extensiones recomendadas</strong></summary>

<br>

After no depende de ningún IDE concreto.

Aunque actualmente trabajemos con **VSCode**, el repositorio debe seguir siendo independiente del editor.

Extensiones recomendadas:

- Angular Language Service
- ESLint
- Prettier
- Extension Pack for Java
- Spring Boot Extension Pack
- GitLens
- Error Lens
- REST Client

Son recomendaciones, no requisitos.

No se versionarán configuraciones exclusivas de `.vscode/`.

</details>

---

## 7. Clonar el repositorio

```powershell
git clone <URL_DEL_REPOSITORIO>
cd After
```

Estructura principal:

```text
After/
├── frontend/
├── backend/
├── media-worker/
├── docs/
├── .github/
├── docker-compose.yml
├── .env.example
├── .editorconfig
└── README.md
```

---

## 8. Crear el entorno local

Desde la raíz:

```powershell
Copy-Item .env.example .env
```

El archivo:

```text
.env
```

es local y **no debe subirse al repositorio**.

---

## 9. Levantar infraestructura

Desde la raíz:

```powershell
docker compose up -d
```

Actualmente Docker Compose prepara:

| Servicio | Función |
|---|---|
| PostgreSQL | Base de datos |
| MinIO | Almacenamiento S3 compatible |
| MinIO Init | Preparación inicial del bucket |
| Media Worker | Worker multimedia |

Comprueba:

```powershell
docker compose ps
```

> `minio-init` puede aparecer terminado con código `0`. Es normal: realiza su trabajo y finaliza.

---

<details>
<summary><strong>10. Preparar el frontend</strong></summary>

<br>

Entra en:

```powershell
cd frontend
```

Instala dependencias:

```powershell
npm install
```

Comprueba que compila:

```powershell
npm run build
```

Ejecuta los tests:

```powershell
npm test
```

Para desarrollo:

```powershell
npm start
```

Después:

```powershell
cd ..
```

</details>

---

<details>
<summary><strong>11. Preparar el backend</strong></summary>

<br>

Entra en:

```powershell
cd backend
```

Ejecuta los tests:

```powershell
.\mvnw.cmd test
```

Para arrancar Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

Después:

```powershell
cd ..
```

</details>

---

<details>
<summary><strong>12. Media Worker</strong></summary>

<br>

El Media Worker normalmente se ejecutará mediante Docker.

Su imagen contiene:

- Java 21
- FFmpeg
- FFprobe
- la aplicación Spring Boot del worker

No necesitas instalar FFmpeg manualmente.

### Ver estado

```powershell
docker compose ps
```

### Ver logs

```powershell
docker compose logs media-worker
```

### Reconstruir la imagen

```powershell
docker compose build media-worker
```

</details>

---

## 13. Comprobación final

### Herramientas

```powershell
git --version
node -v
npm -v
java -version
javac -version
docker --version
docker compose version
```

### Infraestructura

```powershell
docker compose up -d
docker compose ps
```

### Frontend

```powershell
cd frontend
npm install
npm run build
cd ..
```

### Backend

```powershell
cd backend
.\mvnw.cmd test
cd ..
```

Si todo lo anterior funciona:

> ✅ **Tu entorno está preparado para trabajar en After.**

El siguiente documento que debes leer es:

```text
docs/setup/WORKFLOW.md
```

Ahí se explica cómo coger tareas, trabajar con ramas, crear Pull Requests y utilizar el CI.

También puedes consultar:

docs/setup/MARTIN_WORKFLOW.md

Ese documento recoge la metodología personal que utiliza Martín para resolver cada tarea de principio a fin. No es obligatorio seguirla exactamente, pero puede servir como referencia para construir tu propio workflow manteniendo compatibilidad con el proceso general del proyecto.