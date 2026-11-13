package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.mycompany.blackpassword.services.ConfigBundleService;
import com.mycompany.blackpassword.services.VirtualKeyboard;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Migrador y Actualizador de Paquete Maestro KEK v1.0.
 * Permite tomar un paquete .kek existente, extraer en memoria sus llaves RSA originales
 * (manteniendo 100% la compatibilidad con contraseñas ya cifradas), inyectar la nueva
 * configuración de Cloud Functions HTTPS y generar el nuevo archivo .kek protegido.
 */
public class MigradorPaqueteKek extends javax.swing.JInternalFrame {

    private final Consumer<File> onMigratedCallback;

    public MigradorPaqueteKek() {
        this(null);
    }

    public MigradorPaqueteKek(Consumer<File> callback) {
        this.onMigratedCallback = callback;
        initComponents();
        applyPremiumStyle();
    }

    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        jPanelCard.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,0,0, #FFFFFF, 0, 20");

        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 20; foreground: #0078D4");
        lblSubtitle.putClientProperty(FlatClientProperties.STYLE, "font: 11; foreground: #666666");

        String fieldStyle = "arc: 12; background: #FFFFFF";
        txtOldKekPath.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtOldKekPath.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Seleccione su archivo .kek actual con sus llaves...");

        txtOldPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txtOldPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Contraseña actual del paquete .kek...");

        txtNewPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txtNewPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nueva Contraseña Maestra (o la misma actual)...");

        txtConfirmNewPass.putClientProperty(FlatClientProperties.STYLE, fieldStyle + "; showRevealButton: true");
        txtConfirmNewPass.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Confirmar Nueva Contraseña...");

        txtTargetDir.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtTargetDir.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Carpeta de destino...");

        txtBaseName.putClientProperty(FlatClientProperties.STYLE, fieldStyle);
        txtBaseName.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Nombre del nuevo paquete (ej. blackpassword_cloud)");

        btnBrowseOldKek.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #E1F0FE; foreground: #0078D4; hoverBackground: #CCE5FF; font: bold 12");
        btnBrowseDir.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #E1F0FE; foreground: #0078D4; hoverBackground: #CCE5FF; font: bold 12");
        btnVirtualKeyOld.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; font: 12");
        btnVirtualKeyNew.putClientProperty(FlatClientProperties.STYLE, "arc: 10; background: #F3F3F3; font: 12");

        btnMigrar.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; hoverBackground: #005A9E; font: bold 14; borderWidth: 0");

        txtNewConfigJson.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }

    private void initComponents() {
        jPanelCard = new JPanel();
        lblTitle = new JLabel("🔄 Actualizador / Migrador de Paquete KEK");
        lblSubtitle = new JLabel("Conserva tus llaves RSA originales e inyecta la nueva configuración Cloud Functions HTTPS");

        lblOldKek = new JLabel("1. Selecciona tu Paquete .kek Actual:");
        txtOldKekPath = new JTextField();
        btnBrowseOldKek = new JButton("BUSCAR 📂");
        btnBrowseOldKek.addActionListener(e -> browseOldKek());

        lblOldPass = new JLabel("2. Contraseña Actual del Paquete (para desbloquear tus llaves RSA):");
        txtOldPass = new JPasswordField();
        btnVirtualKeyOld = new JButton("⌨️");
        btnVirtualKeyOld.addActionListener(e -> VirtualKeyboard.show(SwingUtilities.getWindowAncestor(this), txtOldPass));

        lblNewConfig = new JLabel("3. Pega la Nueva Configuración de Firebase / Cloud Functions:");
        txtNewConfigJson = new JTextArea(5, 30);
        txtNewConfigJson.setLineWrap(true);
        txtNewConfigJson.setWrapStyleWord(true);
        txtNewConfigJson.setText("{\n"
                + "  \"functionBaseUrl\": \"https://us-central1-blackpassword.cloudfunctions.net/api\",\n"
                + "  \"apiKey\": \"AIzaSyD6gPEfnSTS4aFRa1XtsLiH_DZ1YOoiGv0\",\n"
                + "  \"projectId\": \"blackpassword\"\n"
                + "}");
        JScrollPane scrollConfig = new JScrollPane(txtNewConfigJson);

        lblNewPass = new JLabel("4. Contraseña Maestra para el Nuevo Paquete (puede ser la misma):");
        txtNewPass = new JPasswordField();
        btnVirtualKeyNew = new JButton("⌨️");
        btnVirtualKeyNew.addActionListener(e -> VirtualKeyboard.show(SwingUtilities.getWindowAncestor(this), txtNewPass));

        lblConfirmNewPass = new JLabel("Confirmar Nueva Contraseña:");
        txtConfirmNewPass = new JPasswordField();

        lblTargetDir = new JLabel("Carpeta de Guardado:");
        txtTargetDir = new JTextField(System.getProperty("user.home"));
        btnBrowseDir = new JButton("CARPETA 📁");
        btnBrowseDir.addActionListener(e -> browseTargetDir());

        lblBaseName = new JLabel("Nombre del Nuevo Archivo:");
        txtBaseName = new JTextField("blackpassword_cloud");

        btnMigrar = new JButton("🚀 MIGRAR CONFIGURACIÓN Y GENERAR NUEVO .KEK");
        btnMigrar.addActionListener(e -> ejecutarMigracion());

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        setClosable(true);
        setIconifiable(true);
        setTitle("Actualizador de Paquete Maestro KEK - Blackpassword");

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
                        .addComponent(lblOldKek)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(txtOldKekPath, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnBrowseOldKek, GroupLayout.PREFERRED_SIZE, 110, GroupLayout.PREFERRED_SIZE))
                        .addComponent(lblOldPass)
                        .addGroup(layout.createSequentialGroup()
                            .addComponent(txtOldPass)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(btnVirtualKeyOld, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE))
                        .addComponent(lblNewConfig)
                        .addComponent(scrollConfig)
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblNewPass)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(txtNewPass, GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnVirtualKeyNew, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)))
                            .addGap(15, 15, 15)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblConfirmNewPass)
                                .addComponent(txtConfirmNewPass)))
                        .addGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblTargetDir)
                                .addGroup(layout.createSequentialGroup()
                                    .addComponent(txtTargetDir, GroupLayout.PREFERRED_SIZE, 330, GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(btnBrowseDir, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)))
                            .addGap(15, 15, 15)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(lblBaseName)
                                .addComponent(txtBaseName)))
                        .addComponent(progressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnMigrar, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGap(25, 25, 25))
        );

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(lblTitle)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSubtitle)
                .addGap(12, 12, 12)
                .addComponent(lblOldKek)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtOldKekPath, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseOldKek, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(lblOldPass)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtOldPass, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKeyOld, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(lblNewConfig)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollConfig, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblNewPass)
                    .addComponent(lblConfirmNewPass))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNewPass, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnVirtualKeyNew, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtConfirmNewPass, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTargetDir)
                    .addComponent(lblBaseName))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTargetDir, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnBrowseDir, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtBaseName, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(progressBar, GroupLayout.PREFERRED_SIZE, 10, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnMigrar, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
        );

        getContentPane().add(jPanelCard);
        pack();
    }

    private void browseOldKek() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar tu Paquete KEK Actual (.kek o .wfile)");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtOldKekPath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseTargetDir() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar Carpeta para Guardar el Nuevo Paquete");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtTargetDir.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void ejecutarMigracion() {
        String oldKekPath = txtOldKekPath.getText().trim();
        if (oldKekPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar su archivo .kek actual.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File oldFile = new File(oldKekPath);
        if (!oldFile.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo .kek no existe: " + oldKekPath, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        char[] oldPass = txtOldPass.getPassword();
        if (oldPass.length == 0) {
            JOptionPane.showMessageDialog(this, "Ingrese la contraseña actual del archivo .kek.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String newConfigText = txtNewConfigJson.getText().trim();
        if (newConfigText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar el nuevo JSON de configuración.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        char[] newPass = txtNewPass.getPassword();
        char[] confirmNewPass = txtConfirmNewPass.getPassword();

        // Si dejó en blanco la nueva contraseña, reutiliza la contraseña actual
        final char[] passToUse = (newPass.length > 0) ? newPass : oldPass;

        if (newPass.length > 0 && !Arrays.equals(newPass, confirmNewPass)) {
            JOptionPane.showMessageDialog(this, "Las nuevas contraseñas no coinciden.", "Error", JOptionPane.ERROR_MESSAGE);
            Arrays.fill(newPass, (char) 0);
            Arrays.fill(confirmNewPass, (char) 0);
            return;
        }

        String targetDirPath = txtTargetDir.getText().trim();
        if (targetDirPath.isEmpty()) targetDirPath = System.getProperty("user.home");
        File targetDir = new File(targetDirPath);

        String baseName = txtBaseName.getText().trim();
        if (baseName.isEmpty()) baseName = "blackpassword_cloud";
        if (baseName.toLowerCase().endsWith(".kek")) {
            baseName = baseName.substring(0, baseName.length() - 4);
        }

        final File targetFile = new File(targetDir, baseName + ".kek");
        final byte[] newConfigBytes = newConfigText.getBytes(StandardCharsets.UTF_8);

        btnMigrar.setEnabled(false);
        progressBar.setVisible(true);

        SwingWorker<File, Void> worker = new SwingWorker<>() {
            @Override
            protected File doInBackground() throws Exception {
                System.out.println("[MigradorKEK] Desbloqueando paquete actual y extrayendo llaves RSA originales...");
                ConfigBundleService.BundleResult bundle = ConfigBundleService.unpackBundle(oldFile, oldPass);

                if (bundle.publicKey == null || bundle.privateKey == null) {
                    throw new Exception("El paquete actual no contiene un par de llaves RSA válido.");
                }

                System.out.println("[MigradorKEK] Conservando llaves RSA originales y empaquetando nueva configuración HTTPS...");
                ConfigBundleService.createBundle(newConfigBytes, bundle.publicKey, bundle.privateKey, passToUse, targetFile);

                return targetFile;
            }

            @Override
            protected void done() {
                Arrays.fill(oldPass, (char) 0);
                Arrays.fill(newPass, (char) 0);
                Arrays.fill(confirmNewPass, (char) 0);
                btnMigrar.setEnabled(true);
                progressBar.setVisible(false);

                try {
                    File nuevoKek = get();
                    String msg = "🎉 ¡MIGRACIÓN DE PAQUETE KEK COMPLETADA CON ÉXITO!\n\n"
                            + "📦 Nuevo Paquete Maestro Creado:\n"
                            + nuevoKek.getAbsolutePath() + "\n\n"
                            + "🛡️ SEGURIDAD GARANTIZADA:\n"
                            + " • Se conservaron intactas tus llaves RSA originales (100% compatible con tus contraseñas ya guardadas).\n"
                            + " • Se actualizó la conexión a tu Cloud Function privada por HTTPS.\n\n"
                            + "¿Deseas cargar este nuevo archivo .kek ahora para iniciar sesión?";

                    int opcion = JOptionPane.showConfirmDialog(MigradorPaqueteKek.this, msg, "Éxito - Paquete KEK Actualizado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                    if (opcion == JOptionPane.YES_OPTION && onMigratedCallback != null) {
                        onMigratedCallback.accept(nuevoKek);
                    }

                    dispose();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String err = ex.getMessage() != null ? ex.getMessage() : ex.toString();
                    JOptionPane.showMessageDialog(MigradorPaqueteKek.this, "Error durante la migración: " + err + "\n\nVerifique que la contraseña actual del .kek sea la correcta.", "Fallo de Migración", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    private JPanel jPanelCard;
    private JLabel lblTitle;
    private JLabel lblSubtitle;
    private JLabel lblOldKek;
    private JTextField txtOldKekPath;
    private JButton btnBrowseOldKek;
    private JLabel lblOldPass;
    private JPasswordField txtOldPass;
    private JButton btnVirtualKeyOld;
    private JLabel lblNewConfig;
    private JTextArea txtNewConfigJson;
    private JLabel lblNewPass;
    private JPasswordField txtNewPass;
    private JButton btnVirtualKeyNew;
    private JLabel lblConfirmNewPass;
    private JPasswordField txtConfirmNewPass;
    private JLabel lblTargetDir;
    private JTextField txtTargetDir;
    private JButton btnBrowseDir;
    private JLabel lblBaseName;
    private JTextField txtBaseName;
    private JProgressBar progressBar;
    private JButton btnMigrar;
}
