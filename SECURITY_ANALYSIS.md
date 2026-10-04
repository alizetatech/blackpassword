# ANÁLISIS DE SEGURIDAD - Blackpassword
## Fecha: 04 de Abril de 2026 | Versión: 1.0

---

## 1. RESUMEN EJECUTIVO

**Proyecto:** Blackpassword - Gestor de contraseñas con criptografía híbrida RSA/AES-GCM  
**Arquitectura:** Multi-módulo Maven (crypto, services, core, ui) + Firebase Firestore  
**Nivel de riesgo:** MEDIO-ALTO

---

## 2. CRIPTOGRAFÍA

### 2.1 Fortalezas

| Componente | Implementación | Estado |
|------------|----------------|--------|
| RSA | 2048 bits, PKCS1Padding/OAEP | ✅ Correcto |
| AES-GCM | 256 bits, IV 12 bytes, tag 128 bits | ✅ Correcto |
| PBKDF2 | SHA-256, 600,000 iteraciones | ✅ Recomendable |
| CEK Wrapping | AES-GCM con KEK derivada | ✅ Correcto |
| Hash de contraseñas | SHA-256 con salt universal | ⚠️ Aceptable |

### 2.2 Vulnerabilidades Criptográficas

#### CRÍTICO
- **Salt Universal Fijo** (`RSALogica.java:31`): `"Blackpassword_Vault_2026"` está hardcodeado en el código fuente. Si el código fuente es comprometido, todos los hashes son vulnerables a ataques de diccionario precalculados.
  
  ```java
  private static final String UNIVERSAL_SALT = "Blackpassword_Vault_2026";
  ```

- ~~**Legacy V0 (AES-128 ECB)** (`CifradorLogica.java:104-113`)~~: ✅ RESUELTO en v4.3: Límite 3 intentos por sesión + Legacy V1 con AES-256-CBC + HMAC-SHA256

#### ALTO
- **Soporte Legacy V0** (`RSALogica.java:224-230`): Mantiene decryption fallback que usa SHA-256 para derivar clave AES-128, reduciendo la seguridad a solo 128 bits.

- **Iteraciones Legacy Inconsistentes** (`CifradorLogica.java:30`): Usa `ITER_LEGACY` (65536) para cifrado moderno GCM, cuando debería usar `ITER_MODERN` (600000).

#### MEDIO
- **Múltiples Padding Schemes** (`RSALogica.java:238`): Intenta 3 paddings RSA diferentes. Esto puede revelar información sobre el plaintext en ciertos escenarios.

---

## 3. GESTIÓN DE SESIÓN

### 3.1 SecuritySession (`SecuritySession.java`)

#### Fortalezas
- ✅ Limpieza de memoria con `Arrays.fill()` en contraseñas
- ✅ Validación cruzada de identidad (email, UID, pubkey hash)
- ✅ RSA-Binding detection

#### Vulnerabilidades

**CRÍTICO - Almacenamiento en memoria de masterPassword:**
```java
private static char[] masterPassword;  // Línea 24
```
- El password maestro se mantiene en memoria estática durante toda la sesión
- Vulnerable a heap dumps, memory forensics, y cold boot attacks

**ALTO - Constante RSA hardcodeada:**
- La sal universal permite correlacionar archivos de diferentes usuarios
- `getUniversalSalt()` retorna sal fija para toda la aplicación

---

## 4. BASE DE DATOS LOCAL

### 4.1 LocalDatabaseService (`LocalDatabaseService.java`)

#### Implementación
- SQLite cifrado con AES-256-GCM (`blackvault_local.db.enc`)
- PBKDF2 con 300,000 iteraciones para clave de BD
- Sal específica: `"Blackpassword_DB_Key_2026"`

#### Vulnerabilidades

| Severidad | Descripción |
|-----------|-------------|
| 🔴 CRÍTICO | La sal de BD está hardcodeada |
| 🔴 CRÍTICO | La contraseña maestra de BD se deriva del mismo password que el login |
| 🟢 BAJO | ~~Archivo temporal `blackvault_local.db` puede quedar sin cifrar si hay error~~ | ✅ RESUELTO: Escritura atómica + sobrescritura segura en error |
| 🟡 ALTO | `toBytes()` en línea 141 no limpia el ByteBuffer |

---

## 5. FIREBASE SECURITY RULES

### 5.1 Análisis (`FIREBASE_SECURITY_RULES.txt`)

#### Fortalezas
- ✅ Colecciones hasheadas para obfuscación de paths
- ✅ Validación de ownership mediante email_hash en custom claims
- ✅ Regla restrictiva para auditoría (solo lectura admin)

#### Vulnerabilidades

**🟡 MEDIO - Línea 44:**
```javascript
allow create: if true; // Cualquier intento fallido debe poder registrarse
```
- Permite a cualquier usuario crear alertas de intrusión
- Potencial para DoS mediante spam de alertas falsas

**🟢 BAJO - Nombres de colecciones predecibles:**
- Aunque usan SHA-256 para paths, la estructura es conocida
- Ataque de fuerza bruta en paths sería computacionalmente costoso pero posible

---

## 6. AUTENTICACIÓN MULTIFACTOR

### 6.1 Typing DNA (`TypingDNAManager`)

| Aspecto | Evaluación |
|---------|------------|
| Aprendizaje activo | ⚠️ Actualiza perfil tras bypass con PIN - puede ser manipulado |
| Threshold 20% | ⚠️ Muy bajo, fácilmente evadible |
| Sin protección anti-replay | ⚠️ Perfil puede ser capturado |

### 6.2 Biometric Bypass (`InicioSesion.java:215-242`)

```java
if (similarity < 20.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
    // Pide PIN y ACTUALIZA el perfil biométrico
    db.collection(...).document(docId).update("typing_profile", currentProfile);
}
```

**Vulnerabilidad:** Un atacante con PIN podría:
1. Hacer login desde otro dispositivo
2. Con similitud < 20%,触发 backup de PIN
3. Ingresar PIN robado
4. El sistema **actualiza el perfil biométrico** al del atacante

---

## 7. FLUJO DE DATOS SENSIBLES

```
┌─────────────────────────────────────────────────────────────────────┐
│                        BLACKPASSWORD DATA FLOW                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  User Input ──► InMemory ──► [ENCRYPT] ──► Local DB (encrypted)    │
│                                        │                              │
│                                        └────► Firebase (encrypted)    │
│                                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                         SECURITY CONCERNS:                           │
├─────────────────────────────────────────────────────────────────────┤
│  1. VaultManager:91 - String password creado en memoria              │
│     String passStr = new String(password);                           │
│                                                                       │
│  2. SecuritySession:97 - MasterPassword en memoria estática         │
│     masterPassword = Arrays.copyOf(pwd, pwd.length);                │
│                                                                       │
│  3. RSALogica:226 - Legacy password conversion                       │
│     new String(password).getBytes(StandardCharsets.UTF_8)           │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 8. MATRIZ DE AMENAZAS

| Amenaza | Probabilidad | Impacto | Riesgo | Mitigación |
|---------|--------------|---------|--------|------------|
| Heap Dump Attack | Media | Crítico | 🔴 ALTO | No guardar passwords en estática |
| Cold Boot Attack | Baja | Crítico | 🟡 MEDIO | Limpieza inmediata post-uso |
| Rainbow Tables | Alta | Alto | 🔴 ALTO | Usar salt por usuario, no universal |
| Insider Threat (Firebase) | Baja | Crítico | 🟡 MEDIO | Cifrado E2E, Firebase no ve plaintext |
| Social Engineering | Media | Alto | 🟡 MEDIO | 2FA robusto, no auto-update biometrics |
| Man-in-the-Middle | Baja | Alto | 🟢 BAJO | HTTPS/Firebase usa TLS |
| SQL Injection (Local) | Baja | Alto | 🟢 BAJO | PreparedStatements usados |

---

## 9. RECOMENDACIONES PRIORITARIAS

### Inmediatas (Crítico)

1. **Eliminar salt universal hardcodeado**
   - Generar salt único por usuario
   - Almacenar salt cifrado con la clave privada RSA del usuario

2. **No guardar masterPassword en estática**
   - Usar `CharBuffer` con `clear()` inmediatamente
   - Considerar hardware security module (HSM) o keychain

3. **Corregir auto-enrollment biométrico**
   - Eliminar línea 233 de `InicioSesion.java`
   - Requiere verificación explícita del usuario

### Corto Plazo (Alto)

4. **Aumentar threshold de Typing DNA** de 20% a 50-60%

5. **Eliminar soporte legacy V0** 
   - AES-128 ECB es insecure
   - Forzar migración a AES-256-GCM

6. **Separar sales** entre BD local y login

### Mediano Plazo (Medio)

7. Implementar **argon2id** como alternativa a PBKDF2

8. Añadir **rate limiting** en Firebase (Cloud Functions)

9. Implementar **mfaTOTP** como segundo factor

10. Auditoría de dependencias vulnerables

---

## 10. CONCLUSIÓN

Blackpassword implementa una arquitectura criptográfica **fundamentalmente sólida** (RSA-2048 + AES-256-GCM + PBKDF2), pero presenta **vulnerabilidades críticas** en:

1. Uso de salt universal hardcodeado
2. Almacenamiento de contraseñas en memoria estática
3. Auto-enrollment de biometría conductual

**Recomendación:** No utilizar en producción hasta resolver las 3 vulnerabilidades críticas identificadas. El riesgo de compromiso de master passwords es inaceptable para un gestor de contraseñas.

---

## 11. REFERENCIAS DE CÓDIGO

| Archivo | Líneas Críticas | Descripción |
|---------|-----------------|-------------|
| `RSALogica.java` | 31, 224-230 | Salt universal, legacy V0 |
| `SecuritySession.java` | 24, 95-100 | MasterPassword en estática |
| `CifradorLogica.java` | 30, 104-113 | Iteraciones legacy, AES-128 ECB |
| `InicioSesion.java` | 233 | Auto-update biométrico |
| `LocalDatabaseService.java` | 134, 141-146 | Salt BD, toBytes() leak |
| `VaultManager.java` | 91-92 | String de password en memoria |
| `FIREBASE_SECURITY_RULES.txt` | 44 | Creación libre de alertas |

---

*Documento generado automáticamente. Requiere revisión por experto en criptografía certificado.*
