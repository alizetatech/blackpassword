package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.UserRecord;
import com.mycompany.blackpassword.CertificateService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import com.mycompany.blackpassword.SeedPhraseManager;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Registro Blindado v8.2: Almacenamiento de PIN como 'codigopin'.
 */
public class Registro extends javax.swing.JInternalFrame {

    public Registro() {
        initComponents();
        applyPremiumStyle();
        setupPinFilter();
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jLabel3.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #107C10");
        txtNombre.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tu nombre");
        txtApellido.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Tu apellido");
        jTextField1.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "ejemplo@correo.com");
        txtPin.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "PIN de 4 dígitos");
        jPasswordField1.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        txtPin.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; arc: 8");
        jButton1.putClientProperty(FlatClientProperties.STYLE, 
            "arc: 15; background: #107C10; foreground: #FFFFFF; hoverBackground: #006400; borderWidth: 0; font: bold");
        
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    private void setupPinFilter() {
        ((AbstractDocument) txtPin.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("\\d*") && (fb.getDocument().getLength() + string.length() <= 4)) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("\\d*") && (fb.getDocument().getLength() - length + text.length() <= 4)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanelCard = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabelNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        jLabelApellido = new javax.swing.JLabel();
        txtApellido = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabelPin = new javax.swing.JLabel();
        txtPin = new javax.swing.JPasswordField();
        jLabel2 = new javax.swing.JLabel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jButton1 = new javax.swing.JButton();

        setClosable(true);
        setTitle("Registro de Identidad");

        jPanelCard.setBackground(new java.awt.Color(255, 255, 255));

        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel3.setText("Crear Cuenta Blindada");

        jLabelNombre.setText("Nombre:");
        jLabelApellido.setText("Apellido:");
        jLabel1.setText("Correo Electrónico:");
        jLabelPin.setText("PIN de Seguridad (4 números):");
        jLabel2.setText("Contraseña:");

        jButton1.setText("GENERAR IDENTIDAD SSL CERTIFICADA");
        jButton1.addActionListener(evt -> jButton1ActionPerformed(evt));

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelNombre)
                    .addComponent(txtNombre)
                    .addComponent(jLabelApellido)
                    .addComponent(txtApellido)
                    .addComponent(jLabel1)
                    .addComponent(jTextField1)
                    .addComponent(jLabelPin)
                    .addComponent(txtPin)
                    .addComponent(jLabel2)
                    .addComponent(jPasswordField1)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 320, Short.MAX_VALUE))
                .addContainerGap(35, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel3)
                .addGap(25, 25, 25)
                .addComponent(jLabelNombre)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtNombre, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabelApellido)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtApellido, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabelPin)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPin, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(35, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jPanelCard, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(30, 30, 30))
        );

        pack();
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
        String nombre = txtNombre.getText().trim();
        String apellido = txtApellido.getText().trim();
        String correo = jTextField1.getText().trim();
        char[] pin = txtPin.getPassword();
        char[] pass = jPasswordField1.getPassword();
        
        if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || pin.length == 0 || pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Complete todos los campos.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (pin.length != 4) {
            JOptionPane.showMessageDialog(this, "El PIN debe tener exactamente 4 dígitos.", "Error de PIN", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!SecuritySession.isSessionActive()) {
            JOptionPane.showMessageDialog(this, "No hay una sesión RSA activa. Conéctese primero.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String seedPhrase = SeedPhraseManager.generateSeedPhrase();
        char[] seedChars = seedPhrase.toCharArray();
        
        JTextArea area = new JTextArea(5, 30);
        area.setText(seedPhrase);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.BOLD, 14));
        area.setBackground(new Color(240, 240, 240));
        
        Object[] message = {
            "⚠️ IMPORTANTE: GUARDE ESTA FRASE EN UN LUGAR SEGURO.\n" +
            "Es la única forma de recuperar su cuenta si olvida su contraseña.",
            new JScrollPane(area),
            "¿Ha copiado y guardado la frase semilla?"
        };

        int confirm = JOptionPane.showConfirmDialog(this, message, "Generación de Frase Semilla", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            Arrays.fill(seedChars, (char) 0);
            return;
        }

        try {
            String hashedPass = RSALogica.hashSHA256(pass);
            String hashedSeed = RSALogica.hashSHA256(seedChars);
            String hashedPin = RSALogica.hashSHA256(pin);
            
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(correo)
                .setPassword(new String(pass))
                .setDisplayName(nombre + " " + apellido);
            UserRecord userRecord = FirebaseService.getAuth().createUser(request);

            String certBase64 = CertificateService.generateSSLCertificate(
                nombre + " " + apellido, 
                correo, 
                SecuritySession.getPublicKey(), 
                SecuritySession.getPrivateKey()
            );
            String encryptedCert = CertificateService.encryptCertificate(certBase64, SecuritySession.getPublicKey());

            String payload = "hash:" + hashedPass + "|seed_hash:" + hashedSeed + "|correo:" + correo + "|nombre:" + nombre + "|apellido:" + apellido;
            byte[] encryptedPayload = RSALogica.cifrarBytesHibrido(payload.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
            String payloadBase64 = Base64.getEncoder().encodeToString(encryptedPayload);

            Firestore db = FirebaseService.getFirestore();
            Map<String, Object> datos = new HashMap<>();
            datos.put("uid_hash", RSALogica.hashToHex(userRecord.getUid()));
            datos.put("perfil_cifrado", payloadBase64);
            datos.put("certificado_digital", encryptedCert); 
            datos.put("codigopin", hashedPin); // Guardar el hash del PIN como 'codigopin'
            datos.put("fecha_registro", com.google.cloud.Timestamp.now());
            datos.put("status", "ACTIVE");
            
            String colName = RSALogica.hashToHex("usuarios");
            String docId = RSALogica.hashToHex(correo);
            
            db.collection(colName).document(docId).set(datos).get();
            
            JOptionPane.showMessageDialog(this, "¡Registro Exitoso!\nSu identidad SSL ha sido certificada y blindada.", "Seguridad Máxima", JOptionPane.INFORMATION_MESSAGE);
            this.dispose();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en el registro: " + e.getMessage(), "Fallo", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, (char) 0);
            Arrays.fill(pass, (char) 0);
            Arrays.fill(seedChars, (char) 0);
        }
    }

    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelNombre;
    private javax.swing.JLabel jLabelApellido;
    private javax.swing.JLabel jLabelPin;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JPasswordField txtPin;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtApellido;
}
