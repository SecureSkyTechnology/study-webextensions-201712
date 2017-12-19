package com.secureskytech.scdemosrv.swingui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.secureskytech.scdemosrv.AppContext;
import com.secureskytech.scdemosrv.GUIConfig;
import com.secureskytech.scdemosrv.VersionInfo;
import com.secureskytech.scdemosrv.model.JdbcConnectionFactory;
import com.secureskytech.scdemosrv.proxy.LFSMapEntry;
import com.secureskytech.scdemosrv.proxy.LFSMapper;
import com.secureskytech.scdemosrv.proxy.MyHttpProxyServer;
import com.secureskytech.scdemosrv.springmvc.WebUIApp;

import net.miginfocom.swing.MigLayout;

public class MainPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(MainPanel.class);

    private List<LFSMapEntry> mapEntries = new ArrayList<>();
    private MyHttpProxyServer server;
    private WebUIApp webUIApp;

    private JButton btnAdd;
    private JButton btnUp;
    private JButton btnEdit;
    private JButton btnDown;
    private JButton btnDelete;
    private JButton btnStartProxy;
    private JButton btnStopProxy;
    private JButton btnStartWebUI;
    private JButton btnStopWebUI;
    private JTable tblLFSMapping;
    private JSpinner spnProxyPort;
    private JSpinner spnWebUIPort;
    private JTextArea txtaLog;
    private JTextArea txtaVersions;
    private JTextArea txtaTargetHost;
    private JTextArea txtaExcludeFilenameExtension;

    /**
     * Create the panel.
     * @throws IOException 
     */
    public MainPanel(final AppContext appContext) throws IOException {
        setLayout(new BorderLayout(0, 0));

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        add(tabbedPane, BorderLayout.CENTER);

        JPanel panelProxyControl = new JPanel();
        tabbedPane.addTab("Proxy Control", null, panelProxyControl, null);
        panelProxyControl.setLayout(
            new MigLayout(
                "",
                "[100px:n,fill][100px:n,grow,fill][100px:n,fill][100px:n,fill]",
                "[][][][][][][10px:n][][100px:n][100px:n][10px:n][][10px:n][]"));

        btnAdd = new JButton("add");
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                IMapEntryEditorNotifier notifier = new MapEntryEditorNotifierImpl(MainPanel.this);
                MapEntryEditDialog dlg =
                    new MapEntryEditDialog(MainPanel.this.getWindowFrame(), "add mapping", notifier);
                dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dlg.setVisible(true);
            }
        });

        JLabel lblURLMap2LFSConf = new JLabel("URL Map to Local file system Configuration:");
        panelProxyControl.add(lblURLMap2LFSConf, "cell 0 0 3 1");
        panelProxyControl.add(btnAdd, "cell 0 1");

        JScrollPane scrollPaneLFSMapping = new JScrollPane();
        panelProxyControl.add(scrollPaneLFSMapping, "cell 1 1 3 5,grow");

        tblLFSMapping = new JTable();
        tblLFSMapping.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLFSMapping.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblLFSMapping.setModel(
            new DefaultTableModel(
                new Object[][] {},
                new String[] {
                    "hostname",
                    "path prefix (terminate with /)",
                    "local path",
                    "\"/\" handling",
                    "targe filename extensions",
                    "text charset" }) {
                private static final long serialVersionUID = 1L;

                @SuppressWarnings("rawtypes")
                Class[] columnTypes =
                    new Class[] { String.class, String.class, String.class, String.class, String.class, String.class };

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                public Class getColumnClass(int columnIndex) {
                    return columnTypes[columnIndex];
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    // disable all cell editing feature.
                    return false;
                }
            });
        tblLFSMapping.getColumnModel().getColumn(1).setPreferredWidth(200);
        tblLFSMapping.getColumnModel().getColumn(2).setPreferredWidth(200);
        tblLFSMapping.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblLFSMapping.getColumnModel().getColumn(4).setPreferredWidth(150);
        tblLFSMapping.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblLFSMapping.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                if (me.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(me)) {
                    MainPanel.this.onEditButtonClicked();
                }
            }
        });
        scrollPaneLFSMapping.setViewportView(tblLFSMapping);

        btnUp = new JButton("up");
        btnUp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = tblLFSMapping.getSelectedRow();
                if (0 > selectedIndex) {
                    return;
                }
                final int newSelectedIndex = selectedIndex - 1;
                if (0 > newSelectedIndex) {
                    return;
                }

                LFSMapEntry m = mapEntries.get(selectedIndex);
                mapEntries.set(selectedIndex, mapEntries.get(newSelectedIndex));
                mapEntries.set(newSelectedIndex, m);

                DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
                model.moveRow(selectedIndex, selectedIndex, newSelectedIndex);
                tblLFSMapping.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
            }
        });
        panelProxyControl.add(btnUp, "cell 0 2");

        btnEdit = new JButton("edit");
        btnEdit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MainPanel.this.onEditButtonClicked();
            }
        });
        panelProxyControl.add(btnEdit, "cell 0 3");

        btnDown = new JButton("down");
        btnDown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = tblLFSMapping.getSelectedRow();
                if (0 > selectedIndex) {
                    return;
                }
                final int newSelectedIndex = selectedIndex + 1;
                if (newSelectedIndex > (tblLFSMapping.getRowCount() - 1)) {
                    return;
                }

                LFSMapEntry m = mapEntries.get(selectedIndex);
                mapEntries.set(selectedIndex, mapEntries.get(newSelectedIndex));
                mapEntries.set(newSelectedIndex, m);

                DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
                model.moveRow(selectedIndex, selectedIndex, newSelectedIndex);
                tblLFSMapping.setRowSelectionInterval(newSelectedIndex, newSelectedIndex);
            }
        });
        panelProxyControl.add(btnDown, "cell 0 4");

        btnDelete = new JButton("delete");
        btnDelete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final int selectedIndex = tblLFSMapping.getSelectedRow();
                if (0 > selectedIndex) {
                    return;
                }

                mapEntries.remove(selectedIndex);

                DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
                model.removeRow(selectedIndex);
                tblLFSMapping.clearSelection();
            }
        });
        panelProxyControl.add(btnDelete, "cell 0 5");

        JSeparator separator_1 = new JSeparator();
        panelProxyControl.add(separator_1, "cell 0 6 4 1");

        JLabel lblLogTargetConf = new JLabel("Log target configuration:");
        panelProxyControl.add(lblLogTargetConf, "cell 0 7 3 1");

        JLabel lblTargetHost = new JLabel("target host (*:wildcard):");
        lblTargetHost.setHorizontalAlignment(SwingConstants.RIGHT);
        panelProxyControl.add(lblTargetHost, "cell 0 8,aligny top");

        JScrollPane scrollPaneTargetHost = new JScrollPane();
        panelProxyControl.add(scrollPaneTargetHost, "cell 1 8 3 1,grow");

        txtaTargetHost = new JTextArea();
        scrollPaneTargetHost.setViewportView(txtaTargetHost);
        txtaTargetHost.setToolTipText(
            "explicitly set logging target hostname one per line. (* = wildcard, ex: *.localhost => abc.localhost, abc.def.localhost, ... will be logged.)");

        JLabel lblExcludeFilenameExtension = new JLabel("exclude filename extension:");
        lblExcludeFilenameExtension.setHorizontalAlignment(SwingConstants.RIGHT);
        panelProxyControl.add(lblExcludeFilenameExtension, "cell 0 9,aligny top");

        JScrollPane scrollPaneExcludeFilenameExtension = new JScrollPane();
        panelProxyControl.add(scrollPaneExcludeFilenameExtension, "cell 1 9 3 1,grow");

        txtaExcludeFilenameExtension = new JTextArea();
        scrollPaneExcludeFilenameExtension.setViewportView(txtaExcludeFilenameExtension);

        JSeparator separator_2 = new JSeparator();
        panelProxyControl.add(separator_2, "cell 0 10 4 1");

        JLabel lblProxyPort = new JLabel("proxy port :");
        lblProxyPort.setHorizontalAlignment(SwingConstants.RIGHT);
        panelProxyControl.add(lblProxyPort, "cell 0 11");

        spnProxyPort = new JSpinner();
        spnProxyPort.setValue(GUIConfig.DEFAULT_PROXY_LISTENING_PORT);
        panelProxyControl.add(spnProxyPort, "cell 1 11,growx");

        btnStartProxy = new JButton("start");
        btnStartProxy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveConfig();

                List<Pattern> targetHostNameRegexps = new ArrayList<>();
                for (String s : txtaTargetHost.getText().split("\n")) {
                    s = s.trim();
                    if (Strings.isNullOrEmpty(s)) {
                        continue;
                    }
                    String p = GUIConfig.convertWildcardToRegexp(s);
                    if (!GUIConfig.isValidRegexpPattern(p)) {
                        continue;
                    }
                    Pattern regexp = Pattern.compile(p);
                    targetHostNameRegexps.add(regexp);
                }

                List<String> excludeFilenameExtensions = new ArrayList<>();
                for (String s : txtaExcludeFilenameExtension.getText().split("\n")) {
                    s = s.trim();
                    if (Strings.isNullOrEmpty(s)) {
                        continue;
                    }
                    excludeFilenameExtensions.add(s);
                }

                LFSMapper lfsm = new LFSMapper();
                for (LFSMapEntry m : mapEntries) {
                    lfsm.addMap(m);
                }
                final int portnum = Integer.parseInt(spnProxyPort.getValue().toString());
                LOG.info("proxy starting at port:{} ...", portnum);
                server =
                    new MyHttpProxyServer(
                        appContext,
                        new InetSocketAddress("0.0.0.0", portnum),
                        lfsm,
                        targetHostNameRegexps,
                        excludeFilenameExtensions);
                server.start();
                LOG.info("proxy started at port:{}", portnum);
                btnAdd.setEnabled(false);
                btnUp.setEnabled(false);
                btnEdit.setEnabled(false);
                btnDown.setEnabled(false);
                btnDelete.setEnabled(false);
                btnStartProxy.setEnabled(false);
                btnStopProxy.setEnabled(true);
                JOptionPane.showMessageDialog(getWindowFrame(), "proxy started successfuly at port:" + portnum);
            }
        });
        panelProxyControl.add(btnStartProxy, "cell 2 11");

        btnStopProxy = new JButton("stop");
        btnStopProxy.setEnabled(false);
        btnStopProxy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (Objects.nonNull(server)) {
                    LOG.info("proxy stopping ...");
                    server.stop();
                    LOG.info("proxy stopped.");
                    btnAdd.setEnabled(true);
                    btnUp.setEnabled(true);
                    btnEdit.setEnabled(true);
                    btnDown.setEnabled(true);
                    btnDelete.setEnabled(true);
                    btnStartProxy.setEnabled(true);
                    btnStopProxy.setEnabled(false);
                    JOptionPane.showMessageDialog(getWindowFrame(), "proxy stopped.");
                }
            }
        });
        panelProxyControl.add(btnStopProxy, "cell 3 11");

        JSeparator separator = new JSeparator();
        panelProxyControl.add(separator, "cell 0 12 4 1");

        JLabel lblWebUIPort = new JLabel("webui port:");
        lblWebUIPort.setHorizontalAlignment(SwingConstants.RIGHT);
        panelProxyControl.add(lblWebUIPort, "cell 0 13");

        spnWebUIPort = new JSpinner();
        spnWebUIPort.setModel(new SpinnerNumberModel(new Integer(10089), null, null, new Integer(1)));
        panelProxyControl.add(spnWebUIPort, "cell 1 13");

        btnStartWebUI = new JButton("start");
        btnStartWebUI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveConfig();
                final int portnum = Integer.parseInt(spnWebUIPort.getValue().toString());
                LOG.info("webui starting at port:{} ...", portnum);
                try {
                    MainPanel.this.webUIApp.start(portnum);
                    LOG.info("webui started at port:{}", portnum);
                    btnStartWebUI.setEnabled(false);
                    btnStopWebUI.setEnabled(true);
                    JOptionPane.showMessageDialog(getWindowFrame(), "webui started successfuly at port:" + portnum);
                    Desktop desktop = Desktop.getDesktop();
                    try {
                        desktop.browse(new URI("http://127.0.0.1:" + portnum + "/"));
                    } catch (IOException ex) {
                        LOG.warn("webui native browser opening failure.");
                    }
                } catch (Exception ex) {
                    LOG.error("webui start failed.", ex);
                    JOptionPane.showMessageDialog(
                        MainPanel.this.getWindowFrame(),
                        ex.getMessage(),
                        "webui start failed.",
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        panelProxyControl.add(btnStartWebUI, "cell 2 13");

        btnStopWebUI = new JButton("stop");
        btnStopWebUI.setEnabled(false);
        btnStopWebUI.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                LOG.info("webui stopping ...");
                try {
                    MainPanel.this.webUIApp.stop();
                    LOG.info("webui stopped.");
                    btnStartWebUI.setEnabled(true);
                    btnStopWebUI.setEnabled(false);
                    JOptionPane.showMessageDialog(getWindowFrame(), "webui stopped.");
                } catch (Exception ex) {
                    LOG.error("webui stop failed.", ex);
                    JOptionPane.showMessageDialog(
                        MainPanel.this.getWindowFrame(),
                        ex.getMessage(),
                        "webui stop failed.",
                        JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        panelProxyControl.add(btnStopWebUI, "cell 3 13");

        JPanel panelLog = new JPanel();
        tabbedPane.addTab("Log", null, panelLog, null);
        panelLog.setLayout(new GridLayout(1, 0, 0, 0));

        JScrollPane scrollPaneLog = new JScrollPane();
        scrollPaneLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneLog.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelLog.add(scrollPaneLog);

        txtaLog = new JTextArea();
        txtaLog.setEditable(false);
        scrollPaneLog.setViewportView(txtaLog);

        JPanel panelVersions = new JPanel();
        tabbedPane.addTab("Version", null, panelVersions, null);
        panelVersions.setLayout(new GridLayout(1, 0, 0, 0));

        JScrollPane scrollPaneVersions = new JScrollPane();
        scrollPaneVersions.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPaneVersions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        panelVersions.add(scrollPaneVersions);

        txtaVersions = new JTextArea();
        txtaVersions.setEditable(false);
        scrollPaneVersions.setViewportView(txtaVersions);

        loadConfig();
        LogbackSwingTextareaAppender.addToRootLogger(txtaLog);
        // ロガー接続してからDBのmigrateを行う。
        JdbcConnectionFactory.migrate(appContext.getJdbcConnectionFactory());
        this.webUIApp = new WebUIApp(appContext);
    }

    public void loadConfig() {
        try {
            GUIConfig.initIfDefaultConfigFileNotExists();
            GUIConfig config = GUIConfig.load(GUIConfig.DEFAULT_CONFIG_FILE);
            spnProxyPort.setValue(config.getProxyPort());
            spnWebUIPort.setValue(config.getWebUIPort());
            mapEntries = config.convertToLFSMapEntries();
            for (LFSMapEntry m : mapEntries) {
                DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
                model.addRow(m.toJTableRow());
            }
            txtaTargetHost.setText(String.join("\n", config.getTargetHostNames()));
            txtaExcludeFilenameExtension.setText(String.join("\n", config.getExcludeFilenameExtensions()));
            LOG.info("configuration loaded successfully");
        } catch (IOException ex) {
            LOG.error("configuration load failed.", ex);
            JOptionPane.showMessageDialog(
                this.getWindowFrame(),
                ex.getMessage(),
                "configuration load failed.",
                JOptionPane.WARNING_MESSAGE);
        }
        StringBuilder versionInfo = new StringBuilder();
        versionInfo.append(VersionInfo.getManifestInfo());
        versionInfo.append("\n----------------------------\n");
        versionInfo.append(VersionInfo.getThirdPartyLicenses());
        versionInfo.append("\n----------------------------\n");
        versionInfo.append(VersionInfo.getSystemProperties());
        txtaVersions.setText(versionInfo.toString());
        txtaVersions.setCaretPosition(0);
    }

    public void saveConfig() {
        GUIConfig config = new GUIConfig();
        config.setProxyPort(Integer.parseInt(spnProxyPort.getValue().toString()));
        config.setWebUIPort(Integer.parseInt(spnWebUIPort.getValue().toString()));
        config.updateMapEntries(mapEntries);

        config.setTargetHostNames(new ArrayList<>());
        String[] targetHostNames = txtaTargetHost.getText().split("\n");
        for (String s : targetHostNames) {
            s = s.trim();
            if (Strings.isNullOrEmpty(s)) {
                continue;
            }
            String p = GUIConfig.convertWildcardToRegexp(s);
            if (!GUIConfig.isValidRegexpPattern(p)) {
                continue;
            }
            config.getTargetHostNames().add(s);
        }

        config.setExcludeFilenameExtensions(new ArrayList<>());
        String[] excludeFilenameExtensions = txtaExcludeFilenameExtension.getText().split("\n");
        for (String s : excludeFilenameExtensions) {
            s = s.trim();
            if (Strings.isNullOrEmpty(s)) {
                continue;
            }
            config.getExcludeFilenameExtensions().add(s);
        }

        try {
            config.save(GUIConfig.DEFAULT_CONFIG_FILE);
            LOG.info("configuration saved successfully");
        } catch (IOException ex) {
            LOG.error("configuration save failed.", ex);
            JOptionPane.showMessageDialog(
                this.getWindowFrame(),
                Throwables.getStackTraceAsString(ex),
                "configuration save failed  : " + ex.getMessage(),
                JOptionPane.WARNING_MESSAGE);
        }
    }

    public JFrame getWindowFrame() {
        return (JFrame) SwingUtilities.getWindowAncestor(this);
    }

    public void addLFSMapEntry(LFSMapEntry newEntry) {
        mapEntries.add(newEntry);
        DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
        model.addRow(newEntry.toJTableRow());
    }

    public void onEditButtonClicked() {
        final int selectedIndex = tblLFSMapping.getSelectedRow();
        if (0 > selectedIndex) {
            return;
        }
        LFSMapEntry m = mapEntries.get(selectedIndex);
        IMapEntryEditorNotifier notifier = new MapEntryEditorNotifierImpl(MainPanel.this, selectedIndex, m);

        MapEntryEditDialog dlg = new MapEntryEditDialog(MainPanel.this.getWindowFrame(), "edit mapping", notifier);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setVisible(true);
    }

    public void updateLFSMapEntry(LFSMapEntry newEntry, int replaceIndex) {
        mapEntries.set(replaceIndex, newEntry);
        DefaultTableModel model = (DefaultTableModel) tblLFSMapping.getModel();
        Object[] newColumns = newEntry.toJTableRow();
        for (int i = 0; i < newColumns.length; i++) {
            model.setValueAt(newColumns[i], replaceIndex, i);
        }
    }
}
