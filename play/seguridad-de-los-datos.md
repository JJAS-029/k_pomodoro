# Seguridad de los datos: cómo llenar cada pantalla

Play Console › Política › Contenido de la app › **Seguridad de los datos**. Las opciones están escritas tal como aparecen en la consola.

De dónde sale cada dato:
- **Firebase** (Analytics, Auth, Firestore) trabaja para nosotros como proveedor de servicio: lo que recibe cuenta como *recopilado*, no como *compartido*.
- **AdMob** usa los datos para sus propios anuncios: eso sí es *compartido*.
- **Lugares (Pro)**: las coordenadas se guardan solo en el teléfono y nunca llegan a nuestros servidores; el único envío es la consulta del nombre del lugar al servicio de direcciones de Android (ver Ubicación aproximada).

---

## Preguntas generales

| Pregunta | Respuesta |
|---|---|
| ¿Tu app recopila o comparte alguno de los tipos de datos que se requieren de los usuarios? | **Sí** |
| ¿Todos los datos de los usuarios que recopila tu app se encriptan en tránsito? | **Sí** |
| ¿Cuál de los siguientes métodos de creación de cuentas admite la app? | Solo **OAuth** |
| URL para borrar la cuenta | `https://jjas-029.github.io/k_pomodoro/privacy/delete-account.html` |
| ¿Los usuarios pueden pedir que se borren sus datos sin borrar la cuenta? | **No** |
| Insignias adicionales | No marcar nada |

## Tipos de datos (pantalla de selección)

Marca solo estos y deja todo lo demás vacío:

- **Ubicación**: Ubicación aproximada
- **Información personal**: Nombre · Dirección de correo electrónico · IDs de usuario
- **Actividad en la app**: Interacciones con la app · Otro contenido generado por el usuario
- **Información y rendimiento de la app**: Diagnóstico
- **IDs de dispositivo u otros IDs**: IDs de dispositivo u otros IDs

---

## Detalle de cada dato

En cada uno, Play hace hasta cinco preguntas:
1. ¿Recopilados, compartidos o ambos?
2. ¿Efímero?
3. ¿Necesario u opcional?
4. ¿Por qué se recopilan?
5. ¿Para qué se comparten? Solo aparece si marcaste *Compartidos*.

### 1. Ubicación aproximada
*AdMob y Analytics la deducen de la dirección IP. Además, con Lugares (Pro) la app le pasa las coordenadas al servicio de direcciones de Android para sugerir el nombre del lugar (en teléfonos con Google lo resuelve Google como proveedor de servicio; no es "compartir").*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ✅ Compartidos |
| ¿Se procesan de forma efímera? | **No**, los datos recopilados no se procesan de forma efímera |
| ¿Necesarios u opcionales? | **La recopilación de datos es necesaria** |
| ¿Por qué se recopilan? | ✅ Funciones de la app · ✅ Estadísticas · ✅ Publicidad o marketing |
| ¿Para qué se usan y comparten? | ✅ Publicidad o marketing (solo AdMob comparte) |

### 2. Nombre
*Del perfil de Google al iniciar sesión (opcional).*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ❌ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **Los usuarios pueden decidir si los datos se recopilan o no** |
| ¿Por qué se recopilan? | ✅ Funciones de la app · ✅ Administración de la cuenta |

### 3. Dirección de correo electrónico
*De la cuenta de Google al iniciar sesión (opcional).*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ❌ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **Los usuarios pueden decidir si los datos se recopilan o no** |
| ¿Por qué se recopilan? | ✅ Funciones de la app · ✅ Administración de la cuenta |

### 4. IDs de usuario
*El identificador de Firebase de quien inicia sesión.*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ❌ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **Los usuarios pueden decidir si los datos se recopilan o no** |
| ¿Por qué se recopilan? | ✅ Funciones de la app · ✅ Administración de la cuenta |

### 5. Interacciones con la app
*Eventos de Analytics (pomodoro completado, inicio de sesión) y uso que mide AdMob.*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ✅ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **La recopilación de datos es necesaria** |
| ¿Por qué se recopilan? | ✅ Estadísticas · ✅ Publicidad o marketing |
| ¿Para qué se usan y comparten? | ✅ Publicidad o marketing |

### 6. Otro contenido generado por el usuario
*El respaldo en la nube (historial, elementos, ajustes, nombres de lugares) y el apodo de la liga.*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ❌ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **Los usuarios pueden decidir si los datos se recopilan o no** |
| ¿Por qué se recopilan? | ✅ Funciones de la app |

### 7. Diagnóstico
*Datos de rendimiento que recopila el SDK de AdMob.*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ✅ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **La recopilación de datos es necesaria** |
| ¿Por qué se recopilan? | ✅ Estadísticas · ✅ Publicidad o marketing · ✅ Seguridad, cumplimiento y prevención de fraudes |
| ¿Para qué se usan y comparten? | ✅ Publicidad o marketing · ✅ Seguridad, cumplimiento y prevención de fraudes |

### 8. IDs de dispositivo u otros IDs
*ID de publicidad (AdMob) e ID de la instalación (Analytics).*

| Pregunta | Respuesta |
|---|---|
| ¿Se recopilan, se comparten o ambas? | ✅ Recopilados · ✅ Compartidos |
| ¿Se procesan de forma efímera? | **No** |
| ¿Necesarios u opcionales? | **La recopilación de datos es necesaria** |
| ¿Por qué se recopilan? | ✅ Estadísticas · ✅ Publicidad o marketing · ✅ Seguridad, cumplimiento y prevención de fraudes |
| ¿Para qué se usan y comparten? | ✅ Publicidad o marketing · ✅ Seguridad, cumplimiento y prevención de fraudes |

---

## Cuándo revisarlo de nuevo

Si se agrega o quita un SDK (por ejemplo, Crashlytics), o si un dato nuevo sale del teléfono, actualiza este archivo y el formulario. Las declaraciones oficiales de los SDK están en:
- AdMob: https://developers.google.com/admob/android/privacy/play-data-disclosure
- Firebase: https://firebase.google.com/docs/android/play-data-disclosure
