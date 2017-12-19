package com.secureskytech.scdemosrv;

import java.awt.EventQueue;
import java.time.Clock;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.google.common.base.Throwables;
import com.secureskytech.scdemosrv.model.JdbcConnectionFactory;
import com.secureskytech.scdemosrv.swingui.MainWindow;

public class App {

    public static void main(String[] args) throws Exception {
        final AppContext appContext = new AppContext(Clock.systemDefaultZone(), new JdbcConnectionFactory());
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    MainWindow window = new MainWindow(appContext);
                    window.show();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        null,
                        Throwables.getStackTraceAsString(e),
                        "error:" + e.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        });

    }
}
