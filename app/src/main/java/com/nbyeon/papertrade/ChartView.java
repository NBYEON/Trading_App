package com.nbyeon.papertrade;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;
import java.util.Random;

/** Deterministic, synthetic chart. Never presented as historical market data. */
public final class ChartView extends View {
    private final Paint paint = new Paint(3);
    private final boolean compact;
    private final int color;
    private int seed;
    private boolean candles;
    public ChartView(Context context, int seed, int color, boolean compact) {
        super(context); this.seed = seed; this.color = color; this.compact = compact;
        setContentDescription(compact ? "Synthetic price sparkline" : "Illustrative price chart, simulated data");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void setRange(int range) { seed = 41 + range * 19; invalidate(); }
    public void setCandles(boolean value) { candles = value; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = compact ? 2 : 0, right = getWidth() - (compact ? 2 : 49 * d);
        float top = compact ? 5 : 12 * d, bottom = getHeight() - (compact ? 5 : 30 * d);
        if (!compact) {
            paint.setShader(null); paint.setStrokeWidth(d); paint.setColor(0xff242b36);
            for (int i = 0; i < 4; i++) {
                float y = top + (bottom - top) * i / 3;
                canvas.drawLine(left, y, right, y, paint);
                paint.setColor(0xff738093); paint.setTextSize(10 * d);
                canvas.drawText(new String[]{"High", "", "", "Low"}[i], right + 8 * d, y + 4 * d, paint);
                paint.setColor(0xff242b36);
            }
        }
        int n = compact ? 23 : 54;
        float[] values = new float[n];
        Random random = new Random(seed);
        float v = 0.65f;
        for (int i = 0; i < n; i++) {
            v += (random.nextFloat() - 0.48f) * 0.19f - 0.004f;
            v = Math.max(0.08f, Math.min(0.94f, v));
            values[i] = top + v * (bottom - top);
        }
        paint.setShader(null);
        if (candles && !compact) {
            for (int i = 1; i < n; i++) {
                float x = left + (right - left) * i / (n - 1);
                paint.setColor(values[i] < values[i - 1] ? 0xff53d6a0 : 0xffff7088);
                paint.setStrokeWidth(d);
                canvas.drawLine(x, Math.min(values[i], values[i - 1]) - 6 * d,
                    x, Math.max(values[i], values[i - 1]) + 6 * d, paint);
                paint.setStrokeWidth(Math.max(2 * d, (right - left) / n * 0.55f));
                canvas.drawLine(x, values[i - 1], x, values[i] + d, paint);
            }
        } else {
            Path line = new Path();
            line.moveTo(left, values[0]);
            for (int i = 1; i < n; i++) line.lineTo(left + (right - left) * i / (n - 1), values[i]);
            if (!compact) {
                Path fill = new Path(line);
                fill.lineTo(right, bottom); fill.lineTo(left, bottom); fill.close();
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(0, top, 0, bottom, 0x3553d6a0, 0x0053d6a0, Shader.TileMode.CLAMP));
                canvas.drawPath(fill, paint); paint.setShader(null);
            }
            paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth((compact ? 1.5f : 2) * d);
            paint.setStrokeJoin(Paint.Join.ROUND); canvas.drawPath(line, paint); paint.setStyle(Paint.Style.FILL);
            if (!compact) canvas.drawCircle(right, values[n - 1], 3.5f * d, paint);
        }
        if (!compact) {
            paint.setShader(null); paint.setColor(0xff738093); paint.setTextSize(10 * d);
            canvas.drawText("START", left, getHeight() - 4 * d, paint);
            canvas.drawText("SIMULATED", right / 2 - 28 * d, getHeight() - 4 * d, paint);
            canvas.drawText("END", right - 20 * d, getHeight() - 4 * d, paint);
        }
    }
}

