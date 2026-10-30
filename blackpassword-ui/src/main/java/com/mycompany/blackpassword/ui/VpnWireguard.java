package com.mycompany.blackpassword.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.mycompany.blackpassword.core.util.ToastNotification;
import com.mycompany.blackpassword.services.AuditService;
import com.mycompany.blackpassword.services.WireguardService;
import com.mycompany.blackpassword.services.WireguardService.KeyPairWireguard;
import com.mycompany.blackpassword.services.WireguardService.WireguardProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

/**
 * Centro de Comando VPN WireGuard v1.0 - Zero-Disk Cloud Architecture.
 * Permite gestionar túneles WireGuard, almacenar configuraciones cifradas
 * exclusivamente en Firebase Firestore y generar claves Curve25519 nativas.
 */
public class VpnWireguard extends JInternalFrame {

    private final String userEmail;
    private JTabbedPane mainTabbedPane;

    // Componentes Pestaña 1: Conexión y Monitor
    private JComboBox<WireguardProfile> cbPerfiles;
    private JLabel lblStatusBadge;
    private JButton btnToggleTunnel;
    private JLabel lblEndpointVal;
    private JLabel lblAddressVal;
    private JLabel lblDnsVal;
    private JLabel lblMtuVal;
    private JLabel lblUptimeVal;
    private JLabel lblPingVal;
    private Timer uptimeTimer;
    private JPanel bannerWgNotFound;

    // Componentes Pestaña 2: Perfiles Nube
    private JTextField txtNombrePerfil;
    private JTextArea txtConfigRaw;
    private JTable tblPerfiles;
    private DefaultTableModel modelPerfiles;
    private List<WireguardProfile> perfilesCargados;

    // Componentes Pestaña 3: Generador Curve25519
    private JTextField txtGenPrivKey;
    private JTextField txtGenPubKey;
    private JTextField txtGenPsk;

    public VpnWireguard(String email) {
        super("🌐 TÚNEL VPN WIREGUARD - ZERO-DISK CLOUD", true, true, true, true);
        this.userEmail = email;
        setSize(940, 680);
        setMinimumSize(new Dimension(800, 560));
        setLayout(new BorderLayout());

        initComponents();
        cargarPerfilesDesdeNube();
        iniciarTemporizadorMonitor();
    }

    private void initComponents() {
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.putClientProperty(FlatClientProperties.STYLE, "tabAreaInsets: 10,15,0,15; font: bold 12");

        mainTabbedPane.addTab("🛡️ CONEXIÓN & TÚNEL", crearPanelConexion());
        mainTabbedPane.addTab("☁️ PERFILES EN FIREBASE", crearPanelPerfiles());
        mainTabbedPane.addTab("🔑 GENERADOR CURVE25519", crearPanelGeneradorLlaves());

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    // --- PESTAÑA 1: CONEXIÓN & MONITOR EN VIVO ---

    private JScrollPane crearPanelConexion() {
        ResponsiveScrollablePanel main = new ResponsiveScrollablePanel(new BorderLayout(0, 16));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Selector Superior de Perfil
        JPanel topCard = new JPanel(new BorderLayout(15, 0));
        topCard.setBackground(Color.WHITE);
        topCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));
        topCard.putClientProperty(FlatClientProperties.STYLE, "arc: 14");

        JPanel selPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        selPanel.setOpaque(false);

        JLabel lblSelTitle = new JLabel("PERFIL WIREGUARD ACTIVO:");
        lblSelTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #334155");
        selPanel.add(lblSelTitle);

        cbPerfiles = new JComboBox<>();
        cbPerfiles.setPreferredSize(new Dimension(280, 36));
        cbPerfiles.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: 12");
        cbPerfiles.addActionListener(e -> actualizarDetallesPerfilSeleccionado());
        selPanel.add(cbPerfiles);

        JButton btnRecargar = new JButton("🔄 RECARGAR NUBE");
        btnRecargar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #F1F5F9");
        btnRecargar.addActionListener(e -> cargarPerfilesDesdeNube());
        selPanel.add(btnRecargar);

        topCard.add(selPanel, BorderLayout.WEST);

        // Banner de verificación del ejecutable
        boolean wgInstalado = WireguardService.isWireguardInstalado();
        lblStatusBadge = new JLabel(WireguardService.isTunnelActivo() ? "🟢 TÚNEL PROTEGIDO" : "🔴 DESCONECTADO");
        lblStatusBadge.putClientProperty(FlatClientProperties.STYLE, WireguardService.isTunnelActivo()
                ? "font: bold 12; foreground: #059669; background: #D1FAE5; border: 4,14,4,14, #D1FAE5, 1, 20"
                : "font: bold 12; foreground: #DC2626; background: #FEE2E2; border: 4,14,4,14, #FEE2E2, 1, 20");
        topCard.add(lblStatusBadge, BorderLayout.EAST);

        main.add(topCard, BorderLayout.NORTH);

        // Contenedor Central
        JPanel centerPanel = new JPanel(new BorderLayout(0, 16));
        centerPanel.setOpaque(false);

        // Banner si no tiene WireGuard instalado en Windows
        bannerWgNotFound = new JPanel(new BorderLayout(10, 0));
        bannerWgNotFound.setBackground(new Color(254, 243, 199));
        bannerWgNotFound.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11), 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        bannerWgNotFound.putClientProperty(FlatClientProperties.STYLE, "arc: 12");

        JLabel lblWarn = new JLabel("ℹ️ Para conectar directamente desde Windows, asegúrate de tener instalado WireGuard (wireguard.com).");
        lblWarn.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #92400E");
        bannerWgNotFound.add(lblWarn, BorderLayout.CENTER);

        JButton btnDescargarWg = new JButton("🌐 Descargar WireGuard");
        btnDescargarWg.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: bold 11; background: #D97706; foreground: #FFFFFF");
        btnDescargarWg.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI("https://www.wireguard.com/install/")); } catch (Exception ignored) {}
        });
        bannerWgNotFound.add(btnDescargarWg, BorderLayout.EAST);
        bannerWgNotFound.setVisible(!wgInstalado);
        centerPanel.add(bannerWgNotFound, BorderLayout.NORTH);

        // Tarjeta Principal del Túnel
        JPanel tunnelCard = new JPanel(new GridBagLayout());
        tunnelCard.setBackground(Color.WHITE);
        tunnelCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));
        tunnelCard.putClientProperty(FlatClientProperties.STYLE, "arc: 16");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int row = 0;

        // Botonazo de Activación
        btnToggleTunnel = new JButton("🚀 ACTIVAR TÚNEL WIREGUARD");
        btnToggleTunnel.setPreferredSize(new Dimension(360, 56));
        btnToggleTunnel.putClientProperty(FlatClientProperties.STYLE,
                "arc: 16; background: #0078D4; foreground: #FFFFFF; font: bold 15; margin: 12,24,12,24");
        btnToggleTunnel.addActionListener(e -> alternarEstadoTunel());
        gbc.insets = new Insets(0, 0, 24, 0); gbc.gridy = row++;
        tunnelCard.add(btnToggleTunnel, gbc);

        // Cuadrícula de Métricas
        JPanel metricsGrid = new JPanel(new GridLayout(3, 2, 16, 14));
        metricsGrid.setOpaque(false);

        lblEndpointVal = new JLabel("No especificado");
        lblAddressVal = new JLabel("10.10.10.2/32, fd42::2/128");
        lblDnsVal = new JLabel("1.1.1.1 (Protección contra fugas)");
        lblMtuVal = new JLabel("1450 (Optimizado)");
        lblUptimeVal = new JLabel("00:00:00");
        lblPingVal = new JLabel("Esperando conexión...");

        metricsGrid.add(crearCajaMetrica("📍 SERVIDOR ENDPOINT", lblEndpointVal, "#0284C7"));
        metricsGrid.add(crearCajaMetrica("🛡️ IP ASIGNADA (TÚNEL)", lblAddressVal, "#059669"));
        metricsGrid.add(crearCajaMetrica("🔒 SERVIDOR DNS", lblDnsVal, "#4F46E5"));
        metricsGrid.add(crearCajaMetrica("⚙️ PARÁMETROS MTU / KEEPALIVE", lblMtuVal, "#64748B"));
        metricsGrid.add(crearCajaMetrica("⏱️ TIEMPO EN LÍNEA", lblUptimeVal, "#D97706"));
        metricsGrid.add(crearCajaMetrica("📶 ESTADO / LATENCIA", lblPingVal, "#0D9488"));

        gbc.insets = new Insets(0, 0, 10, 0); gbc.gridy = row++;
        tunnelCard.add(metricsGrid, gbc);

        centerPanel.add(tunnelCard, BorderLayout.CENTER);
        main.add(centerPanel, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scroll;
    }

    private JPanel crearCajaMetrica(String titulo, JLabel valorLbl, String colorHex) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(new Color(248, 250, 252));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(241, 245, 249), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        p.putClientProperty(FlatClientProperties.STYLE, "arc: 10");

        JLabel lblT = new JLabel(titulo);
        lblT.putClientProperty(FlatClientProperties.STYLE, "font: bold 10; foreground: " + colorHex);
        p.add(lblT, BorderLayout.NORTH);

        valorLbl.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #1E293B");
        p.add(valorLbl, BorderLayout.CENTER);
        return p;
    }

    // --- PESTAÑA 2: GESTOR DE PERFILES EN FIREBASE ---

    private JScrollPane crearPanelPerfiles() {
        ResponsiveScrollablePanel main = new ResponsiveScrollablePanel(new BorderLayout(0, 18));
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Formulario de Creación / Importación
        JPanel formCard = new JPanel(new BorderLayout(0, 12));
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)
        ));
        formCard.putClientProperty(FlatClientProperties.STYLE, "arc: 14");

        JPanel headerForm = new JPanel(new BorderLayout(10, 0));
        headerForm.setOpaque(false);

        JLabel lblFormTitle = new JLabel("CONFIGURACIÓN WIREGUARD (GUARDADO CIFRADO EN FIREBASE)");
        lblFormTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; foreground: #0078D4");
        headerForm.add(lblFormTitle, BorderLayout.WEST);

        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolsPanel.setOpaque(false);

        JButton btnImportar = new JButton("📂 IMPORTAR .CONF");
        btnImportar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #F1F5F9");
        btnImportar.addActionListener(e -> importarArchivoConf());
        toolsPanel.add(btnImportar);

        JButton btnPegarPlantilla = new JButton("📋 RESTAURAR PLANTILLA");
        btnPegarPlantilla.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #F1F5F9");
        btnPegarPlantilla.addActionListener(e -> cargarPlantillaDefault());
        toolsPanel.add(btnPegarPlantilla);

        headerForm.add(toolsPanel, BorderLayout.EAST);
        formCard.add(headerForm, BorderLayout.NORTH);

        JPanel inputsPanel = new JPanel(new BorderLayout(0, 8));
        inputsPanel.setOpaque(false);

        JPanel nameRow = new JPanel(new BorderLayout(10, 0));
        nameRow.setOpaque(false);
        JLabel lblName = new JLabel("NOMBRE DEL SERVIDOR / PERFIL:");
        lblName.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #475569");
        nameRow.add(lblName, BorderLayout.WEST);

        txtNombrePerfil = new JTextField("Mi Servidor WireGuard");
        txtNombrePerfil.putClientProperty(FlatClientProperties.STYLE, "font: bold 13; background: #F8FAFC; border: 6,10,6,10");
        nameRow.add(txtNombrePerfil, BorderLayout.CENTER);
        inputsPanel.add(nameRow, BorderLayout.NORTH);

        txtConfigRaw = new JTextArea(12, 50);
        txtConfigRaw.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtConfigRaw.putClientProperty(FlatClientProperties.STYLE, "background: #F8FAFC; border: 8,10,8,10");
        cargarPlantillaDefault();

        JScrollPane scrollArea = new JScrollPane(txtConfigRaw);
        scrollArea.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        inputsPanel.add(scrollArea, BorderLayout.CENTER);

        JButton btnGuardar = new JButton("💾 CIFRAR CON RSA-4096 Y GUARDAR EN FIREBASE CLOUD");
        btnGuardar.setPreferredSize(new Dimension(320, 44));
        btnGuardar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #0078D4; foreground: #FFFFFF; font: bold 13; margin: 8,16,8,16");
        btnGuardar.addActionListener(e -> guardarPerfilEnNube());
        inputsPanel.add(btnGuardar, BorderLayout.SOUTH);

        formCard.add(inputsPanel, BorderLayout.CENTER);
        main.add(formCard, BorderLayout.NORTH);

        // Tabla de Perfiles en la Nube
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
        tableCard.putClientProperty(FlatClientProperties.STYLE, "arc: 14");

        JLabel lblTblTitle = new JLabel("PERFILES REGISTRADOS EN SU CUENTA (NUBE CIFRADA)");
        lblTblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #334155");
        tableCard.add(lblTblTitle, BorderLayout.NORTH);

        String[] cols = {"NOMBRE", "ENDPOINT", "IP ASIGNADA", "REGISTRO"};
        modelPerfiles = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblPerfiles = new JTable(modelPerfiles);
        tblPerfiles.getTableHeader().setReorderingAllowed(false);
        tblPerfiles.setRowHeight(32);
        tblPerfiles.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; selectionBackground: #E0E7FF");

        JScrollPane scrollTable = new JScrollPane(tblPerfiles);
        scrollTable.setPreferredSize(new Dimension(800, 160));
        scrollTable.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tableCard.add(scrollTable, BorderLayout.CENTER);

        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        tableActions.setOpaque(false);

        JButton btnCargarEnEditor = new JButton("👁️ CARGAR EN EDITOR");
        btnCargarEnEditor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #F1F5F9");
        btnCargarEnEditor.addActionListener(e -> cargarSeleccionadoEnEditor());
        tableActions.add(btnCargarEnEditor);

        JButton btnEliminar = new JButton("🗑️ ELIMINAR DE FIREBASE");
        btnEliminar.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #FEE2E2; foreground: #DC2626");
        btnEliminar.addActionListener(e -> eliminarPerfilSeleccionado());
        tableActions.add(btnEliminar);

        tableCard.add(tableActions, BorderLayout.SOUTH);
        main.add(tableCard, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scroll;
    }

    private void cargarPlantillaDefault() {
        txtConfigRaw.setText("[Interface]\n"
                + "Address = 10.10.10.2/32,fd42::2/128\n"
                + "PrivateKey = \n"
                + "DNS = 1.1.1.1\n"
                + "MTU = 1450\n\n"
                + "[Peer]\n"
                + "PublicKey = \n"
                + "PresharedKey = \n"
                + "AllowedIPs = 0.0.0.0/0,::/0\n"
                + "Endpoint = \n"
                + "PersistentKeepalive = 15\n");
    }

    // --- PESTAÑA 3: GENERADOR CURVE25519 NATIVO ---

    private JScrollPane crearPanelGeneradorLlaves() {
        ResponsiveScrollablePanel main = new ResponsiveScrollablePanel(new GridBagLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createEmptyBorder(25, 30, 25, 30)
        ));
        card.putClientProperty(FlatClientProperties.STYLE, "arc: 16");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        int row = 0;

        JLabel lblTitle = new JLabel("GENERADOR NATIVO DE LLAVES CURVE25519 (WIREGUARD)");
        lblTitle.putClientProperty(FlatClientProperties.STYLE, "font: bold 15; foreground: #0078D4");
        gbc.insets = new Insets(0, 0, 8, 0); gbc.gridy = row++;
        card.add(lblTitle, gbc);

        JLabel lblDesc = new JLabel("<html>Genera pares de claves criptográficas y PresharedKeys (PSK) para añadir nuevos clientes o servidores a tu red WireGuard.</html>");
        lblDesc.putClientProperty(FlatClientProperties.STYLE, "font: 12; foreground: #64748B");
        gbc.insets = new Insets(0, 0, 20, 0); gbc.gridy = row++;
        card.add(lblDesc, gbc);

        // Clave Privada
        card.add(crearFilaLlave("CLAVE PRIVADA (CLIENTE / INTERFACE):", txtGenPrivKey = new JTextField()), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);

        // Clave Pública
        card.add(crearFilaLlave("CLAVE PÚBLICA (REGISTRAR EN EL SERVIDOR PEER):", txtGenPubKey = new JTextField()), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 16, 0);

        // PresharedKey
        card.add(crearFilaLlave("PRESHARED KEY (PSK SIMÉTRICA OPCIONAL):", txtGenPsk = new JTextField()), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 24, 0);

        // Botón Generar
        JButton btnGenerar = new JButton("⚡ GENERAR NUEVO PAR CURVE25519");
        btnGenerar.setPreferredSize(new Dimension(320, 46));
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "arc: 12; background: #6366F1; foreground: #FFFFFF; font: bold 13");
        btnGenerar.addActionListener(e -> ejecutarGeneracionLlaves());
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(btnGenerar, gbc);

        JButton btnUsarEnEditor = new JButton("📥 Pegar Clave Privada en el Editor de Perfil");
        btnUsarEnEditor.putClientProperty(FlatClientProperties.STYLE, "arc: 10; font: bold 11; background: #EEF2FF; foreground: #4338CA");
        btnUsarEnEditor.addActionListener(e -> {
            String priv = txtGenPrivKey.getText().trim();
            if (priv.isEmpty()) {
                ToastNotification.show(this, "Primero genere las llaves.", ToastNotification.Type.WARNING);
                return;
            }
            String current = txtConfigRaw.getText();
            txtConfigRaw.setText(current.replaceFirst("PrivateKey\\s*=\\s*.*", "PrivateKey = " + priv));
            mainTabbedPane.setSelectedIndex(1);
            ToastNotification.show(this, "Clave privada colocada en el editor.", ToastNotification.Type.SUCCESS);
        });
        gbc.gridy = row++;
        card.add(btnUsarEnEditor, gbc);

        GridBagConstraints gbcMain = new GridBagConstraints();
        gbcMain.gridx = 0; gbcMain.gridy = 0; gbcMain.weightx = 1.0; gbcMain.fill = GridBagConstraints.HORIZONTAL;
        main.add(card, gbcMain);

        JScrollPane scroll = new JScrollPane(main);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel crearFilaLlave(String titulo, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setOpaque(false);

        JLabel lbl = new JLabel(titulo);
        lbl.putClientProperty(FlatClientProperties.STYLE, "font: bold 11; foreground: #334155");
        p.add(lbl, BorderLayout.NORTH);

        field.setEditable(false);
        field.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; background: #F8FAFC; border: 8,10,8,10");
        p.add(field, BorderLayout.CENTER);

        JButton btnCopiar = new JButton("📋 COPIAR");
        btnCopiar.putClientProperty(FlatClientProperties.STYLE, "arc: 8; font: bold 11");
        btnCopiar.addActionListener(e -> {
            String text = field.getText().trim();
            if (!text.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                ToastNotification.show(this, "Copiado al portapapeles.", ToastNotification.Type.SUCCESS);
            }
        });
        p.add(btnCopiar, BorderLayout.EAST);
        return p;
    }

    private void ejecutarGeneracionLlaves() {
        try {
            KeyPairWireguard pair = WireguardService.generarParLlaves();
            txtGenPrivKey.setText(pair.privateKey);
            txtGenPubKey.setText(pair.publicKey);
            txtGenPsk.setText(pair.presharedKey);
            ToastNotification.show(this, "Par Curve25519 generado en memoria.", ToastNotification.Type.SUCCESS);
        } catch (Exception ex) {
            ToastNotification.show(this, "Error generando llaves: " + ex.getMessage(), ToastNotification.Type.ERROR);
        }
    }

    // --- ACCIONES Y SINCRONIZACIÓN FIREBASE ---

    private void cargarPerfilesDesdeNube() {
        SwingWorker<List<WireguardProfile>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<WireguardProfile> doInBackground() {
                return WireguardService.cargarPerfilesCloud(userEmail);
            }

            @Override
            protected void done() {
                try {
                    perfilesCargados = get();
                    cbPerfiles.removeAllItems();
                    modelPerfiles.setRowCount(0);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                    for (WireguardProfile p : perfilesCargados) {
                        cbPerfiles.addItem(p);
                        modelPerfiles.addRow(new Object[]{
                            p.name,
                            p.endpoint != null ? p.endpoint : "N/A",
                            p.address != null ? p.address : "N/A",
                            sdf.format(new Date(p.timestamp))
                        });
                    }

                    if (cbPerfiles.getItemCount() > 0) {
                        cbPerfiles.setSelectedIndex(0);
                    }
                    actualizarDetallesPerfilSeleccionado();
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void guardarPerfilEnNube() {
        String name = txtNombrePerfil.getText().trim();
        String raw = txtConfigRaw.getText().trim();
        if (name.isEmpty() || raw.isEmpty()) {
            ToastNotification.show(this, "Debe ingresar el nombre del perfil y su configuración.", ToastNotification.Type.WARNING);
            return;
        }

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return WireguardService.guardarPerfilCloud(userEmail, name, raw);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        ToastNotification.show(VpnWireguard.this, "Perfil cifrado y sincronizado en Firebase Cloud.", ToastNotification.Type.SUCCESS);
                        AuditService.log(userEmail, "WG_PROFILE_SAVED", "Perfil WireGuard (" + name + ") guardado en Firebase.");
                        cargarPerfilesDesdeNube();
                    } else {
                        ToastNotification.show(VpnWireguard.this, "Error al guardar perfil en la nube.", ToastNotification.Type.ERROR);
                    }
                } catch (Exception ex) {
                    ToastNotification.show(VpnWireguard.this, "Error: " + ex.getMessage(), ToastNotification.Type.ERROR);
                }
            }
        };
        worker.execute();
    }

    private void eliminarPerfilSeleccionado() {
        int row = tblPerfiles.getSelectedRow();
        if (row == -1 || perfilesCargados == null || row >= perfilesCargados.size()) {
            ToastNotification.show(this, "Seleccione un perfil de la tabla para eliminar.", ToastNotification.Type.WARNING);
            return;
        }

        WireguardProfile sel = perfilesCargados.get(row);
        int opt = JOptionPane.showConfirmDialog(this, "¿Desea eliminar el perfil '" + sel.name + "' de Firebase Cloud?", "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return WireguardService.eliminarPerfilCloud(userEmail, sel.id);
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        ToastNotification.show(VpnWireguard.this, "Perfil eliminado de Firebase.", ToastNotification.Type.SUCCESS);
                        AuditService.log(userEmail, "WG_PROFILE_DELETED", "Perfil WireGuard (" + sel.name + ") eliminado de Firebase.");
                        cargarPerfilesDesdeNube();
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private void cargarSeleccionadoEnEditor() {
        int row = tblPerfiles.getSelectedRow();
        if (row != -1 && perfilesCargados != null && row < perfilesCargados.size()) {
            WireguardProfile sel = perfilesCargados.get(row);
            txtNombrePerfil.setText(sel.name);
            txtConfigRaw.setText(sel.rawConfig);
            ToastNotification.show(this, "Perfil cargado en el editor superior.", ToastNotification.Type.INFO);
        }
    }

    private void importarArchivoConf() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar archivo WireGuard (.conf)");
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                String content = Files.readString(f.toPath());
                txtConfigRaw.setText(content);
                String baseName = f.getName().replaceFirst("\\.[^.]+$", "");
                txtNombrePerfil.setText(baseName);
                ToastNotification.show(this, "Archivo importado: " + f.getName(), ToastNotification.Type.SUCCESS);
            } catch (Exception ex) {
                ToastNotification.show(this, "Error leyendo archivo: " + ex.getMessage(), ToastNotification.Type.ERROR);
            }
        }
    }

    private void actualizarDetallesPerfilSeleccionado() {
        WireguardProfile p = (WireguardProfile) cbPerfiles.getSelectedItem();
        if (p != null) {
            lblEndpointVal.setText(p.endpoint != null ? p.endpoint : "No especificado");
            lblAddressVal.setText(p.address != null ? p.address : "No especificada");
            lblDnsVal.setText(p.dns != null ? p.dns : "1.1.1.1");
            lblMtuVal.setText((p.mtu != null ? p.mtu : "Automático") + " / " + (p.persistentKeepalive != null ? p.persistentKeepalive + "s" : "Desactivado"));
        } else {
            lblEndpointVal.setText("Sin perfil seleccionado");
            lblAddressVal.setText("10.10.10.2/32, fd42::2/128");
            lblDnsVal.setText("1.1.1.1");
            lblMtuVal.setText("1450 / 15s");
        }
    }

    // --- ACTIVACIÓN Y CONTROL DEL TÚNEL ---

    private void alternarEstadoTunel() {
        if (WireguardService.isTunnelActivo()) {
            btnToggleTunnel.setEnabled(false);
            btnToggleTunnel.setText("⏳ DESCONECTANDO TÚNEL...");

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    return WireguardService.desactivarTunel();
                }

                @Override
                protected void done() {
                    btnToggleTunnel.setEnabled(true);
                    actualizarVisualesEstado(false);
                    ToastNotification.show(VpnWireguard.this, "Túnel WireGuard desconectado.", ToastNotification.Type.INFO);
                    AuditService.log(userEmail, "WG_TUNNEL_DISCONNECTED", "Túnel WireGuard desactivado.");
                }
            };
            worker.execute();
        } else {
            WireguardProfile p = (WireguardProfile) cbPerfiles.getSelectedItem();
            if (p == null) {
                String raw = txtConfigRaw.getText().trim();
                String name = txtNombrePerfil.getText().trim();
                if (!raw.isEmpty() && raw.contains("[Interface]")) {
                    p = WireguardService.parseConfig(raw, name.isEmpty() ? "Perfil Directo" : name);
                } else {
                    ToastNotification.show(this, "Debe seleccionar un perfil guardado en Firebase o configurarlo en la pestaña 'Perfiles'.", ToastNotification.Type.WARNING);
                    return;
                }
            }

            if (p.privateKey == null || p.privateKey.trim().isEmpty()) {
                ToastNotification.show(this, "El perfil seleccionado no tiene una Clave Privada (PrivateKey). Vaya a la pestaña 'Perfiles' para configurarla.", ToastNotification.Type.WARNING);
                return;
            }

            if (p.endpoint == null || p.endpoint.trim().isEmpty()) {
                ToastNotification.show(this, "El perfil seleccionado no tiene un servidor (Endpoint) configurado. Vaya a la pestaña 'Perfiles'.", ToastNotification.Type.WARNING);
                return;
            }

            if (!WireguardService.isWireguardInstalado()) {
                JOptionPane.showMessageDialog(this,
                        "El motor oficial de WireGuard para Windows no fue detectado en su equipo.\n"
                        + "Descárguelo gratis desde https://www.wireguard.com/install/ para activar el túnel en Windows,\n"
                        + "o utilice la configuración sincronizada en su teléfono Android.",
                        "WireGuard para Windows Requerido",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            final WireguardProfile perfilParaActivar = p;
            btnToggleTunnel.setEnabled(false);
            btnToggleTunnel.setText("⏳ ACTIVANDO TÚNEL (AUTORICE EN WINDOWS)...");

            SwingWorker<WireguardService.TunnelResult, Void> worker = new SwingWorker<>() {
                @Override
                protected WireguardService.TunnelResult doInBackground() {
                    return WireguardService.activarTunelConDetalle(perfilParaActivar);
                }

                @Override
                protected void done() {
                    btnToggleTunnel.setEnabled(true);
                    try {
                        WireguardService.TunnelResult res = get();
                        if (res != null && res.success) {
                            ToastNotification.show(VpnWireguard.this, "¡Túnel WireGuard conectado y protegido!", ToastNotification.Type.SUCCESS);
                            AuditService.log(userEmail, "WG_TUNNEL_CONNECTED", "Túnel conectado hacia: " + perfilParaActivar.endpoint);
                            actualizarVisualesEstado(true);
                        } else {
                            String msg = (res != null && res.errorMessage != null)
                                    ? res.errorMessage
                                    : "No se pudo activar el servicio de túnel. Verifique permisos de Administrador.";
                            ToastNotification.show(VpnWireguard.this, msg, ToastNotification.Type.ERROR);
                            actualizarVisualesEstado(false);
                        }
                    } catch (Exception ex) {
                        ToastNotification.show(VpnWireguard.this, "Error: " + ex.getMessage(), ToastNotification.Type.ERROR);
                        actualizarVisualesEstado(false);
                    }
                }
            };
            worker.execute();
        }
    }

    private void actualizarVisualesEstado(boolean activo) {
        if (activo) {
            lblStatusBadge.setText("🟢 TÚNEL PROTEGIDO");
            lblStatusBadge.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #059669; background: #D1FAE5; border: 4,14,4,14, #D1FAE5, 1, 20");
            btnToggleTunnel.setText("🛑 DESCONECTAR TÚNEL WIREGUARD");
            btnToggleTunnel.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #DC2626; foreground: #FFFFFF; font: bold 15; margin: 12,24,12,24");
        } else {
            lblStatusBadge.setText("🔴 DESCONECTADO");
            lblStatusBadge.putClientProperty(FlatClientProperties.STYLE, "font: bold 12; foreground: #DC2626; background: #FEE2E2; border: 4,14,4,14, #FEE2E2, 1, 20");
            btnToggleTunnel.setText("🚀 ACTIVAR TÚNEL WIREGUARD");
            btnToggleTunnel.putClientProperty(FlatClientProperties.STYLE, "arc: 16; background: #0078D4; foreground: #FFFFFF; font: bold 15; margin: 12,24,12,24");
            lblUptimeVal.setText("00:00:00");
            lblPingVal.setText("Desconectado");
        }
        lblStatusBadge.revalidate();
        lblStatusBadge.repaint();
        btnToggleTunnel.revalidate();
        btnToggleTunnel.repaint();
    }

    private void iniciarTemporizadorMonitor() {
        uptimeTimer = new Timer(1000, e -> {
            boolean activo = WireguardService.isTunnelActivo();
            if (activo) {
                long diff = System.currentTimeMillis() - WireguardService.getConnectedTimestamp();
                if (diff < 0) diff = 0;
                long secs = (diff / 1000) % 60;
                long mins = (diff / (1000 * 60)) % 60;
                long hours = (diff / (1000 * 60 * 60));
                lblUptimeVal.setText(String.format("%02d:%02d:%02d", hours, mins, secs));

                if (secs % 5 == 0) { // Ping cada 5 segundos
                    verificarLatenciaEnSegundoPlano();
                }
            }
        });
        uptimeTimer.start();
    }

    private void verificarLatenciaEnSegundoPlano() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                try {
                    long t1 = System.currentTimeMillis();
                    boolean reachable = InetAddress.getByName("1.1.1.1").isReachable(1500);
                    long t2 = System.currentTimeMillis();
                    if (reachable) {
                        return (t2 - t1) + " ms (Excelente)";
                    } else {
                        return "Túnel Activo";
                    }
                } catch (Exception ex) {
                    return "Túnel Activo";
                }
            }

            @Override
            protected void done() {
                try {
                    lblPingVal.setText(get());
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    @Override
    public void dispose() {
        if (uptimeTimer != null) uptimeTimer.stop();
        super.dispose();
    }

    /**
     * Panel adaptable responsivo para ScrollPane.
     */
    private static class ResponsiveScrollablePanel extends JPanel implements javax.swing.Scrollable {
        public ResponsiveScrollablePanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 64; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            Container parent = getParent();
            if (parent instanceof JViewport) {
                return parent.getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }
}
