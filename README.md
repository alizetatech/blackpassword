# Blackpassword

<p align="center">
  <img src="https://img.shields.io/badge/Security-Zero--Knowledge-brightgreen?style=for-the-badge&logo=shield" alt="Zero-Knowledge" />
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+" />
  <img src="https://img.shields.io/badge/Cryptography-RSA--4096%20%7C%20AES--256--GCM-blue?style=for-the-badge" alt="Cryptography" />
  <img src="https://img.shields.io/badge/Backend-Firebase%20Cloud%20Functions-orange?style=for-the-badge&logo=firebase" alt="Firebase" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-blueviolet?style=for-the-badge" alt="License: GPL-3.0" />
</p>

---

## 🔒 Descripción General

**Blackpassword** es una suite empresarial de ciberseguridad, bóveda de contraseñas y gestión de activos digitales diseñada bajo una estricta arquitectura **Zero-Knowledge (Cero Conocimiento)**. Toda la información confidencial (credenciales, archivos, imágenes, perfiles VPN y billeteras) se cifra y descifra exclusivamente en el dispositivo del usuario antes de cualquier transmisión o sincronización remota.

El servidor backend y la nube nunca tienen acceso a claves privadas, contraseñas en texto plano ni contenido descifrado.

---

## ✨ Características Principales

### 🛡️ 1. Bóveda Zero-Knowledge de Contraseñas y Credenciales
- **Cifrado Híbrido Asimétrico:** Combinación de **RSA-4096** con **AES-256-GCM** para máxima velocidad y seguridad a nivel militar.
- **Sincronización HTTPS Serverless:** Comunicación mediante TLS 1.3 con Firebase Cloud Functions.
- **Control de Duplicados & Auditoría:** Prevención inteligente de sobreescritura accidental y trazabilidad de cambios por credencial.

### 📁 2. Gestor de Archivos e Imágenes Blindadas
- **Cifrador de Medios y Documentos:** Protección en reposo de imágenes, PDFs y documentos confidenciales.
- **Visor Seguro Integrado:** Previsualización y visualización en memoria sin persistir copias temporales descifradas en disco.
- **Almacenamiento Dual:** Soporte para bóveda local blindada y sincronización remota cifrada.

### 🔑 3. Blindaje de Memoria & KEK de Hardware
- **Limpieza de Memoria RAM:** Limpieza activa e inmediata de arrays de memoria (`SecureCharArray`) tras su uso para mitigar ataques de volcado de memoria (RAM dumping).
- **Key Encryption Key (KEK):** Derivación de llaves maestras enlazadas criptográficamente a la huella de hardware del equipo (CPU ID, BIOS, volumen de disco).
- **Protección PIN & Salting Robusto:** Protección mediante funciones de derivación de claves con PBKDF2/Argon2.

### 🌐 4. Túneles VPN WireGuard
- Gestión, generación y sincronización segura de perfiles de conexión cifrada **WireGuard**.
- Almacenamiento blindado de configuraciones de red, IPs de punto final (endpoints) y claves privadas de interfaz VPN.

### 🪙 5. Billeteras Cripto HD (Bitcoin & Dogecoin)
- Soporte para billeteras jerárquicas deterministas (**BIP-32, BIP-39, BIP-44**).
- Generación y custodia de frases semilla mnemónicas cifradas.
- Consulta de saldos, generación de direcciones y códigos QR.

### 🚨 6. Defensa Activa, Honeypots y Kill-Switch
- **Detección de Intrusiones:** Bloqueo reactivo por intentos fallidos de autenticación o manipulación de PIN.
- **Kill-Switch Remoto:** Capacidad de aislar o invalidar sesiones comprometidas en caso de detección de incidentes.
- **Auditoría Criptográfica:** Registro inmutable local y remoto de eventos de seguridad.

---

## 🏛️ Arquitectura del Proyecto

El proyecto está estructurado como un reactor multimódulo de Apache Maven:

```plaintext
Blackpassword/
├── blackpassword-core/       # Modelos de datos, utilidades base y definiciones compartidas.
├── blackpassword-crypto/     # Motores criptográficos (RSA-4096, AES-GCM, KEK, SHA-256, SecureCharArray).
├── blackpassword-services/   # Clientes Cloud Functions HTTPS, WireGuard, Bitcoin/Dogecoin, auditoría y SQLite local.
├── blackpassword-ui/         # Interfaz gráfica moderna con FlatLaf Dark, diálogos, visores y diálogos de seguridad.
└── functions/                # Backend serverless en Node.js (Firebase Cloud Functions con Express y Firebase Admin).
```

---

## 🚀 Requisitos del Sistema

- **Java Development Kit (JDK):** Versión 17 o superior.
- **Apache Maven:** 3.8+ (o el wrapper incluido `./mvnw`).
- **Node.js & npm:** Node.js 22+ (para mantenimiento o despliegue de `functions/`).
- **Firebase CLI:** `npm install -g firebase-tools` (para despliegue de Cloud Functions).

---

## 🛠️ Compilación y Ejecución

### 1. Clonar el Repositorio
```bash
git clone https://github.com/alizetatech/blackpassword.git
cd blackpassword
```

### 2. Compilar y Empaquetar
Para compilar todos los módulos y generar los ejecutables protegidos con ProGuard:

```bash
# En Windows:
.\mvnw.cmd clean package -DskipTests

# En Linux / macOS:
./mvnw clean package -DskipTests
```

### 3. Ejecutar la Aplicación
Una vez compilado, el ejecutable seguro estará disponible en `blackpassword-ui/target/`:

```bash
java -jar blackpassword-ui/target/blackpassword-ui-1.0-SNAPSHOT-secure.jar
```

---

## ☁️ Despliegue de Backend (Cloud Functions)

Si realizas modificaciones en la API serverless de `functions/`:

```bash
cd functions
npm install
firebase deploy --only functions
```

---

## 📄 Licencia

Este proyecto está distribuido bajo la licencia **GNU General Public License v3.0 (GPL-3.0)**. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

<p align="center">
  Desarrollado con enfoque en privacidad absoluta y seguridad de grado criptográfico.
</p>
