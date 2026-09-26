# TV Closer v3.0 AutoActivate

**Aplicación Android para cerrar apps en Xiaomi TV sin necesidad de ROOT, Device Owner, o ADB**

## Descripción

TV Closer v3.0 es una versión mejorada de la aplicación que permite cerrar/suspender aplicaciones en Android TV (Xiaomi, Vontar, etc.) sin necesidad de permisos de administrador previos ni configuración por ADB.

### Mejoras en v3.0

| Aspecto | v2.0 | v3.0 |
|--------|------|------|
| Requiere ADB | ✅ | ❌ |
| Requiere PC | ✅ | ❌ |
| Requiere Device Owner | ✅ | ❌* |
| UI Principal | ❌ (NativeActivity) | ✅ (Activity Java) |
| Accessibility Service | ❌ | ✅ |
| Autoupdate de estado | ❌ | ✅ |
| Método alternativo | ❌ | ✅ |

*Device Owner sigue siendo soportado opcionalmente si ya está activado

---

## CARACTERÍSTICA PRINCIPAL: Sin necesidad de ADB

### Antes (v2.0):
1. Descargar ADB
2. Conectar PC a TV
3. Ejecutar comandos ADB
4. Instalar APK
5. Activar Device Owner
6. Finalmente usar la app ⏱️ 15 minutos

### Ahora (v3.0):
1. Instalar el APK
2. Abrir la app
3. Tocar "Activar ahora"
4. Activar Accessibility Service (desde Ajustes)
5. ¡Listo! ⏱️ 2 minutos

---

## REQUISITOS

- **Dispositivo**: Xiaomi TV / Android TV 7.0+ (API 24+)
- **Compilación**: Android Studio 2024.x
- **JDK**: 11 o superior
- **SDK**: API Level 35 (compatible con 24+)

---

## COMPILACIÓN

### Android Studio
```bash
1. Abrir proyecto en Android Studio
2. Build → Build APK(s)
3. El APK está en app/build/outputs/apk/debug/
```

### Línea de comandos
```bash
./gradlew assembleDebug
# o
./gradlew assembleRelease
```

Ver `COMPILACION_E_INSTALACION.md` para instrucciones detalladas.

---

## USO

### Opción A: Modo Accessibility (SIN ADB) ⭐ NUEVO

```bash
adb install -r app-debug.apk

# En el TV:
1. Abre TV Closer
2. Toca "Activar ahora"
3. Ajustes → Accesibilidad → Activa TV Closer
4. Regresa a la app
5. Toca "Cerrar aplicaciones"
```

### Opción B: Modo Device Owner (Con ADB)

```bash
adb install -r app-debug.apk
adb shell dpm set-device-owner com.vid.tvcloser2/android.app.admin.DeviceAdminReceiver
adb disconnect

# En el TV:
Abre TV Closer y usa normalmente
```

---

## ARQUITECTURA

```
com.vid.tvcloser2
├── MainActivity (Activity principal)
│   ├── Detecta permisos disponibles
│   ├── UI con estado y botón de acción
│   └── Gestiona flujo de activación
│
├── AppManager (Device Owner)
│   ├── setPackagesSuspended() [más rápido]
│   └── Requiere Device Owner previo
│
├── AppCloserAccessibilityService (NUEVO)
│   ├── Acceso sin permisos previos
│   ├── killBackgroundProcesses()
│   └── Altamente compatible
│
└── DeviceAdminReceiver (Receptor)
    └── Componente Device Admin
```

---

## CÓDIGO

### Archivos principales (406 líneas Java)

- `MainActivity.java` (154 líneas)
- `AppCloserAccessibilityService.java` (118 líneas)
- `AppManager.java` (111 líneas)
- `DeviceAdminReceiver.java` (23 líneas)

### Recursos

- `activity_main.xml` - Interfaz gráfica
- `device_admin.xml` - Configuración Device Admin
- `accessibility_service_config.xml` - Configuración Accessibility
- `strings.xml` - Textos multilengua

### Configuración

- `AndroidManifest.xml` - Manifest actualizado
- `build.gradle` - Gradle sin NDK (simplificado)
- `settings.gradle` - Configuración de proyecto

---

## PERMISOS

```xml
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.GET_RUNNING_APPS" />
<uses-permission android:name="android.permission.KILL_BACKGROUND_PROCESSES" />
```

---

## COMPATIBILIDAD

✅ Xiaomi TV (MiTV-MSSP3, etc.)
✅ Vontar TV
✅ Android TV (cualquier marca)
✅ Google TV
✅ API 24+ (Android 7.0+)
✅ arm64-v8a, armeabi-v7a, x86_64

---

## SEGURIDAD

- ✅ Sin ROOT
- ✅ Sin modificaciones de sistema
- ✅ Totalmente reversible
- ✅ Sin telemetría
- ✅ Sin conexiones externas
- ✅ Código abierto

---

## CAMBIOS RESPECTO A v2.0

### Removido
- ❌ Código C nativo (`tvcloser2.c`)
- ❌ NativeActivity
- ❌ CMake/NDK

### Agregado
- ✅ MainActivity.java
- ✅ AppCloserAccessibilityService.java
- ✅ Interfaz gráfica (layout)
- ✅ Activación sin ADB
- ✅ Automatic permission detection
- ✅ AppCompat theme

### Mejorado
- ✅ Compilación más simple (solo Gradle)
- ✅ Mantenimiento más fácil
- ✅ Código más legible
- ✅ Más flexible

---

## ESTRUCTURA DEL PROYECTO

```
tv_closer_new/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/vid/tvcloser2/
│       │   ├── MainActivity.java
│       │   ├── AppManager.java
│       │   ├── AppCloserAccessibilityService.java
│       │   └── DeviceAdminReceiver.java
│       ├── res/
│       │   ├── layout/
│       │   │   └── activity_main.xml
│       │   ├── values/
│       │   │   └── strings.xml
│       │   └── xml/
│       │       ├── device_admin.xml
│       │       └── accessibility_service_config.xml
│       └── AndroidManifest.xml
├── build.gradle
├── settings.gradle
├── README.md
└── COMPILACION_E_INSTALACION.md
```

---

## COMPILACIÓN ESPERADA

```
APK Size: ~2.5 MB
Min SDK: 24 (Android 7.0)
Target SDK: 35 (Android 15)
Version: 3.0-AutoActivate
Package: com.vid.tvcloser2
Signature: Debug (auto-generada)
```

---

## FAQ

**¿Necesito ADB para la v3.0?**
No con Accessibility Service. Device Owner todavía lo requiere si quieres ese modo.

**¿Es más lento que v2.0?**
Device Owner es igual de rápido. Accessibility Service es ligeramente más lento (~100ms).

**¿Puedo usar ambos modos?**
Sí. La app intenta Device Owner primero, y si no está activado, ofrece Accessibility.

**¿Qué pasa si ambos están desactivados?**
La app muestra "Activar ahora" y guía al usuario a través del proceso.

---

## DESARROLLO FUTURO

Posibles mejoras para futuras versiones:
- [ ] Interfaz de selección de apps (elegir qué cerrar)
- [ ] Perfiles de cierre automático
- [ ] Historial de apps cerradas
- [ ] Widget en launcher
- [ ] Integración con Google Assistant

---

## LICENCIA

Código propietario de Basetis / viD (David Fuente Quintana)

---

## AUTOR

**viD** (David Fuente Quintana)
Basetis - Senior Engineer Consultant

---

## SOPORTE

Para problemas de compilación, ver `COMPILACION_E_INSTALACION.md`

Para problemas en ejecución:
1. Verifica Accessibility Service está activado
2. Reinicia el TV
3. Reinstala el APK

---

**TV Closer v3.0 AutoActivate - Cierra apps sin ADB** 🎉
