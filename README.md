# Rentamaq

Rentamaq es una app Android para explorar, ofrecer y reservar maquinaria en Cajamarca. Esta versión guarda los datos en el teléfono. Los mensajes, pagos y reembolsos forman parte del flujo local.

## Instalación

La app requiere Android 8.0 o posterior. Desde PowerShell en la carpeta del proyecto, compila e instala la versión de presentación con un dispositivo conectado por ADB.

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

La compilación requiere JDK 21 y Android SDK 37.1. El mapa referencial necesita conexión para cargar las teselas de OpenStreetMap.

## Recorrido de presentación

1. En **Mi cuenta**, crea un perfil de ofertante. Abre **Registrar maquinaria**, guarda un anuncio con una foto y revisa la publicación. En **Calendario y mantenimiento** puedes bloquear fechas, guardar registros y exportar una ficha en PDF.
2. Cierra sesión y crea un segundo perfil para el cliente. En **Explorar**, busca la máquina publicada, toca **Solicitar reserva** y revisa las fechas, la ventana de llegada y el importe.
3. Vuelve al perfil ofertante. Abre **Mis publicaciones → Ver reservas** para ver las solicitudes y clientes distintos de esa máquina. Acepta la solicitud desde **Recibidas**.
4. Vuelve al perfil cliente. Abre **Reservas → Mis solicitudes**, conversa con el ofertante y registra el pago. Al cancelar una reserva pagada, puedes revisar el importe reembolsable, la comisión y el plazo antes de confirmar.
5. En el perfil ofertante, abre **Recibidas → Historial → Ver seguimiento**. Inicia la revisión y registra el abono. El cliente puede consultar el resultado en su historial.

Puedes entrar con `operaciones@rentamaq.test` para recorrer las publicaciones precargadas del ofertante Maquinaria Cajamarca. En esta versión el acceso local usa el correo registrado.

## Implementación

Android nativo con Kotlin, Jetpack Compose, MVVM y Room sobre SQLite. Los cambios de estado y las validaciones de cruces de horario se ejecutan localmente. Los sectores del mapa son referenciales. Las licencias y créditos de las fotos se consultan desde **Mi cuenta → Créditos de las fotos**.
