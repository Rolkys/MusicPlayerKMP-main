@echo off
echo ========================================
echo MusicPlayerKMP - Gradle %1
echo ========================================

rem Usa el Java que ya configuraste
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

rem Descarga gradle-wrapper.jar si falta
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo ?? Descargando gradle-wrapper.jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.5/gradle-wrapper-8.5.jar' -OutFile 'gradle/wrapper/gradle-wrapper.jar'"
)

rem Ejecuta Gradle
java -Xmx1024m -Dfile.encoding=UTF-8 -cp "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
