package com.arpit.tree;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class FamilyTreeView extends View {
    public interface OnMemberClickListener {
        void onMemberClick(FamilyMember member);
    }

    private static final int BG = Color.rgb(23, 21, 34);
    private static final int MALE_BG = Color.rgb(42, 36, 64);
    private static final int MALE_BD = Color.rgb(168, 85, 247);
    private static final int MALE_TXT = Color.rgb(245, 243, 255);
    private static final int FEMALE_BG = Color.rgb(50, 41, 78);
    private static final int FEMALE_BD = Color.rgb(192, 132, 252);
    private static final int FEMALE_TXT = Color.rgb(245, 243, 255);
    private static final int STAR_BG = Color.rgb(54, 42, 82);
    private static final int STAR_BD = Color.rgb(216, 180, 254);
    private static final int STAR_TXT = Color.rgb(245, 243, 255);
    private static final int LINE = Color.argb(102, 124, 58, 237);
    private static final int HIGHLIGHT = Color.rgb(192, 132, 252);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Integer, Position> positions = new HashMap<>();
    private final Map<Integer, Float> widths = new HashMap<>();
    private FamilyRepository.FamilyData data;
    private OnMemberClickListener listener;
    private float cardW;
    private float cardH;
    private float gapH;
    private float gapV;
    private float radius;
    private float minX;
    private float minY;
    private float contentW;
    private float contentH;
    private float offsetX;
    private float offsetY;
    private float lastX;
    private float lastY;
    private float downX;
    private float downY;
    private boolean dragging;
    private Integer highlightedId;

    public FamilyTreeView(Context context) {
        super(context);
        init();
    }

    public FamilyTreeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setBackgroundColor(BG);
        cardW = dp(130);
        cardH = dp(58);
        gapH = dp(34);
        gapV = dp(76);
        radius = dp(10);
        textPaint.setTextAlign(Paint.Align.CENTER);
        setFocusable(true);
    }

    public void setData(FamilyRepository.FamilyData data) {
        this.data = data;
        widths.clear();
        positions.clear();
        if (data != null && data.byId.containsKey(data.rootId)) {
            subtreeWidth(data.rootId);
            layout(data.rootId, 0, 0);
            computeBounds();
        }
        invalidate();
    }

    public void setOnMemberClickListener(OnMemberClickListener listener) {
        this.listener = listener;
    }

    public void highlightMember(int id) {
        highlightedId = id;
        centerOn(id);
        invalidate();
    }

    public void clearHighlight() {
        highlightedId = null;
        invalidate();
    }

    public void centerOn(int id) {
        Position p = positions.get(id);
        if (p == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        offsetX = clampX(getWidth() / 2f - (p.cx - minX));
        offsetY = clampY(getHeight() / 2f - (p.cy - minY));
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (data != null && oldw == 0 && oldh == 0) {
            centerOn(data.rootId);
        }
    }

    private float subtreeWidth(int id) {
        FamilyMember member = data.byId.get(id);
        if (member == null || member.children.isEmpty()) {
            widths.put(id, cardW);
            return cardW;
        }
        float width = 0;
        for (Integer childId : member.children) {
            width += subtreeWidth(childId);
        }
        width += gapH * Math.max(0, member.children.size() - 1);
        width = Math.max(cardW, width);
        widths.put(id, width);
        return width;
    }

    private void layout(int id, float left, int depth) {
        FamilyMember member = data.byId.get(id);
        if (member == null) return;

        float width = widths.get(id);
        float cy = depth * gapV;
        if (member.children.isEmpty()) {
            positions.put(id, new Position(left + width / 2f, cy));
            return;
        }

        float childLeft = left;
        for (Integer childId : member.children) {
            Float childWidth = widths.get(childId);
            if (childWidth == null) continue;
            layout(childId, childLeft, depth + 1);
            childLeft += childWidth + gapH;
        }

        Position first = positions.get(member.children.get(0));
        Position last = positions.get(member.children.get(member.children.size() - 1));
        float cx = first != null && last != null ? (first.cx + last.cx) / 2f : left + width / 2f;
        positions.put(id, new Position(cx, cy));
    }

    private void computeBounds() {
        float maxX = 0;
        float maxY = 0;
        minX = Float.MAX_VALUE;
        minY = Float.MAX_VALUE;
        for (Position p : positions.values()) {
            minX = Math.min(minX, p.cx - cardW / 2f - dp(30));
            minY = Math.min(minY, p.cy - dp(10));
            maxX = Math.max(maxX, p.cx + cardW / 2f + dp(30));
            maxY = Math.max(maxY, p.cy + cardH + dp(30));
        }
        contentW = Math.max(1, maxX - minX);
        contentH = Math.max(1, maxY - minY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;
        canvas.save();
        canvas.translate(offsetX - minX, offsetY - minY);
        drawLines(canvas);
        drawCards(canvas);
        canvas.restore();
    }

    private void drawLines(Canvas canvas) {
        paint.setColor(LINE);
        paint.setStrokeWidth(dp(1.6f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (FamilyMember member : data.members) {
            if (member.children.isEmpty()) continue;
            Position parent = positions.get(member.id);
            if (parent == null) continue;
            float bottom = parent.cy + cardH;
            Position first = positions.get(member.children.get(0));
            if (first == null) continue;
            float elbowY = bottom + (first.cy - bottom) / 2f;
            canvas.drawLine(parent.cx, bottom, parent.cx, elbowY, paint);
            for (Integer childId : member.children) {
                Position child = positions.get(childId);
                if (child == null) continue;
                canvas.drawLine(parent.cx, elbowY, child.cx, elbowY, paint);
                canvas.drawLine(child.cx, elbowY, child.cx, child.cy, paint);
            }
        }
    }

    private void drawCards(Canvas canvas) {
        for (FamilyMember member : data.members) {
            Position p = positions.get(member.id);
            if (p == null) continue;
            RectF rect = new RectF(p.cx - cardW / 2f, p.cy, p.cx + cardW / 2f, p.cy + cardH);
            boolean female = "F".equals(member.gender);
            int bg = member.star ? STAR_BG : female ? FEMALE_BG : MALE_BG;
            int bd = member.star ? STAR_BD : female ? FEMALE_BD : MALE_BD;
            int txt = member.star ? STAR_TXT : female ? FEMALE_TXT : MALE_TXT;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(member.deceased ? 24 : 64, 168, 85, 247));
            RectF glow = new RectF(rect);
            glow.inset(-dp(2), -dp(2));
            canvas.drawRoundRect(glow, radius + dp(2), radius + dp(2), paint);

            paint.setColor(bg);
            paint.setAlpha(member.deceased ? 110 : 255);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(bd);
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setAlpha(255);

            if (highlightedId != null && highlightedId == member.id) {
                paint.setColor(HIGHLIGHT);
                paint.setStrokeWidth(dp(2.5f));
                RectF hi = new RectF(rect);
                hi.inset(-dp(3), -dp(3));
                canvas.drawRoundRect(hi, radius + dp(3), radius + dp(3), paint);
            }

            textPaint.setColor(txt);
            textPaint.setTextSize(dp(12));
            textPaint.setFakeBoldText(true);
            drawCenteredText(canvas, member.name, rect.centerX(), rect.top + dp(13), (int) (cardW - dp(16)), 2);
            textPaint.setFakeBoldText(false);

            if (member.alias != null) {
                textPaint.setColor(Color.argb(190, 184, 179, 201));
                textPaint.setTextSize(dp(9));
                canvas.drawText(member.alias, rect.centerX(), rect.bottom - dp(7), textPaint);
            } else if (member.note != null && member.note.length() <= 18) {
                textPaint.setColor(Color.argb(170, 184, 179, 201));
                textPaint.setTextSize(dp(8.5f));
                canvas.drawText(member.note, rect.centerX(), rect.bottom - dp(7), textPaint);
            }

            textPaint.setColor(Color.argb(120, 184, 179, 201));
            textPaint.setTextSize(dp(7));
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("#" + member.id, rect.right - dp(4), rect.top + dp(8), textPaint);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }
    }

    private void drawCenteredText(Canvas canvas, String text, float centerX, float top, int width, int maxLines) {
        Paint.Align oldAlign = textPaint.getTextAlign();
        textPaint.setTextAlign(Paint.Align.LEFT);
        StaticLayout layout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0, 1)
            .setMaxLines(maxLines)
            .build();
        canvas.save();
        canvas.translate(centerX - width / 2f, top);
        layout.draw(canvas);
        canvas.restore();
        textPaint.setTextAlign(oldAlign);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = downX = event.getX();
                lastY = downY = event.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastX;
                float dy = event.getY() - lastY;
                if (Math.abs(event.getX() - downX) > dp(6) || Math.abs(event.getY() - downY) > dp(6)) {
                    dragging = true;
                }
                offsetX = clampX(offsetX + dx);
                offsetY = clampY(offsetY + dy);
                lastX = event.getX();
                lastY = event.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    FamilyMember member = findMemberAt(event.getX(), event.getY());
                    if (member != null && listener != null) listener.onMemberClick(member);
                }
                return true;
            default:
                return true;
        }
    }

    private FamilyMember findMemberAt(float sx, float sy) {
        float x = sx - offsetX + minX;
        float y = sy - offsetY + minY;
        for (FamilyMember member : data.members) {
            Position p = positions.get(member.id);
            if (p == null) continue;
            if (x >= p.cx - cardW / 2f && x <= p.cx + cardW / 2f && y >= p.cy && y <= p.cy + cardH) {
                return member;
            }
        }
        return null;
    }

    private float clampX(float value) {
        if (contentW <= getWidth()) {
            return (getWidth() - contentW) / 2f;
        }
        return Math.max(getWidth() - contentW, Math.min(0, value));
    }

    private float clampY(float value) {
        if (contentH <= getHeight()) {
            return (getHeight() - contentH) / 2f;
        }
        return Math.max(getHeight() - contentH, Math.min(0, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static class Position {
        final float cx;
        final float cy;

        Position(float cx, float cy) {
            this.cx = cx;
            this.cy = cy;
        }
    }
}
