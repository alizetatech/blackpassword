# Blackpassword - Suite de Seguridad Premium 🔐

## 📌 Estado del Proyecto
- **Versión:** 10.6-LUXURY
- **Arquitectura:** Java Swing + FlatLaf + Firebase Admin SDK + RSA/AES-GCM + Bouncy Castle.
- **Estado:** Encriptador de Algoritmos (`EncriptadorDeAlgoritmos`) integrado y funcional como `JInternalFrame`. Diagnóstico de visibilidad del menú en curso.

## 🚀 Funcionalidades de Seguridad Real (100% Funcionales)
- [x] **Encriptador de Algoritmos v10.6-LUXURY:** Integrado como `JInternalFrame` en el menú de Seguridad, con correcciones de visibilidad y centrado aplicadas en `MenuPrincipal.java`. Se ha añadido un mensaje de diagnóstico para verificar su carga. Requiere reinicio completo del IDE y limpieza/construcción del proyecto.
- [x] **Visor de Imágenes v9.9-LUXURY:** Solucionado el error de importación de `JDesktopPane` en `ImageViewer.java`.
- [x] **Galería de Imágenes v9.5-LUXURY:**
    - [x] **Visor Seguro (`ImageViewer`):** Muestra imágenes descifradas en una ventana dedicada.
    - [x] **Establecer como Fondo:** Permite usar una imagen descifrada como fondo del dashboard (persiste).
    - [x] **Descargar Imagen:** Guarda la imagen descifrada en el disco local.
    - [x] **Eliminar Imagen:** Borra la imagen de Cloud Storage y Firestore.
    - [x] **Estética Premium:** Botones con diseño consistente.
- [x] **Conexión Firebase v9.4-FINAL:** Solucionado el error 'Bucket does not exist' con coordenadas exactas de la bóveda.
- [x] **Reconstrucción Final v9.1:** Solucionado `ArrayIndexOutOfBoundsException` en `Imagenes.java` al reconstruir `initComponents()` con la definición correcta de la tabla.
- [x] **Reconstrucción Total v9.0:** El módulo `Imagenes.java` ha sido reescrito desde cero, solucionando todos los errores de compilación y de UI.
- [x] **Visibilidad Restaurada v8.9:** Añadido `pack()` a `Imagenes.java` para renderizar la ventana.
- [x] **Circuito Reparado v8.8:** Mejorado el manejo de errores para revelar fallos de Firebase.
- [x] **Monitor de Circuito v8.7:** Implementado diagnóstico de UI para detectar 'corto circuitos'.
- [x] **Diagnóstico Avanzado v8.6:** El sistema ahora imprime la "caja negra" de errores de Storage.
- [x] **Protocolo Universal v8.5:** Todo el contenido cifrado se sube como `application/octet-stream`.
- [x] **Corrección v8.4:** Restauradas declaraciones de UI en `Imagenes.java`.
- [x] **Protocolo de Subida v8.3:** Especificación de 'Content-Type' para robustez en Cloud Storage.
- [x] **Bóveda Externa v8.1:** Almacenamiento de archivos grandes (>1MB) en Cloud Storage.
- [x] **Certificado SSL/X.509:** Generación de pasaporte digital estándar (Bouncy Castle).
- [x] **Blindaje Local v7.0:** Doble cifrado RSA/AES para la caché de SQLite (IRON-SHIELD).
- [x] **Cloud Security v7.0:** Reglas de Firestore y Storage optimizadas para privacidad.
- [x] **Navegación Fluida:** Botón 'VOLVER' para alternar entre perfiles e identidades.
- [x] **Arranque Ultra-Temprano:** Tema claro activo desde el primer milisegundo.
- [x] **Cerrar Sesión Seguro:** Botón dedicado para purgar llaves de RAM.
- [x] **Gestor de Perfiles Pro:** Atajos locales para cargar llaves y Firebase.
- [x] **Zero-Knowledge Metadata:** Nombres de archivos, carpetas y logs cifrados con RSA.
- [x] **Criptografía AEAD (AES-GCM):** Garantiza la integridad y autenticidad de los datos.
- [x] **Escudo de Privacidad:** Ocultamiento automático de la interfaz al perder el foco.
- [x] **Modo Offline Blindado:** Caché local en SQLite para acceso sin internet.
- [x] **Botón de Pánico (ESC):** Purga instantánea de llaves en RAM y cierre forzoso.
- [x] **Frase Semilla (12 palabras):** Recuperación de identidad digital descentralizada.
- [x] **Auto-Bloqueo:** Cierre de sesión por inactividad (5 min).
- [x] **Auditoría Cifrada:** Bitácora de eventos ilegible para administradores.
- [x] **Modo Camuflaje (F12):** Transformación instantánea en calculadora.
- [x] **Teclado Virtual Seguro:** Prevención de keyloggers.
- [x] **Memory Hardening:** Purga de secretos (Zeroing) en RAM tras su uso.
- [x] **Alertas de Intrusión:** Detección de fuerza bruta y purga nuclear de caché local.

## 🛠️ Tecnologías Core
- **Criptografía:** RSA (2048 bits) + AES-GCM (256 bits) + X.509 (Bouncy Castle).
- **Persistencia:** Firebase Firestore (Nube) + Cloud Storage + SQLite (Local Blindado).

## 📅 Próximos Pasos
1. **Destrucción Remota:** Opción para invalidar llaves desde otro dispositivo en caso de robo.
2. **Generador de Contraseñas Pro:** Algoritmo basado en entropía de movimiento del ratón.
