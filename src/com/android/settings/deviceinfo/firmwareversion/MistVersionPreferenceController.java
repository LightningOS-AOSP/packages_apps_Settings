package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

public class LightningVersionPreferenceController extends BasePreferenceController {

    @VisibleForTesting
    static final String LIGHTNING_VERSION_PROPERTY = "ro.lightning.version.base";
    static final String LIGHTNING_CODENAME_PROPERTY = "ro.lightning.codename";
    static final String LIGHTNING_PACKAGES = "ro.lightning.packagetype";

    public LightningVersionPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(SystemProperties.get(LIGHTNING_VERSION_PROPERTY)) && !TextUtils.isEmpty(SystemProperties.get(LIGHTNING_CODENAME_PROPERTY)) && !TextUtils.isEmpty(SystemProperties.get(LIGHTNING_PACKAGES))
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        String lightningVersion = SystemProperties.get(LIGHTNING_VERSION_PROPERTY);
        String lightningPackage = SystemProperties.get(LIGHTNING_PACKAGES);
        String lightningCodename = SystemProperties.get(LIGHTNING_CODENAME_PROPERTY);
        if (!lightningVersion.isEmpty() && !lightningCodename.isEmpty() && !lightningPackage.isEmpty()) {
            return lightningVersion + " | " + lightningCodename + " | " + lightningPackage;
        } else {
            return
                mContext.getString(R.string.device_info_default);
        }
    }
}
