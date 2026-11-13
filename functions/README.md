# ☁️ Blackpassword Cloud Functions (Backend Zero-Knowledge)

Este módulo contiene el backend serverless de **Blackpassword** para Firebase Cloud Functions. Actúa como intermediario seguro entre la aplicación de escritorio y tu base de datos, garantizando que:

1. **Tu base de datos Firestore esté 100% aislada:** La app de escritorio nunca se conecta directamente a la base de datos; todas las operaciones pasan por esta API autenticada.
2. **Cero Conocimiento (Zero-Knowledge):** El servidor únicamente recibe y almacena bloques ya cifrados en el cliente (`ciphertext`). Nadie con acceso a Firebase o Google Cloud puede leer las contraseñas o secretos.
3. **Control de Acceso Estricto:** Cada usuario solo puede leer, guardar o borrar sus propios datos (`req.user.uid`).

---

## 🚀 Despliegue en 3 Minutos para tu Propio Firebase

### 1. Requisitos Previos
* Tener una cuenta de Google y crear un proyecto gratuito en [Firebase Console](https://console.firebase.google.com/).
* En tu proyecto de Firebase, activa:
  * **Authentication:** Método de inicio de sesión **Correo electrónico/contraseña**.
  * **Firestore Database:** En modo producción.
  * **Storage:** Para copias cifradas de archivos e imágenes.
* Activa el plan **Blaze** (Pay as you go) en Firebase Console. *(Google incluye 2 millones de llamadas gratis al mes, por lo que para uso personal el costo mensual es de $0.00)*.
* Tener instalado [Node.js](https://nodejs.org/) (versión 18 o 20).

### 2. Instalar Firebase CLI
Si aún no tienes las herramientas de Firebase, abre tu terminal y ejecuta:
```bash
npm install -g firebase-tools
```

### 3. Iniciar Sesión y Seleccionar tu Proyecto
```bash
firebase login
firebase use --add
```
*(Selecciona el ID de tu proyecto de Firebase creado en el paso 1)*.

### 4. Desplegar las Funciones
Dentro de la carpeta `functions/`, instala las dependencias y despliega:
```bash
cd functions
npm install
firebase deploy --only functions,firestore:rules,storage:rules
```

Al terminar, la terminal te mostrará la URL pública de tu API:
```text
✔  functions[api(us-central1)]: Successful create operation.
Function URL (api): https://us-central1-tu-proyecto.cloudfunctions.net/api
```

### 5. Conectar tu Blackpassword Desktop
1. Copia la URL de tu función (`https://us-central1-tu-proyecto.cloudfunctions.net/api`).
2. En la pantalla de inicio de **Blackpassword**, al generar tu paquete `.kek`, introduce:
   * La **URL de tu Cloud Function**.
   * Tu **Firebase Web API Key** (disponible en *Configuración del proyecto > General > Clave de API web*).
3. ¡Listo! Tu bóveda ahora opera a través de tu propio backend privado.
