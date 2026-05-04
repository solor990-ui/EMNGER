# EMNGER

Android app para gestión masiva de CPEs Ubiquiti para WISPs.

Permite:
- Gestión de múltiples CPEs via SSH
- Cambios bulk en lote
- Sistema Pending Changes (Test de 3 min)
- Interfaz estilo Ubiquiti

## Build
[![Build APK](https://github.com/solor990-ui/EMNGER/actions/workflows/build.yml/badge.svg)](https://github.com/solor990-ui/EMNGER/actions)

## Especificaciones

- Target SDK: 35 (Android 15)
- Min SDK: 33 (Android 13+)
- Idioma: Kotlin

## Tech Stack

- Kotlin
- Jetpack Compose
- MVVM Architecture
- JSch for SSH

## Construir

```bash
./gradlew assembleDebug
```

## Changelog

- v0.1.0 - Versión inicial en desarrollo