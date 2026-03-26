package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.google.cloud.firestore.Firestore;
import com.mycompany.blackpassword.CertificateService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.swing.SwingConstants;

/**
 * Visor de Identidad Digital SSL/X.509 v2.0.
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
        
        txtCertInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtCertInfo.setEditable(false);
        txtCertInfo.setBackground(new Color(250, 250, 250));
        
        jScrollPane1.putClientProperty(FlatClientProperties.STYLE, "border: 1,1,1,1,#E0E0E0, 1, 10");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
    }

    private void cargarCertificadoSSL() {
        try {
            Firestore db = FirebaseService.getFirestore();
            String docId = RSALogica.hashToHex(userEmail);
            var doc = db.collection(RSALogica.hashToHex("usuarios")).document(docId).get().get();
            
            if (doc.exists() && doc.contains("certificado_digital")) {
                String encCert = doc.getString("certificado_digital");
                byte[] decBytes = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(encCert), SecuritySession.getPrivateKey());
                String certBase64 = new String(decBytes, StandardCharsets.UTF_8);
                
                X509Certificate cert = CertificateService.parseCertificate(certBase64);
                
                StringBuilder sb = new StringBuilder();
                sb.append("--- PASAPORTE DIGITAL X.509 ---\n\n");
                sb.append("👤 SUJETO: ").append(cert.getSubjectX500Principal().getName()).append("\n");
                sb.append("🏛️ EMISOR: ").append(cert.getIssuerX500Principal().getName()).append("\n");
                sb.append("🔢 SERIAL: ").append(cert.getSerialNumber().toString(16).toUpperCase()).append("\n");
                sb.append("📅 VALIDEZ: Desde ").append(cert.getNotBefore()).append("\n");
                sb.append("            Hasta ").append(cert.getNotAfter()).append("\n");
                sb.append("🔐 ALGORITMO: ").append(cert.getSigAlgName()).append("\n");
                sb.append("🆔 VERSIÓN: ").append(cert.getVersion()).append("\n\n");
                sb.append("--- HUELLA DIGITAL (BASE64) ---\n");
                sb.append(certBase64.substring(0, 50)).append("...");
                
                txtCertInfo.setText(sb.toString());
            } else {
                txtCertInfo.setText("No se encontró un certificado SSL para esta cuenta.");
            }
        } catch (Exception e) {
            txtCertInfo.setText("Error al procesar certificado SSL: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanelCard = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtCertInfo = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();

        setClosable(true);
        setIconifiable(true);
        setTitle("Identidad SSL Certificada");

        lblTitulo.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitulo.setText("Certificado de Identidad SSL");

        txtCertInfo.setColumns(20);
        txtCertInfo.setRows(15);
        jScrollPane1.setViewportView(txtCertInfo);

        jLabel1.setText("<html><center>Este es un certificado X.509 estándar. Su identidad está blindada bajo protocolos de seguridad de grado militar.</center></html>");
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);

        javax.swing.GroupLayout jPanelCardLayout = new javax.swing.GroupLayout(jPanelCard);
        jPanelCard.setLayout(jPanelCardLayout);
        jPanelCardLayout.setHorizontalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(lblTitulo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanelCardLayout.setVerticalGroup(
            jPanelCardLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelCardLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(lblTitulo)
                .addGap(20, 20, 20)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 380, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanelCard;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JTextArea txtCertInfo;
}
