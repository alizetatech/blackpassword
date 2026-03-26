package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.IntrusionService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import com.mycompany.blackpassword.VirtualKeyboard;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;

/**
 * Inicio de Sesión v6.5-BLINDADO.
 * Carga el 'codigopin' del usuario en la sesión de seguridad.
 */
public class InicioSesion extends javax.swing.JInternalFrame {

    public InicioSesion() {
        initComponents();
        applyPremiumStyle();
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jLabel3.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        lblVersion.setText("v6.5-BLINDADO");
        lblVersion.putClientProperty(FlatClientProperties.STYLE, "font: $small.font; foreground: #999999");
        txtCorreo.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ejemplo@correo.com");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        
        btnEntrar.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
        btnVolver.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; foreground: #666666; hoverBackground: #E5E5E5; borderWidth: 0");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
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

        setClosable(false); setTitle("🛡️ ACCESO v6.5");
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
                .addGap(40, 40, 40)
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
                    .addComponent(btnEntrar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnVolver, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnRegistrar, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                .addContainerGap(40, Short.MAX_VALUE))
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

            // Obtener el hash del PIN directamente del documento
            String pinHash = userDoc.getString("codigopin");
            if (pinHash == null || pinHash.isEmpty()) {
                JOptionPane.showMessageDialog(this, "❌ Perfil de usuario corrupto. No se encontró PIN de seguridad.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Descifrar el perfil para verificar la contraseña
            byte[] perfilCifrado = Base64.getDecoder().decode(userDoc.getString("perfil_cifrado"));
            byte[] perfilDescifradoBytes = RSALogica.descifrarBytesHibrido(perfilCifrado, SecuritySession.getPrivateKey());
            String perfilDescifrado = new String(perfilDescifradoBytes, StandardCharsets.UTF_8);
            
            String storedHash = "", nombre = "Usuario";
            for (String parte : perfilDescifrado.split("\\|")) {
                if (parte.startsWith("hash:")) storedHash = parte.substring(5);
                if (parte.startsWith("nombre:")) nombre = parte.substring(7);
            }
            
            // Verificar la contraseña
            if (RSALogica.hashSHA256(pass).equals(storedHash)) {
                // Contraseña correcta: establecer la sesión de seguridad
                SecuritySession.setUserPinHash(pinHash); // Guardar el hash del PIN del usuario
                SecuritySession.setKeySession(SecuritySession.getPublicKey(), SecuritySession.getPrivateKey(), pass); // Usar la contraseña para proteger la sesión
                
                lanzarDashboardSeguro(nombre, correo);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Contraseña incorrecta.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ ERROR DE IDENTIDAD:\n" + e.toString(), "Fallo de Acceso", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pass, (char) 0);
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
