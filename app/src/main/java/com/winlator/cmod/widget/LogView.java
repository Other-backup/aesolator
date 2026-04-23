package com.winlator.cmod.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.winlator.cmod.R;
import com.winlator.cmod.contentdialog.DebugDialog;
import com.winlator.cmod.core.WinlatorLogUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.math.Mathf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LogView extends View {
    private static final int MAX_RETAINED_LINES = 2400;
    private static final int TRIM_BATCH_LINES = 240;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<String> lines = new ArrayList<>();
    private final float rowHeight = UnitUtils.dpToPx(16.5f);
    private final float defaultTextSize = UnitUtils.dpToPx(10.7f);
    private final float minScrollThumbSize = UnitUtils.dpToPx(6);
    private final float horizontalTextInset = UnitUtils.dpToPx(8);
    private final float timestampColumnWidth = UnitUtils.dpToPx(54);
    private final float timestampGap = UnitUtils.dpToPx(6);
    private final PointF lastPoint = new PointF();
    private final PointF scrollPosition = new PointF();
    private final PointF scrollSize = new PointF();
    private boolean isActionDown = false;
    private static String fileName;
    private boolean scrollingHorizontally = false;
    private boolean scrollingVertically = false;
    private final Object lock = new Object();
    private int emptyTextColor;
    private int textColor;
    private int timestampTextColor;
    private int separatorColor;
    private int gutterColor;
    private int thumbColor;
    private int threadTextColor;
    private int levelInfoColor;
    private int levelWarnColor;
    private int levelErrorColor;
    private int levelTraceColor;
    private int channelTextColor;
    private int secondaryTextColor;
    private float maxMeasuredLineWidth = 0f;

    public LogView(Context context) {
        this(context, null);
    }

    public LogView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public LogView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initRuntimePalette(context);
    }

    private void initRuntimePalette(Context context) {
        paint.setTypeface(Typeface.MONOSPACE);
        emptyTextColor = ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_muted);
        textColor = ContextCompat.getColor(context, R.color.surface_runtime_taskmgr_text);
        timestampTextColor = 0xFF93B9D4;
        separatorColor = 0x165E86A0;
        gutterColor = 0x315E86A0;
        thumbColor = 0xD47EC2F2;
        threadTextColor = 0xFF7FB4D4;
        levelInfoColor = 0xFF7BE0D6;
        levelWarnColor = 0xFFF3C969;
        levelErrorColor = 0xFFFF8E7C;
        levelTraceColor = 0xFF8FAEDB;
        channelTextColor = 0xFFB6D3E8;
        secondaryTextColor = 0xFF9CB5C7;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeScrollSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        if (width == 0 || height == 0) return;

        synchronized (lock) {
            paint.setStyle(Paint.Style.FILL);

            if (lines.isEmpty()) {
                paint.setTextSize(UnitUtils.dpToPx(16));
                paint.setColor(emptyTextColor);
                String text = getContext().getString(R.string.no_items_to_display);
                float centerX = (width - paint.measureText(text)) * 0.5f;
                float centerY = (height - paint.getFontSpacing()) * 0.5f - paint.ascent();
                canvas.drawText(text, centerX, centerY, paint);
                return;
            }

            paint.setTextSize(defaultTextSize);
            float textHeight = paint.getFontSpacing();
            int count = lines.size();
            int startIndex = Math.max(0, (int)Math.floor(scrollPosition.y / rowHeight));
            int visibleRows = (int)Math.ceil(height / rowHeight) + 2;
            int endIndex = Math.min(count, startIndex + visibleRows);
            float rowY = startIndex * rowHeight - scrollPosition.y;

            for (int i = startIndex; i < endIndex; i++) {
                float centerY = (rowY - paint.ascent()) + (rowHeight - textHeight) * 0.5f;
                drawConsoleLine(canvas, lines.get(i), rowY, centerY, width);
                rowY += rowHeight;
            }

            drawScrollThumbs(canvas);
        }
    }

    private void drawConsoleLine(Canvas canvas, String line, float rowTop, float baselineY, int width) {
        String safeLine = line == null ? "" : line;
        String timestamp = "";
        String body = safeLine;
        if (safeLine.startsWith("[")) {
            int closing = safeLine.indexOf(']');
            if (closing > 0 && closing <= 10) {
                timestamp = safeLine.substring(0, closing + 1);
                body = safeLine.substring(Math.min(safeLine.length(), closing + 1)).trim();
            }
        }

        float rowBottom = rowTop + rowHeight;
        float textX = horizontalTextInset - scrollPosition.x;
        float timestampRight = textX + timestampColumnWidth;
        if (!timestamp.isEmpty()) {
            paint.setColor(timestampTextColor);
            canvas.drawText(timestamp, textX, baselineY, paint);
            paint.setColor(gutterColor);
            canvas.drawRect(timestampRight, rowTop + UnitUtils.dpToPx(3), timestampRight + UnitUtils.dpToPx(1), rowBottom - UnitUtils.dpToPx(3), paint);
            textX = timestampRight + timestampGap;
        }

        textX = drawStyledBody(canvas, body, textX, baselineY);

        paint.setColor(separatorColor);
        canvas.drawRect(0, rowBottom - UnitUtils.dpToPx(1), width, rowBottom, paint);
    }

    private float drawStyledBody(Canvas canvas, String body, float textX, float baselineY) {
        if (body == null || body.isEmpty()) return textX;
        String[] parts = body.split(":", 4);
        if (parts.length >= 4 && isLikelyThreadToken(parts[0])) {
            textX = drawToken(canvas, parts[0], textX, baselineY, threadTextColor, true);
            textX = drawToken(canvas, ":", textX, baselineY, secondaryTextColor, false);
            int levelColor = resolveLevelColor(parts[1]);
            textX = drawToken(canvas, parts[1], textX, baselineY, levelColor, true);
            textX = drawToken(canvas, ":", textX, baselineY, secondaryTextColor, false);
            textX = drawToken(canvas, parts[2], textX, baselineY, channelTextColor, true);
            textX = drawToken(canvas, ":", textX, baselineY, secondaryTextColor, false);
            return drawToken(canvas, parts[3], textX, baselineY, textColor, false);
        }

        int semanticColor = resolveSemanticColor(body);
        return drawToken(canvas, body, textX, baselineY, semanticColor, false);
    }

    private float drawToken(Canvas canvas, String token, float textX, float baselineY, int color, boolean bold) {
        if (token == null || token.isEmpty()) return textX;
        paint.setColor(color);
        paint.setTypeface(bold ? Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) : Typeface.MONOSPACE);
        canvas.drawText(token, textX, baselineY, paint);
        float nextX = textX + paint.measureText(token);
        paint.setTypeface(Typeface.MONOSPACE);
        return nextX;
    }

    private boolean isLikelyThreadToken(String token) {
        if (token == null || token.length() < 2 || token.length() > 6) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) return false;
        }
        return true;
    }

    private int resolveLevelColor(String level) {
        if (level == null) return textColor;
        String normalized = level.trim().toLowerCase();
        if ("warn".equals(normalized) || "warning".equals(normalized) || "fixme".equals(normalized)) {
            return levelWarnColor;
        }
        if ("err".equals(normalized) || "error".equals(normalized) || "fatal".equals(normalized)) {
            return levelErrorColor;
        }
        if ("trace".equals(normalized) || "debug".equals(normalized)) {
            return levelTraceColor;
        }
        return levelInfoColor;
    }

    private int resolveSemanticColor(String body) {
        if (body == null) return textColor;
        String normalized = body.toLowerCase();
        if (normalized.contains("error") || normalized.contains("fatal") || normalized.contains("failed")) {
            return levelErrorColor;
        }
        if (normalized.contains("warn") || normalized.contains("warning") || normalized.contains("fixme")) {
            return levelWarnColor;
        }
        if (normalized.contains("info") || normalized.contains("started") || normalized.contains("ready")) {
            return levelInfoColor;
        }
        return textColor;
    }

    private void drawScrollThumbs(Canvas canvas) {
        float scrollThumbX = getScrollThumbX();
        float scrollThumbY = getScrollThumbY();
        float scrollThumbWidth = getScrollThumbWidth();
        float scrollThumbHeight = getScrollThumbHeight();

        paint.setColor(thumbColor);
        float radius = minScrollThumbSize * 0.5f;

        canvas.drawRoundRect(scrollThumbX, getHeight() - minScrollThumbSize, scrollThumbX + scrollThumbWidth, getHeight(), radius, radius, paint);
        canvas.drawRoundRect(getWidth() - minScrollThumbSize, scrollThumbY, getWidth(), scrollThumbY + scrollThumbHeight, radius, radius, paint);
    }

    public float getScrollMaxLeft() {
        return Math.max(0, scrollSize.x - getWidth());
    }

    public float getScrollMaxTop() {
        return Math.max(0, scrollSize.y - getHeight());
    }

    public float getScrollThumbX() {
        float width = getWidth();
        if (scrollSize.x > 0 && scrollSize.x > width) return scrollPosition.x * (width / scrollSize.x);
        return -Float.MAX_VALUE;
    }

    public float getScrollThumbY() {
        float height = getHeight();
        if (scrollSize.y > 0 && scrollSize.y > height) return scrollPosition.y * (height / scrollSize.y);
        return -Float.MAX_VALUE;
    }

    public float getScrollThumbWidth() {
        float width = getWidth();
        if (scrollSize.x > 0 && scrollSize.x > width) {
            return Math.max(width - width * (getScrollMaxLeft() / scrollSize.x), minScrollThumbSize);
        }
        return 0;
    }

    public float getScrollThumbHeight() {
        float height = getHeight();
        if (scrollSize.y > 0 && scrollSize.y > height) {
            return Math.max(height - height * (getScrollMaxTop() / scrollSize.y), minScrollThumbSize);
        }
        return 0;
    }

    private void computeScrollSize() {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float maxWidth = 0;
        paint.setTextSize(defaultTextSize);
        for (int i = 0, count = lines.size(); i < count; i++) maxWidth = Math.max(paint.measureText(lines.get(i)), maxWidth);
        maxMeasuredLineWidth = maxWidth;
        scrollSize.x = Math.max(maxMeasuredLineWidth, width);
        scrollSize.y = Math.max(rowHeight * lines.size(), height);
        clampScrollPosition();
    }

    public void clear() {
        synchronized (lock) {
            lines.clear();
            maxMeasuredLineWidth = 0f;
            scrollPosition.set(0, 0);
            scrollSize.set(getWidth(), getHeight());
        }
        postInvalidate();
    }

    public void append(String line) {
        ArrayList<String> batch = new ArrayList<>(1);
        batch.add(line);
        appendBatch(batch);
    }

    public void appendBatch(List<String> batch) {
        if (batch == null || batch.isEmpty()) return;
        synchronized (lock) {
            paint.setTextSize(defaultTextSize);
            for (String rawLine : batch) {
                String formatted = "[" + DateFormat.format("HH:mm:ss", System.currentTimeMillis()) + "]  " + (rawLine == null ? "" : rawLine.replace("\n", ""));
                lines.add(formatted);
                maxMeasuredLineWidth = Math.max(maxMeasuredLineWidth, paint.measureText(formatted));
            }
            trimIfNeededLocked();
            scrollSize.x = Math.max(maxMeasuredLineWidth, getWidth());
            scrollSize.y = Math.max(rowHeight * lines.size(), getHeight());
            scrollPosition.x = 0;
            scrollPosition.y = getScrollMaxTop();
        }
        postInvalidateOnAnimation();
    }

    public void replaceRawText(String rawText) {
        synchronized (lock) {
            lines.clear();
            paint.setTextSize(defaultTextSize);
            maxMeasuredLineWidth = 0f;
            if (rawText != null && !rawText.trim().isEmpty()) {
                String[] rawLines = rawText.split("\\r?\\n", -1);
                for (String rawLine : rawLines) {
                    if (rawLine == null) continue;
                    String line = rawLine.replace('\r', ' ');
                    if (line.isEmpty()) continue;
                    lines.add(line);
                    maxMeasuredLineWidth = Math.max(maxMeasuredLineWidth, paint.measureText(line));
                }
            }
            scrollSize.x = Math.max(maxMeasuredLineWidth, getWidth());
            scrollSize.y = Math.max(rowHeight * lines.size(), getHeight());
            scrollPosition.x = 0f;
            scrollPosition.y = getScrollMaxTop();
        }
        postInvalidateOnAnimation();
    }

    public int getLineCount() {
        synchronized (lock) {
            return lines.size();
        }
    }

    private void trimIfNeededLocked() {
        if (lines.size() <= MAX_RETAINED_LINES) return;
        int trimCount = Math.min(TRIM_BATCH_LINES, lines.size() - MAX_RETAINED_LINES);
        if (trimCount <= 0) return;
        for (int i = 0; i < trimCount; i++) {
            lines.remove(0);
        }
        recomputeMeasuredWidthLocked();
    }

    private void recomputeMeasuredWidthLocked() {
        paint.setTextSize(defaultTextSize);
        float maxWidth = 0f;
        for (int i = 0, count = lines.size(); i < count; i++) {
            maxWidth = Math.max(maxWidth, paint.measureText(lines.get(i)));
        }
        maxMeasuredLineWidth = maxWidth;
    }

    private void clampScrollPosition() {
        scrollPosition.x = Mathf.clamp(scrollPosition.x, 0, getScrollMaxLeft());
        scrollPosition.y = Mathf.clamp(scrollPosition.y, 0, getScrollMaxTop());
    }

    static String normalizeFileStem(String file) {
        String value = file == null ? "" : file.trim();
        if (value.isEmpty()) return "runtime";

        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slashIndex >= 0 && slashIndex + 1 < value.length()) {
            value = value.substring(slashIndex + 1);
        }

        int dotIndex = value.lastIndexOf('.');
        if (dotIndex > 0) {
            value = value.substring(0, dotIndex);
        }

        value = value.trim();
        return value.isEmpty() ? "runtime" : value;
    }

    public static void setFilename(String file) {
        fileName = normalizeFileStem(file);
    }

    public static File getLogFile(Context context) {
        return WinlatorLogUtils.createTimestampedLogFile(context, fileName);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(event.getX(), event.getY());
                isActionDown = true;
                scrollingHorizontally = false;
                scrollingVertically = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (isActionDown) {
                    float dx = event.getX() - lastPoint.x;
                    float dy = event.getY() - lastPoint.y;

                    if (Math.abs(dx) > 10) scrollingHorizontally = true;
                    if (Math.abs(dy) > 10) scrollingVertically = true;

                    if (scrollingHorizontally) {
                        DebugDialog.setPaused(true);
                        scrollPosition.x = Mathf.clamp(scrollPosition.x - dx, 0, getScrollMaxLeft());
                        lastPoint.set(event.getX(), event.getY());
                        invalidate();
                    }

                    if (scrollingVertically) {
                        DebugDialog.setPaused(true);
                        scrollPosition.y = Mathf.clamp(scrollPosition.y - dy, 0, getScrollMaxTop());
                        lastPoint.set(event.getX(), event.getY());
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                DebugDialog.setPaused(false);
                isActionDown = false;
                break;
        }

        return true;
    }
}
