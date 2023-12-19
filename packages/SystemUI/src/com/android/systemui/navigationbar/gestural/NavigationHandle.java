/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.navigationbar.gestural;

import android.animation.ArgbEvaluator;
import android.annotation.ColorInt;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.navigationbar.buttons.ButtonInterface;
import com.android.systemui.tuner.TunerService;

public class NavigationHandle extends View implements ButtonInterface, TunerService.Tunable {

    private static final String GESTURE_NAVBAR_LENGTH_MODE =
            "system:" + "gesture_navbar_length_mode";
    private static final String GESTURE_NAVBAR_RADIUS =
            "system:" + "gesture_navbar_radius";

    protected final Paint mPaint = new Paint();
    private @ColorInt final int mLightColor;
    private @ColorInt final int mDarkColor;
    protected float mRadius;
    protected final float mBottom;
    private int mRadiusMultiplier;
    private int mWidthMultiplier;
    private boolean mRequiresInvalidate;

    public NavigationHandle(Context context) {
        this(context, null);
    }

    public NavigationHandle(Context context, AttributeSet attr) {
        super(context, attr);
        final Resources res = context.getResources();
        mBottom = res.getDimension(R.dimen.navigation_handle_bottom);

        final int dualToneDarkTheme = Utils.getThemeAttr(context, R.attr.darkIconTheme);
        final int dualToneLightTheme = Utils.getThemeAttr(context, R.attr.lightIconTheme);
        Context lightContext = new ContextThemeWrapper(context, dualToneLightTheme);
        Context darkContext = new ContextThemeWrapper(context, dualToneDarkTheme);
        mLightColor = Utils.getColorAttrDefaultColor(lightContext, R.attr.homeHandleColor);
        mDarkColor = Utils.getColorAttrDefaultColor(darkContext, R.attr.homeHandleColor);
        mPaint.setAntiAlias(true);
        setFocusable(false);
        Dependency.get(TunerService.class).addTunable(this, GESTURE_NAVBAR_LENGTH_MODE, GESTURE_NAVBAR_RADIUS);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (GESTURE_NAVBAR_LENGTH_MODE.equals(key)) {
            mWidthMultiplier = TunerService.parseInteger(newValue, 3);
        } else if (GESTURE_NAVBAR_RADIUS.equals(key)) {
            mRadiusMultiplier = TunerService.parseInteger(newValue, 1);
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (alpha > 0f && mRequiresInvalidate) {
            mRequiresInvalidate = false;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int navHeight = getHeight();
        int navWidth = getWidth();
        float baseWidth = getResources().getDimensionPixelSize(R.dimen.navigation_home_handle_width);
        float baseRadius = getResources().getDimensionPixelSize(R.dimen.navigation_handle_radius);
        float[] widthRange = {0, 0.66f, 1.3f};
        float widthMultiplier = widthRange[Math.min(mWidthMultiplier, widthRange.length - 1)];
        int mWidth = widthMultiplier == 0 ? 0 : (int) Math.ceil(baseWidth * widthMultiplier);
        mRadius = baseRadius * mRadiusMultiplier;
        float height = widthMultiplier == 0 ? 0 : mRadius * 2;
        float x = (navWidth - mWidth) / 2;
        float y = (navHeight - mBottom - height);
        canvas.drawRoundRect(x, y, x + mWidth, y + height, mRadius, mRadius, mPaint);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
    }

    @Override
    public void abortCurrentGesture() {
    }

    @Override
    public void setVertical(boolean vertical) {
    }

    @Override
    public void setDarkIntensity(float intensity) {
        int color = (int) ArgbEvaluator.getInstance().evaluate(intensity, mLightColor, mDarkColor);
        if (mPaint.getColor() != color) {
            mPaint.setColor(color);
            if (getVisibility() == VISIBLE && getAlpha() > 0) {
                invalidate();
            } else {
                // If we are currently invisible, then invalidate when we are next made visible
                mRequiresInvalidate = true;
            }
        }
    }

    @Override
    public void setDelayTouchFeedback(boolean shouldDelay) {
    }
}
