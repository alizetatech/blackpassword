package com.mycompany.blackpassword.ui;

import com.mycompany.blackpassword.services.ServerService;
import com.mycompany.blackpassword.core.util.ToastNotification;
import javax.swing.*;
import java.awt.*;

/**
 * Interfaz de conexión con la Fortaleza Digital.
 */
public class ConexionServidor extends JInternalFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JTextField txtUrl;
    private JButton btnConectar;
    private JButton btnRegistrar;
    private JLabel lblEstado;

    public ConexionServidor() {
        super("Enlace con la Fortaleza Digital", true, true, true, true);
        initComponents();
        setupLayout();
    }

    private void initComponents() {
        txtUsuario = new JTextField(System.getenv("FORTALEZA_USER") != null ? System.getenv("FORTALEZA_USER") : "", 20);
        txtPassword = new JPasswordField(20);
        String envPass = System.getenv("FORTALEZA_PASS");
        if (envPass != null) {
            txtPassword.setText(envPass);
        }
        txtUrl = new JTextField(System.getenv("FORTALEZA_URL") != null ? System.getenv("FORTALEZA_URL") : "http://localhost:8080/fortaleza/api/v1", 20);
        btnConectar = new JButton("Establecer Enlace Seguro");
        btnRegistrar = new JButton("Registrar Nuevo Agente");
        lblEstado = new JLabel("Estado: Desconectado", SwingConstants.CENTER);
        lblEstado.setForeground(Color.GRAY);

        btnConectar.addActionListener(e -> conectar());
        btnRegistrar.addActionListener(e -> registrar());
        
        // Estética Premium
        btnConectar.putClientProperty("JButton.buttonType", "roundRect");
        btnRegistrar.putClientProperty("JButton.buttonType", "roundRect");
        btnRegistrar.setForeground(new Color(0, 120, 212));
        
        txtUsuario.putClientProperty("JTextField.placeholderText", "Usuario del Servidor");
        txtPassword.putClientProperty("JTextField.placeholderText", "Contraseña Maestra");
    }

    private void setupLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("URL del Servidor:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Usuario:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(btnConectar, gbc);

        gbc.gridy = 4;
        panel.add(btnRegistrar, gbc);

        gbc.gridy = 5;
        panel.add(lblEstado, gbc);

        add(panel);
        pack();
    }

    private void conectar() {
        String user = txtUsuario.getText();
        char[] passwordChars = txtPassword.getPassword();
        String url = txtUrl.getText();

        if (user.isEmpty() || passwordChars.length == 0) {
            ToastNotification.show(this, "Usuario y contraseña son requeridos", ToastNotification.Type.WARNING);
            java.util.Arrays.fill(passwordChars, (char) 0);
            return;
        }

        btnConectar.setEnabled(false);
        lblEstado.setText("Estableciendo enlace...");
        lblEstado.setForeground(Color.ORANGE);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                ServerService service = ServerService.getInstance();
                service.setBaseUrl(url);
                boolean result = service.login(user, passwordChars);
                return result;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        lblEstado.setText("ENLACE ESTABLECIDO - JWT ACTIVO");
                        lblEstado.setForeground(new Color(0, 150, 0));
                        ToastNotification.show(ConexionServidor.this, "Conexión exitosa con la Fortaleza", ToastNotification.Type.SUCCESS);
                    } else {
                        lblEstado.setText("ERROR: Credenciales Inválidas");
                        lblEstado.setForeground(Color.RED);
                        ToastNotification.show(ConexionServidor.this, "Acceso Denegado", ToastNotification.Type.ERROR);
                    }
                } catch (Exception ex) {
                    lblEstado.setText("ERROR DE RED: Servidor no responde");
                    lblEstado.setForeground(Color.RED);
                    ToastNotification.show(ConexionServidor.this, "Fallo de conexión con GlassFish", ToastNotification.Type.ERROR);
                } finally {
                    btnConectar.setEnabled(true);
                    java.util.Arrays.fill(passwordChars, (char) 0);
                }
            }
        }.execute();
    }

    private void registrar() {
        String user = txtUsuario.getText();
        char[] passwordChars = txtPassword.getPassword();
        String url = txtUrl.getText();

        if (user.length() < 4 || passwordChars.length < 8) {
            ToastNotification.show(this, "Usuario (min 4) o Clave (min 8) muy cortos", ToastNotification.Type.WARNING);
            java.util.Arrays.fill(passwordChars, (char) 0);
            return;
        }

        btnRegistrar.setEnabled(false);
        lblEstado.setText("Registrando nueva identidad...");

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                ServerService service = ServerService.getInstance();
                service.setBaseUrl(url);
                return service.register(user, passwordChars);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        lblEstado.setText("IDENTIDAD CREADA CON ÉXITO");
                        lblEstado.setForeground(new Color(0, 150, 0));
                        ToastNotification.show(ConexionServidor.this, "Agente registrado. Ya puede conectar.", ToastNotification.Type.SUCCESS);
                    } else {
                        lblEstado.setText("ERROR: El usuario ya existe");
                        lblEstado.setForeground(Color.RED);
                        ToastNotification.show(ConexionServidor.this, "Fallo al registrar", ToastNotification.Type.ERROR);
                    }
                } catch (Exception ex) {
                    lblEstado.setText("ERROR DE COMUNICACIÓN");
                    ToastNotification.show(ConexionServidor.this, "Servidor fuera de línea", ToastNotification.Type.ERROR);
                } finally {
                    btnRegistrar.setEnabled(true);
                    java.util.Arrays.fill(passwordChars, (char) 0);
                }
            }
        }.execute();
    }
}
