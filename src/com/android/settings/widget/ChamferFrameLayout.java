/*
 * Copyright (C) 2026 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;

import com.android.settings.R;

public class ChamferFrameLayout extends FrameLayout {

    private static final int CORNER_TL = 0;
    private static final int CORNER_TR = 1;
    private static final int CORNER_BR = 2;
    private static final int CORNER_BL = 3;

    private final Path mPath = new Path();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int mChamferCorner = CORNER_TR;
    private float mChamferSize;
    private float mCornerRadius;

    public ChamferFrameLayout(Context context) {
        this(context, null);
    }

    public ChamferFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChamferFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ChamferFrameLayout, defStyleAttr, 0);
        mChamferCorner = a.getInt(R.styleable.ChamferFrameLayout_chamferCorner, CORNER_TR);
        mChamferSize = a.getDimension(R.styleable.ChamferFrameLayout_chamferSize, 0f);
        mCornerRadius = a.getDimension(R.styleable.ChamferFrameLayout_cornerRadius, 0f);
        mPaint.setColor(a.getColor(R.styleable.ChamferFrameLayout_fillColor, 0));
        mPaint.setStyle(Paint.Style.FILL);
        a.recycle();

        setWillNotDraw(false);
        setClipToOutline(true);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (!mPath.isEmpty()) {
                    outline.setPath(mPath);
                }
            }
        });
    }

    public void setFillColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setCornerRadius(float radius) {
        mCornerRadius = radius;
        buildPath();
        invalidateOutline();
        invalidate();
    }

    public void setChamfer(int corner, float size) {
        mChamferCorner = corner;
        mChamferSize = size;
        buildPath();
        invalidateOutline();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath();
        invalidateOutline();
    }

    private void buildPath() {
        final float w = getWidth();
        final float h = getHeight();
        mPath.reset();
        if (w <= 0 || h <= 0) {
            return;
        }
        final float r = Math.min(mCornerRadius, Math.min(w, h) / 2f);
        final float c = Math.min(mChamferSize, Math.min(w, h) / 2f);

        switch (mChamferCorner) {
            case CORNER_TR:
                mPath.moveTo(r, 0f);
                mPath.lineTo(w - c, 0f);
                mPath.lineTo(w, c);
                mPath.lineTo(w, h - r);
                mPath.quadTo(w, h, w - r, h);
                mPath.lineTo(r, h);
                mPath.quadTo(0f, h, 0f, h - r);
                mPath.lineTo(0f, r);
                mPath.quadTo(0f, 0f, r, 0f);
                break;
            case CORNER_BR:
                mPath.moveTo(r, 0f);
                mPath.lineTo(w - r, 0f);
                mPath.quadTo(w, 0f, w, r);
                mPath.lineTo(w, h - c);
                mPath.lineTo(w - c, h);
                mPath.lineTo(r, h);
                mPath.quadTo(0f, h, 0f, h - r);
                mPath.lineTo(0f, r);
                mPath.quadTo(0f, 0f, r, 0f);
                break;
            case CORNER_BL:
                mPath.moveTo(r, 0f);
                mPath.lineTo(w - r, 0f);
                mPath.quadTo(w, 0f, w, r);
                mPath.lineTo(w, h - r);
                mPath.quadTo(w, h, w - r, h);
                mPath.lineTo(c, h);
                mPath.lineTo(0f, h - c);
                mPath.lineTo(0f, r);
                mPath.quadTo(0f, 0f, r, 0f);
                break;
            case CORNER_TL:
            default:
                mPath.moveTo(c, 0f);
                mPath.lineTo(w - r, 0f);
                mPath.quadTo(w, 0f, w, r);
                mPath.lineTo(w, h - r);
                mPath.quadTo(w, h, w - r, h);
                mPath.lineTo(r, h);
                mPath.quadTo(0f, h, 0f, h - r);
                mPath.lineTo(0f, c);
                mPath.lineTo(c, 0f);
                break;
        }
        mPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!mPath.isEmpty()) {
            canvas.drawPath(mPath, mPaint);
        }
    }
}
