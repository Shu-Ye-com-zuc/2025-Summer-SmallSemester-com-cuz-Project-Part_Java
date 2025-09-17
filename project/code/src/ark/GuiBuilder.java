package ark;

import java.awt.*;
import java.awt.event.*;

public class GuiBuilder {
    public static final int MODE_TABLE = 0;
    public static final int MODE_PIE  = 1;
    public static final int MODE_BAR  = 2;

    public static int currentMode = MODE_TABLE;

    public static void create() {
        Frame frame = new Frame("六星干员一览");
        frame.setSize(1200, 1600);
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        DrawCanvas canvas = new DrawCanvas();
        frame.add(canvas, BorderLayout.CENTER);

        Panel btnPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
        Button b1 = new Button("表格");
        Button b2 = new Button("饼状图");
        Button b3 = new Button("柱状图");

        b1.addActionListener(e -> { currentMode = MODE_TABLE; canvas.repaint(); });
        b2.addActionListener(e -> { currentMode = MODE_PIE;  canvas.repaint(); });
        b3.addActionListener(e -> { currentMode = MODE_BAR;  canvas.repaint(); });

        btnPanel.add(b1); btnPanel.add(b2); btnPanel.add(b3);
        frame.add(btnPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}