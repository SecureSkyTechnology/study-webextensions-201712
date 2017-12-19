package com.secureskytech.scdemosrv.swingui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;

import com.secureskytech.scdemosrv.AppContext;

public class MainWindow {

    private JFrame frame;
    private MainPanel mainPanel;

    /**
     * Create the application.
     * @throws IOException 
     */
    public MainWindow(final AppContext appContext) throws IOException {
        initialize(appContext);
    }

    /**
     * デフォルトで生成された public static void main() の中で呼ばれていた
     * setVisible(true)を外部から可能とするために手作業で追加したpublicメソッド。
     */
    public void show() {
        frame.setVisible(true);
    }

    /**
     * Initialize the contents of the frame.
     * @throws IOException 
     */
    private void initialize(final AppContext appContext) throws IOException {
        frame = new JFrame();
        frame.setTitle("alter-proxy");
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(0, 0));
        mainPanel = new MainPanel(appContext);
        frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainPanel.saveConfig();
            }
        });
    }
}
