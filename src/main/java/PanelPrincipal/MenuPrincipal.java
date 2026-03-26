package PanelPrincipal;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.AnimationUtils;
import com.mycompany.blackpassword.AuditService;
import com.mycompany.blackpassword.LocalDatabaseService;
import com.mycompany.blackpassword.RemoteKillService;
import com.mycompany.blackpassword.SecuritySession;
import com.mycompany.blackpassword.ToastNotification;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingConstants;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import PanelPrincipal.EncriptadorDeAlgoritmos;
import java.util.Arrays;
import javax.swing.SwingUtilities; 

/**
 * Dashboard Blindado v11.0: Verificación de PIN para Bóveda.
 */
public class MenuPrincipal extends javax.swing.JInternalFrame {

    static {
        System.out.println("[v11.0] Dashboard cargado. Verificación de PIN activa.");
    }

    private final String userEmail;
    private final String userName;
    private Image backgroundImage = null;
    private final Preferences prefs = Preferences.userNodeForPackage(MenuPrincipal.class);
    
    private Timer inactivityTimer;
    private AWTEventListener globalActivityListener;
    private final int INACTIVITY_TIMEOUT = 5 * 60 * 1000;
    
    private JPanel privacyShield;
    private boolean camouflageActive = false;

    public MenuPrincipal(String nombre, String email) {
        this.userName = nombre;
        this.userEmail = email;
        
        try {
            initComponents();
            applyPremiumStyle();
            setupInternalCentering();
            setupTopNavigation();
            setupPrivacyShield();
            cargarFondoPersistente();
            setupInactivityTimer();
            setupPanicShortcut();
            setupFocusSecurity();
            setupCamouflageShortcut();
            setupRemoteKillMonitoring();
            System.out.println("[v11.0] Dashboard listo para: " + email);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    private void applyPremiumStyle() {
        this.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background");
        this.setBorder(null);
        if (this.getUI() instanceof javax.swing.plaf.basic.BasicInternalFrameUI ui) {
            ui.setNorthPane(null);
        }
    }

    private void setupRemoteKillMonitoring() {
        try {
            RemoteKillService.startMonitoring(userEmail, () -> {
                LocalDatabaseService.clearCache(userEmail);
                limpiarRecursosSeguridad();
            });
        } catch (Exception e) {}
    }

    private void setupPrivacyShield() {
        privacyShield = new JPanel(new BorderLayout());
        privacyShield.setBackground(new Color(10, 10, 10, 230));
        JLabel lblShield = new JLabel("MODO PRIVACIDAD ACTIVO", SwingConstants.CENTER);
        lblShield.setForeground(Color.WHITE);
        lblShield.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        privacyShield.add(lblShield, BorderLayout.CENTER);
        privacyShield.setVisible(false);
        this.getLayeredPane().add(privacyShield, javax.swing.JLayeredPane.DRAG_LAYER);
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { privacyShield.setBounds(0, 0, getWidth(), getHeight()); }
        });
    }

    private void setupFocusSecurity() {
        java.awt.EventQueue.invokeLater(() -> {
            java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
            if (parent != null) {
                parent.addWindowFocusListener(new WindowFocusListener() {
                    @Override public void windowGainedFocus(WindowEvent e) { if (!camouflageActive) privacyShield.setVisible(false); }
                    @Override public void windowLostFocus(WindowEvent e) {
                        if (SecuritySession.isSessionActive() && !camouflageActive) privacyShield.setVisible(true);
                    }
                });
            }
        });
    }

    private void setupPanicShortcut() {
        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "panic");
        this.getActionMap().put("panic", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { ejecutarPanico(); }
        });
    }

    private void setupCamouflageShortcut() {
        this.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "camouflage");
        this.getActionMap().put("camouflage", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { toggleCamouflage(); }
        });
    }

    private void toggleCamouflage() {
        camouflageActive = !camouflageActive;
        java.awt.Window parent = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (parent instanceof FramePrincipal fp) fp.setCamouflageMode(camouflageActive);
        for (JInternalFrame f : internalDesktop.getAllFrames()) f.setVisible(!camouflageActive);
    }

    private void setupInactivityTimer() {
        inactivityTimer = new Timer(INACTIVITY_TIMEOUT, e -> ejecutarCierreSesion());
        inactivityTimer.setRepeats(false);
        globalActivityListener = event -> { if (inactivityTimer != null && inactivityTimer.isRunning()) inactivityTimer.restart(); };
        Toolkit.getDefaultToolkit().addAWTEventListener(globalActivityListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        inactivityTimer.start();
    }

    private void ejecutarCierreSesion() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Desea cerrar su sesión segura?", "Cerrar Sesión", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            limpiarRecursosSeguridad();
            javax.swing.JDesktopPane mainDesktop = this.getDesktopPane();
            this.dispose();
            if (mainDesktop != null) {
                Inicio login = new Inicio(); 
                mainDesktop.add(login); 
                login.setVisible(true);
                try { login.setSelected(true); } catch (Exception e) {}
            }
        }
    }

    private void ejecutarPanico() {
        limpiarRecursosSeguridad();
        System.exit(0);
    }

    private void limpiarRecursosSeguridad() {
        if (inactivityTimer != null) inactivityTimer.stop();
        if (globalActivityListener != null) Toolkit.getDefaultToolkit().removeAWTEventListener(globalActivityListener);
        SecuritySession.clear();
        AuditService.log(userEmail, "LOGOUT_SUCCESS", "Sesión cerrada y llaves purgadas de RAM.");
    }

    private void cargarFondoPersistente() {
        try {
            String path = prefs.get("bg_path_" + userEmail, "");
            if (!path.isEmpty() && new File(path).exists()) {
                backgroundImage = ImageIO.read(new File(path));
            } else {
                java.net.URL imgUrl = getClass().getResource("/img/blackpassword.png");
                if (imgUrl != null) backgroundImage = ImageIO.read(imgUrl);
            }
        } catch (Exception e) {
            System.err.println("[MenuPrincipal] Error al cargar fondo persistente: " + e.getMessage());
        }
    }
    
    public void setDashboardBackground(File imageFile) {
        try {
            this.backgroundImage = ImageIO.read(imageFile);
            prefs.put("bg_path_" + userEmail, imageFile.getAbsolutePath());
            internalDesktop.repaint();
            AuditService.log(userEmail, "DASHBOARD_BACKGROUND_SET", "Fondo de dashboard personalizado.");
        } catch (Exception e) {
            System.err.println("[MenuPrincipal] Error al establecer fondo: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Error al establecer la imagen como fondo: " + e.getMessage(), "Error de Fondo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupInternalCentering() {
        internalDesktop.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                for (JInternalFrame f : internalDesktop.getAllFrames()) if (f.isVisible() && !f.isMaximum()) centerInternalFrame(f);
            }
        });
    }

    private void setupTopNavigation() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setPreferredSize(new Dimension(getWidth(), 65));
        topBar.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; border: 0,0,1,0,#D0D0D0");
        JPanel buttonsContainer = new JPanel(new BorderLayout());
        buttonsContainer.setOpaque(false);
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        leftGroup.setOpaque(false);
        
        leftGroup.add(createNavButton("BÓVEDA", "vault.svg", e -> solicitarPinParaBoveda()));
        leftGroup.add(createNavButton("ARCHIVOS", "files.svg", e -> abrirVentanaUnica(Archivos.class, () -> new Archivos(userEmail))));
        leftGroup.add(createNavButton("IMÁGENES", "images.svg", e -> abrirVentanaUnica(Imagenes.class, () -> new Imagenes(userEmail, this))));
        leftGroup.add(createNavButton("REPORTES", "report.svg", e -> abrirVentanaUnica(Reportes.class, () -> new Reportes(userEmail))));
        leftGroup.add(createNavButton("IDENTIDAD", "user.svg", e -> abrirVentanaUnica(IdentidadDigital.class, () -> new IdentidadDigital(userEmail))));
        
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        rightGroup.setOpaque(false);
        
        rightGroup.add(createNavButton("SALIR", "unlock.svg", e -> ejecutarCierreSesion()));
        rightGroup.add(createNavButton("PÁNICO", "panic.svg", e -> ejecutarPanico()));
        
        JButton btnUser = createNavButton(userName.toUpperCase(), "user.svg", null);
        btnUser.putClientProperty(FlatClientProperties.STYLE, "foreground: #0078D4; font: bold");
        rightGroup.add(btnUser);
        
        buttonsContainer.add(leftGroup, BorderLayout.WEST);
        buttonsContainer.add(rightGroup, BorderLayout.EAST);
        topBar.add(buttonsContainer, BorderLayout.CENTER);
        getContentPane().add(topBar, BorderLayout.NORTH);
    }

    private void solicitarPinParaBoveda() {
        JPasswordField pinField = new JPasswordField(4);
        pinField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true;");
        Object[] message = {"Para acceder a la bóveda, ingrese su PIN de seguridad:", pinField};
        
        int option = JOptionPane.showConfirmDialog(this, message, "Verificación de Seguridad", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            char[] pin = pinField.getPassword();
            if (SecuritySession.verifyUserPin(pin)) {
                AuditService.log(userEmail, "VAULT_ACCESS_SUCCESS", "Acceso a la bóveda autorizado por PIN.");
                abrirVentanaUnica(Contrasenas.class, () -> new Contrasenas(userEmail));
            } else {
                AuditService.log(userEmail, "VAULT_ACCESS_DENIED", "Intento de acceso a la bóveda con PIN incorrecto.");
                JOptionPane.showMessageDialog(this, "PIN incorrecto. Acceso denegado.", "Error de Seguridad", JOptionPane.ERROR_MESSAGE);
            }
            Arrays.fill(pin, '0'); // Limpiar PIN de la memoria
        }
    }

    private JButton createNavButton(String text, String iconName, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text);
        try {
            FlatSVGIcon icon = new FlatSVGIcon("img/" + iconName, 18, 18);
            if (icon != null) btn.setIcon(icon);
        } catch (Exception e) {}
        if (action != null) btn.addActionListener(action);
        btn.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; arc: 12; margin: 6,12,6,12; font: bold; hoverForeground: #0078D4");
        return btn;
    }

    private void abrirVentanaUnica(Class<?> claseVentana, java.util.function.Supplier<JInternalFrame> creador) {
        for (JInternalFrame f : internalDesktop.getAllFrames()) {
            if (claseVentana.isInstance(f)) {
                try {
                    f.setSelected(true); f.toFront(); return;
                } catch (Exception e) {}
            }
        }
        try {
            JInternalFrame nueva = creador.get();
            internalDesktop.add(nueva);
            centerInternalFrame(nueva);
            AnimationUtils.fadeInAndSlide(nueva, internalDesktop);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al abrir la ventana: " + e.getMessage(), "Error de UI", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initComponents() {
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        internalDesktop = new javax.swing.JDesktopPane() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null && !camouflageActive) {
                    Graphics2D g2d = (Graphics2D) g;
                    int iw = backgroundImage.getWidth(this), ih = backgroundImage.getHeight(this);
                    double scale = Math.max((double)getWidth()/iw, (double)getHeight()/ih);
                    g2d.drawImage(backgroundImage, (getWidth()-(int)(iw*scale))/2, (getHeight()-(int)(ih*scale))/2, (int)(iw*scale), (int)(ih*scale), this);
                }
            }
        };
        setClosable(false); setIconifiable(false); setMaximizable(false); setResizable(false);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(internalDesktop, BorderLayout.CENTER);
        
        jMenu1.setText("Archivo");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Seguridad");
        JMenuItem itemGenRsa = new JMenuItem("🔑 Generador de Llaves RSA");
        itemGenRsa.addActionListener(e -> {
            GeneradorLlavesRsa rsaFrame = new GeneradorLlavesRsa();
            internalDesktop.add(rsaFrame); centerInternalFrame(rsaFrame); rsaFrame.setVisible(true);
        });
        jMenu2.add(itemGenRsa);
        
        JMenuItem itemEncriptador = new JMenuItem("🔐 Encriptador de Algoritmos");
        itemEncriptador.addActionListener(e -> {
            abrirVentanaUnica(EncriptadorDeAlgoritmos.class, () -> new EncriptadorDeAlgoritmos());
        });
        jMenu2.add(itemEncriptador);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Apariencia");
        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void centerInternalFrame(JInternalFrame frame) {
        frame.setLocation((internalDesktop.getWidth() - frame.getWidth()) / 2, (internalDesktop.getHeight() - frame.getHeight()) / 2);
    }

    private javax.swing.JDesktopPane internalDesktop;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
}
