package ui;

import model.LSystem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The main drawing canvas.
 *
 * Features:
 *   - Auto-fits the L-system to the panel on every rebuild
 *   - Animated reveal: draws commands one by one when animating
 *   - Mouse wheel zoom, click-drag pan
 *   - Depth-coloured lines (amber trunk → green branches → bright tips)
 *   - Line width proportional to branch depth
 *   - Glow effect on bright tip segments
 */
public class DrawPanel extends JPanel {

    private LSystem lsystem;

    // Viewport transform
    private double offsetX = 0, offsetY = 0;
    private double scale   = 1.0;
    private boolean autoFit = true;

    // Pan state
    private int dragStartX, dragStartY;
    private double dragStartOffX, dragStartOffY;

    // Animation
    private int    animIndex    = -1;   // -1 = draw all
    private boolean animating   = false;

    // Cached image for non-animating render
    private BufferedImage cache;
    private boolean        cacheDirty = true;

    public DrawPanel() {
        setBackground(Theme.BG_VOID);
        setPreferredSize(new Dimension(640, 560));

        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.15 : 0.87;
            scale *= factor;
            autoFit = false;
            cacheDirty = true;
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragStartX = e.getX(); dragStartY = e.getY();
                dragStartOffX = offsetX; dragStartOffY = offsetY;
                autoFit = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                offsetX = dragStartOffX + (e.getX() - dragStartX);
                offsetY = dragStartOffY + (e.getY() - dragStartY);
                cacheDirty = true;
                repaint();
            }
        });
    }

    public void setLSystem(LSystem ls) {
        this.lsystem    = ls;
        this.animating  = false;
        this.animIndex  = -1;
        this.autoFit    = true;
        this.cacheDirty = true;
        repaint();
    }

    public void startAnimation() {
        if (lsystem == null) return;
        animIndex  = 0;
        animating  = true;
        cacheDirty = true;
        repaint();
    }

    /** Advance animation by one step. Returns true if complete. */
    public boolean animStep(int steps) {
        if (!animating || lsystem == null) return true;
        List<LSystem.DrawCommand> cmds = lsystem.getCommands();
        animIndex = Math.min(animIndex + steps, cmds.size());
        cacheDirty = true;
        repaint();
        if (animIndex >= cmds.size()) {
            animating  = false;
            animIndex  = -1;
            return true;
        }
        return false;
    }

    public boolean isAnimating() { return animating; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (lsystem == null) { drawPlaceholder(g); return; }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,    RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Compute auto-fit transform
        if (autoFit) computeAutoFit();

        // Apply viewport transform
        AffineTransform savedTx = g2.getTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        drawCommands(g2);

        g2.setTransform(savedTx);
        drawOverlay(g2);
    }

    private void computeAutoFit() {
        double w = lsystem.getMaxX() - lsystem.getMinX();
        double h = lsystem.getMaxY() - lsystem.getMinY();
        if (w < 1) w = 1; if (h < 1) h = 1;

        double margin = 30;
        double scaleX = (getWidth()  - margin*2) / w;
        double scaleY = (getHeight() - margin*2) / h;
        scale = Math.min(scaleX, scaleY);

        // Centre the drawing
        double centreX = (lsystem.getMinX() + lsystem.getMaxX()) / 2.0;
        double centreY = (lsystem.getMinY() + lsystem.getMaxY()) / 2.0;
        offsetX = getWidth()  / 2.0 - centreX * scale;
        offsetY = getHeight() / 2.0 - centreY * scale;
    }

    private void drawCommands(Graphics2D g2) {
        List<LSystem.DrawCommand> cmds = lsystem.getCommands();
        int limit = (animIndex >= 0) ? animIndex : cmds.size();
        limit = Math.min(limit, cmds.size());

        for (int i = 0; i < limit; i++) {
            LSystem.DrawCommand cmd = cmds.get(i);
            if (cmd.type == LSystem.DrawCommand.Type.MOVE) continue;

            Color col   = Theme.branchColor(cmd.depth, 220);
            float lw    = Math.max(0.4f, (float)(cmd.width * 0.8));

            // Glow on deep-depth (trunk) segments
            if (cmd.depth <= 2 && scale > 0.3) {
                g2.setColor(Theme.withAlpha(col, 40));
                g2.setStroke(new BasicStroke(lw * 4,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int)cmd.x1, (int)cmd.y1, (int)cmd.x2, (int)cmd.y2);
            }

            g2.setColor(col);
            g2.setStroke(new BasicStroke(lw,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)cmd.x1, (int)cmd.y1, (int)cmd.x2, (int)cmd.y2);
        }
    }

    private void drawOverlay(Graphics2D g2) {
        g2.setFont(Theme.FONT_SMALL);
        // Generation and string length
        String info = String.format("Gen %d  |  %,d segments  |  string length %,d",
            lsystem.getGeneration(),
            lsystem.getCommands().size(),
            lsystem.getStringLength());
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(info, 8, getHeight() - 6);

        // Zoom
        g2.setColor(Theme.TEXT_SECONDARY);
        g2.drawString(String.format("zoom %.0f%%  scroll=zoom  drag=pan",
            scale * 100), getWidth() - 230, getHeight() - 6);

        // Animation progress bar
        if (animating && lsystem != null) {
            int total = lsystem.getCommands().size();
            int bw = getWidth() - 16;
            g2.setColor(Theme.BG_CARD);
            g2.fillRect(8, getHeight()-18, bw, 4);
            g2.setColor(Theme.TEXT_ACCENT);
            g2.fillRect(8, getHeight()-18, (int)((double)animIndex/total*bw), 4);
        }
    }

    private void drawPlaceholder(Graphics g) {
        g.setColor(Theme.TEXT_SECONDARY);
        g.setFont(Theme.FONT_LABEL);
        g.drawString("Select a preset or enter grammar, then press Draw", 40, getHeight()/2);
    }

    public void resetView() {
        autoFit    = true;
        cacheDirty = true;
        repaint();
    }
}
