@echo off
REM ============================================================================
REM TV Closer v3.0 - Script de compilación mejorado para Windows
REM Version 2 - Sin dependencia de gradlew.bat
REM ============================================================================

setlocal enabledelayedexpansion

echo.
echo ============================================================================
echo TV CLOSER v3.0 AutoActivate - Compilacion Automatica v2
echo ============================================================================
echo.

REM Detectar si estamos en la carpeta correcta
if not exist "app\src\main\AndroidManifest.xml" (
    echo ERROR: No estoy en la carpeta correcta del proyecto
    echo.
    echo Necesito que ejecutes este script DENTRO de:
    echo TV_Closer_v3.0_AutoActivate\
    echo.
    echo Mueve este script a esa carpeta y ejecuta de nuevo.
    echo.
    pause
    exit /b 1
)

echo [1/4] Verificando Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ERROR: Java no esta instalado
    echo.
    echo Descarga Java desde:
    echo https://www.oracle.com/java/technologies/downloads/
    echo.
    echo Instalalo, reinicia, y vuelve a ejecutar este script.
    echo.
    pause
    exit /b 1
)
echo ✓ Java encontrado

echo.
echo [2/4] Descargando Gradle...
if not exist "..\gradle-8.2.0-bin.zip" (
    echo Descargando Gradle 8.2.0 (puede tardar 1-2 minutos)...
    powershell -Command "(New-Object Net.ServicePointManager).SecurityProtocol = 'tls12'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.2.0-bin.zip' -OutFile '..\gradle-8.2.0-bin.zip'" 2>nul
    if errorlevel 1 (
        echo.
        echo ERROR: No se pudo descargar Gradle
        echo Comprueba tu conexion a internet
        echo.
        pause
        exit /b 1
    )
    echo ✓ Descargado
) else (
    echo ✓ Ya existe
)

echo.
echo [3/4] Extrayendo Gradle...
if not exist "..\gradle-8.2.0" (
    powershell -Command "Expand-Archive -Path '..\gradle-8.2.0-bin.zip' -DestinationPath '..' -Force" 2>nul
    if errorlevel 1 (
        echo ERROR: No se pudo extraer Gradle
        pause
        exit /b 1
    )
    echo ✓ Extraido
) else (
    echo ✓ Ya existe
)

echo.
echo [4/4] Compilando proyecto (esto tarda 2-3 minutos)...
echo.

REM Configurar variables de entorno
set GRADLE_HOME=..\gradle-8.2.0
set PATH=%GRADLE_HOME%\bin;%PATH%

REM Compilar
call gradle.bat assembleDebug

if errorlevel 1 (
    echo.
    echo ============================================================================
    echo ERROR DURANTE LA COMPILACION
    echo ============================================================================
    echo.
    echo Si ves "build failed", puede ser por:
    echo - Falta de espacio en disco (necesita 2-3 GB)
    echo - Problema de permisos
    echo - Conexion a internet interrumpida
    echo.
    echo Intenta:
    echo 1. Cierra este script
    echo 2. Abre CMD como Administrador
    echo 3. Navega a esta carpeta: cd C:\Users\viD\Desktop\Android Aplicacion\TV_Closer_v3.0_AutoActivate
    echo 4. Ejecuta de nuevo: COMPILAR_WINDOWS_v2.bat
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo COMPILACION COMPLETADA CON EXITO
echo ============================================================================
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo APK GENERADO: app\build\outputs\apk\debug\app-debug.apk
    echo Tamaño: (~2.5 MB)
    echo.
    echo QUE HACER AHORA:
    echo.
    echo 1. Abre esta carpeta:
    echo    app\build\outputs\apk\debug\
    echo.
    echo 2. Encontraras el archivo: app-debug.apk
    echo.
    echo 3. Copia este archivo a un USB
    echo.
    echo 4. Conecta el USB a tu TV
    echo.
    echo 5. En el TV:
    echo    - Abre File Manager
    echo    - Ve a USB
    echo    - Abre app-debug.apk
    echo    - Instala
    echo    - LISTO!
    echo.
) else (
    echo ERROR: El APK no se genero
    echo Comprueba los errores arriba
)

echo.
echo Presiona cualquier tecla para cerrar...
pause
