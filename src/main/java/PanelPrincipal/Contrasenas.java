package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.FirebaseService;
import com.mycompany.blackpassword.RSALogica;
import com.mycompany.blackpassword.SecuritySession;
import com.mycompany.blackpassword.ToastNotification;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Bóveda de Contraseñas v13.3 - CORRECCIÓN DE NULLPOINTEREXCEPTION.
 * Inicializa btnVolver antes de aplicar estilos.
 */
public class Contrasenas extends javax.swing.JInternalFrame {

    private final String userEmail;
    private DefaultTableModel model;
    private JTable table;
    private JTextField txtService;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    
    private JButton btnSave;
    private JButton btnView;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnVolver;
    
    private JLabel jLabelTitulo;
    private List<DocumentSnapshot> secretDocuments = new ArrayList<>();

    public Contrasenas(String email) {
        super("Bóveda de Seguridad", false, false, false, false);
        this.userEmail = email;
        System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Constructor de Contrasenas iniciado ---");
        try {
            initComponentsCustom(); // Primero inicializar componentes
            System.out.println("--- DIAGNÓSTICO DE BÓVEDA: initComponentsCustom finalizado ---");
        } catch (Exception e) {
            System.err.println("--- DIAGNÓSTICO DE BÓVEDA: ERROR en initComponentsCustom: " + e.getMessage() + " ---");
            e.printStackTrace();
        }
        try {
            applyPremiumStyle();    // Luego aplicar estilos
            System.out.println("--- DIAGNÓSTICO DE BÓVEDA: applyPremiumStyle finalizado ---");
        } catch (Exception e) {
            System.err.println("--- DIAGNÓSTICO DE BÓVEDA: ERROR en applyPremiumStyle: " + e.getMessage() + " ---");
            e.printStackTrace();
        }
        loadSecretsFromFirebase();
        System.out.println("--- DIAGNÓSTICO DE BÓVEDA: loadSecretsFromFirebase llamado ---");
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        
        // Estilo de Título
        jLabelTitulo.putClientProperty(FlatClientProperties.STYLE, "font: $h1.font; foreground: #0078D4");
        
        // Estilo de Tabla
        table.putClientProperty(FlatClientProperties.STYLE, ""
                + "rowHeight: 40;"
                + "showHorizontalLines: true;"
                + "intercellSpacing: 0,1;"
                + "selectionBackground: #E5F3FF;"
                + "selectionForeground: #000000;"
                + "showVerticalLines: false");
        
        table.getTableHeader().putClientProperty(FlatClientProperties.STYLE, "height: 40; font: bold; background: #F8F9FA; separatorColor: #D0D0D0");
        
        // Estilo de Inputs
        txtService.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Sitio o Servicio");
        txtUsername.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Usuario / Email");
        txtPassword.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true"); // Eliminado 'arc: 8'
        
        // Estilo de Botones
        btnSave.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #107C10; foreground: #FFFFFF; hoverBackground: #0E6A0E; borderWidth: 0; font: bold");
        btnView.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; borderWidth: 0; font: bold");
        btnDelete.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #D83B01; foreground: #FFFFFF; hoverBackground: #B83201; borderWidth: 0; font: bold");
        btnRefresh.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #666666; foreground: #FFFFFF; hoverBackground: #444444; borderWidth: 0; font: bold");
        
        // DIAGNÓSTICO: Verificar btnVolver antes de usarlo
        System.out.println("--- DIAGNÓSTICO DE BÓVEDA: btnVolver en applyPremiumStyle es: " + btnVolver + " ---");
        if (btnVolver != null) {
            btnVolver.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; arc: 10; foreground: #D83B01; font: bold");
            btnVolver.setIcon(new FlatSVGIcon("img/undo.svg", 18, 18));
        }

        try {
            btnSave.setIcon(new FlatSVGIcon("img/vault.svg", 22, 22));
            btnView.setIcon(new FlatSVGIcon("img/unlock.svg", 22, 22));
            btnDelete.setIcon(new FlatSVGIcon("img/panic.svg", 22, 22));
            btnRefresh.setIcon(new FlatSVGIcon("img/files.svg", 22, 22));
        } catch (Exception e) {}
    }

    private void initComponentsCustom() {
        setSize(950, 700);
        setLayout(new BorderLayout(20, 20));

        // --- PANEL CABECERA (Header) ---
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setOpaque(false);
        panelHeader.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 20));
        
        // Panel izquierdo para el botón Volver y el título
        JPanel panelLeftHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelLeftHeader.setOpaque(false);
        
        btnVolver = new JButton("VOLVER"); // Inicialización del botón
        btnVolver.setPreferredSize(new Dimension(120, 35));
        btnVolver.addActionListener(e -> dispose()); // Cierra la ventana al hacer clic
        panelLeftHeader.add(btnVolver);
        
        jLabelTitulo = new JLabel("BÓVEDA DE CREDENCIALES");
        panelLeftHeader.add(jLabelTitulo);
        
        panelHeader.add(panelLeftHeader, BorderLayout.WEST);
        
        // Panel para la imagen y los botones de acción (superior derecha)
        JPanel panelTopRightActions = new JPanel(new BorderLayout());
        panelTopRightActions.setOpaque(false);
        
        JLabel imgHeader = new JLabel(new FlatSVGIcon("img/vault.svg", 40, 40)); // Imagen 40x40
        imgHeader.setHorizontalAlignment(SwingConstants.RIGHT);
        panelTopRightActions.add(imgHeader, BorderLayout.NORTH);
        
        JPanel panelActionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5)); // Botones alineados a la derecha
        panelActionButtons.setOpaque(false);
        
        btnRefresh = new JButton("SINCRONIZAR");
        btnRefresh.setPreferredSize(new Dimension(180, 35));
        btnRefresh.addActionListener(e -> loadSecretsFromFirebase());
        panelActionButtons.add(btnRefresh);
        
        btnView = new JButton("VER");
        btnView.setPreferredSize(new Dimension(120, 35));
        btnView.addActionListener(e -> viewSecret());
        panelActionButtons.add(btnView);
        
        btnDelete = new JButton("ELIMINAR");
        btnDelete.setPreferredSize(new Dimension(150, 35));
        btnDelete.addActionListener(e -> deleteSecret());
        panelActionButtons.add(btnDelete);
        
        panelTopRightActions.add(panelActionButtons, BorderLayout.CENTER);
        
        panelHeader.add(panelTopRightActions, BorderLayout.EAST);
        
        add(panelHeader, BorderLayout.NORTH);

        // --- PANEL CENTRAL (Tabla) ---
        model = new DefaultTableModel(new Object[]{"SITIO / SERVICIO", "USUARIO / IDENTIDAD", "ACTUALIZACIÓN"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1, true));
        
        JPanel panelCenter = new JPanel(new BorderLayout());
        panelCenter.setOpaque(false);
        panelCenter.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        panelCenter.add(scroll, BorderLayout.CENTER);
        add(panelCenter, BorderLayout.CENTER);

        // --- PANEL INFERIOR (Formulario) ---
        JPanel panelBottom = new JPanel(new BorderLayout(15, 15));
        panelBottom.setOpaque(false);
        panelBottom.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));

        // Formulario de Creación
        JPanel panelForm = new JPanel(new GridBagLayout());
        panelForm.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)), " PROTEGER NUEVA CREDENCIAL ", 0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(100, 100, 100)));
        panelForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtService = new JTextField(15);
        txtUsername = new JTextField(15);
        txtPassword = new JPasswordField(15);
        btnSave = new JButton("GUARDAR EN NUBE");
        btnSave.setPreferredSize(new Dimension(200, 40));
        btnSave.addActionListener(e -> saveSecret());

        gbc.gridx = 0; gbc.gridy = 0; panelForm.add(new JLabel("SITIO:"), gbc);
        gbc.gridx = 1; panelForm.add(txtService, gbc);
        gbc.gridx = 2; panelForm.add(new JLabel("USUARIO:"), gbc);
        gbc.gridx = 3; panelForm.add(txtUsername, gbc);
        gbc.gridx = 4; panelForm.add(new JLabel("PASS:"), gbc);
        gbc.gridx = 5; panelForm.add(txtPassword, gbc);
        gbc.gridx = 6; gbc.weightx = 1.0; panelForm.add(btnSave, gbc);

        panelBottom.add(panelForm, BorderLayout.NORTH);
        
        add(panelBottom, BorderLayout.SOUTH);
    }

    private void loadSecretsFromFirebase() {
        System.out.println("--- DIAGNÓSTICO DE BÓVEDA: loadSecretsFromFirebase iniciado ---");
        ToastNotification.show(this, "Sincronizando Bóveda Luxury...", ToastNotification.Type.INFO);
        model.setRowCount(0);
        secretDocuments.clear();
        
        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                System.out.println("--- DIAGNÓSTICO DE BÓVEDA: SwingWorker.doInBackground iniciado ---");
                if (!SecuritySession.isSessionActive()) {
                    System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Sesión de seguridad inactiva en doInBackground ---");
                    throw new IllegalStateException("Sesión inactiva");
                }

                List<Object[]> results = new ArrayList<>(); // Declaración de 'results'
                Firestore db = FirebaseService.getFirestore();
                
                String usersCollection = RSALogica.hashToHex("usuarios");
                String userDocument = RSALogica.hashToHex(userEmail);
                String secretsSubCollection = RSALogica.hashToHex("boveda"); 

                System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Intentando leer de la colección: " + usersCollection + "/" + userDocument + "/" + secretsSubCollection + " ---");

                List<QueryDocumentSnapshot> docs = db.collection(usersCollection)
                                                     .document(userDocument)
                                                     .collection(secretsSubCollection)
                                                     .get()
                                                     .get()
                                                     .getDocuments();
                
                System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Documentos encontrados: " + docs.size() + " ---");
                secretDocuments.addAll(docs);

                for (DocumentSnapshot doc : docs) {
                    String encryptedSecret = doc.getString("secreto_cifrado");
                    com.google.cloud.Timestamp ts = doc.getTimestamp("fecha_actualizacion");
                    String dateStr = (ts != null) ? ts.toDate().toString().substring(0, 19) : "N/A";

                    if (encryptedSecret != null) {
                        try {
                            byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(encryptedSecret), SecuritySession.getPrivateKey());
                            String raw = new String(dec, StandardCharsets.UTF_8);
                            String s = "Desconocido", u = "Desconocido";
                            for (String part : raw.split("\\|")) {
                                if (part.startsWith("s:")) s = part.substring(2);
                                if (part.startsWith("u:")) u = part.substring(2);
                            }
                            results.add(new Object[]{s.toUpperCase(), u, dateStr});
                            System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Secreto descifrado: " + s + " - " + u + " ---");
                        } catch (Exception e) {
                            System.err.println("--- DIAGNÓSTICO DE BÓVEDA: FALLO AL DESCIFRAR doc: " + doc.getId() + ". Error: " + e.getMessage() + " ---");
                            results.add(new Object[]{"⚠️ ERROR", "FALLO DE DESCIFRADO", dateStr});
                        }
                    }
                }
                return results;
            }

            @Override
            protected void done() {
                System.out.println("--- DIAGNÓSTICO DE BÓVEDA: SwingWorker.done iniciado ---");
                try {
                    List<Object[]> data = get();
                    for (Object[] row : data) model.addRow(row);
                    ToastNotification.show(Contrasenas.this, "Bóveda sincronizada. Encontrados: " + data.size() + " registros.", ToastNotification.Type.SUCCESS);
                    System.out.println("--- DIAGNÓSTICO DE BÓVEDA: Bóveda sincronizada. Encontrados: " + data.size() + " ---");
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastNotification.show(Contrasenas.this, "Error de conexión con Firebase", ToastNotification.Type.ERROR);
                    System.err.println("--- DIAGNÓSTICO DE BÓVEDA: Error en done: " + e.getMessage() + " ---");
                }
                System.out.println("--- FIN DEL DIAGNÓSTICO DE BÓVEDA ---");
            }
        }.execute();
    }

    private void saveSecret() {
        String s = txtService.getText().trim();
        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword());

        if (s.isEmpty() || u.isEmpty() || p.isEmpty()) {
            ToastNotification.show(this, "Por favor, complete todos los campos.", ToastNotification.Type.WARNING);
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                String raw = "s:" + s + "|u:" + u + "|p:" + p;
                byte[] enc = RSALogica.cifrarBytesHibrido(raw.getBytes(StandardCharsets.UTF_8), SecuritySession.getPublicKey());
                
                Map<String, Object> data = new HashMap<>();
                data.put("secreto_cifrado", Base64.getEncoder().encodeToString(enc));
                data.put("fecha_actualizacion", com.google.cloud.Timestamp.now());

                FirebaseService.getFirestore()
                        .collection(RSALogica.hashToHex("usuarios"))
                        .document(RSALogica.hashToHex(userEmail))
                        .collection(RSALogica.hashToHex("boveda"))
                        .add(data).get();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.show(Contrasenas.this, "Credencial cifrada y guardada", ToastNotification.Type.SUCCESS);
                    AuditService.log(userEmail, "VAULT_ADD", "Sitio: " + s);
                    txtService.setText(""); txtUsername.setText(""); txtPassword.setText("");
                    loadSecretsFromFirebase();
                } catch (Exception e) {
                    ToastNotification.show(Contrasenas.this, "Error al guardar en la nube", ToastNotification.Type.ERROR);
                }
            }
        }.execute();
    }

    private void viewSecret() {
        int row = table.getSelectedRow();
        if (row == -1) {
            ToastNotification.show(this, "Seleccione una credencial de la lista", ToastNotification.Type.INFO);
            return;
        }
        
        try {
            DocumentSnapshot doc = secretDocuments.get(table.convertRowIndexToModel(row));
            String enc = doc.getString("secreto_cifrado");
            byte[] dec = RSALogica.descifrarBytesHibrido(Base64.getDecoder().decode(enc), SecuritySession.getPrivateKey());
            String raw = new String(dec, StandardCharsets.UTF_8);
            String pass = "";
            for (String part : raw.split("\\|")) {
                if (part.startsWith("p:")) {
                    pass = part.substring(2);
                    break;
                }
            }
            
            JPasswordField pf = new JPasswordField(pass);
            pf.setEditable(false);
            pf.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");
            JOptionPane.showMessageDialog(this, pf, "CONTRASEÑA: " + model.getValueAt(row, 0), JOptionPane.INFORMATION_MESSAGE);
            AuditService.log(userEmail, "VAULT_VIEW", "Acceso a: " + model.getValueAt(row, 0));
        } catch (Exception e) {
            ToastNotification.show(this, "Error crítico al descifrar", ToastNotification.Type.ERROR);
        }
    }

    private void deleteSecret() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        
        String sitio = model.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "¿Desea eliminar permanentemente la credencial de " + sitio + "?\nEsta acción no se puede deshacer.", "CONFIRMAR ELIMINACIÓN", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                DocumentSnapshot doc = secretDocuments.get(table.convertRowIndexToModel(row));
                doc.getReference().delete().get();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastNotification.show(Contrasenas.this, "Registro eliminado con éxito", ToastNotification.Type.SUCCESS);
                    AuditService.log(userEmail, "VAULT_DELETE", "Eliminado: " + sitio);
                    loadSecretsFromFirebase();
                } catch (Exception e) {
                    ToastNotification.show(Contrasenas.this, "Error al eliminar de Firebase", ToastNotification.Type.ERROR);
                }
            }
        }.execute();
    }
}
