package model;

import java.util.*;

/**
 * L-System engine.
 *
 * Supports:
 *   - Deterministic rules (one production per symbol)
 *   - Stochastic rules (multiple productions per symbol with probabilities)
 *   - Standard turtle command symbols:
 *       F  = draw forward
 *       f  = move forward (no draw)
 *       G  = draw forward (alternate, used in some grammars)
 *       +  = turn left by angle
 *       -  = turn right by angle
 *       |  = turn 180°
 *       [  = push state
 *       ]  = pop state
 *       <  = decrease step length
 *       >  = increase step length
 *       !  = decrease line width
 *       ;  = increase line width
 *       A-Z, a-z = variables (expanded by rules, no drawing unless F/G)
 *
 * The engine expands the axiom for N generations then interprets the
 * resulting string as turtle commands, recording a list of DrawCommand
 * objects that the renderer can paint.
 */
public class LSystem {

    // ── Production rule ───────────────────────────────────────────────────────
    public static class Rule {
        public final char   symbol;
        public final String production;
        public final double probability;  // for stochastic rules; 1.0 for deterministic

        public Rule(char symbol, String production, double probability) {
            this.symbol      = symbol;
            this.production  = production;
            this.probability = probability;
        }
        public Rule(char symbol, String production) {
            this(symbol, production, 1.0);
        }
    }

    // ── Draw command produced by turtle interpretation ────────────────────────
    public static class DrawCommand {
        public enum Type { LINE, MOVE }
        public final Type   type;
        public final double x1, y1, x2, y2;
        public final int    depth;    // stack depth when drawn (for colouring)
        public final double width;    // line width

        public DrawCommand(Type type, double x1, double y1,
                           double x2, double y2, int depth, double width) {
            this.type  = type;
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.depth = depth;
            this.width = width;
        }
    }

    // ── Configuration ─────────────────────────────────────────────────────────
    private String         axiom       = "F";
    private List<Rule>     rules       = new ArrayList<>();
    private double         angle       = 90.0;   // degrees
    private double         stepLength  = 10.0;
    private double         stepScale   = 1.0;    // multiplier per generation
    private long           seed        = 42L;

    // ── Results ───────────────────────────────────────────────────────────────
    private String              currentString = "";
    private List<DrawCommand>   commands      = new ArrayList<>();
    private int                 generation    = 0;

    // Bounding box of last render (for auto-fit)
    private double minX, minY, maxX, maxY;

    // ── Presets ───────────────────────────────────────────────────────────────
    public static LSystem preset(String name) {
        LSystem ls = new LSystem();
        switch (name) {

            case "Koch Snowflake" -> {
                ls.axiom = "F--F--F";
                ls.rules.add(new Rule('F', "F+F--F+F"));
                ls.angle = 60;
            }

            case "Koch Island" -> {
                ls.axiom = "F+F+F+F";
                ls.rules.add(new Rule('F', "F+F-F-FF+F+F-F"));
                ls.angle = 90;
            }

            case "Sierpinski Triangle" -> {
                ls.axiom = "F-G-G";
                ls.rules.add(new Rule('F', "F-G+F+G-F"));
                ls.rules.add(new Rule('G', "GG"));
                ls.angle = 120;
            }

            case "Dragon Curve" -> {
                ls.axiom = "FX";
                ls.rules.add(new Rule('X', "X+YF+"));
                ls.rules.add(new Rule('Y', "-FX-Y"));
                ls.angle = 90;
            }

            case "Hilbert Curve" -> {
                ls.axiom = "A";
                ls.rules.add(new Rule('A', "+BF-AFA-FB+"));
                ls.rules.add(new Rule('B', "-AF+BFB+FA-"));
                ls.angle = 90;
            }

            case "Simple Fern" -> {
                ls.axiom = "X";
                ls.rules.add(new Rule('F', "FF"));
                ls.rules.add(new Rule('X', "F+[[X]-X]-F[-FX]+X"));
                ls.angle = 25;
                ls.stepLength = 6;
            }

            case "Barnsley Fern" -> {
                ls.axiom = "X";
                ls.rules.add(new Rule('F', "FF"));
                ls.rules.add(new Rule('X', "F-[[X]+X]+F[+FX]-X"));
                ls.angle = 22.5;
                ls.stepLength = 5;
            }

            case "Binary Tree" -> {
                ls.axiom = "0";
                ls.rules.add(new Rule('1', "11"));
                ls.rules.add(new Rule('0', "1[0]0"));
                ls.angle = 45;
            }

            case "Symmetric Tree" -> {
                ls.axiom = "F";
                ls.rules.add(new Rule('F', "FF-[-F+F+F]+[+F-F-F]"));
                ls.angle = 22.5;
                ls.stepLength = 8;
            }

            case "Bush" -> {
                ls.axiom = "F";
                ls.rules.add(new Rule('F', "FF+[+F-F-F]-[-F+F+F]"));
                ls.angle = 25.7;
                ls.stepLength = 7;
            }

            case "Stochastic Plant" -> {
                ls.axiom = "F";
                ls.rules.add(new Rule('F', "F[+F]F[-F]F",    0.33));
                ls.rules.add(new Rule('F', "F[+F]F",         0.33));
                ls.rules.add(new Rule('F', "F[-F]F",         0.34));
                ls.angle = 25.7;
                ls.stepLength = 7;
            }

            case "Penrose Tiling" -> {
                ls.axiom = "[X]++[X]++[X]++[X]++[X]";
                ls.rules.add(new Rule('F', ""));
                ls.rules.add(new Rule('W', "YF++ZF----XF[-YF----WF]++"));
                ls.rules.add(new Rule('X', "+YF--ZF[---WF--XF]+"));
                ls.rules.add(new Rule('Y', "-WF++XF[+++YF++ZF]-"));
                ls.rules.add(new Rule('Z', "--YF++++WF[+ZF++++XF]--XF"));
                ls.angle = 36;
            }

            case "Gosper Curve" -> {
                ls.axiom = "XF";
                ls.rules.add(new Rule('X', "XF+YF++YF-XF--XFXF-YF+"));
                ls.rules.add(new Rule('Y', "-XF+YFYF++YF+XF--XF-YF"));
                ls.angle = 60;
            }

            default -> {
                ls.axiom = "F";
                ls.rules.add(new Rule('F', "F+F-F-F+F"));
                ls.angle = 90;
            }
        }
        ls.expand(0);
        return ls;
    }

    public static String[] presetNames() {
        return new String[]{
            "Koch Snowflake", "Koch Island", "Sierpinski Triangle",
            "Dragon Curve", "Hilbert Curve", "Simple Fern",
            "Barnsley Fern", "Binary Tree", "Symmetric Tree",
            "Bush", "Stochastic Plant", "Gosper Curve"
        };
    }

    // ── Expansion ─────────────────────────────────────────────────────────────
    public void expand(int generations) {
        Random rng = new Random(seed);
        currentString = axiom;
        for (int g = 0; g < generations; g++) {
            currentString = expandOnce(currentString, rng);
        }
        generation = generations;
        interpret();
    }

    private String expandOnce(String s, Random rng) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            List<Rule> matching = rulesFor(c);
            if (matching.isEmpty()) {
                sb.append(c);
            } else if (matching.size() == 1) {
                sb.append(matching.get(0).production);
            } else {
                // Stochastic: choose rule by probability
                double r = rng.nextDouble();
                double cumulative = 0;
                String chosen = matching.get(matching.size()-1).production;
                for (Rule rule : matching) {
                    cumulative += rule.probability;
                    if (r < cumulative) { chosen = rule.production; break; }
                }
                sb.append(chosen);
            }
        }
        return sb.toString();
    }

    private List<Rule> rulesFor(char c) {
        List<Rule> result = new ArrayList<>();
        for (Rule r : rules) if (r.symbol == c) result.add(r);
        return result;
    }

    // ── Turtle interpretation ─────────────────────────────────────────────────
    private void interpret() {
        commands = new ArrayList<>();

        // Turtle state
        double x = 0, y = 0;
        double heading = -90.0;  // start pointing up
        double step = stepLength;
        double width = 1.5;
        int    stackDepth = 0;

        // State stack for [ ]
        Deque<double[]> stack = new ArrayDeque<>();

        minX = minY = Double.MAX_VALUE;
        maxX = maxY = Double.MIN_VALUE;

        for (int i = 0; i < currentString.length(); i++) {
            char c = currentString.charAt(i);
            switch (c) {
                case 'F', 'G' -> {
                    double rad = Math.toRadians(heading);
                    double nx  = x + step * Math.cos(rad);
                    double ny  = y + step * Math.sin(rad);
                    commands.add(new DrawCommand(
                        DrawCommand.Type.LINE, x, y, nx, ny,
                        Math.min(stackDepth, 20), width));
                    updateBounds(nx, ny);
                    x = nx; y = ny;
                }
                case 'f' -> {
                    double rad = Math.toRadians(heading);
                    double nx  = x + step * Math.cos(rad);
                    double ny  = y + step * Math.sin(rad);
                    commands.add(new DrawCommand(
                        DrawCommand.Type.MOVE, x, y, nx, ny, stackDepth, width));
                    x = nx; y = ny;
                }
                case '+' -> heading -= angle;
                case '-' -> heading += angle;
                case '|' -> heading += 180;
                case '[' -> {
                    stack.push(new double[]{x, y, heading, step, width});
                    stackDepth++;
                }
                case ']' -> {
                    if (!stack.isEmpty()) {
                        double[] state = stack.pop();
                        x = state[0]; y = state[1];
                        heading = state[2]; step = state[3]; width = state[4];
                        stackDepth = Math.max(0, stackDepth-1);
                    }
                }
                case '<' -> step *= 0.7;
                case '>' -> step *= 1.43;
                case '!' -> width = Math.max(0.3, width * 0.7);
                case ';' -> width = Math.min(8, width * 1.4);
                // Ignore all other symbols (variables)
            }
        }

        // Make sure we have bounding box data
        if (commands.isEmpty()) { minX=minY=0; maxX=maxY=100; }
    }

    private void updateBounds(double x, double y) {
        if (x < minX) minX = x;  if (x > maxX) maxX = x;
        if (y < minY) minY = y;  if (y > maxY) maxY = y;
    }

    // ── Getters / setters ─────────────────────────────────────────────────────
    public List<DrawCommand> getCommands()    { return commands; }
    public String            getCurrentString(){ return currentString; }
    public int               getGeneration()  { return generation; }
    public String            getAxiom()       { return axiom; }
    public List<Rule>        getRules()        { return rules; }
    public double            getAngle()       { return angle; }
    public double            getStepLength()  { return stepLength; }

    public double getMinX() { return minX; } public double getMaxX() { return maxX; }
    public double getMinY() { return minY; } public double getMaxY() { return maxY; }

    public void setAxiom(String axiom)           { this.axiom = axiom; }
    public void setAngle(double angle)           { this.angle = angle; }
    public void setStepLength(double s)          { this.stepLength = s; }
    public void setRules(List<Rule> rules)        { this.rules = rules; }
    public void setSeed(long seed)               { this.seed = seed; }
    public int  getStringLength()                { return currentString.length(); }
}
