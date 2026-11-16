/*
 * Copyright (C) 2026 LightningOS
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

package com.android.settings.deviceinfo.aboutphone;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settingslib.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Card grid shown when About phone is opened from the settings dashboard.
 */
public class LightningAboutFragment extends Fragment {

    private static final String TAG = "LightningAboutFragment";
    private static final String BASEBAND_PROPERTY = "gsm.version.baseband";
    private static final int EASTER_EGG_TAPS = 3;
    private static final int EASTER_EGG_WINDOW_MILLIS = 1000;
    private static final Uri SECURITY_BULLETIN_URI =
            Uri.parse("https://source.android.com/docs/security/bulletin/");

    private final long[] mVersionHits = new long[EASTER_EGG_TAPS];

    private ImageView mDeviceIcon;
    private TextView mUptime;
    private boolean mAttached;
    private ExecutorService mWallpaperExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lightning_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUptime = view.findViewById(R.id.uptime);
        mDeviceIcon = view.findViewById(R.id.device_icon);

        bindHeader(view);
        bindTiles(view);
        bindCards(view);

        bindValueDialogs(view);
        bindAndroidVersionEasterEgg(view);
        bindRename(view);
        loadWallpaper();

        final View more = view.findViewById(R.id.l_more_info);
        if (more != null) {
            more.setOnClickListener(v -> {
                final Intent intent = new Intent(Intent.ACTION_MAIN)
                        .setClassName(requireContext().getPackageName(),
                                com.android.settings.Settings.MyDeviceInfoActivity.class
                                        .getName());
                startActivity(intent);
            });
        }
    }

    private void bindRename(View root) {
        final View rename = root.findViewById(R.id.about_rename);
        if (rename == null) {
            return;
        }
        rename.setOnClickListener(v -> showRenameDialog());
    }

    private void showRenameDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final EditText input = new EditText(requireContext());
        final String current = Settings.Global.getString(
                requireContext().getContentResolver(), Settings.Global.DEVICE_NAME);
        input.setText(TextUtils.isEmpty(current) ? android.os.Build.MODEL : current);
        input.setSingleLine(true);
        final int padding = getResources().getDimensionPixelSize(R.dimen.about_dialog_padding);
        input.setPadding(padding, padding, padding, padding);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.vl_about_rename)
                .setView(input)
                .setPositiveButton(R.string.about_dialog_ok, (dialog, which) -> {
                    final String value = input.getText().toString().trim();
                    Settings.Global.putString(requireContext().getContentResolver(),
                            Settings.Global.DEVICE_NAME, value);
                    final View root = getView();
                    if (root != null) {
                        setText(root, R.id.device_name, value);
                        bindSub(root);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void loadWallpaper() {
        if (mDeviceIcon == null || getContext() == null) {
            return;
        }
        final Context appContext = requireContext().getApplicationContext();
        final ImageView target = mDeviceIcon;
        if (mWallpaperExecutor == null) {
            mWallpaperExecutor = Executors.newSingleThreadExecutor();
        }
        mWallpaperExecutor.execute(() -> {
            Drawable drawable = null;
            try {
                drawable = WallpaperManager.getInstance(appContext).getDrawable();
            } catch (RuntimeException e) {
                Log.w(TAG, "Wallpaper unavailable", e);
            }
            if (drawable == null) {
                return;
            }
            final Drawable wallpaper = drawable;
            target.post(() -> {
                target.setImageDrawable(wallpaper);
            });
        });
    }

    private void bindValueDialogs(View root) {
        bindValueDialog(root, R.id.l_nama_hape, R.string.about_label_device_model,
                R.id.device_name);
        bindHeaderDialog(root);
        bindValueDialog(root, R.id.l_build_number, R.string.about_label_build_number,
                R.id.build_number);
        bindValueDialog(root, R.id.l_kernel, R.string.about_label_kernel, R.id.kernel_info);
        bindValueDialog(root, R.id.l_selinux, R.string.about_label_selinux,
                R.id.selinux_status);
        bindValueDialog(root, R.id.l_base_band, R.string.about_label_base_band,
                R.id.base_band);
        bindValueDialog(root, R.id.l_ip_addres, R.string.about_label_ip_address,
                R.id.ip_address);
        bindValueDialog(root, R.id.l_mac_address, R.string.about_label_mac_address,
                R.id.mac_address);
        bindValueDialog(root, R.id.l_uptime_info, R.string.about_label_uptime, R.id.uptime);

        final View security = root.findViewById(R.id.l_secure);
        if (security != null) {
            security.setClickable(true);
            security.setFocusable(true);
            security.setOnClickListener(v -> openSecurityBulletin());
        }
    }

    private void openSecurityBulletin() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_VIEW).setData(SECURITY_BULLETIN_URI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            Log.w(TAG, "Security bulletin unavailable", e);
        }
    }

    private void bindAndroidVersionEasterEgg(View root) {
        final View card = root.findViewById(R.id.l_versi_android);
        if (card == null) {
            return;
        }
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> {
            System.arraycopy(mVersionHits, 1, mVersionHits, 0, mVersionHits.length - 1);
            mVersionHits[mVersionHits.length - 1] = SystemClock.uptimeMillis();
            if (mVersionHits[0] < SystemClock.uptimeMillis() - EASTER_EGG_WINDOW_MILLIS) {
                return;
            }
            launchEasterEgg();
        });
    }

    private void launchEasterEgg() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final android.os.UserManager userManager =
                getContext().getSystemService(android.os.UserManager.class);
        if (userManager != null && userManager.hasUserRestriction(
                android.os.UserManager.DISALLOW_FUN)) {
            Log.d(TAG, "Easter egg disabled by user restriction");
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .setClassName("android", "com.android.internal.app.PlatLogoActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start easter egg", e);
        }
    }

    private void bindHeaderDialog(View root) {
        final TextView version = root.findViewById(R.id.brand_version);
        if (version == null) {
            return;
        }
        version.setClickable(true);
        version.setFocusable(true);
        version.setOnClickListener(v -> showValueDialog(
                R.string.about_label_lightning_version, version.getText().toString()));
        version.setOnLongClickListener(v -> {
            copyValue(version.getText().toString());
            return true;
        });
    }

    private void bindValueDialog(View root, int cardId, int labelId, int valueId) {
        final View card = root.findViewById(cardId);
        final TextView value = root.findViewById(valueId);
        if (card == null || value == null) {
            return;
        }
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showValueDialog(labelId, value.getText().toString()));
        card.setOnLongClickListener(v -> {
            copyValue(value.getText().toString());
            return true;
        });
    }

    private void showValueTextDialog(String label, String value) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final TextView message = new TextView(requireContext());
        final int padding = getResources().getDimensionPixelSize(R.dimen.about_dialog_padding);
        message.setPadding(padding, padding, padding, padding);
        message.setGravity(android.view.Gravity.CENTER
                | android.view.Gravity.CENTER_VERTICAL);
        message.setTextColor(Utils.getColorAttrDefaultColor(requireContext(),
                android.R.attr.textColorPrimary));
        message.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        message.setTypeface(null, android.graphics.Typeface.BOLD);
        message.setText(value);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(label)
                .setView(message)
                .setPositiveButton(R.string.about_dialog_ok, null)
                .show();
    }

    private void showValueDialog(int labelId, String value) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final TextView message = new TextView(requireContext());
        final int padding = getResources().getDimensionPixelSize(R.dimen.about_dialog_padding);
        message.setPadding(padding, padding, padding, padding);
        message.setGravity(android.view.Gravity.CENTER
                | android.view.Gravity.CENTER_VERTICAL);
        message.setTextColor(Utils.getColorAttrDefaultColor(requireContext(),
                android.R.attr.textColorPrimary));
        message.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
        message.setTypeface(null, android.graphics.Typeface.BOLD);
        message.setText(value);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(labelId)
                .setView(message)
                .setPositiveButton(R.string.about_dialog_ok, null)
                .show();
    }

    private void bindHeader(View root) {
        setText(root, R.id.brand_version, prop("ro.lightning.version.base"));
        setText(root, R.id.about_package, prop("ro.lightning.packagetype").toUpperCase());
        setText(root, R.id.about_build_type, prop("ro.lightning.buildtype").toUpperCase());
        setText(root, R.id.about_build_id, lightningBuildId());
        bindSub(root);
    }

    private void bindSub(View root) {
        final TextView sub = root.findViewById(R.id.about_sub);
        if (sub == null) {
            return;
        }
        final String release = android.os.Build.VERSION.RELEASE;
        final String base = prop("ro.lightning.version.base");
        final String text = TextUtils.isEmpty(base)
                ? getString(R.string.vl_about_android_fmt, release)
                : getString(R.string.vl_about_android_version_fmt, release, base);
        sub.setText(text);
    }

    private String lightningBuildId() {
        final String display = android.os.Build.DISPLAY;
        return TextUtils.isEmpty(display) ? android.os.Build.ID : display;
    }

    private void bindTiles(View root) {
        bindTile(root, R.id.l_processor, R.id.processor_info, prop("ro.lightning.soc"));
        bindTile(root, R.id.l_platform, R.id.chipset_info, prop("ro.lightning.platform"));
        bindTile(root, R.id.l_memory, R.id.ram_info, totalRam());
        bindTile(root, R.id.l_screen, R.id.screen_info, firstNonEmpty(
                prop("ro.lightning.screen"), prop("ro.lightning.display")));
        bindTile(root, R.id.l_battery_spec, R.id.battery_spec, prop("ro.lightning.battery"));
        bindTile(root, R.id.l_display_spec, R.id.display_spec, prop("ro.lightning.display"));
        bindTile(root, R.id.l_camera_spec, R.id.camera_spec, cameraSpec());
        bindTile(root, R.id.l_maintainer, R.id.maintainer, maintainer(),
                R.string.about_label_maintainer);
    }

    private String cameraSpec() {
        final String rear = prop("ro.lightning.camera");
        if (TextUtils.isEmpty(rear)) {
            return null;
        }
        final String front = prop("ro.lightning.front");
        if (TextUtils.isEmpty(front)) {
            return rear;
        }
        return getString(R.string.vl_about_camera_fmt, rear, front);
    }

    private void bindTile(View root, int containerId, int valueId, String value) {
        bindTile(root, containerId, valueId, value, R.string.vl_about_this_device);
    }

    private void bindTile(View root, int containerId, int valueId, String value, int labelId) {
        final TextView view = root.findViewById(valueId);
        final View container = root.findViewById(containerId);
        if (view == null || container == null) {
            return;
        }
        if (TextUtils.isEmpty(value)) {
            container.setVisibility(View.GONE);
            return;
        }
        view.setText(value);
        container.setClickable(true);
        container.setFocusable(true);
        container.setOnClickListener(v -> showValueTextDialog(getString(labelId), value));
        container.setOnLongClickListener(v -> {
            copyValue(value);
            return true;
        });
    }

    private void copyValue(String value) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        final ClipboardManager clipboard =
                requireContext().getSystemService(ClipboardManager.class);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.vl_about_this_device),
                value));
        Toast.makeText(requireContext(), R.string.vl_about_copied, Toast.LENGTH_SHORT).show();
    }

    private void bindCards(View root) {
        setText(root, R.id.device_name, deviceName());
        setText(root, R.id.android_version, androidVersion());
        setText(root, R.id.build_number, android.os.Build.DISPLAY);
        setText(root, R.id.security_update, android.os.Build.VERSION.SECURITY_PATCH);
        setText(root, R.id.kernel_info, System.getProperty("os.version", ""));
        setText(root, R.id.selinux_status, selinuxStatus());
        setText(root, R.id.maintainer, maintainer());
        setText(root, R.id.base_band, baseBand());
        setText(root, R.id.ip_address, ipAddress());
        setText(root, R.id.mac_address, wifiMac());
        setText(root, R.id.more, getString(R.string.vl_about_more));
        applySelinuxPill(root);
    }

    private void applySelinuxPill(View root) {
        final TextView pill = root.findViewById(R.id.selinux_status);
        if (pill == null) {
            return;
        }
        final boolean enforcing;
        try {
            enforcing = SELinux.isSELinuxEnforced();
        } catch (RuntimeException e) {
            Log.w(TAG, "SELinux status unavailable", e);
            return;
        }
        pill.setBackgroundResource(enforcing
                ? R.drawable.vl_selinux_enforcing : R.drawable.vl_selinux_permissive);
        pill.setTextColor(requireContext().getColor(enforcing
                ? R.color.vl_on_volt : R.color.vl_selinux_permissive_text));
    }

    private void setText(View root, int id, String value) {
        final TextView view = root.findViewById(id);
        if (view == null) {
            return;
        }
        view.setText(TextUtils.isEmpty(value)
                ? getString(R.string.device_info_default) : value);
    }

    private static String marketRamSize(double gib) {
        final double[] sizes = {1, 1.5, 2, 2.5, 3, 4, 6, 8, 10, 12, 16, 18, 24, 32, 48, 64};
        double best = sizes[0];
        for (double size : sizes) {
            if (Math.abs(size - gib) < Math.abs(best - gib)) {
                best = size;
            }
        }
        return best == Math.floor(best)
                ? String.valueOf((long) best) : String.valueOf(best);
    }

    private String totalRam() {
        try {
            final android.app.ActivityManager am =
                    requireContext().getSystemService(android.app.ActivityManager.class);
            if (am == null) {
                return null;
            }
            final android.app.ActivityManager.MemoryInfo info =
                    new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            if (info.totalMem <= 0L) {
                return null;
            }
            final double gib = info.totalMem / 1024d / 1024d / 1024d;
            return marketRamSize(gib) + " GB";
        } catch (RuntimeException e) {
            Log.w(TAG, "Total RAM unavailable", e);
            return null;
        }
    }

    private String prop(String key) {
        return SystemProperties.get(key, "").trim();
    }

    private static String firstNonEmpty(String first, String second) {
        if (!TextUtils.isEmpty(first)) {
            return first;
        }
        return TextUtils.isEmpty(second) ? null : second;
    }

    private String deviceName() {
        final String name = Settings.Global.getString(
                requireContext().getContentResolver(), Settings.Global.DEVICE_NAME);
        final String model = TextUtils.isEmpty(name) ? android.os.Build.MODEL : name;
        if (TextUtils.isEmpty(model)) {
            return null;
        }
        final String codename = android.os.Build.DEVICE;
        return TextUtils.isEmpty(codename) ? model : model + " | " + codename;
    }

    private String maintainer() {
        final String maintainer = prop("ro.lightningos.maintainer");
        final String status = prop("ro.lightning.buildtype");
        if (TextUtils.isEmpty(maintainer)) {
            return null;
        }
        return TextUtils.isEmpty(status) ? maintainer : maintainer + " | " + status;
    }

    private String androidVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    private String selinuxStatus() {
        try {
            return SELinux.isSELinuxEnforced()
                    ? getString(R.string.selinux_enforcing)
                    : getString(R.string.selinux_permissive);
        } catch (RuntimeException e) {
            Log.w(TAG, "SELinux status unavailable", e);
            return null;
        }
    }

    private String baseBand() {
        final String value = SystemProperties.get(BASEBAND_PROPERTY, "");
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        final String[] parts = value.split(",");
        return parts.length > 0 ? parts[0].trim() : value.trim();
    }

    private String ipAddress() {
        try {
            final ConnectivityManager cm =
                    requireContext().getSystemService(ConnectivityManager.class);
            if (cm == null) {
                return null;
            }
            final Network network = cm.getActiveNetwork();
            final LinkProperties lp = network == null ? null : cm.getLinkProperties(network);
            if (lp == null) {
                return null;
            }
            for (LinkAddress address : lp.getLinkAddresses()) {
                final InetAddress inet = address.getAddress();
                if (inet instanceof Inet4Address) {
                    return inet.getHostAddress();
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "IP address unavailable", e);
        }
        return null;
    }

    private String wifiMac() {
        try {
            final WifiManager wm = requireContext().getSystemService(WifiManager.class);
            if (wm == null) {
                return null;
            }
            final WifiInfo info = wm.getConnectionInfo();
            if (info != null && !TextUtils.isEmpty(info.getMacAddress())) {
                return info.getMacAddress();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Wi-Fi MAC unavailable", e);
        }
        return null;
    }


    private void updateUptime() {
        if (mUptime != null) {
            mUptime.setText(DateUtils.formatElapsedTime(SystemClock.elapsedRealtime() / 1000));
        }
    }

    private final Runnable mUptimeUpdater = new Runnable() {
        @Override
        public void run() {
            if (!mAttached) {
                return;
            }
            updateUptime();
            if (mUptime != null) {
                mUptime.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        mAttached = true;
        updateUptime();
        if (mUptime != null) {
            mUptime.postDelayed(mUptimeUpdater, 1000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mAttached = false;
        if (mUptime != null) {
            mUptime.removeCallbacks(mUptimeUpdater);
        }
    }
}
