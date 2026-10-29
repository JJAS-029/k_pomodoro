# Publicar Lab Pomodoro en Google Play

Todo lo que hace falta para la ficha está en esta carpeta. Los pasos que siguen son los que solo puedes hacer tú (cuentas, llaves y formularios).

| Archivo | Para qué |
|---|---|
| `listing/es-419/`, `listing/en-US/` | Título, descripción corta y completa, imagen destacada (1024×500) y 8 capturas (1080×2160) por idioma: temporizador, plan, tabla, ficha, sintetizador, estadísticas, tu año y medallas |
| `icon-512.png` | Ícono de la ficha |
| `listing/*/wear-screenshots/` | 3 capturas del reloj por idioma (454×454): app, tile y complicación |
| `../privacy/index.html` | Política de privacidad (se publica con GitHub Pages) |
| `../privacy/delete-account.html` | Página para pedir que se borre la cuenta |

Con GitHub Pages activo en el repositorio, las páginas quedan en:
- Política de privacidad: `https://jjas-029.github.io/k_pomodoro/privacy/`
- Borrar la cuenta: `https://jjas-029.github.io/k_pomodoro/privacy/delete-account.html`

---

## 1. Llave de subida (una sola vez) ✅ hecha el 6 de octubre de 2026

La llave que Play tiene registrada es la del asistente de Android Studio: `key/key` (alias `key`), SHA-1 `69:F6:74:6A:BB:E3:79:24:56:31:4C:47:49:93:7B:96:3F:1C:FC:D9`. `android/keystore.properties` apunta a ella con `storeFile=../key/key` (en `.properties` usa `/`, no `\`). Git ignora los dos archivos. `android/upload-key.jks` (SHA-1 `E4:0D:…`) se creó antes y **no** sirve para Play.

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

1. **AdMob** ✅: app `ca-app-pub-9843969378937419~7427910282`, banner `…/8985543694` e intersticial `…/4586124520`, en el bloque `release` de `app/build.gradle.kts`. La versión debug usa siempre los IDs de prueba de Google (hacer clic en anuncios reales propios puede suspender la cuenta). Los mensajes de RGPD y de estados de EE. UU. ya están publicados en *Privacidad y mensajería*. Pendiente al publicar: `app-ads.txt` en la raíz del sitio web de desarrollador.
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
| Alarmas exactas | La app ya **no** usa `USE_EXACT_ALARM` (es solo para despertadores y calendarios). Usa `SCHEDULE_EXACT_ALARM`, que en Android 14+ el usuario concede: la app lo pide una vez al primer plan y deja la opción en Configuración › General. Si Play pregunta por alarmas exactas, la respuesta es que no se usa `USE_EXACT_ALARM` |

### Seguridad de los datos
Pantalla por pantalla, con cada opción a marcar: [seguridad-de-los-datos.md](seguridad-de-los-datos.md).

## 5. Lanzamiento

1. Sube el `.aab` a **Prueba interna** y pruébalo: compra de Pro con tester de licencias, inicio de sesión, respaldo, ligas y borrar la cuenta.
2. **Cuentas personales nuevas**: Google exige una **prueba cerrada con al menos 12 testers durante 14 días seguidos** antes de poder publicar en producción.
3. Revisa el *Informe previo al lanzamiento* que genera Play con cada versión.
4. Producción: empieza con un porcentaje pequeño y súbelo si no hay errores.

## 6. App del reloj (Wear OS)

La app del reloj vive en `android/wear` y se publica en la **misma app** de Play Console (mismo paquete, misma llave). Cada versión lleva dos paquetes:

| Paquete | Archivo | versionCode |
|---|---|---|
| Teléfono | `android/app/build/outputs/bundle/release/app-release.aab` | 6 (versión 1.4) |
| Reloj | `android/wear/build/outputs/bundle/release/wear-release.aab` | 1000006 (1.4) |

Para compilar los dos: `./gradlew :app:bundleRelease :wear:bundleRelease` dentro de `android/`. El versionCode del reloj es 1 000 000 más el del teléfono; al subir el del teléfono, súbelo también en `android/wear/build.gradle.kts`.

**Solo la primera vez:**
1. Play Console › Lab Pomodoro › **Configuración › Configuración avanzada › Formatos** › **Agregar formato** › **Wear OS**. Acepta los requisitos.
2. **Presencia en la tienda › Ficha de Play Store principal**: en la sección **Wear OS**, sube las capturas de `listing/es-419/wear-screenshots/` (y las de `en-US` en la ficha en inglés).
3. **Pruebas › Prueba cerrada**: aparece una pista **"Wear OS"** aparte. Crea una versión ahí con `wear-release.aab` y agrega a los mismos testers.
4. La primera versión del reloj pasa por una revisión extra de Wear OS (calidad de la app en el reloj); puede tardar unos días más.

**Cada versión:** sube `app-release.aab` a la pista del teléfono y `wear-release.aab` a la pista de Wear OS, con las mismas notas.
