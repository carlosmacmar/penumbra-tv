# Penumbra · filtro nocturno para Google TV

<sub>🇬🇧 [Read in English](README.md)</sub>

App para Google TV / Android TV que oscurece la pantalla y aplica un filtro de color cálido para ver la tele de noche sin deslumbrarse.

**Descarga:** el APK listo para instalar está en la [última versión publicada](../../releases/latest).

## Funciones

| Requisito | Cómo lo resuelve Penumbra |
|---|---|
| Oscurecer la pantalla | **Atenuación** de 0 a 100 % con una capa negra sobre todo lo que se ve. Opcionalmente, baja también el **brillo del sistema** en las TV que lo admiten. |
| Filtro de color nocturno | Seis tonos (**Neutro, Cálido, Ámbar, Vela, Ocaso, Rojo**) con **intensidad** regulable. |
| Programar el oscurecimiento | **Oscurecimiento gradual**: al encenderse a la hora programada, la pantalla se oscurece poco a poco (5, 15, 30 o 60 min). |
| Encendido y apagado automáticos a una hora | **Encender a las / Apagar a las**: cada día, también si la ventana cruza la medianoche (p. ej. 22:00 → 07:00). Usa alarmas exactas y se recupera si la TV estaba en reposo, se reinició o cambió la hora. |
| Arranque automático | **Activar al encender la TV**: el filtro se activa solo cada vez que la TV se enciende o se reinicia. |
| Interfaz bonita y minimalista | Pantalla única, oscura, pensada para el mando: reloj grande, botón de encendido con halo y un color de acento que sigue al tono elegido. |

Los cambios se ven al instante: la propia interfaz queda bajo el filtro.

## Manejo con el mando

- **↑ ↓**: moverse entre opciones.
- **← →**: cambiar el valor de la opción enfocada (manténlas pulsadas para ir más rápido con las horas).
- **OK**: activar o desactivar.
- **Atrás**: vuelve al botón de encendido; una segunda pulsación sale de la app.

## Compilar

Requiere JDK 17 o 21 (las versiones más nuevas de Java no funcionan con Gradle 8.11) y el SDK de Android con la plataforma 35.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew assembleRelease
```

El APK queda en `app/build/outputs/apk/release/app-release.apk`.

Para firmarlo con tu propia clave, copia `keystore.properties.example` como `keystore.properties` y rellena los datos de tu keystore. Ese archivo y las claves nunca se suben a git. Sin él, el APK se firma con la clave de depuración, que solo sirve para pruebas.

Para abrir el proyecto en Android Studio hace falta la versión Ladybug (2024.2) o posterior, por el Android Gradle Plugin 8.7.

## Instalar en la TV

1. En la TV: **Ajustes › Sistema › Información** y pulsa 7 veces sobre **Compilación del SO de Android TV** para activar las opciones de desarrollador.
2. En **Ajustes › Sistema › Opciones para desarrolladores**, activa la **depuración por USB** (o la **depuración inalámbrica** en Google TV con Android 14).
3. Desde el ordenador, en la misma red:

   ```bash
   adb connect IP_DE_LA_TV:5555
   ```

   Con depuración inalámbrica se usa antes `adb pair IP:PUERTO CÓDIGO` con los datos que muestra la TV.

4. Instala el APK:

   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

## Primera configuración

Penumbra necesita **un** permiso para dibujar el filtro encima de otras apps. La app lo explica al abrirla:

- **Recomendado: servicio de accesibilidad.** Pulsa **Abrir accesibilidad › Servicios › Penumbra › Activar › Aceptar**. Funciona desde el propio mando, el filtro cubre también los menús del sistema y Android lo vuelve a iniciar en cada arranque.

  Android muestra un aviso genérico («puede recopilar el texto que escribes…») para cualquier servicio de accesibilidad. Penumbra no lee nada: el servicio no recibe eventos ni tiene acceso al contenido de las ventanas (ver `res/xml/accessibility_service_config.xml`).

- **Alternativa: «Mostrar sobre otras apps».** Muchas Google TV ocultan este ajuste, así que se concede con ADB:

  ```bash
  adb shell appops set com.penumbra.tv SYSTEM_ALERT_WINDOW allow
  ```

- **Opcional, brillo del sistema** (solo TV compatibles):

  ```bash
  adb shell appops set com.penumbra.tv WRITE_SETTINGS allow
  ```

## Cómo funciona

- **Filtro**: una ventana a pantalla completa, transparente al mando, que pinta una capa de color y encima una capa negra. La aloja el servicio de accesibilidad (`TYPE_ACCESSIBILITY_OVERLAY`) o, si no está activo, un servicio en primer plano con permiso de superposición (`TYPE_APPLICATION_OVERLAY`).
- **Programación**: cada hora de inicio o fin equivale a pulsar el botón de encendido a esa hora. Los cambios manuales se respetan hasta la siguiente hora programada. Una alarma exacta despierta la app en cada cambio, y además el estado se comprueba cada minuto, al encender la pantalla, al arrancar y al cambiar la hora, así que no se pierde ningún cambio aunque la TV estuviera en reposo.
- **Estado**: todo se guarda en `SharedPreferences` a través de `FilterStore`, que es la única fuente de verdad que observan la interfaz y el filtro.

## Limitaciones

- Una capa superpuesta reduce la **luminosidad que se percibe**, pero no puede apagar la retroiluminación del panel. Para eso está la opción de brillo del sistema, que muchas TV ignoran porque controlan la imagen con sus propios ajustes.
- Los tonos de color aclaran ligeramente los negros. Les pasa a todos los filtros de pantalla de Android que funcionan por superposición.
- **Google Play**: el permiso `USE_EXACT_ALARM` y el uso de la API de accesibilidad requieren una justificación en la ficha de la app. Para instalarla por tu cuenta no hace falta nada.

## Estructura

```
app/src/main/java/com/penumbra/tv/
├── PenumbraApp.kt              Inicializa el estado y el canal de notificación
├── core/
│   ├── FilterState.kt          Modelo: atenuación, tono, horario, rampa gradual
│   ├── FilterStore.kt          Estado persistente y observable (fuente única de verdad)
│   ├── Schedule.kt             Cálculo de la ventana diaria (cruza medianoche, cambios de hora)
│   ├── Alarms.kt               Alarma exacta para el siguiente cambio
│   ├── PenumbraController.kt   Todas las acciones: encender, ajustes, horario, arranque…
│   └── SystemBrightness.kt     Brillo real opcional, con restauración del valor original
├── overlay/
│   ├── FilterView.kt           Dibuja las capas con transiciones suaves
│   ├── OverlayRenderer.kt      Ventana del filtro, rampa gradual y eventos del sistema
│   ├── OverlayAccessibilityService.kt   Anfitrión preferido
│   ├── OverlayService.kt       Anfitrión alternativo (servicio en primer plano)
│   └── OverlayHost.kt          Elige anfitrión según los permisos
├── receiver/                   Alarmas, arranque, actualización y cambios de hora
└── ui/                         Interfaz Compose for TV
```

Tests unitarios del horario y del modelo: `./gradlew testDebugUnitTest`.
