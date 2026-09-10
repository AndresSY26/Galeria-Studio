# 📷 Galería Studio

> **Galería fotográfica profesional con gestión inteligente de almacenamiento local, bóveda privada cifrada y análisis visual con Google Gemini AI.**

---

## 🏷️ Lenguajes de Programación y Tecnologías (Versiones)

A continuación se detallan las versiones oficiales del lenguaje de programación y las principales herramientas del proyecto en formato visual:

<div align="center">

### Lenguaje Principal
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)

### Ecosistema de Desarrollo
[![Java](https://img.shields.io/badge/Java-JDK%2011%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Android SDK](https://img.shields.io/badge/Android%20SDK-Min%2024%20|%20Target%2036-34A853?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Gradle / AGP](https://img.shields.io/badge/Gradle%20AGP-9.1.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)
[![KSP](https://img.shields.io/badge/Google%20KSP-2.3.6-F88900?style=for-the-badge)](https://github.com/google/ksp)
[![Room Database](https://img.shields.io/badge/Room%20DB-2.7.0-3DDC84?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Google Gemini API](https://img.shields.io/badge/Google%20Gemini-Flash%20Vision-8E75FF?style=for-the-badge&logo=google&logoColor=white)](https://aistudio.google.com/)

</div>

---

## ✨ Características Principales

1. **Línea de Tiempo y Galería Fluida**:
   - Visualización cronológica organizada por meses y años.
   - Zoom interactivo en cuadrícula y visor fullscreen (Lightbox) de alta resolución.
   - Sincronización en tiempo real con la galería local del dispositivo vía `ContentObserver` y `DeviceMediaScanner`.

2. **Exploración Inteligente con Inteligencia Artificial**:
   - Clasificación por categorías dinámicas: Naturaleza, Retratos, Documentos, Mascotas, etc.
   - Detección de rostros y personas identificadas en fotografías.
   - Etiquetado semántico asistido por **Google Gemini AI Vision**.

3. **Bóveda Privada Cifrada**:
   - Aislamiento de fotografías sensibles en un contenedor seguro dentro de la aplicación.
   - Generación de hashes de verificación y cifrado `AES-256-GCM`.
   - Protección de privacidad con acceso controlado por PIN y biometría.

4. **Gestión Inteligente de Almacenamiento**:
   - Panel de control de espacio con desglose: Almacenamiento en dispositivo vs. Copias respaldadas.
   - Función para liberar espacio de manera segura en el dispositivo móvil.
   - Filtros rápidos por fotos pendientes de respaldo o listas para descargar.

---

## 🏛️ Arquitectura del Software

El proyecto sigue los principios de **Arquitectura Limpia (Clean Architecture)** y el patrón de diseño **MVVM (Model-View-ViewModel)** recomendado por Google para Android:

```
app/src/main/java/com/example/
├── data/
│   ├── local/            # Base de datos Room (AppDatabase, PhotoDao, DeviceMediaScanner)
│   ├── model/            # Modelos de datos (Photo, FaceDetection, StorageOverview)
│   ├── remote/           # Conectividad con GeminiService (REST API v1beta)
│   └── repository/       # Repositorio centralizado de datos (PhotoRepository)
├── ui/
│   ├── components/       # Componentes reutilizables de Jetpack Compose (BottomBar, TopBar, Lightbox)
│   ├── screens/          # Pantallas principales (MainScreen, TimelineScreen, ExploreCategoriesScreen, VaultScreen, StorageManagerScreen)
│   ├── theme/            # Tema Material Design 3 (Color, Type, Theme)
│   └── viewmodel/        # Lógica de estado reactiva (GalleryViewModel)
└── util/                 # Utilidades para compresión de imagen y manejo de URIs
```

---

## 🔐 Configuración de Variables de Entorno (.env)

El proyecto utiliza **Secrets Gradle Plugin** para gestionar credenciales y secretos de forma segura en tiempo de compilación sin exponerlos en el código fuente.

### 1. Archivos de Configuración

- **`.env.example`**: Archivo de plantilla versionado en Git que documenta todas las variables requeridas.
- **`.env`**: Archivo local que contiene los valores reales. **Nunca se sube a Git** (protegido por `.gitignore`).

### 2. Pasos para Configuración Local

1. Crea tu archivo local a partir de la plantilla:
   ```bash
   cp .env.example .env
   ```
2. Abre `.env` y coloca tu clave de API de Google Gemini:
   ```env
   GEMINI_API_KEY=tu_clave_real_aqui
   ```
3. En **Google AI Studio**:
   - No necesitas editar archivos manualmente en el servidor.
   - Ingresa en el menú **Secrets** y añade la variable `GEMINI_API_KEY` con tu clave de API.
   - El compilador inyectará automáticamente el valor en `BuildConfig.GEMINI_API_KEY`.

---

## 📦 Requisitos Previos

- **JDK**: Java Development Kit 11 o superior (Recomendado JDK 17).
- **Android SDK**: Min SDK 24 (Android 7.0) / Target SDK 36 (Android 15+).
- **Gradle**: 9.x con Android Gradle Plugin (AGP) 9.1.1.
- **KSP**: Kotlin Symbol Processing versión 2.3.6.

---

## 🚀 Compilación y Ejecución

### Compilar en Modo Debug
```bash
gradle assembleDebug
```

### Ejecutar Pruebas Unitarias y de Renderizado
```bash
# Pruebas unitarias de JVM y Robolectric
gradle :app:testDebugUnitTest

# Verificación de captura de pantalla con Roborazzi
gradle :app:verifyRoborazziDebug
```

---

## 🛡️ Seguridad y Buenas Prácticas

- **Cero Claves en Código**: Toda clave de API o secreto es consumido a través de `BuildConfig` generado por Gradle.
- **Almacenamiento Local Protegido**: La base de datos SQLite y los archivos de la bóveda residen en el directorio de datos privado de la aplicación (`context.filesDir`).
- **Control de Versiones Limpio**: El archivo `.gitignore` excluye caches de compilación, artefactos binarios (`.apk`, `.aab`), archivos `.env` y almacenes de claves (`.keystore`, `.jks`).

---

## 📄 Licencia

Este proyecto está desarrollado bajo la licencia de código abierto del proyecto para Google AI Studio.
