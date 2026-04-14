package ui;

import model.LSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable grammar panel showing axiom, rules, angle, and step.
 * Parses rules in the format:   F -> F+F-F-F+F
 * Multiple rules for the same symbol = stochastic (equal probability split).
 */
public class GrammarPanel extends JPanel {

    private final JTextField axiomField = monoField("F", 20);
    private final JTextArea  rulesArea  = new JTextArea(5, 22);
    private final JSpinner   angleSpinner;
    private final JSpinner   stepSpinner;

    public GrammarPanel() {
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BG_BORDER));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        angleSpinner = new JSpinner(new SpinnerNumberModel(90.0, 1.0, 180.0, 0.5));
        stepSpinner  = new JSpinner(new SpinnerNumberModel(10.0, 1.0, 100.0, 1.0));
        styleSpinner(angleSpinner);
        styleSpinner(stepSpinner);

        rulesArea.setBackground(Theme.BG_CARD);
        rulesArea.setForeground(Theme.TEXT_PRIMARY);
        rulesArea.setFont(Theme.FONT_MONO);
        rulesArea.setCaretColor(Theme.TEXT_ACCENT);
        rulesArea.setBorder(new EmptyBorder(4,4,4,4));
        rulesArea.setText("F -> F+F-F-F+F");

        JScrollPane rulesScroll = new JScrollPane(rulesArea);
        rulesScroll.setBorder(BorderFactory.createLineBorder(Theme.BG_BORDER));
        rulesScroll.setBackground(Theme.BG_CARD);
        rulesScroll.getViewport().setBackground(Theme.BG_CARD);

        add(sectionLabel("AXIOM"));
        add(padded(axiomField));

        add(sectionLabel("RULES  (symbol -> production)"));
        add(padded(rulesScroll));

        add(sectionLabel("ANGLE (°)"));
        add(padded(angleSpinner));

        add(sectionLabel("STEP LENGTH"));
        add(padded(stepSpinner));
    }

    /** Load values from an LSystem into the editor. */
    public void loadFrom(LSystem ls) {
        axiomField.setText(ls.getAxiom());
        angleSpinner.setValue(ls.getAngle());
        stepSpinner.setValue(ls.getStepLength());

        StringBuilder sb = new StringBuilder();
        for (LSystem.Rule r : ls.getRules()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(r.symbol).append(" -> ").append(r.production);
        }
        rulesArea.setText(sb.toString());
    }

    /** Parse editor contents into an LSystem (expand(0)). */
    public LSystem buildLSystem() {
        LSystem ls = new LSystem();
        ls.setAxiom(axiomField.getText().trim());
        ls.setAngle((Double) angleSpinner.getValue());
        ls.setStepLength((Double) stepSpinner.getValue());

        List<LSystem.Rule> rules = new ArrayList<>();
        for (String line : rulesArea.getText().split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            // Accept:  F -> production   or   F=production
            String[] parts = line.split("->|=", 2);
            if (parts.length == 2) {
                String sym  = parts[0].trim();
                String prod = parts[1].trim();
                if (!sym.isEmpty()) {
                    rules.add(new LSystem.Rule(sym.charAt(0), prod));
                }
            }
        }
        ls.setRules(rules);
        ls.expand(0);
        return ls;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_HEADER);
        l.setForeground(Theme.TEXT_LABEL);
        l.setBorder(new EmptyBorder(8,8,2,8));
        return l;
    }

    private JPanel padded(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0,8,4,8));
        p.add(c);
        return p;
    }

    private static JTextField monoField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setFont(Theme.FONT_MONO);
        f.setBackground(Theme.BG_CARD);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.TEXT_ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.BG_BORDER),
            new EmptyBorder(3,4,3,4)));
        return f;
    }

    private static void styleSpinner(JSpinner s) {
        s.setBackground(Theme.BG_CARD);
        JComponent editor = s.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(Theme.BG_CARD);
            de.getTextField().setForeground(Theme.TEXT_PRIMARY);
            de.getTextField().setFont(Theme.FONT_MONO);
            de.getTextField().setBorder(new EmptyBorder(2,4,2,4));
        }
    }
}
