# After — Development Requirements

This document defines the software requirements for developing After.

## Required software

| Tool | Required version |
|---|---|
| Git | 2.x |
| Node.js | 22.x |
| npm | 10.x or compatible |
| Java JDK | 21 |
| Docker | Recent stable version |
| Docker Compose | Recent stable version |

## Project-managed tools

The following tools do not need to be installed globally.

### Maven

Both Java projects include Maven Wrapper:

```text
backend/
├── mvnw
├── mvnw.cmd
└── .mvn/

media-worker/
├── mvnw
├── mvnw.cmd
└── .mvn/
```

Use the wrapper instead of a globally installed Maven version.

### Angular CLI

The frontend contains its own Angular dependencies.

A global Angular CLI installation is optional.

### FFmpeg

FFmpeg and FFprobe are provided by the `media-worker` Docker image.

They do not need to be installed on the host machine.

## IDE

After is IDE-agnostic.

Developers may use VSCode, IntelliJ IDEA, Eclipse, Neovim or any other suitable editor.

No IDE-specific configuration is required to build, test or run the project.

## Local infrastructure

Docker Compose provides the local infrastructure required by After.

Currently:

- PostgreSQL
- MinIO
- Media Worker

## Operating systems

Development is expected to work on:

- Windows
- Linux
- macOS

Windows developers should use Docker Desktop with WSL2 support.

## Environment variables

Local configuration is defined through:

```text
.env
```

The repository provides:

```text
.env.example
```

`.env` must never be committed.