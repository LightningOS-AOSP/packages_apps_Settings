/*
 * Copyright (C) 2026 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.widget;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class VlPreferenceCategory extends PreferenceCategory {

    public VlPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.vl_preference_category);
    }

    public VlPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayoutResource(R.layout.vl_preference_category);
    }

    public VlPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.vl_preference_category);
    }

    public VlPreferenceCategory(Context context) {
        super(context);
        setLayoutResource(R.layout.vl_preference_category);
    }

    private int getZoneColor() {
        final String key = getKey();
        if ("top_level_connectivity_category".equals(key)
                || "device_detail_category".equals(key)) {
            return R.color.vl_zone_connection;
        }
        if ("top_level_personalize_category".equals(key)) {
            return R.color.vl_zone_personalize;
        }
        if ("top_level_system_info_category".equals(key)) {
            return R.color.vl_zone_system;
        }
        if ("top_level_security_privacy_category".equals(key)
                || "legal_category".equals(key)) {
            return R.color.vl_zone_security;
        }
        if ("basic_info_category".equals(key)) {
            return R.color.vl_zone_system;
        }
        return R.color.vl_zone_support;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);
        final boolean hasCaption = !TextUtils.isEmpty(getTitle());
        final int color = getContext().getColor(getZoneColor());
        final View pill = holder.findViewById(R.id.vl_zone_pill);
        if (pill != null) {
            pill.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
            if (hasCaption) {
                pill.getBackground().mutate().setTint(color);
            }
        }
        final View title = holder.findViewById(android.R.id.title);
        if (title instanceof TextView) {
            title.setVisibility(hasCaption ? View.VISIBLE : View.GONE);
            ((TextView) title).setTextColor(color);
        }
    }
}
