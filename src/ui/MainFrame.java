package ui;

import model.LSystem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Main application window — L-System Explorer.
 *
 * Layout (fits MacBook Pro with Dock visible):
 *
 *  ┌────────────────────────────────────────────────────────────┐
 *  │  Header                                                    │
 *  ├──────────────────────┬─────────────────────────────────────┤
 *  │  Grammar editor      │  Drawing canvas                     │
 *  │  (axiom, rules,      │  (auto-fit, zoom/pan, depth colour) │
 *  │   angle, step)       │                                     │
 *  ├──────────────────────┴─────────────────────────────────────┤
 *  │  Controls: preset picker, generation slider, draw/animate  │
 *  └────────────────────────────────────────────────────────────┘
 */
public class MainFrame extends JFrame {

    private LSystem      lsystem;
    private final DrawPanel    drawPanel    = new DrawPanel();
    private final GrammarPanel grammarPanel = new GrammarPanel();

    private final JComboBox<String> presetBox = new JComboBox<>(LSystem.presetNames());
    private final JSlider           genSlider = new JSlider(0, 8, 3);
    private final JLabel            genLabel  = label("Gen: 3");
    private final JLabel            statusLabel = label("Select preset or edit grammar");

    private final javax.swing.Timer animTimer = new javax.swing.Timer(16, null);
    private static final int ANIM_STEPS_PER_TICK = 80;

    public MainFrame() {
        setTitle("L-System Explorer — Lindenmayer Systems");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(Theme.BG_VOID);

        buildUI();
        loadPreset("Koch Snowflake");
        pack();
        setLocationRelativeTo(null);

        animTimer.addActionListener(e -> {
            boolean done = drawPanel.animStep(ANIM_STEPS_PER_TICK);
            if (done) {
                animTimer.stop();
                statusLabel.setText("Complete — " +
                    String.format("%,d segments", lsystem.getCommands().size()));
            }
        });
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(0,0));
        add(buildHeader(),   BorderLayout.NORTH);
        add(buildCentre(),   BorderLayout.CENTER);
        add(buildControls(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(new Color(0x030810));
        h.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0,0,1,0, Theme.BG_BORDER),
            new EmptyBorder(6,14,6,14)));

        JLabel title = new JLabel("L-SYSTEM EXPLORER");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_ACCENT);

        JLabel sub = new JLabel(
            "Lindenmayer Systems · Parallel string rewriting · " +
            "Turtle graphics interpretation");
        sub.setFont(new Font("Monospaced", Font.PLAIN, 9));
        sub.setForeground(Theme.TEXT_SECONDARY);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(title); left.add(sub);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(statusLabel);

        h.add(left,  BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JPanel buildCentre() {
        JPanel c = new JPanel(new BorderLayout(0,0));
        c.setBackground(Theme.BG_VOID);
        c.add(grammarPanel, BorderLayout.WEST);
        c.add(drawPanel,    BorderLayout.CENTER);
        return c;
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bar.setBackground(new Color(0x030810));
        bar.setBorder(BorderFactory.createMatteBorder(1,0,0,0, Theme.BG_BORDER));

        // Preset picker
        presetBox.setBackground(Theme.BG_CARD);
        presetBox.setForeground(Theme.TEXT_PRIMARY);
        presetBox.setFont(Theme.FONT_LABEL);
        presetBox.setPreferredSize(new Dimension(160, 24));
        presetBox.addActionListener(e -> {
            if (animTimer.isRunning()) animTimer.stop();
            loadPreset((String) presetBox.getSelectedItem());
        });

        // Generation slider
        genSlider.setOpaque(false);
        genSlider.setPreferredSize(new Dimension(160, 22));
        genSlider.setMajorTickSpacing(1);
        genSlider.setPaintTicks(true);
        genSlider.addChangeListener(e -> {
            genLabel.setText("Gen: " + genSlider.getValue());
        });

        // Buttons
        JButton drawBtn   = btn("⬛ Draw",    Theme.BTN_ACCENT);
        JButton animBtn   = btn("▶ Animate", Theme.BTN_ALT);
        JButton fitBtn    = btn("⊡ Fit",     Theme.TEXT_SECONDARY);

        drawBtn.addActionListener(e -> {
            animTimer.stop();
            rebuildAndDraw(false);
        });

        animBtn.addActionListener(e -> {
            animTimer.stop();
            rebuildAndDraw(true);
        });

        fitBtn.addActionListener(e -> drawPanel.resetView());

        // Colour key
        bar.add(label("Preset:"));  bar.add(presetBox);
        bar.add(label("Gen:"));     bar.add(genSlider); bar.add(genLabel);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        bar.add(drawBtn); bar.add(animBtn); bar.add(fitBtn);
        bar.add(new JSeparator(SwingConstants.VERTICAL));
        addKey(bar, "■ Trunk",    Theme.BRANCH_DEEP);
        addKey(bar, "■ Branch",   Theme.BRANCH_MID);
        addKey(bar, "■ Tip",      Theme.BRANCH_TIP);

        return bar;
    }

    private void addKey(JPanel bar, String text, Color col) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(col);
        bar.add(l);
    }

    // ── Logic ─────────────────────────────────────────────────────────────────
    private void loadPreset(String name) {
        lsystem = LSystem.preset(name);
        grammarPanel.loadFrom(lsystem);
        // Set a sensible default generation for the preset
        int defaultGen = switch (name) {
            case "Koch Snowflake", "Koch Island"      -> 4;
            case "Sierpinski Triangle"                 -> 6;
            case "Dragon Curve"                        -> 10;
            case "Hilbert Curve"                       -> 5;
            case "Simple Fern", "Barnsley Fern"        -> 5;
            case "Binary Tree", "Symmetric Tree"       -> 5;
            case "Bush", "Stochastic Plant"            -> 4;
            case "Gosper Curve"                        -> 4;
            default                                    -> 4;
        };
        genSlider.setValue(Math.min(defaultGen, genSlider.getMaximum()));
        rebuildAndDraw(false);
    }

    private void rebuildAndDraw(boolean animate) {
        // Parse grammar from editor
        lsystem = grammarPanel.buildLSystem();
        int gen = genSlider.getValue();
        genLabel.setText("Gen: " + gen);

        statusLabel.setText("Expanding...");
        SwingWorker<Void,Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() {
                lsystem.expand(gen);
                return null;
            }
            @Override protected void done() {
                drawPanel.setLSystem(lsystem);
                if (animate) {
                    drawPanel.startAnimation();
                    animTimer.start();
                    statusLabel.setText("Animating...");
                } else {
                    statusLabel.setText(String.format(
                        "Gen %d  ·  %,d segments  ·  string length %,d",
                        gen,
                        lsystem.getCommands().size(),
                        lsystem.getStringLength()));
                }
            }
        };
        worker.execute();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(Theme.FONT_LABEL);
        l.setForeground(Theme.TEXT_SECONDARY);
        return l;
    }

    private static JButton btn(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_LABEL);
        b.setForeground(Theme.TEXT_SECONDARY);
        b.setBackground(Theme.withAlpha(accent, 35));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.withAlpha(accent, 90), 1),
            new EmptyBorder(3,8,3,8)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBackground(Theme.withAlpha(accent, 70));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBackground(Theme.withAlpha(accent, 35));
            }
        });
        return b;
    }

    // Expose branch colours for use in Theme
    private static final Color BRANCH_DEEP = Theme.BRANCH_DEEP;
    private static final Color BRANCH_MID  = Theme.BRANCH_MID;
    private static final Color BRANCH_TIP  = Theme.BRANCH_TIP;
    private static final Color BTN_ACCENT  = Theme.BTN_ACCENT;
    private static final Color BTN_ALT     = Theme.BTN_ALT;
}
