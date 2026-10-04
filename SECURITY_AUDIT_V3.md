# ANALISIS DE SEGURIDAD - Blackpassword v1.0
**Fecha:** 5 de Abril de 2026  
**Proyecto:** Blackpassword - Gestor de contraseñas con criptografía híbrida RSA/AES-GCM  
**Arquitectura:** Multi-módulo Maven + Firebase Firestore  
**Nivel de riesgo:** 🔴 CRÍTICO  

---

## RESUMEN EJECUTIVO

El proyecto implementa criptografía sólida (RSA-2048, AES-256-GCM, PBKDF2) pero presenta **vulnerabilidades críticas** que comprometen la seguridad. **No se recomienda uso en producción** hasta remediar las vulnerabilidades de nivel crítico.

---

## 1. VULNERABILIDADES CRÍTICAS (CWE)

### 1.1 Credenciales Hardcodeadas
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🔴 CRÍTICO |
| **CWE** | CWE-798, CWE-547 |
| **Ubicación** | `ConexionServidor.java:30-31`, `ServerService.java:22` |

```java
// ConexionServidor.java:30-31
txtUsuario = new JTextField("admin_fortaleza", 20);
txtPassword = new JPasswordField("clave_maestra_2026", 20);

// ServerService.java:22
private String baseUrl = "http://localhost:8080/fortaleza/api/v1";
```

**Impacto:** Acceso directo al servidor con credenciales conocidas. Exposición de la estructura de API.

**Remediación:**
```java
txtUsuario = new JTextField(System.getenv("FORTALEZA_USER"), 20);
txtPassword = new JPasswordField(System.getenv("FORTALEZA_PASS"), 20);
```

---

### 1.2 Deshabilitación de Verificación SSL
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🔴 CRÍTICO |
| **CWE** | CWE-295, CWE-295 |
| **Ubicación** | `ServerService.java:122-143` |

```java
// ServerService.java:122-137
private OkHttpClient createUnsafeOkHttpClient() {
    final TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            @Override public void checkServerTrusted(...) {} // ACEPTA TODO
        }
    };
    return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
            .hostnameVerifier((hostname, session) -> true) // ACEPTA CUALQUIER HOST
```

**Impacto:** Vulnerable a ataques MITM. Contraseñas transmitidas en texto plano.

**Remediación:**
```java
private OkHttpClient createSecureOkHttpClient() {
    X509TrustManager trustManager = new DefaultTrustManager();
    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
    sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
    return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
            .hostnameVerifier(StandardHostnameVerifier.getInstance())
            .build();
}
```

---

### 1.3 Sal Universal Hardcodeada
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🔴 CRÍTICO |
| **CWE** | CWE-916 |
| **Ubicación** | `RSALogica.java:36` |

```java
@Deprecated
private static final String LEGACY_SALT = "Blackpassword_Vault_2026";
```

**Impacto:** Rainbow tables pueden precalcularse. Correlación entre archivos de diferentes usuarios.

**Remediación:**
- Generar sales únicas por usuario
- Almacenar sales cifradas con clave privada RSA
- Migrar a Argon2id

---

### 1.4 Contraseña Maestra en Memoria Estática
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🔴 CRÍTICO |
| **CWE** | CWE-316 |
| **Ubicación** | `SecuritySession.java` |

```java
// SecuritySession.java
private static String sessionPinHash; 
private static String userPinHash;    
private static Map<String, Object> userClaims;
```

**Impacto:** Vulnerable a heap dumps, forénsica de memoria, ataques cold boot.

---

### 1.5 Conversiones char[] a String Inseguras
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🔴 CRÍTICO |
| **CWE** | CWE-590 |
| **Ubicaciones** | Múltiples archivos |

```java
// RSALogica.java:281
byte[] key = Arrays.copyOf(md.digest(new String(password).getBytes(...)), 16);

// ConexionServidor.java:88
String pass = new String(txtPassword.getPassword());

// SecuritySession.java:123
String decoded = new String(unsealed, StandardCharsets.UTF_8);
```

**Impacto:** `String` es inmutable en Java. Persiste en String Pool hasta GC.

---

## 2. VULNERABILIDADES DE ALTA PRIORIDAD

### 2.1 Algoritmo AES-128 ECB Legacy
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟠 ALTA |
| **CWE** | CWE-327 |
| **Ubicación** | `RSALogica.java:279-285` |

```java
private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) throws Exception {
    Cipher c = Cipher.getInstance("AES"); // ECB por defecto!
    c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
    return c.doFinal(data);
}
```

**Impacto:** Modo ECB vulnerable a ataques de patrón. Sin confidencialidad verdadera.

---

### 2.2 Auto-Enrollment de Biometría Conductual
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟠 ALTA |
| **CWE** | CWE-287 |
| **Ubicación** | `InicioSesion.java:243-248` |

```java
if (hashPin.equals(pinHash)) {
    // Actualiza el perfil biométrico con el del atacante
    db.collection(...).document(docId).update("typing_profile", currentProfile);
}
```

**Impacto:** Un atacante con solo el PIN puede:
1. Login desde otro dispositivo
2. Con similitud < 20%, trigger backup de PIN
3. Ingresar PIN robado
4. Sistema actualiza perfil biométrico al del atacante
5. Futuros ataques solo necesitan el PIN

---

### 2.3 Threshold de TypingDNA Muy Bajo
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟠 ALTA |
| **CWE** | CWE-307 |
| **Ubicación** | `InicioSesion.java:223` |

```java
if (similarity < 20.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
```

**Impacto:** 20% de similitud es extremadamente bajo. Fácilmente evadible.

**Remediación:** Subir threshold a 50-60%.

---

## 3. VULNERABILIDADES DE MEDIA PRIORIDAD

### 3.1 Logging Excesivo de Información Sensible
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟡 MEDIA |
| **CWE** | CWE-532 |
| **Hallazgo** | 86+ usages de System.out/err |

```java
System.out.println("[FirebaseService] Conexion v12.5 establecida: " + getProjectId());
System.out.println("[TypingDNA] Similitud detectada: " + similarity + "%");
System.err.println("[SecuritySession] Falla: Hash de Email no coincide.");
```

**Remediación:** Usar SLF4J con niveles apropiados. No loggear información sensible.

---

### 3.2 Exportación de Contraseñas en Plano
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟡 MEDIA |
| **CWE** | CWE-313 |
| **Ubicación** | `Contrasenas.java:593-596` |

```java
case "PLAIN":
    Files.write(target.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
```

**Impacto:** Exporta TODAS las contraseñas en CSV sin cifrar.

---

### 3.3 Creación Libre de Alertas de Intrusión
| Atributo | Valor |
|----------|-------|
| **Severidad** | 🟡 MEDIA |
| **CWE** | CWE-284 |
| **Ubicación** | `FIREBASE_SECURITY_RULES.txt:44` |

```javascript
allow create: if true; // Cualquiera puede crear alertas
```

**Impacto:** DoS mediante spam de alertas falsas.

---

## 4. VULNERABILIDADES DE BAJA PRIORIDAD

### 4.1 Inconsistencia en Iteraciones PBKDF2
```java
// RSALogica usa 600000
PBKDF2WithHmacSHA256, iterations = 600000

// LocalDatabaseService usa 300000
SecretKeySpec key = RSALogica.derivarLlavePBKDF2Legacy(password, "Blackpassword_DB_Key_2026", 300000);
```

---

### 4.2 Dependencias con Versiones Antiguas
- `firebase-admin`: 9.8.0 (actual ~12.x)
- Otras dependencias están actualizadas (BouncyCastle 1.80, OkHttp 4.12.0)

---

## 5. MATRIZ DE RIESGOS

| Vulnerabilidad | Severidad | Probabilidad | Impacto | Riesgo |
|----------------|-----------|--------------|---------|--------|
| Credenciales hardcodeadas | CRITICO | Alta | Critico | 🔴 |
| SSL bypass | CRITICO | Alta | Critico | 🔴 |
| Sal universal | CRITICO | Media | Critico | 🔴 |
| Password en memoria estática | CRITICO | Alta | Critico | 🔴 |
| String conversion leaks | CRITICO | Alta | Alto | 🔴 |
| AES-128 ECB legacy | ALTO | Baja | Alto | 🟠 |
| Auto-enrollment biometric | ALTO | Media | Alto | 🟠 |
| Threshold TypingDNA bajo | ALTO | Media | Medio | 🟠 |
| Logging excesivo | MEDIO | Alta | Bajo | 🟡 |
| Alertas libres en Firebase | MEDIO | Media | Medio | 🟡 |
| Exportación en plano | MEDIO | Baja | Alto | 🟡 |

---

## 6. ROADMAP DE REMEDIACIÓN

### 🔴 Inmediato (0-7 días)
1. Eliminar credenciales hardcodeadas de `ConexionServidor.java`
2. Implementar verificación SSL en `ServerService.java`
3. Eliminar sal universal, generar sales únicas por usuario
4. No almacenar masterPassword en variables estáticas
5. Eliminar conversiones char[] a String innecesarias

### 🟠 Corto plazo (1-4 semanas)
1. Aumentar threshold TypingDNA a 50-60%
2. Eliminar auto-enrollment biométrico
3. Forzar migración de archivos legacy a AES-256-GCM
4. Implementar logging apropiado con SLF4J
5. Eliminar opción de exportación en plano

### 🟡 Mediano plazo (1-3 meses)
1. Migrar a Argon2id como función de derivación
2. Implementar TOTP como 2FA
3. Implementar rate limiting en Firebase
4. Auditoría de dependencias con OWASP Dependency-Check

### 🟢 Largo plazo
1. Implementar HSM para almacenamiento de claves
2. Auditoría de seguridad por empresa externa
3. Penetration testing regular
4. Certificación de seguridad (SOC2, ISO27001)

---

## 7. ARQUITECTURA CRIPTOGRÁFICA ACTUAL

### ✅ Fortalezas
- RSA-2048 para cifrado asimétrico
- AES-256-GCM con IV de 12 bytes y tag de 128 bits
- PBKDF2WithHmacSHA256 con 600,000 iteraciones (moderno)
- KEK dinámicas por usuario
- CEK generadas con SecureRandom
- Soporte para wrap/unwrap de claves

### ❌ Debilidades
- Soporte legacy para AES-128 ECB (vulnerable)
- Sal universal hardcodeada
- Derivación de claves en múltiples sitios con diferentes iteraciones

---

## 8. CONCLUSIONES

El proyecto Blackpassword demuestra un entendimiento de criptografía moderna, pero la **implementación contiene errores críticos** que comprometen la seguridad del sistema:

**Top 5 Acciones Prioritarias:**
1. ✅ Eliminar todas las credenciales y sales hardcodeadas
2. ✅ Implementar verificación SSL/TLS correcta
3. ✅ Eliminar almacenamiento de contraseñas en memoria estática
4. ✅ Eliminar conversiones char[] a String innecesarias
5. ✅ Aumentar threshold biométrico y eliminar auto-enrollment

**Recomendación:** No utilizar en producción hasta resolver vulnerabilidades críticas.

---

*Análisis generado: 5 de Abril de 2026*  
*Herramientas: Análisis estático de código fuente*
