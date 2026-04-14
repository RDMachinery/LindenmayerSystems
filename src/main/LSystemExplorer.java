package main;

import javax.swing.SwingUtilities;
import ui.MainFrame;

/**
 * L-System Explorer — Lindenmayer Systems.
 * Interactive exploration of parallel string rewriting systems
 * and their turtle graphics interpretation.
 */
public class LSystemExplorer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
