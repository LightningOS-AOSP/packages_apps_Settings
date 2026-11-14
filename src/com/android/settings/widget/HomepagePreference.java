/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.widget;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settingslib.drawer.EntriesProvider;

/** A customized layout for homepage preference. */
public class HomepagePreference extends Preference implements
        HomepagePreferenceLayoutHelper.HomepagePreferenceLayout {

    private final HomepagePreferenceLayoutHelper mHelper;

    public HomepagePreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mHelper = new HomepagePreferenceLayoutHelper(this);
    }

    public HomepagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHelper = new HomepagePreferenceLayoutHelper(this);
    }

    public HomepagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHelper = new HomepagePreferenceLayoutHelper(this);
    }

    public HomepagePreference(Context context) {
        super(context);
        mHelper = new HomepagePreferenceLayoutHelper(this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHelper.onBindViewHolder(holder);
        applyGroupBackground(holder.itemView);
    }

    private void applyGroupBackground(View itemView) {
        final Context context = getContext();
        final PreferenceGroup parent = getParent();
        int index = 0;
        int count = 1;
        if (parent != null) {
            int visible = 0;
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                final Preference sibling = parent.getPreference(i);
                if (sibling == this) {
                    index = visible;
                }
                if (sibling.isVisible()) {
                    visible++;
                }
            }
            count = visible;
        }
        final boolean first = index == 0;
        final boolean last = index == count - 1;
        final float outer = context.getResources().getDimension(R.dimen.vl_group_radius_outer);
        final float inner = context.getResources().getDimension(R.dimen.vl_group_radius_inner);
        final float top = first ? outer : inner;
        final float bottom = last ? outer : inner;

        final GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(context.getColor(R.color.vl_card));
        shape.setCornerRadii(new float[] {
                top, top, top, top, bottom, bottom, bottom, bottom });

        final int gap = Math.round(context.getResources().getDisplayMetrics().density);
        final int side = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                16f, context.getResources().getDisplayMetrics()));
        final RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(context.getColor(R.color.vl_ripple)), shape, shape);
        itemView.setBackground(new InsetDrawable(ripple, side, gap, side, gap));
    }

    @Override
    public HomepagePreferenceLayoutHelper getHelper() {
        return mHelper;
    }

    /**
     * Set the alert count to show for this Preference.
     */
    public void setAlert(int value) {
        if (Flags.homepageTileAlert()) {
            Bundle extras = getExtras();
            int alertValue = extras.getInt(EntriesProvider.EXTRA_ALERT_VALUE, 0);
            if (value != alertValue) {
                mHelper.setAlert(value);
                extras.putInt(EntriesProvider.EXTRA_ALERT_VALUE, value);
            }
        }
    }
}
