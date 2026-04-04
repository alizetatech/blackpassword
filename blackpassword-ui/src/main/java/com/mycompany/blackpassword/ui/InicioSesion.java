package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.services.IntrusionService;
import com.mycompany.blackpassword.services.LocationService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.services.TypingDNAManager;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import com.mycompany.blackpassword.services.LocationService;
import com.mycompany.blackpassword.services.TypingDNAManager;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @SuppressWarnings("unchecked")
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
            Firestore db = FirebaseService.getFirestore();
            String docId = RSALogica.hashToHex(correo);
            DocumentSnapshot userDoc = db.collection(RSALogica.hashToHex("usuarios")).document(docId).get().get();
            
            if (!userDoc.exists()) {
                JOptionPane.showMessageDialog(this, "❌ Usuario no encontrado.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String pinHash = userDoc.getString("codigopin");
            String dbUidHash = userDoc.getString("uid_hash");
            String storedTypingProfile = userDoc.getString("typing_profile");
            String lastLocation = userDoc.getString("last_location");

            if (pinHash == null || pinHash.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Perfil de usuario corrupto.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] perfilCifrado = Base64.getDecoder().decode(userDoc.getString("perfil_cifrado"));
            byte[] perfilDescifradoBytes = RSALogica.descifrarBytesHibrido(perfilCifrado, SecuritySession.getPrivateKey());
            String perfilDescifrado = new String(perfilDescifradoBytes, StandardCharsets.UTF_8);
            
            String storedHash = "", nombre = "Usuario";
            for (String parte : perfilDescifrado.split("\\|")) {
                if (parte.startsWith("hash:")) storedHash = parte.substring(5);
                if (parte.startsWith("nombre:")) nombre = parte.substring(7);
            }
            
            String hashPassConSal = RSALogica.hashSHA256(pass, RSALogica.getUniversalSalt());
            String hashPassSinSal = RSALogica.hashSHA256(pass, null);

            if (hashPassConSal.equals(storedHash) || hashPassSinSal.equals(storedHash)) {
                
                // --- VALIDACIÓN BIOMÉTRICA CONDUCTUAL (TYPING DNA) ---
                String currentProfile = typingDNA.getTypingProfile();
                double similarity = TypingDNAManager.compareProfiles(currentProfile, storedTypingProfile);
                
                System.out.println("[TypingDNA] Similitud detectada: " + String.format("%.2f", similarity) + "%");
                
                // Si la similitud es baja (< 20%), pedimos el PIN como rescate y actualizamos el perfil
                if (similarity < 20.0 && !currentProfile.equals("INSUFFICIENT_DATA")) {
                    JPasswordField pfPin = new JPasswordField(4);
                    Object[] msg = {
                        "🛡️ VERIFICACIÓN DE SEGURIDAD ADICIONAL\n" +
                        "Se ha detectado un cambio en su entorno de tecleo.\n" +
                        "Por favor, confirme su identidad con su PIN de 4 dígitos:", 
                        pfPin
                    };
                    
                    int opt = JOptionPane.showConfirmDialog(this, msg, "Protección de Identidad", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    
                    if (opt == JOptionPane.OK_OPTION) {
                        String hashPinConSal = RSALogica.hashSHA256(pfPin.getPassword(), RSALogica.getUniversalSalt());
                        String hashPinSinSal = RSALogica.hashSHA256(pfPin.getPassword(), null);
                        
                        if (hashPinConSal.equals(pinHash) || hashPinSinSal.equals(pinHash)) {
                            AuditService.log(correo, "BIOMETRIC_LEARNING", "Perfil biométrico actualizado tras validación por PIN.");
                            // APRENDIZAJE ACTIVO: Actualizamos el perfil en la nube con el nuevo ritmo
                            db.collection(RSALogica.hashToHex("usuarios")).document(docId).update("typing_profile", currentProfile);
                        } else {
                            IntrusionService.reportFailure(correo, "TYPING_DNA_PIN_FAILURE");
                            JOptionPane.showMessageDialog(this, "PIN incorrecto. Acceso denegado.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    } else {
                        return;
                    }
                }

                // --- RASTREO DE UBICACIÓN SEGURA ---
                String currentLocation = LocationService.getLocationString();
                SecuritySession.setCurrentLocation(currentLocation);
                
                if (lastLocation != null && !lastLocation.equals("N/A") && !currentLocation.equals(lastLocation)) {
                    AuditService.log(correo, "LOCATION_CHANGE", "Cambio de ubicación detectado. Anterior: " + lastLocation + " | Actual: " + currentLocation);
                }

                // Actualizar última ubicación en Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("last_location", currentLocation);
                updates.put("last_login", com.google.cloud.Timestamp.now());
                db.collection(RSALogica.hashToHex("usuarios")).document(docId).update(updates);

                SecuritySession.setUserPinHash(pinHash);
                SecuritySession.setLocalUidHash(dbUidHash);
                SecuritySession.setTypingProfile(currentProfile.equals("INSUFFICIENT_DATA") ? storedTypingProfile : currentProfile);
                SecuritySession.setKeySession(SecuritySession.getPublicKey(), SecuritySession.getPrivateKey(), pass);
                SecuritySession.setMasterPassword(pass.clone());
                
                try {
                    Map<String, Object> claims = FirebaseService.getCustomClaims(correo);
                    SecuritySession.setUserClaims(claims);
                } catch (Exception e) {
                    System.err.println("[InicioSesion] Error al cargar claims: " + e.getMessage());
                }

                lanzarDashboardSeguro(nombre, correo);
                this.dispose();
            } else {
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

    private void btnRegistrarActionPerformed(java.awt.event.ActionEvent evt) {
        Registro reg = new Registro(); this.getDesktopPane().add(reg); reg.setVisible(true);
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
