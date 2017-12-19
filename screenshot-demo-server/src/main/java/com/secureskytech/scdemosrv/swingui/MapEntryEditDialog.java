package com.secureskytech.scdemosrv.swingui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.StringUtils;

import com.secureskytech.scdemosrv.proxy.LFSMapEntry;
import com.secureskytech.scdemosrv.proxy.LFSMapper.IndexHandlePolicy;

import net.miginfocom.swing.MigLayout;

public class MapEntryEditDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private static List<String> availableCharsetNames;

    static {
        Map<String, Charset> acs = Charset.availableCharsets();
        List<String> l = new ArrayList<>(acs.size());
        for (Map.Entry<String, Charset> e : acs.entrySet()) {
            l.add(e.getValue().name());
        }
        Collections.sort(l);
        // for usability adjustment in Japan locale
        int sz = l.size();
        List<String> prefs =
            Arrays.asList("UTF-8", "Shift_JIS", "windows-31j", "EUC-JP", "ISO-2022-JP", "ISO-8859-1", "US-ASCII");
        l.removeAll(prefs);
        List<String> l2 = new ArrayList<>(sz);
        l2.addAll(prefs);
        l2.addAll(l);
        availableCharsetNames = Collections.unmodifiableList(l2);
    }

    private final JPanel contentPanel = new JPanel();
    private JTextField txtTargetHost;
    private JTextField txtPathPrefix;
    private JTextField txtLocalDir;
    private JComboBox<IndexHandlePolicy> cmbIndexHandlePolicy;
    private JTextField txtTargetFilenameExteinsions;
    private JComboBox<String> cmbTextCharset;

    /**
     * Create the dialog.
     */
    public MapEntryEditDialog(JFrame owner, String title, IMapEntryEditorNotifier notifier) {
        super(owner, title, true);

        setBounds(100, 100, 450, 240);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new MigLayout("", "[][grow]", "[][][][][][]"));
        {
            JLabel lblTargetHost = new JLabel("target host :");
            lblTargetHost.setToolTipText("plain string match (can't use wildcard nor regexp)");
            contentPanel.add(lblTargetHost, "cell 0 0,alignx trailing");
        }
        {
            txtTargetHost = new JTextField();
            contentPanel.add(txtTargetHost, "cell 1 0,growx");
            txtTargetHost.setColumns(10);
        }
        {
            JLabel lblPathPrefix = new JLabel("path prefix :");
            lblPathPrefix.setToolTipText("must terminate by \"/\"");
            contentPanel.add(lblPathPrefix, "cell 0 1,alignx trailing");
        }
        {
            txtPathPrefix = new JTextField();
            contentPanel.add(txtPathPrefix, "cell 1 1,growx");
            txtPathPrefix.setColumns(10);
        }
        {
            JButton btnChooseLocalDir = new JButton("local directory");
            btnChooseLocalDir.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser filechooser = new JFileChooser();
                    filechooser.setDialogTitle("choose local direcotory (maps to " + txtPathPrefix.getText() + ")");
                    filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    filechooser.setCurrentDirectory(new File("."));
                    int selected = filechooser.showOpenDialog(MapEntryEditDialog.this.getOwner());
                    if (selected == JFileChooser.APPROVE_OPTION) {
                        File file = filechooser.getSelectedFile();
                        txtLocalDir.setText(file.getAbsolutePath());
                    }
                }
            });
            contentPanel.add(btnChooseLocalDir, "cell 0 2");
        }
        {
            txtLocalDir = new JTextField();
            txtLocalDir.setEditable(false);
            contentPanel.add(txtLocalDir, "cell 1 2,growx");
            txtLocalDir.setColumns(10);
        }
        {
            JLabel lblIndexHandle = new JLabel("\"/\" handling :");
            contentPanel.add(lblIndexHandle, "cell 0 3,alignx trailing");
        }
        {
            cmbIndexHandlePolicy = new JComboBox<>();
            cmbIndexHandlePolicy.setModel(new DefaultComboBoxModel<IndexHandlePolicy>(IndexHandlePolicy.values()));
            contentPanel.add(cmbIndexHandlePolicy, "cell 1 3,growx");
        }
        {
            JLabel lblTargetFilenameExtensions = new JLabel("filename extensions :");
            lblTargetFilenameExtensions.setToolTipText("target filename extensions");
            contentPanel.add(lblTargetFilenameExtensions, "cell 0 4,alignx trailing");
        }
        {
            txtTargetFilenameExteinsions = new JTextField();
            txtTargetFilenameExteinsions
                .setToolTipText("extension list separated by \",\" or \" \"(0x20). (\".\" is not needed)");
            txtTargetFilenameExteinsions.setText("html, js, css, json, xml");
            contentPanel.add(txtTargetFilenameExteinsions, "cell 1 4,growx");
            txtTargetFilenameExteinsions.setColumns(10);
        }
        {
            JLabel lblTextCharset = new JLabel("text charset :");
            contentPanel.add(lblTextCharset, "cell 0 5,alignx trailing");
        }
        {
            cmbTextCharset = new JComboBox<>();
            cmbTextCharset.setToolTipText("select charset when \"text/*\" or \"application/javascript\" response");
            for (String cn : availableCharsetNames) {
                cmbTextCharset.addItem(cn);
            }
            contentPanel.add(cmbTextCharset, "cell 1 5,growx");
        }
        {
            JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
            getContentPane().add(buttonPane, BorderLayout.SOUTH);
            {
                JButton okButton = new JButton("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        final String targetHost = txtTargetHost.getText().trim();
                        if (StringUtils.isBlank(targetHost)) {
                            showInputValidationError("Input target hostname.");
                            return;
                        }
                        final String path = txtPathPrefix.getText().trim();
                        if (StringUtils.isBlank(path)) {
                            showInputValidationError("Input path prefix.");
                            return;
                        }
                        if (path.indexOf("/") != 0) {
                            showInputValidationError("insert \"/\" to head of path prefix.");
                            return;
                        }
                        if (path.lastIndexOf("/") != path.length() - 1) {
                            showInputValidationError("add last \"/\" to path prefix.");
                            return;
                        }
                        final File localDir = new File(txtLocalDir.getText().trim());
                        if (!localDir.exists()) {
                            showInputValidationError("selected local directory does NOT exists.");
                            return;
                        }
                        if (!localDir.isDirectory()) {
                            showInputValidationError("selected local directory is NOT directory.");
                            return;
                        }
                        final IndexHandlePolicy policy = (IndexHandlePolicy) cmbIndexHandlePolicy.getSelectedItem();
                        String[] extensionarr =
                            txtTargetFilenameExteinsions.getText().replace(" ", "").replace(".", "").split(",");
                        if (extensionarr.length == 1 && StringUtils.isBlank(extensionarr[0])) {
                            showInputValidationError("set 1 or more filename extensions.");
                            return;
                        }
                        Set<String> extensions = new TreeSet<>(Arrays.asList(extensionarr));
                        Charset txtCharset = null;
                        try {
                            txtCharset = Charset.forName((String) cmbTextCharset.getSelectedItem());
                        } catch (Exception charsetEx) {
                            showInputValidationError("text charset selection error : " + charsetEx.getMessage());
                            return;
                        }
                        LFSMapEntry mapentry =
                            new LFSMapEntry(
                                targetHost,
                                path,
                                localDir.getAbsolutePath(),
                                policy,
                                extensions,
                                txtCharset);
                        notifier.notifyNewEntry(mapentry);
                        dispose();
                    }
                });
                okButton.setActionCommand("OK");
                buttonPane.add(okButton);
                getRootPane().setDefaultButton(okButton);
            }
            {
                JButton cancelButton = new JButton("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dispose();
                    }
                });
                cancelButton.setActionCommand("Cancel");
                buttonPane.add(cancelButton);
            }
        }

        LFSMapEntry src = notifier.getSourceEntry();
        if (Objects.nonNull(src)) {
            txtTargetHost.setText(src.getHostHeader());
            txtPathPrefix.setText(src.getPath());
            txtLocalDir.setText(src.getLocalDir().getAbsolutePath());
            cmbIndexHandlePolicy.setSelectedItem(src.getIndexHandlePolicy());
            txtTargetFilenameExteinsions.setText(String.join(",", src.getMappedExtensions()));
            cmbTextCharset.setSelectedItem(src.getTextCharset().name());
        }
    }

    public void showInputValidationError(String message) {
        JOptionPane.showMessageDialog(this.getOwner(), message, "Input Validation Error", JOptionPane.WARNING_MESSAGE);
    }
}
