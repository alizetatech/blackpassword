package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.mycompany.blackpassword.services.CloudFunctionsClient;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.LocationService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.services.TypingDNAManager;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

/**
 * Inicio de Sesión v8.0-SMART-LEARNING.
 * Implementa Aprendizaje Activo: El sistema actualiza el perfil biométrico tras un bypass exitoso por PIN.
 */
public class InicioSesion extends javax.swing.JInternalFrame {

    private final TypingDNAManager typingDNA = new TypingDNAManager();

    public InicioSesion() {
        initComponents();
        applyPremiumStyle();
        setupTypingDNA();
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jLabel3.putClientProperty(FlatClientProperties.STYLE, "font: bold 24; foreground: #0078D4");
        
        lblVersion.setText("v8.5-SMART-SECURE");
        lblVersion.putClientProperty(FlatClientProperties.STYLE, "font: bold 10; foreground: #999999; background: #F8F9FA; border: 2,8,2,8, #F8F9FA, 1, 10");
        
        String inputStyle = "arc: 12; background: #FFFFFF";
        txtCorreo.putClientProperty(FlatClientProperties.STYLE, inputStyle);
        txtCorreo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ejemplo@correo.com");
        
        txtPassword.putClientProperty(FlatClientProperties.STYLE, inputStyle + "; showRevealButton: true");
        
        btnEntrar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold 14");
        btnVolver.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #F3F3F3; foreground: #666666; hoverBackground: #E5E5E5; borderWidth: 0; font: bold");
        btnRegistrar.putClientProperty(FlatClientProperties.STYLE, "foreground: #0078D4; font: bold 12");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 1,1,1,1, #E0E0E0, 1, 20");
    }

    private void setupTypingDNA() {
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_BACK_SPACE && e.getKeyCode() != KeyEvent.VK_DELETE) {
                    typingDNA.recordKeyPress();
                } else {
                    typingDNA.reset();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_BACK_SPACE && e.getKeyCode() != KeyEvent.VK_DELETE) {
                    typingDNA.recordKeyRelease();
                }
            }
        });
    }

    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        lblVersion = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        txtCorreo = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        btnVirtualKey = new javax.swing.JButton();
        btnEntrar = new javax.swing.JButton();
        btnVolver = new javax.swing.JButton();
        btnRegistrar = new javax.swing.JButton();

        setClosable(false); setTitle("🛡️ ACCESO v8.0");
        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(SwingConstants.CENTER); jLabel3.setText("Identidad Digital");
        lblVersion.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel1.setText("Correo Electrónico:"); jLabel2.setText("Contraseña:");
        btnVirtualKey.setText("⌨️"); btnVirtualKey.addActionListener(evt -> VirtualKeyboard.show(javax.swing.SwingUtilities.getWindowAncestor(this), txtPassword));
        btnEntrar.setText("DESCIFRAR Y ENTRAR"); btnEntrar.addActionListener(evt -> btnEntrarActionPerformed(evt));
        btnVolver.setText("⬅️ VOLVER AL PERFIL"); btnVolver.addActionListener(evt -> btnVolverActionPerformed(evt));
        btnRegistrar.setText("¿No tienes cuenta? Regístrate aquí"); btnRegistrar.addActionListener(evt -> btnRegistrarActionPerformed(evt));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblVersion, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jLabel1)
                    .addComponent(txtCorreo)
                    .addComponent(jLabel2)
                    .addGroup(jPanelCardLayout.createSequentialGroup()
                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnEntrar, javax.swing.GroupLayout.PREFERRED_SIZE, 301, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnRegistrar, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
                .addContainerGap(45, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lblVersion))
                .addGap(30, 30, 30)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKey, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(25, 25, 25)
                .addComponent(btnEntrar, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnVolver, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(btnRegistrar, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );
        getContentPane().add(jPanelCard); pack();
    }

    private void btnVolverActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JDesktopPane desktop = this.getDesktopPane();
        if (desktop != null) {
            Inicio inicio = new Inicio();
            desktop.add(inicio);
            inicio.setVisible(true);
            try { inicio.setSelected(true); } catch (Exception e) {}
            this.dispose();
        }
    }

    private void btnEntrarActionPerformed(java.awt.event.ActionEvent evt) {
        String correo = txtCorreo.getText().trim();
        char[] pass = txtPassword.getPassword();
        if (correo.isEmpty() || pass.length == 0) return;

        try {
            // --- MODO CLOUD FUNCTIONS HTTPS ZERO-ADMIN-SDK ---
            if (FirebaseService.isCloudFunctionsActive()) {
                CloudFunctionsClient client = FirebaseService.getCloudFunctionsClient();
                boolean authOk = client.signIn(correo, new String(pass));
                if (!authOk) {
                    com.mycompany.blackpassword.services.IntrusionService.reportFailure(correo, "LOGIN_FIREBASE_AUTH");
                    JOptionPane.showMessageDialog(this, "❌ Credenciales incorrectas o usuario no registrado.", "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JsonObject resp = client.getProfile(correo);
                if (resp == null || !resp.has("profile")) {
                    JOptionPane.showMessageDialog(this, "❌ Perfil no encontrado en el servidor.\nVerifique su conexión o credenciales.", "Error de Identidad", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JsonObject userDoc = resp.getAsJsonObject("profile");
                String docPath = getJsonString(resp, "docPath");

                String pinHash = getJsonString(userDoc, "pin_hash");
                if (pinHash == null) pinHash = getJsonString(userDoc, "codigopin");

                String dbUidHash = getJsonString(userDoc, "uid_hash");
                if (dbUidHash == null) dbUidHash = client.getUserUid();

                String storedTypingProfile = getJsonString(userDoc, "typing_profile");
                if (storedTypingProfile == null) storedTypingProfile = "";

                String lastLocation = "N/A";
                String lastLocCifrada = getJsonString(userDoc, "last_location_cifrada");
                if (lastLocCifrada != null) {
                    try {
                        byte[] encLoc = Base64.getDecoder().decode(lastLocCifrada);
                        lastLocation = new String(RSALogica.descifrarBytesHibrido(encLoc, SecuritySession.getPrivateKey()), StandardCharsets.UTF_8);
                    } catch (Exception ignored) {}
                } else {
                    String ll = getJsonString(userDoc, "last_location");
                    if (ll != null) lastLocation = ll;
                }

                String perfilCifradoStr = getJsonString(userDoc, "perfil_cifrado");
                if (perfilCifradoStr == null || perfilCifradoStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "❌ Perfil cifrado ausente en el servidor.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                byte[] perfilCifrado = Base64.getDecoder().decode(perfilCifradoStr);
                byte[] perfilDescifradoBytes = RSALogica.descifrarBytesHibrido(perfilCifrado, SecuritySession.getPrivateKey());

                if (pinHash == null || pinHash.isEmpty()) {
                    pinHash = RSALogica.hashConstantToHex("DEFAULT_PIN");
                }

                String storedHash = "", nombre = "Usuario";
                int start = 0;
                for (int i = 0; i <= perfilDescifradoBytes.length; i++) {
                    if (i == perfilDescifradoBytes.length || perfilDescifradoBytes[i] == '|') {
                        int len = i - start;
                        if (len > 0) {
                            String segment = new String(perfilDescifradoBytes, start, len).trim();
                            if (segment.startsWith("hash:")) storedHash = segment.substring(5);
                            if (segment.startsWith("nombre:")) nombre = segment.substring(7);
                        }
                        start = i + 1;
                    }
                }
                Arrays.fill(perfilDescifradoBytes, (byte) 0);

                String userSaltBase64 = userDoc.has("user_salt") ? userDoc.get("user_salt").getAsString() : null;
                byte[] userSalt = (userSaltBase64 != null) ? Base64.getDecoder().decode(userSaltBase64) : null;

                String hashPass;
                if (userSalt != null) {
                    hashPass = RSALogica.hashSHA256(pass, userSalt);
                } else {
                    hashPass = RSALogica.hashSHA256(pass, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
                }

                byte[] computedPassBytes = hashPass.toLowerCase().getBytes(StandardCharsets.UTF_8);
                byte[] storedPassBytes = storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8);

                if (java.security.MessageDigest.isEqual(computedPassBytes, storedPassBytes)) {
                    SecuritySession.setUserSalt(userSalt);
                    SecuritySession.setUserPinHash(pinHash);

                    String currentProfile = typingDNA.getTypingProfile();
                    double similarity = TypingDNAManager.compareProfiles(currentProfile, storedTypingProfile);

                    if (similarity < 15.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
                        JPasswordField pfPin = new JPasswordField(4);
                        Object[] msg = {"🛡️ VERIFICACIÓN BIOMÉTRICA FALLIDA\nConfirme con su PIN:", pfPin};

                        if (JOptionPane.showConfirmDialog(this, msg, "Seguridad", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                            if (!SecuritySession.verifyUserPin(pfPin.getPassword())) {
                                com.mycompany.blackpassword.services.IntrusionService.reportFailure(correo, "LOGIN_BIOMETRIC_PIN");
                                JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                            JsonObject up = new JsonObject();
                            up.addProperty("typing_profile", currentProfile);
                            client.updateProfile(up, docPath);
                        } else return;
                    }

                    String currentLocation = LocationService.getLocationString();
                    SecuritySession.setCurrentLocation(currentLocation);

                    if (!lastLocation.equals("N/A") && !currentLocation.equals(lastLocation)) {
                        AuditService.log(correo, "LOCATION_CHANGE", "Anterior: " + lastLocation + " | Actual: " + currentLocation);
                    }

                    JsonObject updates = new JsonObject();
                    byte[] encLoc = RSALogica.cifrarBytesHibrido(currentLocation.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                    updates.addProperty("last_location_cifrada", Base64.getEncoder().encodeToString(encLoc));
                    client.updateProfile(updates, docPath);

                    SecuritySession.setLocalUidHash(dbUidHash);
                    SecuritySession.setTypingProfile(currentProfile.equals("INSUFFICIENT_DATA") ? storedTypingProfile : currentProfile);

                    char[] passForSession = pass.clone();
                    SecuritySession.setKeySession(SecuritySession.getPublicKey(), SecuritySession.getPrivateKey(), passForSession);
                    SecuritySession.setMasterPassword(pass);

                    if (resp.has("customClaims") && resp.get("customClaims").isJsonObject()) {
                        try {
                            Map<String, Object> claims = new Gson().fromJson(resp.get("customClaims"), Map.class);
                            SecuritySession.setUserClaims(claims);
                        } catch (Exception ignored) {}
                    }

                    lanzarDashboardSeguro(nombre, correo);
                    this.dispose();
                } else {
                    com.mycompany.blackpassword.services.IntrusionService.reportFailure(correo, "LOGIN_PASSWORD");
                    JOptionPane.showMessageDialog(this, "❌ Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                return;
            }

            com.google.cloud.firestore.Firestore db = com.mycompany.blackpassword.services.FirebaseService.getFirestore();
            
            // Migración Nivel 11: Uso de constantes seguras para infraestructura
            String colName = com.mycompany.blackpassword.crypto.RSALogica.hashConstantToHex("usuarios");
            String docId = com.mycompany.blackpassword.crypto.RSALogica.hashConstantToHex(correo);

            // --- MOTOR DE BÚSQUEDA DE IDENTIDAD ROBUSTO (4-Way Intersection) ---
            com.google.cloud.firestore.DocumentReference userRef = db.collection(Objects.requireNonNull(colName)).document(Objects.requireNonNull(docId));
            com.google.cloud.firestore.DocumentSnapshot userDoc = userRef.get().get();
            
            if (!userDoc.exists()) {
                // Caso 1: Colección Moderna, ID Legacy
                userRef = db.collection(Objects.requireNonNull(colName)).document(Objects.requireNonNull(docId.toUpperCase()));
                userDoc = userRef.get().get();
                
                if (!userDoc.exists()) {
                    // Caso 2: Colección Legacy, ID Moderno
                    String colLegacy = Objects.requireNonNull(colName.toUpperCase());
                    userRef = db.collection(Objects.requireNonNull(colLegacy)).document(Objects.requireNonNull(docId));
                    userDoc = userRef.get().get();
                    
                    if (!userDoc.exists()) {
                        // Caso 3: Colección Legacy, ID Legacy
                        userRef = db.collection(Objects.requireNonNull(colLegacy)).document(Objects.requireNonNull(docId.toUpperCase()));
                        userDoc = userRef.get().get();
                    }
                }
            }

            // Purga integrada de buffers (Gestionada por hashConstantToHex)

            if (!userDoc.exists()) {
                JOptionPane.showMessageDialog(this, "❌ Usuario no encontrado.\nVerifique su conexión o espere 15 segundos.", "Error de Identidad", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // --- EXTRACCIÓN DE ATRIBUTOS DE IDENTIDAD ---
            String pinHash = userDoc.getString("pin_hash");
            if (pinHash == null) pinHash = userDoc.getString("codigopin");
            
            String dbUidHash = userDoc.getString("uid_hash");
            String storedTypingProfile = userDoc.getString("typing_profile");
            String lastLocation = "N/A";
            if (userDoc.contains("last_location_cifrada")) {
                try {
                    byte[] encLoc = Base64.getDecoder().decode(userDoc.getString("last_location_cifrada"));
                    lastLocation = new String(RSALogica.descifrarBytesHibrido(encLoc, SecuritySession.getPrivateKey()), StandardCharsets.UTF_8);
                } catch (Exception e) {}
            } else {
                String ll = userDoc.getString("last_location");
                if (ll != null) {
                    lastLocation = ll;
                }
            }
            
            if (pinHash == null || pinHash.isEmpty() || dbUidHash == null) {
                JOptionPane.showMessageDialog(this, "❌ Perfil de usuario incompleto o corrupto.", "Error de Integridad", JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] perfilCifrado = Base64.getDecoder().decode(userDoc.getString("perfil_cifrado"));
            byte[] perfilDescifradoBytes = RSALogica.descifrarBytesHibrido(perfilCifrado, SecuritySession.getPrivateKey());
            
            String storedHash = "", nombre = "Usuario";
            int start = 0;
            for (int i = 0; i <= perfilDescifradoBytes.length; i++) {
                if (i == perfilDescifradoBytes.length || perfilDescifradoBytes[i] == '|') {
                    int len = i - start;
                    if (len > 0) {
                        String segment = new String(perfilDescifradoBytes, start, len).trim();
                        if (segment.startsWith("hash:")) storedHash = segment.substring(5);
                        if (segment.startsWith("nombre:")) nombre = segment.substring(7);
                    }
                    start = i + 1;
                }
            }
            
            Arrays.fill(perfilDescifradoBytes, (byte) 0);
            
            String userSaltBase64 = userDoc.getString("user_salt");
            byte[] userSalt = (userSaltBase64 != null) ? Base64.getDecoder().decode(userSaltBase64) : null;

            String hashPass;
            if (userSalt != null) {
                hashPass = RSALogica.hashSHA256(pass, userSalt);
            } else {
                hashPass = RSALogica.hashSHA256(pass, RSALogica.getUniversalSalt().getBytes(StandardCharsets.UTF_8));
            }

            byte[] computedPassBytes = hashPass.toLowerCase().getBytes(StandardCharsets.UTF_8);
            byte[] storedPassBytes = storedHash.toLowerCase().getBytes(StandardCharsets.UTF_8);

            if (java.security.MessageDigest.isEqual(computedPassBytes, storedPassBytes)) {
                SecuritySession.setUserSalt(userSalt); 
                SecuritySession.setUserPinHash(pinHash);
                
                String currentProfile = typingDNA.getTypingProfile();
                double similarity = TypingDNAManager.compareProfiles(currentProfile, storedTypingProfile);
                
                System.out.println("[TypingDNA] Similitud detectada: " + String.format("%.2f", similarity) + "%");
                
                if (similarity < 15.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
                    JPasswordField pfPin = new JPasswordField(4);
                    Object[] msg = {"🛡️ VERIFICACIÓN BIOMÉTRICA FALLIDA\nConfirme con su PIN:", pfPin};
                    
                    if (JOptionPane.showConfirmDialog(this, msg, "Seguridad", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                        if (!SecuritySession.verifyUserPin(pfPin.getPassword())) {
                            com.mycompany.blackpassword.services.IntrusionService.reportFailure(correo, "LOGIN_BIOMETRIC_PIN");
                            JOptionPane.showMessageDialog(this, "PIN incorrecto.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        db.collection(Objects.requireNonNull(RSALogica.hashConstantToHex("usuarios"))).document(Objects.requireNonNull(docId)).update("typing_profile", currentProfile);
                    } else return;
                }

                // --- RASTREO DE UBICACIÓN SEGURA ---
                String currentLocation = LocationService.getLocationString();
                SecuritySession.setCurrentLocation(currentLocation);
                
                if (!lastLocation.equals("N/A") && !currentLocation.equals(lastLocation)) {
                    AuditService.log(correo, "LOCATION_CHANGE", "Anterior: " + lastLocation + " | Actual: " + currentLocation);
                }

                Map<String, Object> updates = new HashMap<>();
                byte[] encLoc = RSALogica.cifrarBytesHibrido(currentLocation.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                updates.put("last_location_cifrada", Base64.getEncoder().encodeToString(encLoc));
                updates.put("last_login", com.google.cloud.Timestamp.now());
                db.collection(Objects.requireNonNull(RSALogica.hashConstantToHex("usuarios"))).document(Objects.requireNonNull(docId)).update(updates);

                SecuritySession.setLocalUidHash(dbUidHash);
                SecuritySession.setTypingProfile(currentProfile.equals("INSUFFICIENT_DATA") ? storedTypingProfile : currentProfile);
                
                char[] passForSession = pass.clone();
                SecuritySession.setKeySession(SecuritySession.getPublicKey(), SecuritySession.getPrivateKey(), passForSession);
                SecuritySession.setMasterPassword(pass); 
                // (Nota: setKeySession y setMasterPassword limpian sus copias de 'pass', y se refuerza en finally)
                
                try {
                    Map<String, Object> claims = FirebaseService.getCustomClaims(correo);
                    SecuritySession.setUserClaims(claims);
                } catch (Exception e) {
                    System.err.println("[InicioSesion] Error al cargar claims: " + e.getMessage());
                }

                lanzarDashboardSeguro(nombre, correo);
                this.dispose();
            } else {
                com.mycompany.blackpassword.services.IntrusionService.reportFailure(correo, "LOGIN_PASSWORD");
                JOptionPane.showMessageDialog(this, "❌ Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Throwable t) {
            String msg = t.toString();
            if (msg.contains("FirestoreException") || msg.contains("FirebaseException")) {
                JOptionPane.showMessageDialog(this, "📡 ERROR DE CONEXIÓN:\nNo se pudo contactar con el servidor.\nVerifique su internet e intente de nuevo.", "Fallo de Red", JOptionPane.WARNING_MESSAGE);
            } else if (msg.contains("NoSuchMethodError") || msg.contains("NoClassDefFoundError")) {
                JOptionPane.showMessageDialog(this, "🧩 ERROR DE COMPATIBILIDAD:\nFaltan componentes en el archivo JAR.\nReinstale o reconstruya el proyecto.", "Error del Sistema", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "❌ AVISO DE ACCESO:\nNo se pudo validar la identidad.\nDetalle: " + msg, "Fallo de Acceso", JOptionPane.INFORMATION_MESSAGE);
            }
            System.err.println("[InicioSesion] Error capturado: " + t.getMessage());
            t.printStackTrace();
        } finally {
            if (pass != null) Arrays.fill(pass, (char) 0);
            typingDNA.reset();
        }
    }

    private void lanzarDashboardSeguro(String nombre, String correo) {
        try {
            javax.swing.JDesktopPane desktop = this.getDesktopPane();
            if (desktop != null) {
                MenuPrincipal menu = new MenuPrincipal(nombre, correo);
                desktop.add(menu);
                menu.setVisible(true);
                java.awt.EventQueue.invokeLater(() -> {
                    try { menu.setMaximum(true); menu.setSelected(true); menu.toFront(); } catch (Exception e) {}
                });
            }
        } catch (Throwable t) {
            JOptionPane.showMessageDialog(null, "🚨 ERROR AL LANZAR DASHBOARD:\n" + t.toString(), "Error Crítico", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String getJsonString(JsonObject obj, String key) {
        if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsString();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {
        javax.swing.JDesktopPane desktop = this.getDesktopPane();
        if (desktop != null) {
            for (javax.swing.JInternalFrame f : desktop.getAllFrames()) {
                if (f instanceof Registro && f.isVisible() && !f.isClosed()) {
                    try { if (f.isIcon()) f.setIcon(false); f.toFront(); f.setSelected(true); } catch (Exception ignored) {}
                    return;
                }
            }
            Registro reg = new Registro();
            desktop.add(reg);
            reg.setVisible(true);
        }
    }

    private javax.swing.JButton btnEntrar;
    private javax.swing.JButton btnVolver;
    private javax.swing.JButton btnRegistrar;
    private javax.swing.JButton btnVirtualKey;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel lblVersion;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JPasswordField txtPassword;
}
