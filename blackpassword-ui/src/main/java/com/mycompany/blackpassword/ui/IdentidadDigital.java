package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.services.CertificateService;
import com.mycompany.blackpassword.services.FirebaseService;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.RemoteKillService;
import com.mycompany.blackpassword.services.SecuritySession;
import com.mycompany.blackpassword.core.util.ToastNotification;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Centro de Comando de Identidad v3.7.
 * Incluye Validación de Identidad Cruzada, RSA-Binding, Typing DNA y Ubicación.
 * Corregido error de sintaxis en Layout.
 */
public class IdentidadDigital extends javax.swing.JInternalFrame {

    private final String userEmail;

    public IdentidadDigital(String email) {
        this.userEmail = email;
        initComponents();
        applyPremiumStyle();
        cargarCertificadoSSL();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        lblTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        txtCertInfo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtCertInfo.setEditable(false);
        txtCertInfo.setBackground(new Color(250, 250, 250));
        
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 1,1,1,1,#E0E0E0, 1, 10");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
        
        btnKillSwitch.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #D83B01; foreground: #FFFFFF; hoverBackground: #A82A01; borderWidth: 0; font: bold");
    }

    private void cargarCertificadoSSL() {
        try {
            Firestore db = FirebaseService.getFirestore();
            String docId = RSALogica.hashConstantToHex(userEmail);
            var doc = db.collection(java.util.Objects.requireNonNull(RSALogica.hashConstantToHex("usuarios")))
                        .document(java.util.Objects.requireNonNull(docId)).get().get();
            
            StringBuilder sb = new StringBuilder();
            
            // --- SECCIÓN 1: VALIDACIÓN DE IDENTIDAD CRUZADA ---
            sb.append("╔══════════════════════════════════════════════════════╗\n");
            sb.append("║       PROTOCOLO DE INTEGRIDAD DE IDENTIDAD           ║\n");
            sb.append("╚══════════════════════════════════════════════════════╝\n\n");
            
            Map<String, Object> claims = SecuritySession.getUserClaims();
            if (claims != null) {
                String claimEmailHash = (String) claims.get("email_hash");
                String claimUidHash = (String) claims.get("uid_hash");
                String claimPubKeyHash = (String) claims.get("pubkey_hash");
                
                String localEmailHash = RSALogica.hashConstantToHex(userEmail);
                String localUidHash = SecuritySession.getLocalUidHash();
                String localPubKeyHash = RSALogica.hashPublicKey(SecuritySession.getPublicKey());
                String typingProfile = SecuritySession.getTypingProfile();
                String currentLocation = SecuritySession.getCurrentLocation();
                
                boolean emailValid = localEmailHash.equals(claimEmailHash);
                boolean uidValid = localUidHash != null && localUidHash.equals(claimUidHash);
                boolean rsaValid = claimPubKeyHash != null && claimPubKeyHash.equals(localPubKeyHash);
                boolean typingValid = typingProfile != null && !typingProfile.isEmpty();
                
                sb.append(" > EMAIL HASH: ").append(truncate(localEmailHash)).append("\n");
                sb.append("   ESTADO: ").append(emailValid ? "✅ VALIDADO" : "❌ ERROR").append("\n\n");
                
                sb.append(" > UID HASH:   ").append(truncate(localUidHash)).append("\n");
                sb.append("   ESTADO: ").append(uidValid ? "✅ VALIDADO" : "❌ ERROR").append("\n\n");

                sb.append(" > RSA-BINDING (HUELLA DE LLAVE):\n");
                sb.append("   TOKEN: ").append(truncate(claimPubKeyHash)).append("\n");
                sb.append("   LOCAL: ").append(truncate(localPubKeyHash)).append("\n");
                sb.append("   ESTADO: ").append(rsaValid ? "✅ VINCULADO" : "❌ NO VINCULADO").append("\n\n");

                sb.append(" > BIOMETRÍA CONDUCTUAL (TYPING DNA):\n");
                sb.append("   ESTADO: ").append(typingValid ? "✅ PERFIL ACTIVO" : "⚠️ NO CONFIGURADO").append("\n\n");

                sb.append(" > RASTREO DE UBICACIÓN SEGURA:\n");
                sb.append("   ACTUAL: ").append(currentLocation).append("\n\n");
                
                if (emailValid && uidValid && rsaValid && typingValid) {
                    sb.append(" [!] IDENTIDAD BLINDADA NIVEL 3: ACCESO TOTAL\n\n");
                }
            }

            // --- SECCIÓN 2: CERTIFICADO SSL ---
            if (doc.exists() && doc.contains("certificado_digital")) {
                String encCert = doc.getString("certificado_digital");
                byte[] decBytes = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(encCert), SecuritySession.getPrivateKey());
                String certBase64 = new String(decBytes, StandardCharsets.UTF_8);
                
                X509Certificate cert = CertificateService.parseCertificate(certBase64);
                
                sb.append("╔══════════════════════════════════════════════════════╗\n");
                sb.append("║          PASAPORTE DIGITAL X.509 (SSL)               ║\n");
                sb.append("╚══════════════════════════════════════════════════════╝\n\n");
                sb.append(" 👤 SUJETO: ").append(cert.getSubjectX500Principal().getName()).append("\n");
                sb.append(" 🏛️ EMISOR: ").append(cert.getIssuerX500Principal().getName()).append("\n");
                sb.append(" 🔢 SERIAL: ").append(cert.getSerialNumber().toString(16).toUpperCase()).append("\n");
                sb.append(" 🔐 ALGO:   ").append(cert.getSigAlgName()).append("\n\n");
                
                sb.append(" --- HUELLA RSA ---\n");
                sb.append(certBase64.substring(0, 64)).append("...");
            }
            
            txtCertInfo.setText(sb.toString());
            
        } catch (Exception e) {
            txtCertInfo.setText("Error crítico de identidad: " + e.getMessage());
        }
    }
    
    private String truncate(String s) {
        if (s == null) return "N/A";
        if (s.length() <= 20) return s;
        return s.substring(0, 10) + "..." + s.substring(s.length() - 10);
    }

    private void btnKillSwitchActionPerformed(java.awt.event.ActionEvent evt) {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "🚨 ADVERTENCIA DE SEGURIDAD MÁXIMA 🚨\n\n" +
            "¿Desea activar la AUTODESTRUCCIÓN de esta cuenta?\n" +
            "Esta acción marcará su identidad como COMPROMETIDA en la nube.\n" +
            "TODAS las sesiones abiertas se cerrarán y purgarán datos inmediatamente.\n\n" +
            "¿ESTÁ SEGURO?", "PROTOCOLO DE PÁNICO GLOBAL", 
            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            String pin = JOptionPane.showInputDialog(this, "Ingrese su PIN para confirmar la destrucción:");
            if (pin != null && SecuritySession.verifyUserPin(pin.toCharArray())) {
                RemoteKillService.selfDestruct(userEmail);
                JOptionPane.showMessageDialog(this, "Protocolo activado. Cerrando sistema...");
                System.exit(0);
            } else {
                ToastNotification.show(this, "PIN incorrecto. Abortando.", ToastNotification.Type.ERROR);
            }
        }
    }

    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtCertInfo = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        btnKillSwitch = new javax.swing.JButton();

        setClosable(true);
        setIconifiable(true);
        setTitle("Centro de Comando de Identidad");

        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setText("Identidad Digital Blindada");

        txtCertInfo.setColumns(20);
        txtCertInfo.setRows(15);
        jScrollPane1.setViewportView(txtCertInfo);

        jLabel1.setText("<html><center>Gestione su identidad y active el protocolo de pánico global si sospecha que su cuenta ha sido comprometida.</center></html>");
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);

        btnKillSwitch.setText("🔥 ACTIVAR AUTODESTRUCCIÓN GLOBAL (KILL SWITCH)");
        btnKillSwitch.addActionListener(evt -> btnKillSwitchActionPerformed(evt));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 520, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(btnKillSwitch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(btnKillSwitch, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(25, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    private javax.swing.JButton btnKillSwitch;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextArea txtCertInfo;
}
