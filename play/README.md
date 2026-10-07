# Publicar Lab Pomodoro en Google Play

Todo lo que hace falta para la ficha está en esta carpeta. Los pasos que siguen son los que solo puedes hacer tú (cuentas, llaves y formularios).

| Archivo | Para qué |
|---|---|
| `listing/es-419/`, `listing/en-US/` | Título, descripción corta y completa, imagen destacada (1024×500) y 5 capturas (1080×2160) por idioma |
| `icon-512.png` | Ícono de la ficha |
| `../privacy/index.html` | Política de privacidad (se publica con GitHub Pages) |
| `../privacy/delete-account.html` | Página para pedir que se borre la cuenta |

Con GitHub Pages activo en el repositorio, las páginas quedan en:
- Política de privacidad: `https://jjas-029.github.io/k_pomodoro/privacy/`
- Borrar la cuenta: `https://jjas-029.github.io/k_pomodoro/privacy/delete-account.html`

---

## 1. Llave de subida (una sola vez) ✅ hecha el 6 de octubre de 2026

La llave está en `android/upload-key.jks` y sus contraseñas en `android/keystore.properties` (git ignora los dos). SHA-1: `E4:0D:E7:18:19:13:D8:AF:EE:D4:79:98:81:4E:63:3E:F4:E2:A4:67`.

Si algún día hay que crearla de nuevo, en la terminal de Android Studio (PowerShell), dentro de `android/`:

```powershell
& "C:\Program Files\Android\Android Studio1\jbr\bin\keytool.exe" -genkeypair -v -keystore upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

O con el asistente: *Build › Generate Signed App Bundle or APK… › Android App Bundle › Create new…* (se puede cancelar después de crear la llave). Luego `android/keystore.properties`:

```properties
storeFile=upload-key.jks
storePassword=LA_CONTRASEÑA
keyAlias=upload
keyPassword=LA_CONTRASEÑA
```

Guarda una copia de `upload-key.jks` y la contraseña fuera de la computadora. Con *Firma de apps de Google Play*, si se pierde se puede pedir una nueva, pero tarda días. Sin `keystore.properties`, el release se firma con la llave de debug (sirve para probar, Play lo rechaza).

## 2. Antes de compilar la versión para Play

1. **AdMob**: crea la app y dos bloques (banner adaptable e intersticial) y pon sus IDs en `android/gradle.properties`:
   ```properties
   admobAppId=ca-app-pub-XXXXXXXX~XXXXXXXX
   admobBannerId=ca-app-pub-XXXXXXXX/XXXXXXXX
   admobInterstitialId=ca-app-pub-XXXXXXXX/XXXXXXXX
   ```
   Sin ellos se usan los de prueba de Google. Configura también el mensaje de consentimiento (GDPR) en *Privacidad y mensajería*.
2. **Compilar**: en `android/`, `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
3. Sube `versionCode` en `app/build.gradle.kts` en cada versión nueva.

## 3. Firebase después de crear la app en Play

El inicio de sesión con Google solo funciona con las huellas de la llave que firma la app instalada:

1. Play Console › *Prueba y lanzamiento › Integridad de la app › Firma de apps*: copia el **SHA-1** de la llave de firma de apps (la de subida ya está en Firebase ✅).
2. Firebase Console › Configuración del proyecto › tu app Android › *Agregar huella digital*: agrégalo. No borres las demás (la de debug sirve para las pruebas desde Android Studio).
3. Descarga de nuevo `google-services.json` a `android/app/` y vuelve a compilar.

Publica también las reglas de `android/firebase/firestore.rules`.

## 4. Play Console

### Crear la app
- Idioma predeterminado: **Español (Latinoamérica) – es-419**; agrega **Inglés (Estados Unidos) – en-US** como traducción.
- App gratuita (con compras dentro de la app).

### Ficha de Play Store
Copia los textos y sube las imágenes de `listing/`. Categoría: **Productividad**. Correo de contacto: `ciencia.koala@gmail.com`.

### Productos (Monetizar)
- Suscripción `pro_subscription` con planes base `monthly` y `yearly` (opcional: prueba gratis de 3 días).
- Producto único `pro_lifetime`.
- Agrega tu cuenta como *tester de licencias* para comprar sin cargo real.

### Contenido de la app (Política)
| Sección | Respuesta |
|---|---|
| Política de privacidad | La URL de arriba |
| Anuncios | Sí, contiene anuncios |
| Acceso a la app | Todas las funciones están disponibles sin acceso especial. El inicio de sesión es opcional y es con cualquier cuenta de Google (no hace falta darles credenciales) |
| Clasificación del contenido | Utilidad/productividad; sin violencia, sexo, lenguaje, drogas ni apuestas. Interacción entre usuarios: solo se ven apodos en las ligas (sin chat). Compras digitales: sí |
| Público objetivo | 13 años o más (no está dirigida a niños) |
| ID de publicidad | Sí: anuncios y estadísticas |
| Borrar la cuenta | En la app: Configuración › Respaldo en la nube › Borrar mi cuenta. Web: la URL de `delete-account.html` |
| Servicio en primer plano (`specialUse`) | "Temporizador Pomodoro iniciado por el usuario: mantiene la cuenta regresiva y sus controles en la notificación mientras el plan está en curso, y se detiene al terminar o al detenerlo." Puede pedir un video corto: inicia un plan y muestra la notificación |
| Alarmas exactas (`USE_EXACT_ALARM`) | Es una app de temporizador: la alarma marca el fin exacto de cada pomodoro y descanso |

### Seguridad de los datos
¿Recopila o comparte datos? **Sí**. ¿Cifrados en tránsito? **Sí**. ¿Se pueden pedir que se borren? **Sí**.

| Tipo de dato | Recopilado | Compartido | Opcional | Para qué |
|---|---|---|---|---|
| Nombre | Sí | No | Sí (iniciar sesión) | Funcionalidad de la app, administración de la cuenta |
| Correo electrónico | Sí | No | Sí | Administración de la cuenta |
| ID de usuario | Sí | No | Sí | Funcionalidad de la app, administración de la cuenta |
| Ubicación aproximada | Sí | Sí | No | Publicidad (AdMob usa la IP). Lugares (Pro) no cuenta: las coordenadas no salen del teléfono |
| Interacciones con la app | Sí | Sí | No | Estadísticas (Firebase Analytics), publicidad (AdMob) |
| Otro contenido generado por el usuario | Sí | No | Sí | Funcionalidad de la app: respaldo del historial y apodo en las ligas |
| Diagnóstico | Sí | Sí | No | Publicidad (AdMob) |
| ID del dispositivo u otros | Sí | Sí | No | Publicidad, estadísticas |

"Compartido" se refiere a AdMob; si cambian los SDK, revisa de nuevo las [declaraciones de Google para AdMob](https://developers.google.com/admob/android/privacy/play-data-disclosure) y [Firebase](https://firebase.google.com/docs/android/play-data-disclosure).

## 5. Lanzamiento

1. Sube el `.aab` a **Prueba interna** y pruébalo: compra de Pro con tester de licencias, inicio de sesión, respaldo, ligas y borrar la cuenta.
2. **Cuentas personales nuevas**: Google exige una **prueba cerrada con al menos 12 testers durante 14 días seguidos** antes de poder publicar en producción.
3. Revisa el *Informe previo al lanzamiento* que genera Play con cada versión.
4. Producción: empieza con un porcentaje pequeño y súbelo si no hay errores.
