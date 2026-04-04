# Blackpassword - Suite de Seguridad Premium 🔐

## 📌 Estado del Proyecto
- **Versión:** 16.5-COMMAND-CENTER
- **Arquitectura:** Java Swing + FlatLaf + Firebase Admin SDK + RSA/AES-GCM + Bouncy Castle + ZXing + Typing DNA.
- **Estado:** Diseño Ergonómico Lateral y Acceso Total Ilimitado.

## 🚀 Funcionalidades de Seguridad Real (100% Funcionales)
- [x] **Diseño Command Center (v16.5):** Bóveda rediseñada con panel de control lateral izquierdo para una ergonomía superior y gestión rápida de credenciales.
- [x] **Acceso Total Ilimitado:** Eliminación de cualquier restricción de suscripción. Todas las funciones (QR, GEN, Imágenes) están abiertas para todos los usuarios.
- [x] **Destrucción Remota (Kill Switch):** Capacidad de invalidar la cuenta instantáneamente desde el panel de Identidad o vía Firebase, provocando la purga de datos locales.
- [x] **Biometría Conductual (Typing DNA):** Validación del ritmo de tecleo durante el login con algoritmo de Distancia de Manhattan. Bloqueo automático si la similitud es < 60%.
- [x] **Rastreo de Ubicación Segura:** Detección de IP y ciudad en cada inicio de sesión con alertas por cambio de ubicación sospechoso.
- [x] **Auditoría Avanzada:** Sistema de detección de intrusos que reporta intentos de carga de llaves RSA no vinculadas y activa la purga inmediata de datos locales.
- [x] **Generador de Contraseñas Pro:** Algoritmo de grado militar que utiliza entropía recolectada del movimiento del ratón.
- [x] **Protocolo Zero-Trace QR:** Generación de códigos QR con autodestrucción a los 15 segundos y sobreescritura de raster en RAM.
- [x] **Bóveda de Contraseñas:** Gestión de credenciales con **Copiado Seguro de Usuario/Pass** y validación por PIN de 4 dígitos.
- [x] **Cifrador de Archivos AES-256 v1.3:** Herramienta de cifrado simétrico local con PBKDF2 y opción de **Destrucción Segura** del archivo original.
- [x] **Validación de Identidad Cruzada:** Blindaje Zero-Trust que compara hashes de Email y UID entre el Token JWT, Firestore y la RAM.
- [x] **RSA-Binding (Nivel 2):** Vinculación física de la cuenta con la llave RSA mediante el claim `pubkey_hash`.
- [x] **Custom Claims de Firebase:** Inyección de roles, estado de cuenta y huellas digitales criptográficas directamente en el token.
- [x] **Visor de Imágenes v9.9-LUXURY:** Soporte para establecer fondo de dashboard y descarga segura.
- [x] **Certificado SSL/X.509:** Generación de pasaporte digital estándar (Bouncy Castle).
- [x] **Blindaje Local v7.0:** Doble cifrado RSA/AES para la caché de SQLite (IRON-SHIELD).
- [x] **Modo Camuflaje (F12):** Transformación instantánea en calculadora.

## 🛠️ Tecnologías Core
- **Criptografía:** RSA (2048 bits) + AES-GCM (256 bits) + X.509 (Bouncy Castle).
- **Biometría:** Typing DNA (Behavioral Biometrics).
- **Geolocalización:** IP-API Tracking.
- **Generación QR:** ZXing (Zebra Crossing) v3.5.3.
- **Persistencia:** Firebase Firestore (Nube) + Cloud Storage + SQLite (Local Blindado).

## 📅 Próximos Pasos
1. **Análisis de Amenazas IA:** Detección proactiva de patrones de ataque en los logs de auditoría.
2. **Refuerzo de Auditoría:** Alertas automáticas al administrador ante anomalías biométricas.
