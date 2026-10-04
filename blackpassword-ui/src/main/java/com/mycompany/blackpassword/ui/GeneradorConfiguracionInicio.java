package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.crypto.RSALogica;
import com.mycompany.blackpassword.services.ConfigBundleService;
import com.mycompany.blackpassword.services.VirtualKeyboard;
import com.mycompany.blackpassword.core.util.ToastNotification;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Generador de Configuración de Inicio de Sesión v1.0.
 * Permite ingresar la configuración de Firebase (archivo o texto JSON),
 * genera un par de llaves RSA seguras (2048-bit), cifra todos los componentes
 * con la contraseña maestra y los empaqueta en un contenedor KEK maestro (.kek)
 * para un arranque simplificado de una sola credencial.
 */
public class GeneradorConfiguracionInicio extends javax.swing.JInternalFrame {

    private Consumer<File> onBundleCreatedCallback;

    public GeneradorConfiguracionInicio() {
        this(null);
    }

    public GeneradorConfiguracionInicio(Consumer<File> callback) {
        this.onBundleCreatedCallback = callback;
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");
        
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 20; foreground: #0078D4");
        lblSubtitle.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #666666");

        String fieldStyle = "arc: 12; background: #FFFFFF";
        txtFirebasePath.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtFirebasePath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione archivo serviceAccount.json o .black...");
        
        txtTargetDir.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtTargetDir.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Carpeta donde se guardará el paquete KEK...");
        
        txtBaseName.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtBaseName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre base (ej. blackpassword_inicio)");

        txtPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txtPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña Maestra...");
        
        txtConfirmPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txtConfirmPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirmar Contraseña...");

        btnBrowseFirebase.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #E1F0FE; foreground: #0078D4; hoverBackground: #CCE5FF; font: bold 12");
        btnBrowseDir.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #E1F0FE; foreground: #0078D4; hoverBackground: #CCE5FF; font: bold 12");
        btnVirtualKey.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; font: 12");

        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #107C10; foreground: #FFFFFF; hoverBackground: #0E680E; font: bold 14; borderWidth: 0");

        txtFirebaseJson.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private void initComponents() {
        jPanelCard = new JPanel();
        lblTitle = new JLabel("⚙️ Generador de Configuración de Inicio");
        lblSubtitle = new JLabel("Empaqueta Firebase + Llaves RSA en un Contenedor KEK Maestro");

        tabbedPaneFirebase = new JTabbedPane();
        
        // Tab 1: Archivo
        JPanel panelFile = new JPanel(new BorderLayout(8, 8));
        panelFile.setBackground(Color.WHITE);
        txtFirebasePath = new JTextField();
        btnBrowseFirebase = new JButton("BUSCAR 📂");
        btnBrowseFirebase.addActionListener(e -> browseFirebaseFile());
        panelFile.add(txtFirebasePath, BorderLayout.CENTER);
        panelFile.add(btnBrowseFirebase, BorderLayout.EAST);
        tabbedPaneFirebase.addTab("📁 Desde Archivo JSON / .black", panelFile);

        // Tab 2: Pegar JSON
        JPanel panelText = new JPanel(new BorderLayout());
        panelText.setBackground(Color.WHITE);
        txtFirebaseJson = new JTextArea(4, 30);
        txtFirebaseJson.setLineWrap(true);
        txtFirebaseJson.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtFirebaseJson);
        panelText.add(scrollPane, BorderLayout.CENTER);
        tabbedPaneFirebase.addTab("📋 Pegar Contenido JSON Directo", panelText);

        lblPass = new JLabel("Contraseña Maestra para Cifrar el Paquete:");
        txtPass = new JPasswordField();
        btnVirtualKey = new JButton("⌨️");
        btnVirtualKey.addActionListener(e -> VirtualKeyboard.show(SwingUtilities.getWindowAncestor(this), txtPass));

        lblConfirmPass = new JLabel("Confirmar Contraseña Maestra:");
        txtConfirmPass = new JPasswordField();

        lblTargetDir = new JLabel("Carpeta de Guardado:");
        txtTargetDir = new JTextField(System.getProperty("user.home"));
        btnBrowseDir = new JButton("CARPETA 📁");
        btnBrowseDir.addActionListener(e -> browseTargetDirectory());

        lblBaseName = new JLabel("Nombre del Paquete:");
        txtBaseName = new JTextField("blackpassword_inicio");

        chkExportIndividuals = new JCheckBox("Exportar también copias individuales de respaldo (.black, .pub, .key)", true);
        chkExportIndividuals.setOpaque(false);

        btnGenerar = new JButton("🔒 GENERAR LLAVES Y BLINDAR PAQUETE KEK");
        btnGenerar.addActionListener(e -> procesarGeneracion());

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        setClosable(true);
        setIconifiable(true);
        setTitle("Generador de Configuración de Inicio - Blackpassword");

        // Layout
        GroupLayout layout = new GroupLayout(jPanelCard);
        jPanelCard.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(25, 25, 25)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(lblTitle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblSubtitle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(tabbedPaneFirebase)
                        .addComponent(lblPass)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(txtPass)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnVirtualKey, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE))
                        .addComponent(lblConfirmPass)
                        .addComponent(txtConfirmPass)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblTargetDir)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(txtTargetDir, GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnBrowseDir, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)))
                            .addGap(15, 15, 15)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblBaseName)
                                .addComponent(txtBaseName, GroupLayout.DEFAULT_SIZE, 160, Short.MAX_VALUE)))
                        .addComponent(chkExportIndividuals)
                        .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnGenerar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(25, 25, 25))
        );

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(lblTitle)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSubtitle)
                .addGap(15, 15, 15)
                .addComponent(tabbedPaneFirebase, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(lblPass)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPass, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKey, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(lblConfirmPass)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtConfirmPass, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTargetDir)
                    .addComponent(lblBaseName))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTargetDir, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseDir, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBaseName, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(chkExportIndividuals)
                .addGap(15, 15, 15)
                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnGenerar, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
        );

        getContentPane().add(jPanelCard);
        pack();
    }

    private void browseFirebaseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar Configuración de Firebase (.json o .black)");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtFirebasePath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseTargetDirectory() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar Carpeta de Destino");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtTargetDir.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void procesarGeneracion() {
        char[] pass = txtPass.getPassword();
        char[] confirm = txtConfirmPass.getPassword();

        if (pass.length == 0) {
            JOptionPane.showMessageDialog(this, "Debe ingresar una contraseña maestra.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Arrays.equals(pass, confirm)) {
            JOptionPane.showMessageDialog(this, "Las contraseñas maestras no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(pass, (char) 0);
            Arrays.fill(confirm, (char) 0);
            return;
        }

        // Obtener configuración de Firebase (del archivo o del área de texto)
        byte[] firebaseBytes = null;
        try {
            if (tabbedPaneFirebase.getSelectedIndex() == 0) {
                String path = txtFirebasePath.getText().trim();
                if (path.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Seleccione un archivo de configuración de Firebase.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                File f = new File(path);
                if (!f.exists()) {
                    JOptionPane.showMessageDialog(this, "El archivo de Firebase no existe: " + path, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                firebaseBytes = Files.readAllBytes(f.toPath());
            } else {
                String jsonText = txtFirebaseJson.getText().trim();
                if (jsonText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Ingrese o pegue el contenido JSON de Firebase.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                firebaseBytes = jsonText.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al leer configuración de Firebase: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String targetDirPath = txtTargetDir.getText().trim();
        if (targetDirPath.isEmpty()) targetDirPath = System.getProperty("user.home");
        File targetDir = new File(targetDirPath);

        String baseName = txtBaseName.getText().trim();
        if (baseName.isEmpty()) baseName = "blackpassword_inicio";
        if (baseName.toLowerCase().endsWith(".kek")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        final byte[] finalFbBytes = firebaseBytes;
        final String finalBaseName = baseName;
        final boolean exportIndividuals = chkExportIndividuals.isSelected();

        btnGenerar.setEnabled(false);
        progressBar.setVisible(true);

        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                System.out.println("[GeneradorConfig] Generando nuevo par de llaves RSA 2048...");
                KeyPair kp = RSALogica.generarLlaves();

                System.out.println("[GeneradorConfig] Empaquetando y blindando paquete KEK maestro...");
                File bundleFile = new File(targetDir, finalBaseName + ".kek");

                if (exportIndividuals) {
                    ConfigBundleService.exportCompleteSuite(targetDir, finalBaseName, finalFbBytes, kp.getPublic(), kp.getPrivate(), pass);
                } else {
                    ConfigBundleService.createBundle(finalFbBytes, kp.getPublic(), kp.getPrivate(), pass, bundleFile);
                }

                return bundleFile;
            }

            @Override
            protected void done() {
                Arrays.fill(pass, (char) 0);
                Arrays.fill(confirm, (char) 0);
                Arrays.fill(finalFbBytes, (byte) 0);
                btnGenerar.setEnabled(true);
                progressBar.setVisible(false);

                try {
                    File bundleFile = get();
                    String msg = "🎉 ¡CONFIGURACIÓN GENERADA Y BLINDADA CON ÉXITO!\n\n"
                            + "📦 Paquete Maestro Todo-en-Uno Creado:\n"
                            + bundleFile.getAbsolutePath() + "\n\n";

                    if (exportIndividuals) {
                        msg += "📁 Copias de respaldo individuales exportadas:\n"
                             + " • " + finalBaseName + ".pub (Llave Pública)\n"
                             + " • " + finalBaseName + ".key (Llave Privada Cifrada)\n"
                             + " • " + finalBaseName + ".black (Firebase Cifrado)\n\n";
                    }

                    msg += "Para iniciar sesión:\n"
                         + "1. Selecciona el archivo .kek en la pantalla de inicio.\n"
                         + "2. Escribe tu contraseña maestra.\n"
                         + "3. Haz clic en 'DESCIFRAR Y CONECTAR'.";

                    JOptionPane.showMessageDialog(GeneradorConfiguracionInicio.this, msg, "Éxito - Paquete KEK Creado", JOptionPane.INFORMATION_MESSAGE);

                    if (onBundleCreatedCallback != null) {
                        onBundleCreatedCallback.accept(bundleFile);
                    }

                    dispose();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(GeneradorConfiguracionInicio.this, "Error al generar configuración: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private JPanel jPanelCard;
    private JLabel lblTitle;
    private JLabel lblSubtitle;
    private JTabbedPane tabbedPaneFirebase;
    private JTextField txtFirebasePath;
    private JButton btnBrowseFirebase;
    private JTextArea txtFirebaseJson;
    private JLabel lblPass;
    private JPasswordField txtPass;
    private JButton btnVirtualKey;
    private JLabel lblConfirmPass;
    private JPasswordField txtConfirmPass;
    private JLabel lblTargetDir;
    private JTextField txtTargetDir;
    private JButton btnBrowseDir;
    private JLabel lblBaseName;
    private JTextField txtBaseName;
    private JCheckBox chkExportIndividuals;
    private JProgressBar progressBar;
    private JButton btnGenerar;
}
