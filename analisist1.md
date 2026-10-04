# Analisis Tecnico - Blackpassword

## 1. Resumen del Proyecto

**Blackpassword** es una aplicacion de escritorio Java (Swing) para gestion de contrasenas con caracteristicas de seguridad de nivel empresarial.

---

## 2. Estructura del Proyecto

```
Blackpassword/
├── .mvn/                          # Maven wrapper
├── .vscode/                       # Config VS Code
├── blackpassword-core/             # Modulo core (utilidades)
├── blackpassword-crypto/           # Modulo criptografico (RSA, AES, PGP)
├── blackpassword-services/         # Modulo de servicios (Firebase, DB, seguridad)
├── blackpassword-ui/              # Modulo UI (Swing + FlatLaf)
├── src/                           # Legacy root source (deprecado)
├── target/                        # Build output
├── pom.xml                        # Parent POM (multi-modulo)
├── mvnw, mvnw.cmd                 # Maven wrapper scripts
└── blackvault_local.db.enc        # Base de datos local encriptada
```

---

## 3. Stack Tecnologico

| Categoria | Tecnologia | Version |
|-----------|------------|---------|
| **Lenguaje** | Java | 17 |
| **Build** | Maven | 3.x |
| **UI** | Swing + FlatLaf | 3.6.2 |
| **Criptografia** | Bouncy Castle | 1.80 |
| **QR Codes** | ZXing | 3.5.3 |
| **Backend Cloud** | Firebase Admin SDK | 9.8.0 |
| **DB Local** | SQLite | 3.48.0 |
| **HTTP Client** | OkHttp | 4.12.0 |
| **JSON** | Gson | 2.10.1 |
| **PDF** | Apache PDFBox | 2.0.32 |
| **Ofuscacion** | ProGuard | 7.4.2 |

---

## 4. Arquitectura

### Capas de la Aplicacion

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (blackpassword-ui)               │
│  Swing Components, FlatLaf, User Interactions               │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│               Services Layer (blackpassword-services)        │
│  SecuritySession, FirebaseService, LocalDatabaseService,    │
│  AuditService, IntrusionService, TypingDNAManager          │
└─────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│               Crypto Layer (blackpassword-crypto)           │
│  RSALogica, CifradorLogica, PGPLogica                       │
└─────────────────────────────────────────────────────────────┘
```

### Flujo de la Aplicacion

1. **Inicio** -> `FramePrincipal` (Ventana Principal)
2. **Landing** -> `Inicio` pantalla de inicio
3. **Login** -> `InicioSesion` con validacion biometrica
4. **Dashboard** -> `MenuPrincipal` con navegacion a:
   - Bovedas de contrasenas (`Contrasenas`)
   - Gestor de archivos (`Archivos`)
   - Galeria de imagenes (`Imagenes`)
   - Herramientas de seguridad (RSA, PGP, Algoritmos)
   - Reportes (`Reportes`)

---

## 5. Base de Datos

### Base de Datos Local
- **Tipo:** SQLite
- **Ubicacion:** `blackvault_local.db`
- **Encriptacion:** AES-256-GCM (archivo completo)
- **Derivacion de clave:** PBKDF2 con 300,000 iteraciones

### Base de Datos en la Nube
- **Tipo:** Firebase Firestore
- **Colecciones:**
  - Perfiles de usuario
  - Logs de auditoria
  - Alertas de intrusion
  - Monitoreo de kill switch remoto

### Schema Local (tabla `boveda`)
```sql
CREATE TABLE boveda (
    id TEXT PRIMARY KEY,       -- Nombre del servicio
    u_h TEXT,                 -- Hash de email del usuario
    d_c TEXT,                 -- Datos encriptados (RSA hibrido)
    ts INTEGER                -- Timestamp
)
```

---

## 6. Caracteristicas de Seguridad

### Criptografia

| Caracteristica | Implementacion |
|----------------|----------------|
| **Encriptacion Hibrida** | RSA-2048 + AES-256-GCM |
| **Hash de contrasenas** | PBKDF2-SHA256 (300,000 iteraciones) |
| **Encriptacion de archivos** | AES-256-GCM con IV aleatorio |
| **Intercambio de claves** | Esquema hibrido RSA |
| **Soporte legacy** | AES-128 + SHA-256 |
| **Soporte PGP** | OpenPGP via Bouncy Castle |

### Identidad y Autenticacion

| Caracteristica | Descripcion |
|----------------|-------------|
| **Typing DNA** | Biometria comportamental (patron de escritura) |
| **Rastreo de ubicacion** | Geolocalizacion IP con alertas |
| **Verificacion PIN** | PIN de 4 digitos para operaciones sensibles |
| **Validacion cruzada** | Valida hash email/UID entre token, Firestore y RAM |

### Controles de Seguridad

| Caracteristica | Descripcion |
|----------------|-------------|
| **RSA-Binding** | Vincula cuenta a clave publica RSA |
| **Kill Switch Remoto** | Puede deshabilitar cuenta remotamente |
| **Deteccion de intrusion** | Detecta violaciones de binding RSA |
| **Timeout de sesion** | Auto-logout por inactividad (5 minutos) |
| **Privacy Shield** | Difumina app cuando pierde foco |
| **Modo camuflaje** | F12 transforma app en "Calculadora" |
| **Boton panico** | Tecla ESC apaga de forma segura |
| **Limpieza de memoria** | Limpia datos sensibles de RAM al cerrar sesion |

### Auditoria y Monitoreo

| Caracteristica | Descripcion |
|----------------|-------------|
| **Logs de auditoria** | Todos los eventos de seguridad en Firestore |
| **Alertas de intrusion** | Alertas criticas por violaciones |
| **Deteccion de cambio de ubicacion** | Alertas por login desde ubicacion diferente |
| **Notificaciones en tiempo real** | Eventos via patron listener |

---

## 7. Detalles de Implementacion de Seguridad

### Esquema de Encriptacion Hibrida
1. Generar clave AES-256 aleatoria
2. Encriptar datos con AES-GCM + IV aleatorio
3. Encriptar clave AES con clave publica RSA
4. Almacenar: `[keyLen|encryptedAESKey|IV|encryptedData]`

### RSA Key Binding
```java
// Huella de clave publica almacenada en Firebase claims
claim_pubkey_hash = SHA256(publicKey.getEncoded())

// Validado en cada operacion sensible
if (currentKeyHash != claimPubKeyHash) → IntrusionService.reportFailure()
```

### Flujo de Encriptacion de Base de Datos
1. **Escritura:** SQLite plano -> AES-256-GCM -> `blackvault_local.db.enc`
2. **Lectura:** `blackvault_local.db.enc` -> AES-256-GCM decrypt -> SQLite plano
3. **Derivacion:** `PBKDF2(masterPassword, "Blackpassword_DB_Key_2026", 300000)`

### Typing DNA Biometria
- Registra timestamps de press/release de teclas
- Calcula tiempo de espera (dwell) y tiempo de vuelo (flight)
- Compara perfiles usando distancia Manhattan normalizada
- Umbral de similitud: <20% requiere rescate con PIN

---

## 8. Modulos del Proyecto

### blackpassword-core
Modulo de utilidades basicas (estub minimo).

### blackpassword-crypto
- `bcprov-jdk18on` - Proveedor crypto Bouncy Castle
- `bcpkix-jdk18on` - Soporte PKI/X.509
- `bcpg-jdk18on` - Soporte PGP/OpenPGP
- `bcutil-jdk18on` - Utilidades BC

### blackpassword-services
- `firebase-admin` - Firebase Admin SDK
- `google-cloud-firestore` - Base de datos en la nube
- `google-cloud-storage` - Almacenamiento de archivos
- `grpc-okhttp` - Transporte gRPC
- `gson` - Procesamiento JSON
- `okhttp` - Cliente HTTP

### blackpassword-ui
- `flatlaf` - Look moderno para Swing
- `flatlaf-extras` - Componentes adicionales FlatLaf
- `pdfbox` - Generacion/lectura PDF
- `sqlite-jdbc` - Driver SQLite
- `zxing-core`, `zxing-javase` - Generacion codigos QR
- `slf4j-simple` - Logging

---

## 9. Conclusion

**Blackpassword** es un administrador de contrasenas de escritorio con caracteristicas de seguridad de nivel empresarial, incluyendo biometria comportamental, capacidades de eliminacion remota y logging de auditoria completo. La arquitectura modular permite separacion clara de responsabilidades entre criptografia, servicios y UI.
