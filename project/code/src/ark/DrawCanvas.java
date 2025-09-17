package ark;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class DrawCanvas extends Canvas {
    private Image offScreen;   // 离线缓冲区
    private Graphics offG;
    // 表格绘制常量
    private static final int TABLE_X           = 60;
    private static final int TABLE_Y           = 20;
    private static final int ROW_HEIGHT        = 26;
    private static final int CELL_PADDING      = 10;
    private static final int SCROLLBAR_WIDTH   = 16;
    private static final int SCROLLBAR_MARGIN  = 10;
    // 图表通用
    private static final int CHART_PADDING     = 50;
    private static final int LEGEND_PADDING    = 5;
    // 柱状图专用
    private static final int BAR_CHART_PADDING = 80;
    private static final int BAR_WIDTH         = 60;
    private static final int BAR_SPACING       = 80;
    private static final int AXIS_TAIL   = 20;   // 箭头斜线长度
    private static final int X_AXIS_EXTRA = 40;  // 轴比最后一个柱子再伸出 40 px
    /* ===================== 实例变量 ===================== */
    private int[] columnWidths;      // 各列表格宽度
    private int   scrollOffset = 0;  // 垂直滚动偏移
    private boolean isDragging = false;
    private int dragStartY;
    private int scrollbarY;
    private int scrollbarHeight;
    private int thumbHeight;


    public DrawCanvas() {
        // 鼠标按下
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (GuiBuilder.currentMode == GuiBuilder.MODE_TABLE) {
                    int x = e.getX(), y = e.getY();
                    int scrollbarX = TABLE_X + getTableWidth() + SCROLLBAR_MARGIN;
                    int thumbY = getThumbY();

                    if (x >= scrollbarX && x <= scrollbarX + SCROLLBAR_WIDTH) {
                        if (y >= thumbY && y <= thumbY + thumbHeight) {
                            isDragging = true;
                            dragStartY = y - thumbY;
                        } else if (y >= scrollbarY && y <= scrollbarY + scrollbarHeight) {
                            scrollOffset += (y < thumbY)
                                    ? -getVisibleRows() * ROW_HEIGHT
                                    :  getVisibleRows() * ROW_HEIGHT;
                            scrollOffset = Math.max(0,
                                    Math.min(scrollOffset, getMaxScrollOffset()));
                            repaint();
                        }
                    }
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });

        // 鼠标拖动
        addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (isDragging && GuiBuilder.currentMode == GuiBuilder.MODE_TABLE) {
                    int newThumbY = e.getY() - dragStartY;
                    newThumbY = Math.max(scrollbarY,
                            Math.min(newThumbY,
                                    scrollbarY + scrollbarHeight - thumbHeight));
                    double ratio = (double) (newThumbY - scrollbarY)
                            / (scrollbarHeight - thumbHeight);
                    scrollOffset = (int) (ratio * getMaxScrollOffset());
                    repaint();
                }
            }
        });

        // 滚轮
        addMouseWheelListener(e -> {
            if (GuiBuilder.currentMode == GuiBuilder.MODE_TABLE) {
                scrollOffset += e.getWheelRotation() * ROW_HEIGHT;
                scrollOffset = Math.max(0,
                        Math.min(scrollOffset, getMaxScrollOffset()));
                repaint();
            }
        });
    }

    /* ===================== 绘制 ===================== */

    @Override
    public void update(Graphics g) {
        paint(g);          // 不再清屏，直接画
    }

    @Override
    public void paint(Graphics g) {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        // 1. 缓冲区尺寸变化时重建
        if (offScreen == null ||
                offScreen.getWidth(this) != getWidth() ||
                offScreen.getHeight(this) != getHeight()) {
            offScreen = createImage(getWidth(), getHeight());
            offG = offScreen.getGraphics();
        }

        // 2. 在内存里完整画一遍
        offG.setColor(getBackground());
        offG.fillRect(0, 0, getWidth(), getHeight());   // 只清内存缓冲区
        paintContent(offG);                             // 把原 paint 逻辑拆到这里

        // 3. 一次性贴到屏幕
        g.drawImage(offScreen, 0, 0, this);
    }

    private void paintContent(Graphics g) {
        if (CsvReader.DATA.isEmpty()) {
            g.drawString("没有数据可显示", 50, 50);
            return;
        }

        g.setColor(Color.BLACK);
        switch (GuiBuilder.currentMode) {
            case GuiBuilder.MODE_TABLE -> drawTableMode(g);
            case GuiBuilder.MODE_PIE   -> drawPieChartMode(g);
            case GuiBuilder.MODE_BAR   -> drawBarChartMode(g);
        }
    }

    /* --------------------- 表格模式 --------------------- */

    private void drawTableMode(Graphics g) {
        calculateColumnWidths(g);
        int visibleRows = getVisibleRows();
        int startRow = scrollOffset / ROW_HEIGHT;
        int endRow = Math.min(startRow + visibleRows, CsvReader.DATA.size());
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12));
        drawTableRow(g, CsvReader.DATA.getFirst(), TABLE_X, TABLE_Y, true);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12));
        for (int i = startRow; i < endRow; i++) {
            if (i == 0) continue;
            int rowY = TABLE_Y + (i - startRow +1 ) * ROW_HEIGHT;
            drawTableRow(g, CsvReader.DATA.get(i), TABLE_X, rowY, false);
        }
        drawScrollbar(g);
    }

    /* --------------------- 饼图模式 --------------------- */

    private void drawPieChartMode(Graphics g) {
        if (DataAnalyzer.OCCUPATION_COUNT.isEmpty()) {
            g.drawString("没有职业数据可显示", 50, 50);
            return;
        }

        int width   = getWidth()  - 2 * CHART_PADDING;
        int height  = getHeight() - 3 * CHART_PADDING;
        int centerX = CHART_PADDING + width / 2;
        int centerY = CHART_PADDING + height / 2;
        int radius  = Math.min(width, height) / 2;

        int total = DataAnalyzer.OCCUPATION_COUNT.values()
                .stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return;
        Color[] colors = {
                new Color(255, 99, 71),
                new Color(70, 130, 180),
                new Color(50, 205, 50),
                new Color(255, 165, 0),
                new Color(138, 43, 226),
                new Color(255, 215, 0),
                new Color(128, 0, 128),
                new Color(0, 128, 128)
        };

        List<Map.Entry<String,Integer>> sorted =
                new ArrayList<>(DataAnalyzer.OCCUPATION_COUNT.entrySet());
        sorted.sort(Map.Entry.<String,Integer>comparingByValue().reversed());

        int[] angles = new int[sorted.size()];
        int totalAngle = 0;
        for (int j = 0; j < sorted.size(); j++) {
            angles[j] = (int) ((float) sorted.get(j).getValue() / total * 360);
            totalAngle += angles[j];
        }
        if (totalAngle < 360) angles[angles.length-1] += 360 - totalAngle;

        int startAngle = 0, colorIdx = 0, legendY = CHART_PADDING;
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12));
        for (int j = 0; j < sorted.size(); j++) {
            Map.Entry<String,Integer> e = sorted.get(j);
            String occ = e.getKey();
            int    cnt = e.getValue();
            float  pct = (float) cnt / total * 100;

            Color c = colors[colorIdx % colors.length];
            g.setColor(c);
            g.fillRect(100, legendY, 15, 15);
            g.setColor(Color.BLACK);
            g.drawString(String.format("%s (%.1f%%)", occ, pct), 120, legendY + 15);
            legendY += 25;

            int arc = angles[j];
            g.setColor(c);
            g.fillArc(centerX - radius, centerY - radius,
                    2*radius, 2*radius, startAngle, arc);
            g.setColor(Color.BLACK);
            g.drawArc(centerX - radius, centerY - radius,
                    2*radius, 2*radius, startAngle, arc);

            if (arc > 10) {
                int labelAngle = startAngle + arc / 2;
                double labelR  = radius * 0.7;
                int lx = centerX + (int)(labelR * Math.cos(Math.toRadians(labelAngle)));
                int ly = centerY - (int)(labelR * Math.sin(Math.toRadians(labelAngle)));
                g.drawString(String.format("%.1f%%", pct), lx - 15, ly + 5);
            }
            startAngle += arc;
            colorIdx++;
        }

        g.setFont(g.getFont().deriveFont(Font.BOLD, 16));
        g.drawString("六星干员职业分布", centerX - 100, CHART_PADDING / 2);
    }

    /* --------------------- 柱状图模式 --------------------- */

    private void drawBarChartMode(Graphics g) {
        if (DataAnalyzer.COST_DISTRIBUTION.isEmpty()) {
            g.drawString("没有费用数据可显示", 50, 50);
            return;
        }

        int width  = getWidth()  - 2 * BAR_CHART_PADDING;
        int height = getHeight() - 2 * BAR_CHART_PADDING;
        int totalBarWidth = DataAnalyzer.COST_INTERVALS.length * (BAR_WIDTH + BAR_SPACING);
        int axisExtra     = 40;                 // 箭头 + 文字“费用”
        int wholeChartWidth = totalBarWidth + axisExtra;
        int usableWidth = getWidth() - 2 * BAR_CHART_PADDING;   // 去掉左右边距
        int startX = BAR_CHART_PADDING + (usableWidth - wholeChartWidth) / 2;
        int startY = getHeight() - BAR_CHART_PADDING;

        int max = 0;
        for (String k : DataAnalyzer.COST_INTERVALS)
            max = Math.max(max, DataAnalyzer.COST_DISTRIBUTION.getOrDefault(k, 0));
        max = Math.max(1, max);

        double yMax = calculateNiceNumber(max, false);
        int    yCnt = 5;
        double yStep = yMax / (yCnt - 1);

        g.setFont(g.getFont().deriveFont(Font.BOLD, 16));
        g.drawString("六星干员费用分布", 700, 90);

        g.setColor(Color.BLACK);
        int lastBarRight = startX + DataAnalyzer.COST_INTERVALS.length * (BAR_WIDTH + BAR_SPACING);
        int axisEnd = lastBarRight + 40;                 // 再伸出 40 px
        g.drawLine(startX, startY, axisEnd, startY);
        int arr = 20;
        g.drawLine(axisEnd, startY, axisEnd - arr, startY - arr/2);
        g.drawLine(axisEnd, startY, axisEnd - arr, startY + arr/2);
        g.drawString("费用区间", axisEnd + 5, startY + 5);                      // X轴
        g.drawLine(startX, startY, startX, startY - height);       // Y轴

        g.setFont(g.getFont().deriveFont(Font.PLAIN, 12));

        // X轴标签
        for (int i = 0; i < DataAnalyzer.COST_INTERVALS.length; i++) {
            String label = DataAnalyzer.COST_INTERVALS[i];
            int x = startX + i * (BAR_WIDTH + BAR_SPACING) + BAR_WIDTH / 2;
            g.drawString(label, x - 15, startY + 25);
        }

        // Y轴标签
        for (int i = 0; i <= yCnt; i++) {
            double val = i * yStep;
            int y = startY - (int)(i * height / yCnt)+1;
            g.drawString(String.valueOf((int)val), startX - 30, y + 3);
            g.drawLine(startX - 5, y, startX, y);
        }
// 给柱子准备一组柔和颜色，循环用
        final Color[] BAR_COLORS = {
                new Color( 70, 130, 180),  // 钢蓝
                new Color(255, 153,  51),  // 橙
                new Color( 46, 204, 113),  // 翡翠绿
                new Color(155,  89, 182),  // 紫
                new Color( 52, 152, 219),  // 亮蓝
                new Color(231,  76,  60)   // 番茄红
        };

// 画柱子
        for (int i = 0; i < DataAnalyzer.COST_INTERVALS.length; i++) {
            String key = DataAnalyzer.COST_INTERVALS[i];
            int cnt = DataAnalyzer.COST_DISTRIBUTION.getOrDefault(key, 0);

            int barH = (int) ((float) cnt / yMax * height);
            int barX = startX + i * (BAR_WIDTH + BAR_SPACING);
            int barY = startY - barH;

            /* 1. 先填色 */
            g.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            g.fillRect(barX, barY, BAR_WIDTH, barH);

            /* 2. 再画边框 */
            g.setColor(Color.BLACK);
            g.drawRect(barX, barY, BAR_WIDTH, barH);

            /* 3. 顶部数字 */
            if (cnt > 0) {
                g.drawString(String.valueOf(cnt),
                        barX + BAR_WIDTH / 2 - 5, barY - 5);
            }
        }
    }

    /* --------------------- 工具方法 --------------------- */

    private double calculateNiceNumber(double range, boolean round) {
        double exp = Math.floor(Math.log10(range));
        double frac = range / Math.pow(10, exp);
        double nice;
        if (round) {
            if (frac < 1.5) nice = 1;
            else if (frac < 3) nice = 2;
            else if (frac < 7) nice = 5;
            else nice = 10;
        } else {
            if (frac <= 1) nice = 1;
            else if (frac <= 2) nice = 2;
            else if (frac <= 5) nice = 5;
            else nice = 10;
        }
        return nice * Math.pow(10, exp);
    }

    private void calculateColumnWidths(Graphics g) {
        if (CsvReader.DATA.isEmpty()) return;
        DataRow header = CsvReader.DATA.getFirst();
        int cols = header.getData().length;
        columnWidths = new int[cols];
        FontMetrics fm = g.getFontMetrics();
        for (int c = 0; c < cols; c++) {
            int max = 0;
            for (DataRow r : CsvReader.DATA) {
                if (c < r.getData().length) {
                    int w = fm.stringWidth(r.getData()[c]) + 2 * CELL_PADDING;
                    max = Math.max(max, w);
                }
            }
            columnWidths[c] = max;
        }
    }

    private void drawTableRow(Graphics g, DataRow row, int x, int y, boolean header) {
        String[] cells = row.getData();
        int curX = x;

        for (int i = 0; i < Math.min(cells.length, columnWidths.length); i++) {
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(curX, y, columnWidths[i], ROW_HEIGHT);
            g.setColor(Color.BLACK);
            FontMetrics fm = g.getFontMetrics();
            int tx = curX + CELL_PADDING;
            int ty = y + ROW_HEIGHT / 2 + fm.getAscent() / 2 - 1;
            g.drawString(cells[i], tx, ty);
            curX += columnWidths[i];
        }

    }

    private void drawScrollbar(Graphics g) {
        if (CsvReader.DATA.size() <= getVisibleRows()) return;
        int tableW = getTableWidth();
        int scrollbarX = TABLE_X + tableW + SCROLLBAR_MARGIN;
        scrollbarY = TABLE_Y;
        scrollbarHeight = getHeight() - 3 * CHART_PADDING;
        thumbHeight = Math.max(30,
                (int)(scrollbarHeight * (double)getVisibleRows() / CsvReader.DATA.size()));
        int thumbY = getThumbY();

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight);

        g.setColor(Color.GRAY);
        g.fillRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight);
    }

    private int getThumbY() {
        if (CsvReader.DATA.size() <= getVisibleRows()) return scrollbarY;
        double ratio = (double)scrollOffset / getMaxScrollOffset();
        return scrollbarY + (int)(ratio * (scrollbarHeight - thumbHeight));
    }

    private int getVisibleRows() {
        int availH = getHeight() - 3 * CHART_PADDING;   // 画布可用高度
        int rows   = availH / ROW_HEIGHT;               // 完整能放几行
        return Math.max(1, rows + 1 );                       // 不再多减
    }

    private int getMaxScrollOffset() {
        return Math.max(0, (CsvReader.DATA.size() - getVisibleRows() - 1) * ROW_HEIGHT);
    }

    private int getTableWidth() {
        int w = 0;
        for (int cw : columnWidths) w += cw;
        return w;
    }
}
