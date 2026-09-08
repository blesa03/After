# After — Preparación del entorno de desarrollo

Esta guía explica cómo preparar un equipo desde cero para trabajar en After.

Cuando completes todos los pasos deberías poder:

- clonar el repositorio;
- instalar las dependencias del frontend;
- compilar el backend;
- levantar la infraestructura local;
- ejecutar el media worker;
- empezar a trabajar con normalidad.

> Esta documentación es temporal y está pensada para la preparación inicial del equipo de desarrollo.

---

## 1. Requisitos

Necesitas tener instalado:

- Git
- Node.js 22
- npm
- JDK 21
- Docker Desktop
- Docker Compose

No necesitas instalar:

- Maven globalmente
- FFmpeg globalmente
- PostgreSQL localmente
- MinIO localmente

El proyecto se encarga de esas dependencias mediante Maven Wrapper y Docker.

---

## 2. Comprobar las herramientas instaladas

Abre una terminal y ejecuta:

```powershell
git --version
node -v
npm -v
java -version
javac -version
docker --version
docker compose version
```

Las versiones principales esperadas son:

```text
Git             2.x
Node.js         22.x
npm             10.x o compatible
Java            21
Docker          versión reciente
Docker Compose  versión reciente
```

No es necesario utilizar exactamente las mismas versiones menores.

---

## 3. Instalar Git

En Windows puedes instalarlo con Winget:

```powershell
winget install Git.Git
```

Después comprueba:

```powershell
git --version
```

---

## 4. Instalar Node.js 22

After utiliza Node.js 22.

Instálalo desde la web oficial de Node.js o mediante el método que prefieras.

Después:

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

El proyecto utilizará la versión definida en sus propias dependencias.

Si quieres tener disponible el comando `ng` globalmente:

```powershell
npm install -g @angular/cli@22
```

---

## 5. Instalar Java 21

After utiliza JDK 21.

En Windows recomendamos Eclipse Temurin.

Instálalo con:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Cuando termine, cierra la terminal y abre una nueva.

Comprueba:

```powershell
java -version
javac -version
```

Ambos deben mostrar Java 21.

---

## 6. Maven

No instales Maven globalmente.

Tanto el backend como el media worker incluyen Maven Wrapper:

```text
mvnw
mvnw.cmd
.mvn/
```

En Windows utilizaremos:

```powershell
.\mvnw.cmd
```

En Linux o macOS:

```bash
./mvnw
```

---

## 7. Instalar Docker

Instala Docker Desktop.

En Windows debe funcionar con WSL2.

Comprueba:

```powershell
docker --version
docker compose version
```

Para asegurarte de que Docker está funcionando realmente:

```powershell
docker run hello-world
```

---

## 8. IDE o editor

Puedes utilizar el editor que quieras.

After no depende de ninguna configuración exclusiva de VSCode, IntelliJ, Eclipse u otro IDE.

Si utilizas VSCode, se recomiendan estas extensiones:

- Angular Language Service
- ESLint
- Prettier
- Extension Pack for Java
- Spring Boot Extension Pack
- GitLens
- Error Lens
- REST Client

Son opcionales.

---

## 9. Clonar el repositorio

Clona After:

```powershell
git clone <URL_DEL_REPOSITORIO>
```

Entra en la carpeta:

```powershell
cd After
```

La estructura principal debería ser parecida a:

```text
After/
├── frontend/
├── backend/
├── media-worker/
├── docs/
├── docker-compose.yml
├── .env.example
└── .editorconfig
```

---

## 10. Preparar variables de entorno

Crea tu `.env` local a partir del ejemplo.

En PowerShell:

```powershell
Copy-Item .env.example .env
```

El archivo `.env` no debe subirse al repositorio.

Para desarrollo local, los valores incluidos en `.env.example` deberían ser suficientes salvo que necesites cambiar alguno.

---

## 11. Levantar la infraestructura local

Desde la raíz del proyecto:

```powershell
docker compose up -d
```

Esto levantará actualmente:

- PostgreSQL
- MinIO
- Media Worker

Comprueba:

```powershell
docker compose ps
```

`minio-init` puede aparecer como finalizado con código `0`.

Eso es correcto. Su trabajo consiste en preparar MinIO y terminar.

---

## 12. Preparar el frontend

Entra en:

```powershell
cd frontend
```

Instala las dependencias:

```powershell
npm install
```

Comprueba que compila:

```powershell
npm run build
```

Para arrancarlo en desarrollo:

```powershell
npm start
```

Después vuelve a la raíz:

```powershell
cd ..
```

---

## 13. Preparar el backend

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

Después vuelve a la raíz:

```powershell
cd ..
```

---

## 14. Media Worker

El media worker normalmente se ejecutará mediante Docker.

Su imagen incluye:

- Java 21
- FFmpeg
- FFprobe
- la aplicación del worker

No necesitas instalar FFmpeg en Windows.

Comprueba el estado:

```powershell
docker compose ps
```

Ver logs:

```powershell
docker compose logs media-worker
```

Si necesitas reconstruir la imagen:

```powershell
docker compose build media-worker
```

---

## 15. Comprobación final

Comprueba:

```powershell
git --version
node -v
npm -v
java -version
javac -version
docker --version
docker compose version
```

Y desde la raíz de After:

```powershell
docker compose up -d
```

Frontend:

```powershell
cd frontend
npm install
npm run build
```

Backend:

```powershell
cd ../backend
.\mvnw.cmd test
```

Si todo funciona correctamente, el entorno está preparado.

---

## 16. Flujo de trabajo

Las tareas se gestionan mediante GitHub Projects.

Cuando vayas a empezar:

1. Busca la tarea `READY` con el número más bajo.
2. Asígnatela.
3. Muévela a `IN PROGRESS`.
4. Crea una rama para esa tarea.
5. Implementa y prueba los cambios.
6. Abre una Pull Request.
7. Mueve la tarea a `REVIEW`.
8. Mientras espera revisión, coge la siguiente tarea disponible.

Si la siguiente tarea numerada está bloqueada, coge la siguiente `READY` que no lo esté.

---

## Entorno preparado

Si has llegado hasta aquí, ya puedes empezar a trabajar en After.