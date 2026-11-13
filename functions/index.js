const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const crypto = require("crypto");

// Inicialización del Firebase Admin SDK en el servidor seguro de Google Cloud
admin.initializeApp();
const db = admin.firestore();
const storage = admin.storage();

const app = express();

// Habilitar CORS para peticiones HTTPS seguras
app.use(cors({ origin: true }));
app.use(express.json({ limit: "10mb" }));

/**
 * Middleware de Autenticación Zero-Trust:
 * Valida el ID Token JWT emitido por Firebase Auth en el encabezado Authorization.
 * Verifica también el estado de la cuenta para bloqueo o Remote Kill instantáneo.
 */
async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      error: "UNAUTHORIZED",
      message: "Encabezado de autorización ausente o con formato inválido.",
    });
  }

  const idToken = authHeader.split("Bearer ")[1].trim();

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);

    // Verificación de estado de cuenta (Remote Kill)
    if (decodedToken.account_status === "COMPROMISED" || decodedToken.disabled) {
      return res.status(403).json({
        error: "ACCOUNT_COMPROMISED",
        message: "🚨 La cuenta ha sido bloqueada por protocolo de seguridad.",
      });
    }

    req.user = decodedToken;
    next();
  } catch (error) {
    console.error("[Auth] Error al verificar token:", error.message);
    return res.status(401).json({
      error: "INVALID_TOKEN",
      message: "Token de sesión expirado o inválido.",
    });
  }
}

// -------------------------------------------------------------
// HELPER PARA BÚSQUEDA DE PERFIL DE USUARIO MULTI-VERSIÓN
// -------------------------------------------------------------
async function findUserDoc(uid, email) {
  function isCompleteProfile(doc) {
    if (!doc || !doc.exists) return false;
    const data = doc.data();
    return data && (data.perfil_cifrado || data.pin_hash || data.codigopin);
  }

  let foundDoc = null;
  let foundDocPath = null;

  // 1. Priorizar búsqueda por hash de correo en colecciones históricas (donde reside el perfil original)
  if (email) {
    const emailLower = email.toLowerCase().trim();
    const emailRaw = email.trim();

    const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
    const emailHashUpper = emailHashLower.toUpperCase();
    const emailHashRaw = crypto.createHash("sha256").update(emailRaw).digest("hex");

    const colHashLower = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colHashUpper = colHashLower.toUpperCase();

    const candidates = [
      { col: colHashLower, doc: emailHashLower },
      { col: colHashLower, doc: emailHashUpper },
      { col: colHashLower, doc: emailHashRaw },
      { col: colHashUpper, doc: emailHashLower },
      { col: colHashUpper, doc: emailHashUpper },
      { col: "usuarios", doc: emailHashLower },
      { col: "usuarios", doc: emailHashUpper },
      { col: "usuarios", doc: emailLower },
    ];

    for (const c of candidates) {
      try {
        const snap = await db.collection(c.col).doc(c.doc).get();
        if (isCompleteProfile(snap)) {
          foundDoc = snap;
          foundDocPath = snap.ref.path;
          break;
        }
      } catch (err) {}
    }
  }

  // 2. Si no se encontró por hash, verificar si usuarios/{uid} contiene un perfil completo
  if (!foundDoc && uid) {
    const docByUid = await db.collection("usuarios").doc(uid).get();
    if (isCompleteProfile(docByUid)) {
      foundDoc = docByUid;
      foundDocPath = docByUid.ref.path;
    }
  }

  // 3. Sincronizar el perfil completo hacia usuarios/{uid} para consolidar la base de datos
  if (foundDoc && uid && foundDocPath !== `usuarios/${uid}`) {
    try {
      await db.collection("usuarios").doc(uid).set(foundDoc.data(), { merge: true });
    } catch (err) {
      console.warn("[findUserDoc] No se pudo copiar a usuarios/{uid}:", err.message);
    }
  }

  return { doc: foundDoc, docPath: foundDocPath };
}

// -------------------------------------------------------------
// RUTAS DE AUTENTICACIÓN Y PERFIL DE USUARIO
// -------------------------------------------------------------

/**
 * GET /auth/profile
 * Obtiene el documento de perfil cifrado del usuario autenticado.
 */
app.get("/auth/profile", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = req.user.email || req.query.email || "";
    const result = await findUserDoc(uid, email);

    if (!result.doc || !result.doc.exists) {
      return res.status(404).json({
        error: "NOT_FOUND",
        message: "Perfil de usuario no encontrado.",
      });
    }

    const docData = result.doc.data();
    const emailToUse = (email || req.user.email || docData.correo || "").toLowerCase().trim();
    const emailHash = crypto.createHash("sha256").update(emailToUse).digest("hex");
    const uidHash = crypto.createHash("sha256").update(uid).digest("hex");

    const fullClaims = {
      role: req.user.role || docData.role || "USER",
      security_level: req.user.security_level || docData.security_level || 1,
      account_status: req.user.account_status || docData.status || "ACTIVE",
      uid_hash: req.user.uid_hash || docData.uid_hash || uidHash,
      email_hash: req.user.email_hash || docData.email_hash || emailHash,
      pubkey_hash: req.user.pubkey_hash || docData.pubkey_hash || "",
      ...req.user,
    };

    return res.json({
      success: true,
      profile: docData,
      docPath: result.docPath,
      customClaims: fullClaims,
    });
  } catch (err) {
    console.error("[Auth Profile] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * POST /auth/profile/update
 * Actualiza campos del perfil (ubicación cifrada, typing profile, last_login, etc.)
 */
app.post("/auth/profile/update", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = req.user.email || req.body.email || "";
    const { updates, docPath } = req.body;

    if (!updates || typeof updates !== "object") {
      return res.status(400).json({ error: "MISSING_DATA", message: "Faltan datos de actualización." });
    }

    updates.actualizado_en = admin.firestore.FieldValue.serverTimestamp();

    if (docPath) {
      await db.doc(docPath).set(updates, { merge: true });
    } else {
      const result = await findUserDoc(uid, email);
      if (result.doc) {
        await result.doc.ref.set(updates, { merge: true });
      }
    }

    await db.collection("usuarios").doc(uid).set(updates, { merge: true });

    return res.json({ success: true, message: "Perfil actualizado." });
  } catch (err) {
    console.error("[Auth Profile Update] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * POST /auth/register-profile
 * Guarda el perfil inicial cifrado y establece los Custom Claims de seguridad.
 */
app.post("/auth/register-profile", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.user.email || req.body.email || "").toLowerCase().trim();
    const {
      uid_hash,
      perfil_cifrado,
      certificado_digital,
      user_salt,
      pin_hash,
      typing_profile,
      pubkey_hash,
    } = req.body;

    if (!perfil_cifrado || !user_salt || !pin_hash) {
      return res.status(400).json({ error: "MISSING_DATA", message: "Faltan campos obligatorios para el registro." });
    }

    const emailHash = crypto.createHash("sha256").update(email).digest("hex");
    const colHash = crypto.createHash("sha256").update("usuarios").digest("hex");

    await admin.auth().setCustomUserClaims(uid, {
      role: "USER",
      security_level: 1,
      account_status: "ACTIVE",
      uid_hash: uid_hash || "",
      email_hash: emailHash,
      pubkey_hash: pubkey_hash || "",
    });

    const docData = {
      uid_hash: uid_hash || "",
      perfil_cifrado: perfil_cifrado,
      certificado_digital: certificado_digital || "",
      user_salt: user_salt,
      pin_hash: pin_hash,
      typing_profile: typing_profile || "",
      status: "ACTIVE",
      email_hash: emailHash,
      pubkey_hash: pubkey_hash || "",
      fecha_registro: admin.firestore.FieldValue.serverTimestamp(),
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("usuarios").doc(uid).set(docData, { merge: true });
    await db.collection(colHash).doc(emailHash).set(docData, { merge: true });

    return res.json({ success: true, message: "Registro completado con éxito." });
  } catch (err) {
    console.error("[Auth Register Profile] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * POST /auth/kill-switch
 * Activa el kill switch para la cuenta actual.
 */
app.post("/auth/kill-switch", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.user.email || req.body.email || "").toLowerCase().trim();
    const emailHash = crypto.createHash("sha256").update(email).digest("hex");
    const colHash = crypto.createHash("sha256").update("usuarios").digest("hex");

    const currentClaims = req.user || {};
    await admin.auth().setCustomUserClaims(uid, {
      ...currentClaims,
      account_status: "COMPROMISED",
    });

    const updates = {
      status: "COMPROMISED",
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("usuarios").doc(uid).set(updates, { merge: true });
    await db.collection(colHash).doc(emailHash).set(updates, { merge: true });

    return res.json({ success: true, message: "Kill switch activado." });
  } catch (err) {
    console.error("[Kill Switch] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * GET /auth/status
 * Verifica el estado de seguridad de la cuenta.
 */
app.get("/auth/status", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = req.user.email || "";
    const result = await findUserDoc(uid, email);
    const data = result.doc && result.doc.exists ? result.doc.data() : {};
    const status = data.status || req.user.account_status || "ACTIVE";
    return res.json({ success: true, status: status });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// RUTAS DE LA BÓVEDA (VAULT ZERO-KNOWLEDGE MULTI-VERSIÓN)
// -------------------------------------------------------------

async function getVaultCollections(uid, emailInput) {
  let email = emailInput || "";
  if (!email && uid) {
    try {
      const u = await admin.auth().getUser(uid);
      if (u && u.email) email = u.email;
    } catch (err) {}
  }

  const collections = [];

  // 1. Colección moderna por UID
  if (uid) {
    collections.push(db.collection("usuarios").doc(uid).collection("boveda"));
  }

  // 2. Colecciones legacy basadas en hashes de colección y usuario
  if (email) {
    const emailLower = email.toLowerCase().trim();
    const emailRaw = email.trim();

    const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
    const emailHashUpper = emailHashLower.toUpperCase();
    const emailHashRaw = crypto.createHash("sha256").update(emailRaw).digest("hex");
    const emailHashRawUpper = emailHashRaw.toUpperCase();

    const colUsuariosLower = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colUsuariosUpper = colUsuariosLower.toUpperCase();
    const colBovedaLower = crypto.createHash("sha256").update("boveda").digest("hex");
    const colBovedaUpper = colBovedaLower.toUpperCase();

    const emailHashes = Array.from(new Set([emailHashLower, emailHashUpper, emailHashRaw, emailHashRawUpper]));
    const colUsuariosList = [colUsuariosLower, colUsuariosUpper, "usuarios"];
    const colBovedaList = [colBovedaLower, colBovedaUpper, "boveda"];

    for (const cUser of colUsuariosList) {
      for (const eHash of emailHashes) {
        for (const cBov of colBovedaList) {
          collections.push(db.collection(cUser).doc(eHash).collection(cBov));
        }
      }
    }
  }

  return { collections, email };
}

/**
 * POST /vault/save
 * Guarda o actualiza una credencial cifrada en la bóveda del usuario (dual-sync).
 */
app.post("/vault/save", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.query.email || req.user.email || "").toLowerCase().trim();
    const { id, payload_cifrado, metadata_cifrada, tag } = req.body;

    if (!id || !payload_cifrado) {
      return res.status(400).json({ error: "MISSING_DATA", message: "Faltan campos obligatorios." });
    }

    const payloadToSave = {
      id: id,
      payload_cifrado: payload_cifrado,
      secreto_cifrado: payload_cifrado,
      metadata_cifrada: metadata_cifrada || null,
      tag: tag || "general",
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
      fecha_actualizacion: admin.firestore.FieldValue.serverTimestamp(),
    };

    // Guardar en colección moderna
    await db.collection("usuarios").doc(uid).collection("boveda").doc(id).set(payloadToSave, { merge: true });

    // Guardar en colección legacy para compatibilidad
    if (email) {
      const emailHash = crypto.createHash("sha256").update(email).digest("hex");
      const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
      const colBoveda = crypto.createHash("sha256").update("boveda").digest("hex");
      await db.collection(colUsuarios).doc(emailHash).collection(colBoveda).doc(id).set(payloadToSave, { merge: true });
    }

    return res.json({ success: true, message: "Elemento guardado en bóveda." });
  } catch (err) {
    console.error("[Vault Save] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * GET /vault/list
 * Recupera todas las credenciales cifradas del usuario autenticado (busca en todas las colecciones históricas).
 */
app.get("/vault/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const { collections } = await getVaultCollections(uid, email);

    const itemsMap = new Map();

    const snapshots = await Promise.allSettled(
      collections.map((colRef) => colRef.get())
    );

    for (const snapResult of snapshots) {
      if (snapResult.status === "fulfilled" && !snapResult.value.empty) {
        snapResult.value.forEach((doc) => {
          if (!itemsMap.has(doc.id)) {
            const data = doc.data();
            const payload = data.payload_cifrado || data.secreto_cifrado || null;
            if (payload) {
              itemsMap.set(doc.id, {
                id: doc.id,
                payload_cifrado: payload,
                secreto_cifrado: payload,
                metadata_cifrada: data.metadata_cifrada || "",
                tag: data.tag || data.service || "general",
                fecha_actualizacion: data.fecha_actualizacion || data.actualizado_en || null,
              });
            }
          }
        });
      }
    }

    const items = Array.from(itemsMap.values());
    console.log(`[Vault List] Recuperados ${items.length} secretos para uid=${uid}, email=${email}`);
    return res.json({ success: true, count: items.length, items: items });
  } catch (err) {
    console.error("[Vault List] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

/**
 * POST /vault/delete
 * Elimina una credencial de la bóveda en todas las colecciones del usuario.
 */
app.post("/vault/delete", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.query.email || req.user.email || "").toLowerCase().trim();
    const { id } = req.body;

    if (!id) {
      return res.status(400).json({ error: "MISSING_ID", message: "Debe indicar el ID a eliminar." });
    }

    const { collections } = await getVaultCollections(uid, email);
    await Promise.allSettled(collections.map((colRef) => colRef.doc(id).delete()));

    return res.json({ success: true, message: "Elemento eliminado de la bóveda." });
  } catch (err) {
    console.error("[Vault Delete] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// RUTAS DE ARCHIVOS (FILES ZERO-KNOWLEDGE MULTI-VERSIÓN)
// -------------------------------------------------------------

async function getFilesCollections(uid, emailInput) {
  let email = emailInput || "";
  if (!email && uid) {
    try {
      const u = await admin.auth().getUser(uid);
      if (u && u.email) email = u.email;
    } catch (err) {}
  }

  const collections = [];
  if (uid) {
    collections.push(db.collection("usuarios").doc(uid).collection("archivos"));
  }

  if (email) {
    const emailLower = email.toLowerCase().trim();
    const emailRaw = email.trim();
    const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
    const emailHashUpper = emailHashLower.toUpperCase();
    const emailHashRaw = crypto.createHash("sha256").update(emailRaw).digest("hex");
    const colUsuariosLower = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colUsuariosUpper = colUsuariosLower.toUpperCase();
    const colArchivosLower = crypto.createHash("sha256").update("archivos").digest("hex");
    const colArchivosUpper = colArchivosLower.toUpperCase();

    const emailHashes = Array.from(new Set([emailHashLower, emailHashUpper, emailHashRaw]));
    const colUsuariosList = [colUsuariosLower, colUsuariosUpper, "usuarios"];
    const colArchivosList = [colArchivosLower, colArchivosUpper, "archivos"];

    for (const cUser of colUsuariosList) {
      for (const eHash of emailHashes) {
        for (const cArch of colArchivosList) {
          collections.push(db.collection(cUser).doc(eHash).collection(cArch));
        }
      }
    }
  }

  return { collections, email };
}

app.get("/files/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const { collections } = await getFilesCollections(uid, email);

    const itemsMap = new Map();
    const snapshots = await Promise.allSettled(collections.map((c) => c.get()));

    for (const snapResult of snapshots) {
      if (snapResult.status === "fulfilled" && !snapResult.value.empty) {
        snapResult.value.forEach((doc) => {
          if (!itemsMap.has(doc.id)) {
            const data = doc.data();
            itemsMap.set(doc.id, {
              id: doc.id,
              metadata_cifrada: data.metadata_cifrada || "",
              contenido_cifrado: data.contenido_cifrado || "",
              fecha: data.fecha_actualizacion || data.actualizado_en || doc.createTime || null,
            });
          }
        });
      }
    }

    return res.json({ success: true, count: itemsMap.size, items: Array.from(itemsMap.values()) });
  } catch (err) {
    console.error("[Files List] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/files/save", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.query.email || req.user.email || "").toLowerCase().trim();
    const { id, metadata_cifrada, contenido_cifrado } = req.body;

    if (!metadata_cifrada) {
      return res.status(400).json({ error: "MISSING_DATA", message: "Falta metadata cifrada." });
    }

    const docId = id || crypto.randomUUID();
    const fileData = {
      id: docId,
      metadata_cifrada: metadata_cifrada,
      contenido_cifrado: contenido_cifrado || "",
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
      fecha_actualizacion: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("usuarios").doc(uid).collection("archivos").doc(docId).set(fileData, { merge: true });

    if (email) {
      const emailHash = crypto.createHash("sha256").update(email).digest("hex");
      const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
      const colArchivos = crypto.createHash("sha256").update("archivos").digest("hex");
      await db.collection(colUsuarios).doc(emailHash).collection(colArchivos).doc(docId).set(fileData, { merge: true });
    }

    return res.json({ success: true, id: docId, message: "Archivo guardado." });
  } catch (err) {
    console.error("[Files Save] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.get("/files/get", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const docId = req.query.id;

    if (!docId) return res.status(400).json({ error: "MISSING_ID" });

    const { collections } = await getFilesCollections(uid, email);
    for (const c of collections) {
      try {
        const snap = await c.doc(docId).get();
        if (snap.exists) {
          const data = snap.data();
          return res.json({
            success: true,
            id: snap.id,
            metadata_cifrada: data.metadata_cifrada || "",
            contenido_cifrado: data.contenido_cifrado || "",
          });
        }
      } catch (err) {}
    }

    return res.status(404).json({ error: "NOT_FOUND", message: "Archivo no encontrado." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/files/delete", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { id } = req.body;

    if (!id) return res.status(400).json({ error: "MISSING_ID" });

    const { collections } = await getFilesCollections(uid, email);
    await Promise.allSettled(collections.map((c) => c.doc(id).delete()));

    return res.json({ success: true, message: "Archivo eliminado." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// RUTAS DE IMÁGENES (IMAGES ZERO-KNOWLEDGE MULTI-VERSIÓN)
// -------------------------------------------------------------

async function getImagesCollections(uid, emailInput) {
  let email = emailInput || "";
  if (!email && uid) {
    try {
      const u = await admin.auth().getUser(uid);
      if (u && u.email) email = u.email;
    } catch (err) {}
  }

  const collections = [];
  const visitedPaths = new Set();

  function addCollection(colRef) {
    if (colRef && !visitedPaths.has(colRef.path)) {
      visitedPaths.add(colRef.path);
      collections.push(colRef);
    }
  }

  if (uid) {
    addCollection(db.collection("usuarios").doc(uid).collection("imagenes"));
    addCollection(db.collection("usuarios").doc(uid).collection("images"));
  }

  // 1. Descubrir documento real del usuario
  const candidateDocs = [];
  if (uid) {
    candidateDocs.push(db.collection("usuarios").doc(uid));
  }

  if (email) {
    const emailLower = email.toLowerCase().trim();
    const emailRaw = email.trim();
    const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
    const emailHashUpper = emailHashLower.toUpperCase();
    const emailHashRaw = crypto.createHash("sha256").update(emailRaw).digest("hex");
    const emailHashRawUpper = emailHashRaw.toUpperCase();

    const colUsuariosLower = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colUsuariosUpper = colUsuariosLower.toUpperCase();
    const colImagenesLower = crypto.createHash("sha256").update("imagenes").digest("hex");
    const colImagenesUpper = colImagenesLower.toUpperCase();
    const colImagesLower = crypto.createHash("sha256").update("images").digest("hex");
    const colImagesUpper = colImagesLower.toUpperCase();

    const emailHashes = Array.from(new Set([emailHashLower, emailHashUpper, emailHashRaw, emailHashRawUpper]));
    const colUsuariosList = [colUsuariosLower, colUsuariosUpper, "usuarios"];
    const colImagenesList = [
      "imagenes", "IMAGENES", "images", "IMAGES",
      colImagenesLower, colImagenesUpper, colImagesLower, colImagesUpper
    ];

    for (const cUser of colUsuariosList) {
      for (const eHash of emailHashes) {
        candidateDocs.push(db.collection(cUser).doc(eHash));
        for (const cImg of colImagenesList) {
          addCollection(db.collection(cUser).doc(eHash).collection(cImg));
        }
      }
    }
  }

  // 2. Inspección dinámica de subcolecciones en todos los documentos candidatos
  for (const docRef of candidateDocs) {
    try {
      const subcols = await docRef.listCollections();
      for (const sc of subcols) {
        const idLower = sc.id.toLowerCase();
        const scHashLower = crypto.createHash("sha256").update("imagenes").digest("hex").toLowerCase();
        const scHashImages = crypto.createHash("sha256").update("images").digest("hex").toLowerCase();
        if (
          idLower === "imagenes" ||
          idLower === "images" ||
          idLower === "image" ||
          idLower === "imagen" ||
          idLower === scHashLower ||
          idLower === scHashImages ||
          idLower.includes("imag") ||
          idLower.includes("foto") ||
          idLower.includes("galer")
        ) {
          addCollection(sc);
        }
      }
    } catch (err) {
      // Ignorar si no existe el documento
    }
  }

  return { collections, email };
}

app.get("/images/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    console.log(`[Images List] Solicitud recibida para uid=${uid}, email=${email}`);

    const { collections } = await getImagesCollections(uid, email);
    console.log(`[Images List] Colecciones a consultar (${collections.length}):`, collections.map((c) => c.path));

    const itemsMap = new Map();
    const snapshots = await Promise.allSettled(collections.map((c) => c.get()));

    for (let i = 0; i < snapshots.length; i++) {
      const snapResult = snapshots[i];
      if (snapResult.status === "fulfilled" && !snapResult.value.empty) {
        console.log(`[Images List] Colección ${collections[i].path} tiene ${snapResult.value.size} documentos`);
        snapResult.value.forEach((doc) => {
          if (!itemsMap.has(doc.id)) {
            const data = doc.data();
            console.log(`[Images List] Documento encontrado en ${collections[i].path}: id=${doc.id}, campos=`, Object.keys(data));
            itemsMap.set(doc.id, {
              id: doc.id,
              metadata_cifrada: data.metadata_cifrada || data.meta_cifrada || data.metadata || data.payload_cifrado || "",
              storage_path: data.storage_path || data.storagePath || data.path || data.ruta || "",
              timestamp: data.timestamp || data.actualizado_en || null,
            });
          }
        });
      }
    }

    // Si aún no se encuentran documentos, buscar en Storage directamente como fallback
    if (itemsMap.size === 0 && email) {
      try {
        const emailLower = email.toLowerCase().trim();
        const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
        const emailHashRaw = crypto.createHash("sha256").update(email.trim()).digest("hex");
        const bucket = storage.bucket();

        for (const eHash of [emailHashLower, emailHashRaw, uid]) {
          const [files] = await bucket.getFiles({ prefix: `imagenes/${eHash}/`, maxResults: 100 });
          if (files && files.length > 0) {
            console.log(`[Images List] Encontrados ${files.length} archivos en Storage bajo imagenes/${eHash}/`);
            for (const file of files) {
              const fileId = file.name.split("/").pop();
              if (fileId && !itemsMap.has(fileId)) {
                itemsMap.set(fileId, {
                  id: fileId,
                  metadata_cifrada: "",
                  storage_path: file.name,
                  timestamp: file.metadata ? file.metadata.timeCreated : null,
                });
              }
            }
          }
        }
      } catch (storageErr) {
        console.warn("[Images List] Fallback Storage error:", storageErr.message);
      }
    }

    const items = Array.from(itemsMap.values());
    console.log(`[Images List] Devolviendo ${items.length} imágenes.`);
    return res.json({ success: true, count: items.length, items: items });
  } catch (err) {
    console.error("[Images List] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/images/save", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { docId, metadata_cifrada, image_bytes_base64, storage_path } = req.body;

    const emailHash = email ? crypto.createHash("sha256").update(email).digest("hex") : uid;
    const finalDocId = docId || crypto.randomUUID();
    let finalStoragePath = storage_path;

    if (image_bytes_base64) {
      finalStoragePath = `imagenes/${emailHash}/${finalDocId}`;
      const bucket = storage.bucket();
      const file = bucket.file(finalStoragePath);
      const buffer = Buffer.from(image_bytes_base64, "base64");
      await file.save(buffer, { contentType: "application/octet-stream" });
    }

    const docData = {
      id: finalDocId,
      metadata_cifrada: metadata_cifrada || "",
      storage_path: finalStoragePath || "",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("usuarios").doc(uid).collection("imagenes").doc(finalDocId).set(docData, { merge: true });

    if (email) {
      const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
      await db.collection(colUsuarios).doc(emailHash).collection("imagenes").doc(finalDocId).set(docData, { merge: true });
    }

    return res.json({ success: true, docId: finalDocId, storage_path: finalStoragePath });
  } catch (err) {
    console.error("[Images Save] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.get("/images/content", requireAuth, async (req, res) => {
  try {
    const storagePath = req.query.storage_path;
    if (!storagePath) return res.status(400).json({ error: "MISSING_STORAGE_PATH" });

    const bucket = storage.bucket();
    const file = bucket.file(storagePath);
    const [exists] = await file.exists();
    if (!exists) return res.status(404).json({ error: "NOT_FOUND", message: "Archivo de imagen no encontrado en Cloud Storage." });

    const [fileBuffer] = await file.download();
    res.setHeader("Content-Type", "application/octet-stream");
    return res.send(fileBuffer);
  } catch (err) {
    console.error("[Images Content] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/images/delete", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { docId, storage_path } = req.body;

    if (storage_path) {
      try {
        await storage.bucket().file(storage_path).delete();
      } catch (err) {}
    }

    if (docId) {
      const { collections } = await getImagesCollections(uid, email);
      await Promise.allSettled(collections.map((c) => c.doc(docId).delete()));
    }

    return res.json({ success: true, message: "Imagen eliminada." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// RUTAS DE WIREGUARD VPN (MULTI-VERSIÓN)
// -------------------------------------------------------------

async function getWireguardCollections(uid, emailInput) {
  let email = emailInput || "";
  if (!email && uid) {
    try {
      const u = await admin.auth().getUser(uid);
      if (u && u.email) email = u.email;
    } catch (err) {}
  }

  const collections = [];
  if (uid) {
    collections.push(db.collection("usuarios").doc(uid).collection("wireguard"));
  }

  if (email) {
    const emailLower = email.toLowerCase().trim();
    const emailHashLower = crypto.createHash("sha256").update(emailLower).digest("hex");
    const emailHashUpper = emailHashLower.toUpperCase();
    const colUsuariosLower = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colUsuariosUpper = colUsuariosLower.toUpperCase();
    const colWireguardLower = crypto.createHash("sha256").update("wireguard_vpn").digest("hex");
    const colWireguardUpper = colWireguardLower.toUpperCase();

    const emailHashes = Array.from(new Set([emailHashLower, emailHashUpper]));
    const colUsuariosList = [colUsuariosLower, colUsuariosUpper, "usuarios"];
    const colWgList = [colWireguardLower, colWireguardUpper, "wireguard_vpn", "wireguard"];

    for (const cUser of colUsuariosList) {
      for (const eHash of emailHashes) {
        for (const cWg of colWgList) {
          collections.push(db.collection(cUser).doc(eHash).collection(cWg));
        }
      }
    }
  }

  return { collections, email };
}

app.post("/wireguard/save", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { id, nombre_perfil, payload_cifrado, address, endpoint } = req.body;

    if (!id || !payload_cifrado) {
      return res.status(400).json({ error: "MISSING_DATA", message: "Datos de perfil incompletos." });
    }

    const wgData = {
      id: id,
      nombre_perfil: nombre_perfil || "Perfil WireGuard",
      payload_cifrado: payload_cifrado,
      address: address || "",
      endpoint: endpoint || "",
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
      fecha_actualizacion: admin.firestore.FieldValue.serverTimestamp(),
      ts: Date.now(),
    };

    await db.collection("usuarios").doc(uid).collection("wireguard").doc(id).set(wgData, { merge: true });

    if (email) {
      const emailHash = crypto.createHash("sha256").update(email).digest("hex");
      const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
      const colWireguard = crypto.createHash("sha256").update("wireguard_vpn").digest("hex");
      await db.collection(colUsuarios).doc(emailHash).collection(colWireguard).doc(id).set(wgData, { merge: true });
    }

    return res.json({ success: true, message: "Perfil VPN guardado." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.get("/wireguard/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const { collections } = await getWireguardCollections(uid, email);

    const itemsMap = new Map();
    const snapshots = await Promise.allSettled(collections.map((c) => c.get()));

    for (const snapResult of snapshots) {
      if (snapResult.status === "fulfilled" && !snapResult.value.empty) {
        snapResult.value.forEach((doc) => {
          if (!itemsMap.has(doc.id)) {
            const data = doc.data();
            itemsMap.set(doc.id, {
              id: doc.id,
              nombre_perfil: data.nombre_perfil || doc.id,
              payload_cifrado: data.payload_cifrado || "",
              address: data.address || "",
              endpoint: data.endpoint || "",
              ts: data.ts || null,
              actualizado_en: data.actualizado_en || data.fecha_actualizacion || null,
            });
          }
        });
      }
    }

    return res.json({ success: true, count: itemsMap.size, items: Array.from(itemsMap.values()) });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/wireguard/delete", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { id } = req.body;
    if (!id) return res.status(400).json({ error: "MISSING_ID" });

    const { collections } = await getWireguardCollections(uid, email);
    await Promise.allSettled(collections.map((c) => c.doc(id).delete()));

    return res.json({ success: true, message: "Perfil VPN eliminado." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// RUTAS DE BILLETERAS CRIPTO (BITCOIN & DOGECOIN MULTI-VERSIÓN)
// -------------------------------------------------------------

app.get("/wallets/get", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const walletType = (req.query.wallet_type || "BTC").toUpperCase();
    const network = (req.query.network || "testnet").toLowerCase();

    const emailHash = email ? crypto.createHash("sha256").update(email).digest("hex") : uid;
    const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
    const colWalletKey = walletType === "BTC" ? "billetera_btc" : "billetera_doge";
    const colWallet = crypto.createHash("sha256").update(colWalletKey).digest("hex");

    const candidatePaths = [
      db.collection("usuarios").doc(uid).collection("wallets").doc(`${walletType}_${network}`),
      db.collection(colUsuarios).doc(emailHash).collection(colWallet).doc(network),
      db.collection("usuarios").doc(emailHash).collection(colWallet).doc(network),
      db.collection(colUsuarios).doc(emailHash.toUpperCase()).collection(colWallet).doc(network),
    ];

    for (const docRef of candidatePaths) {
      try {
        const snap = await docRef.get();
        if (snap.exists) {
          const data = snap.data();
          return res.json({
            success: true,
            payload_cifrado: data.payload_cifrado || data.seed_cifrada || "",
            red: data.red || network,
          });
        }
      } catch (err) {}
    }

    return res.status(404).json({ error: "NOT_FOUND", message: "Billetera no encontrada." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/wallets/save", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.body.email || req.user.email || "").toLowerCase().trim();
    const { wallet_type, network, payload_cifrado } = req.body;

    if (!payload_cifrado) return res.status(400).json({ error: "MISSING_PAYLOAD" });

    const wType = (wallet_type || "BTC").toUpperCase();
    const net = (network || "testnet").toLowerCase();
    const docId = `${wType}_${net}`;

    const docData = {
      payload_cifrado: payload_cifrado,
      red: net === "testnet" ? "TESTNET" : "MAINNET",
      wallet_type: wType,
      actualizado_en: admin.firestore.FieldValue.serverTimestamp(),
      fecha_actualizacion: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("usuarios").doc(uid).collection("wallets").doc(docId).set(docData, { merge: true });

    if (email) {
      const emailHash = crypto.createHash("sha256").update(email).digest("hex");
      const colUsuarios = crypto.createHash("sha256").update("usuarios").digest("hex");
      const colWalletKey = wType === "BTC" ? "billetera_btc" : "billetera_doge";
      const colWallet = crypto.createHash("sha256").update(colWalletKey).digest("hex");
      await db.collection(colUsuarios).doc(emailHash).collection(colWallet).doc(net).set(docData, { merge: true });
    }

    return res.json({ success: true, message: "Billetera guardada." });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.get("/wallets/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const snapshot = await db.collection("usuarios").doc(uid).collection("wallets").get();
    const wallets = [];
    snapshot.forEach((doc) => wallets.push({ id: doc.id, ...doc.data() }));
    return res.json({ success: true, items: wallets });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// AUDITORÍA Y REPORTES
// -------------------------------------------------------------

app.get("/audit/list", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const email = (req.query.email || req.user.email || "").toLowerCase().trim();
    const emailHash = email ? crypto.createHash("sha256").update(email).digest("hex") : "";

    const queries = [
      db.collection("audit_logs").where("uid", "==", uid).limit(50).get(),
    ];
    if (emailHash) {
      queries.push(db.collection("audit_logs").where("email_hash", "==", emailHash).limit(50).get());
    }

    const snaps = await Promise.allSettled(queries);
    const logsMap = new Map();

    for (const snap of snaps) {
      if (snap.status === "fulfilled" && !snap.value.empty) {
        snap.value.forEach((d) => {
          if (!logsMap.has(d.id)) {
            logsMap.set(d.id, { id: d.id, ...d.data() });
          }
        });
      }
    }

    const items = Array.from(logsMap.values());
    items.sort((a, b) => {
      const tA = a.timestamp ? (a.timestamp._seconds || 0) : 0;
      const tB = b.timestamp ? (b.timestamp._seconds || 0) : 0;
      return tB - tA;
    });

    return res.json({ success: true, count: items.length, items: items.slice(0, 50) });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// STORAGE CON URLs FIRMADAS (V4 SIGNED URLS)
// El cliente sube/descarga directamente a Cloud Storage sin saturar la función
// -------------------------------------------------------------

app.post("/storage/get-upload-url", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const { file_id, content_type } = req.body;

    if (!file_id) return res.status(400).json({ error: "MISSING_FILE_ID" });

    const bucket = storage.bucket();
    const filePath = `archivos/${uid}/${file_id}`;
    const file = bucket.file(filePath);

    // URL firmada válida por 15 minutos para subir (PUT)
    const [signedUrl] = await file.getSignedUrl({
      version: "v4",
      action: "write",
      expires: Date.now() + 15 * 60 * 1000,
      contentType: content_type || "application/octet-stream",
    });

    return res.json({ success: true, upload_url: signedUrl, file_path: filePath });
  } catch (err) {
    console.error("[Storage Upload] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

app.post("/storage/get-download-url", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const { file_id } = req.body;

    if (!file_id) return res.status(400).json({ error: "MISSING_FILE_ID" });

    const bucket = storage.bucket();
    const filePath = `archivos/${uid}/${file_id}`;
    const file = bucket.file(filePath);

    const [exists] = await file.exists();
    if (!exists) {
      return res.status(404).json({ error: "NOT_FOUND", message: "El archivo no existe." });
    }

    // URL firmada válida por 15 minutos para descargar (GET)
    const [signedUrl] = await file.getSignedUrl({
      version: "v4",
      action: "read",
      expires: Date.now() + 15 * 60 * 1000,
    });

    return res.json({ success: true, download_url: signedUrl });
  } catch (err) {
    console.error("[Storage Download] Error:", err);
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// -------------------------------------------------------------
// AUDITORÍA INMUTABLE EN SERVIDOR
// -------------------------------------------------------------

app.post("/audit/log", requireAuth, async (req, res) => {
  try {
    const uid = req.user.uid;
    const { accion, detalles_cifrados } = req.body;

    await db.collection("audit_logs").add({
      uid: uid,
      email: req.user.email || "N/A",
      accion: accion || "UNKNOWN_EVENT",
      detalles_cifrados: detalles_cifrados || "",
      ip: req.ip || req.headers["x-forwarded-for"] || "N/A",
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
    });

    return res.json({ success: true });
  } catch (err) {
    return res.status(500).json({ error: "INTERNAL_ERROR", message: err.message });
  }
});

// Chequeo de estado
app.get("/status", (req, res) => {
  res.json({
    status: "ONLINE",
    service: "Blackpassword Cloud Functions API",
    version: "2.0.0",
    timestamp: new Date().toISOString(),
  });
});

// Exportar como Cloud Function HTTPS
exports.api = functions.https.onRequest(app);
