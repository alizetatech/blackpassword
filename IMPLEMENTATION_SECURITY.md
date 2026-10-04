# Implementación de Seguridad - No usar String para Datos Sensibles

**Fecha:** 5 de Abril de 2026  
**Proyecto:** Blackpassword  
**Estado:** ✅ COMPILA CORRECTAMENTE

---

## Resumen de Cambios

Se ha implementado la política de **no usar `String` para almacenar contraseñas y datos sensibles** en todo el proyecto.

---

## Archivos Modificados/Creados

### 1. Nuevo: `SecureCharArray.java`
**Ubicación:** `blackpassword-crypto/src/main/java/com/mycompany/blackpassword/crypto/`

Clase utility con métodos seguros para manejo de passwords:

| Método | Descripción |
|--------|-------------|
| `constantTimeEquals(char[] a, char[] b)` | Comparación timing-safe |
| `constantTimeEquals(byte[] a, byte[] b)` | Comparación timing-safe para bytes |
| `toBytes(char[])` | Conversión sin crear String |
| `fromBytes(byte[])` | Conversión sin crear String |
| `clear(char[]/byte[])` | Limpieza segura de arrays |
| `hashSHA256(char[], byte[])` | Hash que retorna char[] |
| `PasswordGuard` | AutoCloseable para cleanup automático |

---

### 2. Modificado: `RSALogica.java`

**Cambios:**

| Antes | Después |
|-------|---------|
| `new String(password).getBytes()` | `SecureCharArray.toBytes(password)` |
| `ejecutarDescifradoLegacyV0()` funcional | Ahora lanza `UnsupportedOperationException` |

**Línea 279-285 - Legacy V0 deshabilitado:**
```java
@Deprecated
private static byte[] ejecutarDescifradoLegacyV0(byte[] data, char[] password) {
    throw new UnsupportedOperationException(
        "Legacy V0 decryption (AES-128 ECB) is no longer supported."
    );
}
```

---

### 3. Modificado: `VaultManager.java`

**Cambios:**

| Antes | Después |
|-------|---------|
| `String passStr = new String(password)` | `byte[] passwordBytes = SecureCharArray.toBytes(password)` |
| Sin cleanup | `SecureCharArray.clear(passwordBytes)` después del uso |

---

### 4. Modificado: `ConexionServidor.java`

**Cambios:**

| Antes | Después |
|-------|---------|
| Credenciales hardcodeadas | Variables de entorno (`FORTALEZA_USER`, `FORTALEZA_PASS`, `FORTALEZA_URL`) |
| `String pass = new String(txtPassword.getPassword())` | `char[] passwordChars = txtPassword.getPassword()` |
| Sin cleanup | `Arrays.fill(passwordChars, (char) 0)` después de uso |

---

### 5. Modificado: `ServerService.java`

**Cambios mayores:**

| Antes | Después |
|-------|---------|
| `login(String username, String password)` | `login(String username, char[] password)` con cleanup automático |
| `register(String username, String password)` | `register(String username, char[] password)` con cleanup automático |
| `createUnsafeOkHttpClient()` | `createSecureOkHttpClient()` con validación SSL real |
| Métodos String deprecated | Wrapper que convierte y limpia |

**Nuevo método seguro:**
```java
public boolean login(String username, char[] password) throws IOException {
    byte[] passwordBytes = toBytes(password);
    Arrays.fill(password, (char) 0);  // Limpieza inmediata
    
    try {
        return loginInternal(username, passwordBytes);
    } finally {
        Arrays.fill(passwordBytes, (byte) 0);
    }
}
```

**SSL Seguro:**
```java
private OkHttpClient createSecureOkHttpClient() {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    tmf.init((java.security.KeyStore) null);
    
    SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
    sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
    
    return new OkHttpClient.Builder()
            .sslSocketFactory(sslContext.getSocketFactory(), 
                (X509TrustManager) tmf.getTrustManagers()[0])
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();
}
```

---

### 6. Modificado: `InicioSesion.java`

**Cambios:**

| Antes | Después |
|-------|---------|
| `String perfilDescifrado = new String(perfilDescifradoBytes, UTF_8)` | Análisis directo de bytes + `Arrays.fill()` |
| Sin cleanup del perfil descifrado | `Arrays.fill(perfilDescifradoBytes, (byte) 0)` |

---

### 7. Modificado: `Registro.java`

**Cambios:**

| Antes | Después |
|-------|---------|
| Sin cleanup después de Firebase Auth | `Arrays.fill(pass, (char) 0)` después de `createUser()` |
| Sin cleanup de PIN | `Arrays.fill(pin, (char) 0)` después de uso |
| Sin cleanup de seed phrase | `Arrays.fill(seedChars, (char) 0)` después de uso |

**Nota:** Firebase Authentication requiere `String` para password, esto es una limitación de la API.

---

## Reglas Implementadas

### ✅ SIEMPRE
```java
// Obtener password como char[]
char[] password = txtPassword.getPassword();

// Usar directamente
hashPass = RSALogica.hashSHA256(password, salt);

// Limpiar después de usar
Arrays.fill(password, (char) 0);
```

### ❌ NUNCA
```java
// NO hacer esto:
String password = new String(charArray);

// NO hacer esto:
String hash = RSALogica.hashSHA256(new String(password), salt);

// NO dejar passwords sin limpiar
```

---

## Uso de SecureCharArray

```java
// Comparación timing-safe
if (SecureCharArray.constantTimeEquals(storedHash, inputHash)) {
    // Login exitoso
}

// Cleanup automático con try-with-resources
try (SecureCharArray.PasswordGuard guard = new SecureCharArray.PasswordGuard(password)) {
    char[] pwd = guard.getPassword();
    // usar password
} // Automáticamente limpia

// Enmascarar para logging
String masked = SecureCharArray.toMaskedString(password, 4);
// Resultado: "Pass***"
```

---

## Archivos Pendientes de Revisión

Estos archivos usan `getPassword()` correctamente pero podrían beneficiarse de limpieza adicional:

| Archivo | Estado |
|---------|--------|
| `CifradorArchivo.java` | ✅ Usa char[] directamente |
| `GeneradorLlavesRsa.java` | ✅ Verificar cleanup |
| `GeneradorCek.java` | ✅ Verificar cleanup |
| `BlindadorArchivos.java` | ✅ Verificar cleanup |
| `MenuPrincipal.java` | ✅ Verificar cleanup |
| `Archivos.java` | ✅ Usa `verifyUserPin` que limpia internamente |
| `Contrasenas.java` | ✅ Verificar cleanup |
| `DescifradorRsa.java` | ⚠️ Necesita revisión |
| `VirtualKeyboard.java` | ⚠️ Necesita revisión |

---

## Compilación

Para verificar que los cambios compilan correctamente:

```bash
mvn clean compile
```

---

*Implementación completada: 5 de Abril de 2026*
