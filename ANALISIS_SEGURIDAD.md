# ANALISIS EXHAUSTIVO DE SEGURIDAD - BLACKPASSWORD

**Fecha:** 5 de Abril de 2026  
**Proyecto:** Blackpassword - Gestor de contraseñas con criptografía híbrida RSA/AES-GCM  
**Arquitectura:** Multi-modulo Maven (crypto, services, core, ui) + Firebase Firestore  
**Lenguaje:** Java 17  
**Nivel de riesgo global:** 🔴 CRITICO

---

## RESUMEN EJECUTIVO

El proyecto implementa una arquitectura criptografica fundamental (RSA-2048 + AES-256-GCM + PBKDF2), pero presenta **multiples vulnerabilidades criticas** que podrian comprometer la seguridad de las contrasenas almacenadas.

---

## 1. VULNERABILIDADES CRITICAS

### 1.1 Credenciales Hardcodeadas en Codigo Fuente

**Severidad:** 🔴 CRITICA  
**CWE:** CWE-798 - Use of Hard-coded Credentials

**Ubicacion:** `ConexionServidor.java` lineas 30-31

```java
txtUsuario = new JTextField("admin_fortaleza", 20);
txtPassword = new JPasswordField("clave_maestra_2026", 20);
```

**Impacto:** 
- Un atacante con acceso al codigo fuente obtiene credenciales por defecto
- Estas credenciales podrian dar acceso al servidor "Fortaleza Digital"
- Violacion directa de OWASP A07:2021 - Security Misconfiguration

**Remediacion:**
```java
// Usar variables de entorno o archivo de configuracion externo
txtUsuario = new JTextField(System.getenv("FORTALEZA_USER"), 20);
txtPassword = new JPasswordField(System.getenv("FORTALEZA_PASS"), 20);
```

---

### 1.2 Deshabilitacion de Verificacion SSL/TLS

**Severidad:** 🔴 CRITICA  
**CWE:** CWE-295 - Improper Certificate Validation

**Ubicacion:** `ServerService.java` lineas 122-143

```java
private OkHttpClient createUnsafeOkHttpClient() {
    // TrustManager que acepta TODOS los certificados
    final TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            @Override public void checkClientTrusted(...) {}
            @Override public void checkServerTrusted(...) {}
            @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
                return new java.security.cert.X509Certificate[]{}; 
            }
        }
    };
    // ...
    return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
            .hostnameVerifier((hostname, session) -> true)  // ACEPTA CUALQUIER HOST
```

**Impacto:**
- **Ataque Man-in-the-Middle (MITM)** completo
- Todas las comunicaciones con el servidor son interceptables
- Contrasenas transmitidas en texto plano sobre conexiones no verificadas

**Remediacion:**
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

**Severidad:** 🔴 CRITICA  
**CWE:** CWE-916 - Use of Password Hash With Insufficient Computational Effort

**Ubicacion:** `RSALogica.java` linea 36

```java
@Deprecated
private static final String LEGACY_SALT = "Blackpassword_Vault_2026";
```

**Impacto:**
- Todos los hashes pueden ser precalculados (rainbow tables)
- Si el codigo fuente es comprometido, TODOS los hashes son vulnerables
- Permite correlacionar archivos de diferentes usuarios

**Remediacion:**
- Generar sales unicas por usuario
- Almacenar sales cifradas con la clave privada RSA del usuario
- Migrar a Argon2id como funcion de derivacion

---

### 1.4 Contrasena Maestra Almacenada en Memoria Estatica

**Severidad:** 🔴 CRITICA  
**CWE:** CWE-316 - Cleartext Storage of Sensitive Information in Memory

**Ubicacion:** `SecuritySession.java` lineas 24, 98-101

```java
private static com.mycompany.blackpassword.crypto.GuardedString masterPassword; 

public static void setMasterPassword(char[] pwd) {
    if (masterPassword != null) masterPassword.close();
    masterPassword = pwd != null ? new com.mycompany.blackpassword.crypto.GuardedString(pwd) : null;
}
```

**Impacto:**
- Vulnerable a heap dumps
- Vulnerable a ataques de forensica de memoria
- Vulnerable a ataques de cold boot
- Java Flight Recorder puede capturar datos sensibles

**Remediacion:**
- No almacenar contrasenas en variables estaticas
- Usar Java KeyStore o HSM
- Procesar y limpiar inmediatamente despues de usar

---

### 1.5 Conversion Insegura de char[] a String

**Severidad:** 🔴 CRITICA  
**CWE:** CWE-590 - Exposure of Sensitive Information to a String Object

**Ubicacion multiple:**

```java
// VaultManager.java:91
String passStr = new String(password); // Temporal para el payload

// RSALogica.java:281
byte[] key = Arrays.copyOf(md.digest(new String(password).getBytes(...)), 16);

// Registro.java:266
.setPassword(new String(pass))

// ConexionServidor.java:88, 128
String pass = new String(txtPassword.getPassword());
```

**Impacto:**
- `String` es inmutable en Java y permanece en String Pool
- No puede ser sobrescrito ni eliminado
- Permanece en memoria hasta Garbage Collection
- Posible leaks en heap dumps

**Remediacion:**
```java
// Usar char[] directamente y limpiar despues
try {
    // operacion con password
} finally {
    Arrays.fill(password, '0');
}
```

---

## 2. VULNERABILIDADES DE ALTA PRIORIDAD

### 2.1 Algoritmo Cifrado Legacy AES-128 ECB

**Severidad:** 🟠 ALTA  
**CWE:** CWE-327 - Use of a Broken or Risky Cryptographic Algorithm

**Ubicacion:** `RSALogica.java` linea 279-285, `CifradorLogica.java` linea 183-199

```java
// RSALogica - Legacy V0
private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] key = Arrays.copyOf(md.digest(new String(password).getBytes(...)), 16);
    Cipher cipher = Cipher.getInstance("AES");  // AES por defecto es ECB!
    cipher.init(Cipher.DECRYPT_MODE, secretKey);
    return cipher.doFinal(data);
}
```

**Impacto:**
- AES-128 en modo ECB es vulnerable a ataques de patron
- No provee confidencialidad verdadera
- Aunque hay limite de 3 intentos, el dano ya esta hecho si el archivo fue comprometido

**Remediacion:**
- Forzar migracion a AES-256-GCM
- Eliminar soporte para algoritmos debiles
- Implementar notificacion al usuario de migracion obligatoria

---

### 2.2 Vulnerabilidad de Padding Oracle en Legacy V0

**Severidad:** 🟠 ALTA  
**CWE:** CWE-696 - Incorrect Behavior Order

**Ubicacion:** `CifradorLogica.java` lineas 150-167

```java
// El propio codigo advierte sobre la vulnerabilidad
System.err.println("[CifradorLogica] Advertencia: DEPRECADO: Legacy V0 (AES-128 ECB)...");
System.err.println("[CifradorLogica] Advertencia: Este modo es vulnerable a ataques padding oracle...");
```

**Impacto:**
- Ataques padding oracle pueden revelar plaintext
- Ataques de denegacion de servicio

**Remediacion:**
- Migrar todos los archivos a formato moderno
- Notificar usuarios con archivos legacy para migracion

---

### 2.3 Auto-Enrollment de Biometria Conductual

**Severidad:** 🟠 ALTA  
**CWE:** CWE-287 - Improper Authentication

**Ubicacion:** `InicioSesion.java` lineas 222-253

```java
if (similarity < 20.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
    // Pide PIN y ACTUALIZA el perfil biometric con nuevo ritmo
    db.collection(...).document(docId).update("typing_profile", currentProfile);
}
```

**Impacto:**
- Un atacante con solo el PIN puede:
  1. Hacer login desde otro dispositivo
  2. Con similitud < 20%, trigger backup de PIN
  3. Ingresar PIN robado
  4. El sistema actualiza el perfil biometric al del atacante
  5. Futuros ataques solo necesitan el PIN

**Remediacion:**
- Eliminar auto-actualizacion del perfil biometric
- Requiere verificacion explícita del usuario
- No modificar biometria tras bypass

---

### 2.4 URL de Servidor Hardcodeada

**Severidad:** 🟠 ALTA  
**CWE:** CWE-547 - Use of Hard-coded, Security-relevant String

**Ubicacion:** 
- `ServerService.java` linea 22
- `ConexionServidor.java` linea 32

```java
private String baseUrl = "http://localhost:8080/fortaleza/api/v1";
txtUrl = new JTextField("http://localhost:8080/fortaleza/api/v1", 20);
```

**Impacto:**
- Solo funciona para desarrollo local
- No permite configuracion de servidor en produccion
- Expone estructura de API interna

**Remediacion:**
- Usar archivo de configuracion externo
- Implementar URL configurable via variables de entorno

---

### 2.5 Archivo de Base de Datos Cifrado Presente en Repositorio

**Severidad:** 🟠 ALTA  

**Hallazgo:** `blackvault_local.db.enc` esta presente en el directorio del proyecto

```bash
-rw-r--r-- 1 JReyna 197121 20508 Mar 27 06:10 blackvault_local.db.enc
```

**Impacto:**
- Aunque esta cifrado, si el codigo fuente revela la sal universal, es vulnerable
- Archivo binario no deberia estar en repositorio

**Remediacion:**
- Agregar al .gitignore:
```gitignore
*.enc
```

---

### 2.6 Threshold de Typing DNA Muy Bajo

**Severidad:** 🟠 ALTA  
**CWE:** CWE-307 - Excessive Attempt Rate Limiting

**Ubicacion:** `InicioSesion.java` linea 223

```java
if (similarity < 20.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
```

**Impacto:**
- 20% de similitud es extremadamente bajo
- Facilmente evadible por atacantes
- Reduce significativamente la seguridad del factor biometric

**Remediacion:**
- Subir threshold a 50-60%
- Implementar lockout tras intentos fallidos

---

## 3. VULNERABILIDADES DE MEDIA PRIORIDAD

### 3.1 Exceso de Logging de Informacion Sensible

**Severidad:** 🟡 MEDIA  
**CWE:** CWE-532 - Information Exposure Through Log Files

**Hallazgo:** 86+ usages de System.out/err.print

```java
System.out.println("[FirebaseService] Conexion v12.5 establecida: " + getProjectId());
System.err.println("[SecuritySession] Falla: Hash de Email no coincide.");
System.out.println("[TypingDNA] Similitud detectada: " + similarity + "%");
```

**Impacto:**
- Contrasenas podrian filtrarse en logs
- Informacion de debug en produccion
- Informacion de ubicacion expuesta

**Remediacion:**
- Usar framework de logging con niveles apropiados
- No loggear informacion sensible
- Configurar log level para produccion

---

### 3.2 Creacion Libre de Alertas de Intrusion

**Severidad:** 🟡 MEDIA  
**CWE:** CWE-284 - Improper Access Control

**Ubicacion:** `FIREBASE_SECURITY_RULES.txt` linea 44

```javascript
allow create: if true; // Cualquier intento fallido debe poder registrarse
```

**Impacto:**
- DoS mediante spam de alertas falsas
- Consumo innecesario de recursos
- Posible saturacion de base de datos

**Remediacion:**
```javascript
allow create: if isAuthenticated(); // Solo usuarios autenticados
```

---

### 3.3 Gestion de Sesiones en Memoria Estatrica

**Severidad:** 🟡 MEDIA  
**CWE:** CWE-6 - J2EE Misconfiguration: Insufficient Session-ID Length

**Ubicacion:** `SecuritySession.java` multiples variables estaticas

```java
private static PublicKey currentPublicKey;
private static PrivateKey currentPrivateKey;
private static String sessionPinHash; 
private static String userPinHash;
private static Map<String, Object> userClaims;
```

**Impacto:**
- Todas las sesiones comparten estado
- Vulnerable a ataques de memoria compartida
- No hay timeout de sesion en SecuritySession

**Remediacion:**
- Usar instancia por sesion
- Implementar timeout de sesion
- Limpiar estado al logout

---

### 3.4 Exportacion de Contrasenas en Plano

**Severidad:** 🟡 MEDIA  
**CWE:** CWE-313 - Cleartext Storage or Transmission

**Ubicacion:** `Contrasenas.java` lineas 593-596

```java
case "PLAIN":
    if (!target.getName().endsWith(".csv")) target = new File(target.getAbsolutePath() + ".csv");
    Files.write(target.toPath(), csvContent.getBytes(StandardCharsets.UTF_8));
    break;
```

**Impacto:**
- Exporta TODAS las contrasenas en CSV sin cifrar
- Archivo vulnerable si se pierde/el equipo es comprometido
- No hay advertencia clara al usuario

**Remediacion:**
- Eliminar opcion de exportacion en plano
- Forzar exportacion solo con cifrado
- Agregar advertencia de seguridad prominente

---

### 3.5 Cifrado Debil en EntropyService

**Severidad:** 🟡 MEDIA  

**Ubicacion:** `EntropyService.java` lineas 16-21

```java
public void addMouseMovement(int x, int y) {
    if (entropyPool.size() < REQUIRED_ENTROPY) {
        entropyPool.add((byte) (x & 0xFF));      // Solo 1 byte de 32
        entropyPool.add((byte) (y & 0xFF));      // Solo 1 byte de 32
        entropyPool.add((byte) (System.nanoTime() & 0xFF));
    }
}
```

**Impacto:**
- Solo usa 1 byte de coordenadas de 32 bits
- Reduce entropia significativamente
- Vulnerable a ataques de prediccion

**Remediacion:**
```java
public void addMouseMovement(int x, int y) {
    entropyPool.add((byte) (x & 0xFF));
    entropyPool.add((byte) ((x >> 8) & 0xFF));
    entropyPool.add((byte) ((x >> 16) & 0xFF));
    entropyPool.add((byte) ((x >> 24) & 0xFF));
    // ... similar para y
    entropyPool.add((byte) (System.nanoTime() & 0xFF));
    entropyPool.add((byte) ((System.nanoTime() >> 8) & 0xFF));
}
```

---

## 4. VULNERABILIDADES DE BAJA PRIORIDAD

### 4.1 Iteraciones PBKDF2 Inconsistentes

**Severidad:** 🟢 BAJA  
**CWE:** CWE-328 - Use of Weak Hash

**Ubicacion:** `RSALogica.java` vs `LocalDatabaseService.java`

```java
// RSALogica usa 600000
PBKDF2WithHmacSHA256, iterations = 600000

// LocalDatabaseService usa 300000
SecretKeySpec key = RSALogica.derivarLlavePBKDF2Legacy(password, "Blackpassword_DB_Key_2026", 300000);
```

**Remediacion:**
- Estandarizar a 600,000 iteraciones en todo el proyecto

---

### 4.2 Dependencias con Versiones Antiguas

**Severidad:** 🟢 BAJA  

**Hallazgo:** firebase-admin version 9.8.0 (version actual ~12.x)

**Remediacion:**
- Actualizar a version mas reciente para parches de seguridad

---

### 4.3 ProGuard Configuration Insegura

**Severidad:** 🟢 BAJA  

**Ubicacion:** `proguard.conf`

```proguard
-dontoptimize   // No optimizar puede dejar codigo redundante
-dontshrink     // No shrink puede dejar clases innecesarias
-ignorewarnings // Ignora advertencias de seguridad
```

**Remediacion:**
```proguard
-optimizationpasses 5
-allowaccessmodification
-dontwarn
```

---

## 5. MATRIZ DE RIESGOS CONSOLIDADA

| Vulnerabilidad | Severidad | Probabilidad | Impacto | Riesgo |
|----------------|-----------|--------------|---------|--------|
| Credenciales hardcodeadas | CRITICO | Alta | Critico | 🔴 |
| SSL bypass | CRITICO | Alta | Critico | 🔴 |
| Sal universal hardcodeada | CRITICO | Media | Critico | 🔴 |
| Password en memoria estatica | CRITICO | Alta | Critico | 🔴 |
| String conversion leaks | CRITICO | Alta | Alto | 🔴 |
| AES-128 ECB legacy | ALTO | Baja | Alto | 🟠 |
| Auto-enrollment biometric | ALTO | Media | Alto | 🟠 |
| URL hardcodeada | ALTO | Alta | Medio | 🟠 |
| Threshold TypingDNA bajo | ALTO | Media | Medio | 🟠 |
| Logging excesivo | MEDIO | Alta | Bajo | 🟡 |
| Alertas libres en Firebase | MEDIO | Media | Medio | 🟡 |
| Exportacion en plano | MEDIO | Baja | Alto | 🟡 |

---

## 6. RECOMENDACIONES DE REMEDIACION

### 6.1 Acciones Inmediatas (Criticas - 0-7 dias)

1. **Eliminar credenciales hardcodeadas** de ConexionServidor.java
2. **Implementar verificacion SSL** en ServerService.java
3. **Eliminar sal universal** - generar sales unicas por usuario
4. **No almacenar masterPassword en variable estatica**
5. **Eliminar conversiones char[] a String** innecesarias
6. **Remover archivo blackvault_local.db.enc** del repositorio
7. **Agregar *.enc al .gitignore**

### 6.2 Acciones a Corto Plazo (Alta - 1-4 semanas)

1. **Aumentar threshold TypingDNA** a 50-60%
2. **Eliminar auto-enrollment biometric**
3. **Forzar migracion de archivos legacy** a AES-256-GCM
4. **Implementar logging apropiacion** con SLF4J
5. **Eliminar opcion de exportacion en plano**
6. **Configurar URL de servidor** via variables de entorno
7. **Estandarizar iteraciones PBKDF2** a 600,000

### 6.3 Acciones a Mediano Plazo (Media - 1-3 meses)

1. **Migrar a Argon2id** como funcion de derivacion
2. **Implementar TOTP como 2FA**
3. **Implementar rate limiting** en Firebase
4. **Actualizar dependencias** a versiones seguras
5. **Auditoria de dependencias** con OWASP Dependency-Check
6. **Implementar timeout de sesion**
7. **Reforzar configuracion ProGuard**

### 6.4 Acciones a Largo Plazo (Mejores Practicas)

1. **Implementar HSM** para almacenamiento de claves
2. **Auditoria de seguridad** por empresa externa
3. **Penetration testing** regular
4. **Bug bounty program**
5. **Certificacion de seguridad** (SOC2, ISO27001)

---

## 7. CONCLUSION

El proyecto Blackpassword tiene una **arquitectura criptografica solida** (RSA-2048, AES-256-GCM, PBKDF2) pero presenta **vulnerabilidades criticas** en la implementacion que comprometen su seguridad:

### Top 5 Acciones Prioritarias:
1. ✅ Eliminar todas las credenciales hardcodeadas
2. ✅ Implementar verificacion SSL/TLS
3. ✅ Eliminar sal universal y generar sales unicas
4. ✅ No almacenar contrasenas en memoria estatica
5. ✅ Eliminar conversiones String innecesarias

**No se recomienda utilizar esta version en produccion** hasta resolver las vulnerabilidades criticas. El riesgo de compromiso de contrasenas maestras es inaceptable para un gestor de contrasenas.

---

*Analisis generado: 5 de Abril de 2026*  
*Herramientas utilizadas: Codigo fuente completo, grep, glob, revision manual*
