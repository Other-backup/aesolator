package com.winlator.cmod;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.winlator.cmod.contentdialog.ContentDialog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.ImageUtils;
import com.winlator.cmod.core.LaunchSecurity;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.WineThemeManager;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerData;
import com.winlator.cmod.container.ContainerUtils;
import com.winlator.cmod.container.IntentLaunchManager;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final @IntRange(from = 1, to = 19) byte CONTAINER_PATTERN_COMPRESSION_LEVEL = 9;
    public static final byte PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    public static final byte OPEN_FILE_REQUEST_CODE = 2;
    public static final byte EDIT_INPUT_CONTROLS_REQUEST_CODE = 3;
    public static final byte OPEN_DIRECTORY_REQUEST_CODE = 4;
    public static final byte OPEN_IMAGE_REQUEST_CODE = 5;
    public static final byte OPEN_DRIVER_PACKAGE_REQUEST_CODE = 6;

    public final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
    private static Callback<Bitmap> imagePickerCallback;
    private static Callback<Uri> driverPackagePickerCallback;

    private boolean editInputControls = false;
    private int selectedProfileId;
    private SharedPreferences sharedPreferences;
    private boolean isDarkMode;
    private boolean pendingAllFilesAccessPrompt = false;
    private boolean pendingNotificationPermissionPrompt = false;
    private long lastExitBackPressAtMs = 0L;
    @Nullable
    private Intent pendingExternalLaunchIntent;
    private boolean externalLaunchInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep app-private prefix-pack assets current even before the runtime drawer is opened.
        ImageFsInstaller.ensurePrefixPackToolkit(this, ImageFs.find(this));

        Intent startupIntent = getIntent();
        pendingExternalLaunchIntent = IntentLaunchManager.INSTANCE.parseLaunchIntent(startupIntent) != null
                ? new Intent(startupIntent)
                : null;

        SharedPreferences startupPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isBigPictureModeEnabled = startupPreferences.getBoolean("enable_big_picture_mode", false);
        if (pendingExternalLaunchIntent == null && isBigPictureModeEnabled) {
            startActivity(new Intent(MainActivity.this, BigPictureActivity.class));
            finish();
            return;
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        setTheme(isDarkMode ? R.style.AppTheme_Dark : R.style.AppTheme);

        setContentView(R.layout.main_activity);
        setSupportActionBar(findViewById(R.id.Toolbar));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
        }

        File winlatorDir = new File(SettingsFragment.DEFAULT_WINLATOR_PATH);
        if (!winlatorDir.exists()) winlatorDir.mkdirs();

        Intent intent = startupIntent;
        editInputControls = intent.getBooleanExtra("edit_input_controls", false);
        if (editInputControls) {
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            openMainMenuItem(R.id.main_menu_input_controls, false);
        } else {
            boolean permissionsRequested = requestAppPermissions();
            if (!permissionsRequested) {
                ImageFsInstaller.installIfNeeded(this);
            }

            pendingAllFilesAccessPrompt =
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager();
            pendingNotificationPermissionPrompt =
                    Build.VERSION.SDK_INT >= 33
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED;

            if (consumePendingExternalLaunchIntent()) {
                updateActionBarNavigationState();
                applyThemeChrome();
                applyThemeAssetTintPass();
                return;
            }

            if (pendingExternalLaunchIntent == null) {
                int selectedMenuItemId = intent.getIntExtra("selected_menu_item_id", 0);
                if (selectedMenuItemId > 0) {
                    openMainMenuItem(selectedMenuItemId, false);
                } else {
                    openPreferredStartupSurface(false);
                }
            }
        }

        updateActionBarNavigationState();
        applyThemeChrome();
        applyThemeAssetTintPass();
    }

    private void showAllFilesAccessDialog() {
        ForensicLogger.logEvent(
                this,
                "info",
                "STORAGE_ALL_FILES_ACCESS_PROMPT",
                null,
                "main",
                "all_files_access_prompt_shown",
                ForensicLogger.fields(
                        "sdk_int", Build.VERSION.SDK_INT,
                        "is_external_storage_manager", Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
                )
        );

        ContentDialog dialog = new ContentDialog(this);
        dialog.setTitle(R.string.all_files_access_required);
        dialog.setMessage(R.string.all_files_access_required_message);
        dialog.setIcon(R.drawable.ae_icon_settings);
        ((TextView) dialog.findViewById(R.id.BTConfirm)).setText(R.string.open_settings);
        dialog.setOnConfirmCallback(() -> {
            ForensicLogger.logEvent(
                    this,
                    "info",
                    "STORAGE_ALL_FILES_ACCESS_OPEN_SETTINGS",
                    null,
                    "main",
                    "all_files_access_open_settings",
                    ForensicLogger.fields("sdk_int", Build.VERSION.SDK_INT)
            );
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        dialog.setOnCancelCallback(() -> ForensicLogger.logEvent(
                this,
                "warn",
                "STORAGE_ALL_FILES_ACCESS_DECLINED",
                null,
                "main",
                "all_files_access_prompt_declined",
                ForensicLogger.fields("sdk_int", Build.VERSION.SDK_INT)
        ));
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ImageFsInstaller.installIfNeeded(this);
                if (consumePendingExternalLaunchIntent()) {
                    return;
                }
            } else {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (editInputControls) {
            super.onBackPressed();
            return;
        }
        if (isHomeDashboardVisible()) {
            if (sharedPreferences != null && sharedPreferences.getBoolean("warn_before_exit", false)) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastExitBackPressAtMs > 2000L) {
                    lastExitBackPressAtMs = now;
                    AppUtils.showToast(this, R.string.back_press_exit_warning);
                    return;
                }
            }
            finish();
            return;
        }
        showHomeDashboard(true);
    }

    private boolean requestAppPermissions() {
        if (hasRequiredAppPermissions()) {
            return false;
        }

        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!hasWritePermission || !hasReadPermission) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        return true;
    }

    private boolean hasRequiredAppPermissions() {
        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasManageStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
        return hasWritePermission && hasReadPermission && hasManageStoragePermission;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            if (editInputControls) {
                onBackPressed();
                return true;
            }
            if (!isHomeDashboardVisible()) {
                showHomeDashboard(true);
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void openMainMenuItem(int menuItemId, boolean reverse) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        switch (menuItemId) {
            case R.id.main_menu_shortcuts:
                show(new ShortcutsFragment(), reverse);
                break;
            case R.id.main_menu_containers:
                show(new ContainersFragment(), reverse);
                break;
            case R.id.main_menu_new_container:
                openNewContainerFlow();
                break;
            case R.id.main_menu_input_controls:
                show(new InputControlsFragment(selectedProfileId), reverse);
                break;
            case R.id.main_menu_big_picture:
                openBigPictureMode();
                break;
            case R.id.main_menu_contents:
                show(new ContentsFragment(), reverse);
                break;
            case R.id.main_menu_adrenotools_gpu_drivers:
                show(new AdrenotoolsFragment(), reverse);
                break;
            case R.id.main_menu_diagnostics:
                show(new ForensicCenterFragment(), reverse);
                break;
            case R.id.main_menu_settings:
                show(new SettingsFragment(), reverse);
                break;
            case R.id.main_menu_about:
                showAboutDialog();
                break;
            default:
                showHomeDashboard(reverse);
                return;
        }
    }

    private void openNewContainerFlow() {
        if (!ImageFs.find(this).isValid()) {
            showHomeDashboard(false);
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                .replace(R.id.FLFragmentContainer, new ContainerDetailFragment())
                .commit();
        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            contentRoot.post(() -> {
                updateActionBarNavigationState();
                applyThemeChrome();
                applyThemeAssetTintPass();
            });
        }
    }

    private void openBigPictureMode() {
        startActivity(new Intent(this, BigPictureActivity.class));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public void showHomeDashboard(boolean reverse) {
        show(new MainMenuGridFragment(), reverse);
    }

    private void openPreferredStartupSurface(boolean reverse) {
        if (sharedPreferences != null && sharedPreferences.getBoolean("show_shortcuts_first", false)) {
            show(new ShortcutsFragment(), reverse);
            return;
        }
        showHomeDashboard(reverse);
    }

    private void show(Fragment fragment, boolean reverse) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (reverse) {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_down, R.anim.slide_out_up)
                    .replace(R.id.FLFragmentContainer, fragment)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                    .replace(R.id.FLFragmentContainer, fragment)
                    .commit();
        }

        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            contentRoot.post(() -> {
                updateActionBarNavigationState();
                applyThemeChrome();
                applyThemeAssetTintPass();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        consumePendingExternalLaunchIntent();
        boolean latestDark = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_mode", false);
        if (latestDark != isDarkMode) isDarkMode = latestDark;
        View decor = getWindow() != null ? getWindow().getDecorView() : null;
        if (decor != null && decor.getAlpha() != 1f) decor.setAlpha(1f);
        updateActionBarNavigationState();
        applyThemeChrome();
        applyThemeAssetTintPass();
        showDeferredStartupPromptsIfNeeded();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        pendingExternalLaunchIntent = IntentLaunchManager.INSTANCE.parseLaunchIntent(intent) != null
                ? new Intent(intent)
                : null;

        if (consumePendingExternalLaunchIntent()) {
            updateActionBarNavigationState();
            applyThemeChrome();
            applyThemeAssetTintPass();
            return;
        }

        editInputControls = intent.getBooleanExtra("edit_input_controls", false);
        if (editInputControls) {
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            openMainMenuItem(R.id.main_menu_input_controls, false);
        } else {
            int selectedMenuItemId = intent.getIntExtra("selected_menu_item_id", 0);
            if (selectedMenuItemId > 0) {
                openMainMenuItem(selectedMenuItemId, false);
            } else {
                openPreferredStartupSurface(false);
            }
        }

        updateActionBarNavigationState();
        applyThemeChrome();
        applyThemeAssetTintPass();
    }

    private boolean isHomeDashboardVisible() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.FLFragmentContainer);
        return current instanceof MainMenuGridFragment;
    }

    private void showDeferredStartupPromptsIfNeeded() {
        if (editInputControls || externalLaunchInProgress || pendingExternalLaunchIntent != null || !isHomeDashboardVisible()) return;

        if (pendingAllFilesAccessPrompt) {
            pendingAllFilesAccessPrompt = false;
            showAllFilesAccessDialog();
            return;
        }

        if (pendingNotificationPermissionPrompt) {
            pendingNotificationPermissionPrompt = false;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }
    }

    private void updateActionBarNavigationState() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) return;
        boolean showBack = editInputControls || !isHomeDashboardVisible();
        actionBar.setDisplayHomeAsUpEnabled(showBack);
        if (showBack) {
            actionBar.setHomeAsUpIndicator(R.drawable.ae_icon_back);
        }
    }

    private boolean consumePendingExternalLaunchIntent() {
        if (externalLaunchInProgress || pendingExternalLaunchIntent == null) return false;
        if (!hasRequiredAppPermissions()) return false;

        Intent launchIntent = pendingExternalLaunchIntent;
        pendingExternalLaunchIntent = null;
        IntentLaunchManager.LaunchRequest launchRequest = IntentLaunchManager.INSTANCE.parseLaunchIntent(launchIntent);
        if (launchRequest == null) return false;

        externalLaunchInProgress = true;
        preloaderDialog.show(R.string.creating_container);
        Thread launchThread = new Thread(() -> processExternalLaunchRequest(launchRequest), "aeso-external-launch");
        launchThread.setDaemon(true);
        launchThread.start();
        return true;
    }

    private void processExternalLaunchRequest(IntentLaunchManager.LaunchRequest launchRequest) {
        String appId = launchRequest.getAppId();
        ContainerData configOverride = launchRequest.getContainerConfig();
        boolean temporaryOverrideActive = false;

        try {
            if (configOverride != null) {
                IntentLaunchManager.INSTANCE.applyTemporaryConfigOverride(this, appId, configOverride);
                temporaryOverrideActive = true;
            } else if (IntentLaunchManager.INSTANCE.hasTemporaryOverride(appId)) {
                IntentLaunchManager.INSTANCE.restoreOriginalConfiguration(this, appId);
                IntentLaunchManager.INSTANCE.clearTemporaryOverride(appId);
            }

            Container container = temporaryOverrideActive
                    ? ContainerUtils.INSTANCE.getOrCreateContainerWithOverride(this, appId)
                    : ContainerUtils.INSTANCE.getOrCreateContainer(this, appId);

            Intent xserverIntent = new Intent(this, XServerDisplayActivity.class);
            xserverIntent.putExtra("container_id", container.id);
            xserverIntent.putExtra(LaunchSecurity.EXTRA_APP_ID, appId);
            xserverIntent.putExtra(LaunchSecurity.EXTRA_LAUNCH_ROUTE_TOKEN, buildExternalLaunchRouteToken(appId));
            if (temporaryOverrideActive) {
                xserverIntent.putExtra(LaunchSecurity.EXTRA_TEMP_OVERRIDE_APP_ID, appId);
            }
            LaunchSecurity.signXServerLaunchIntent(this, xserverIntent);

            boolean finalTemporaryOverrideActive = temporaryOverrideActive;
            runOnUiThread(() -> {
                externalLaunchInProgress = false;
                preloaderDialog.close();
                startActivity(xserverIntent);
                if (!finalTemporaryOverrideActive) {
                    IntentLaunchManager.INSTANCE.clearTemporaryOverride(appId);
                }
                finish();
            });
        } catch (Exception error) {
            if (temporaryOverrideActive) {
                IntentLaunchManager.INSTANCE.restoreOriginalConfiguration(this, appId);
                IntentLaunchManager.INSTANCE.clearTemporaryOverride(appId);
            }
            ForensicLogger.logEvent(
                    this,
                    "error",
                    "MAIN_EXTERNAL_LAUNCH_FAILED",
                    null,
                    "main",
                    "external_launch_failed",
                    ForensicLogger.fields(
                            "app_id", appId,
                            "error_class", error.getClass().getName(),
                            "error_detail", String.valueOf(error.getMessage())
                    )
            );
            runOnUiThread(() -> {
                externalLaunchInProgress = false;
                preloaderDialog.close();
                AppUtils.showToast(this, "Failed to prepare external launch");
                if (!isFinishing()) {
                    showHomeDashboard(false);
                    updateActionBarNavigationState();
                }
            });
        }
    }

    private String buildExternalLaunchRouteToken(String appId) {
        return appId + "|" + SystemClock.elapsedRealtimeNanos();
    }

    private void applyThemeAssetTintPass() {
        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            ThemeAssetPainter.apply(this, contentRoot, isDarkMode);
        }
    }

    private void applyThemeChrome() {
        Toolbar toolbar = findViewById(R.id.Toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(ContextCompat.getColor(this, isDarkMode ? R.color.colorPrimaryDarkMode : R.color.colorPrimary));
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.surface_table_head_text));
        }

        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            contentRoot.setBackgroundColor(ContextCompat.getColor(this, isDarkMode ? R.color.window_background_color_dark : R.color.window_background_color));
        }

        if (getWindow() != null) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, isDarkMode ? R.color.colorPrimaryDarkModeDark : R.color.colorPrimaryDark));
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, isDarkMode ? R.color.window_background_color_dark : R.color.window_background_color));
        }
    }

    @Nullable
    private Fragment createFreshInstanceForCurrentFragment(@Nullable Fragment current) {
        if (current instanceof MainMenuGridFragment) return new MainMenuGridFragment();
        if (current instanceof ShortcutsFragment) return new ShortcutsFragment();
        if (current instanceof ContainersFragment) return new ContainersFragment();
        if (current instanceof InputControlsFragment) return new InputControlsFragment(selectedProfileId);
        if (current instanceof ContentsFragment) return new ContentsFragment();
        if (current instanceof AdrenotoolsFragment) return new AdrenotoolsFragment();
        if (current instanceof ForensicCenterFragment) return new ForensicCenterFragment();
        if (current instanceof SettingsFragment) return new SettingsFragment();
        return null;
    }

    public void applyThemeModeLive(boolean darkMode) {
        isDarkMode = darkMode;
        if (sharedPreferences != null) {
            sharedPreferences.edit().putBoolean("dark_mode", darkMode).apply();
        }
        setTheme(darkMode ? R.style.AppTheme_Dark : R.style.AppTheme);

        View contentRoot = findViewById(android.R.id.content);
        if (contentRoot != null) {
            contentRoot.animate().cancel();
            contentRoot.setAlpha(0.94f);
        }

        applyThemeChrome();

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.FLFragmentContainer);
        Fragment replacement = createFreshInstanceForCurrentFragment(current);
        if (replacement != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.FLFragmentContainer, replacement)
                    .commit();
        }

        Runnable finishThemeApply = () -> {
            updateActionBarNavigationState();
            applyThemeAssetTintPass();
            if (contentRoot != null) {
                contentRoot.animate().alpha(1f).setDuration(180L).start();
            }
        };

        if (contentRoot != null) {
            contentRoot.post(finishThemeApply);
        } else {
            finishThemeApply.run();
        }
    }

    private void showAboutDialog() {
        ContentDialog dialog = new ContentDialog(this, R.layout.about_dialog);
        dialog.setTitle(R.string.about);
        dialog.setIcon(R.drawable.ae_icon_about);
        dialog.findViewById(R.id.LLBottomBar).setVisibility(View.GONE);

        dialog.getWindow().setBackgroundDrawableResource(
                isDarkMode ? R.drawable.surface_dialog_background_dark : R.drawable.surface_dialog_background
        );

        int panelBackground = isDarkMode ? R.drawable.surface_card_background_dark : R.drawable.surface_card_background;
        int badgeBackground = isDarkMode ? R.drawable.surface_badge_background_dark : R.drawable.surface_badge_background;
        int badgeTextColor = ContextCompat.getColor(this, isDarkMode ? R.color.surface_badge_text_dark : R.color.surface_badge_text);
        int bodyTextColor = ContextCompat.getColor(this, isDarkMode ? R.color.surface_body_text_dark : R.color.surface_body_text);

        LinearLayout llAboutHeaderCard = dialog.findViewById(R.id.LLAboutHeaderCard);
        LinearLayout llAboutCreditsCard = dialog.findViewById(R.id.LLAboutCreditsCard);
        LinearLayout llAboutRuntimeCard = dialog.findViewById(R.id.LLAboutRuntimeCard);
        TextView tvAboutAppSummary = dialog.findViewById(R.id.TVAboutAppSummary);
        TextView tvAboutCreditsLabel = dialog.findViewById(R.id.TVAboutCreditsLabel);
        TextView tvAboutRuntimeLabel = dialog.findViewById(R.id.TVAboutRuntimeLabel);
        if (llAboutHeaderCard != null) llAboutHeaderCard.setBackgroundResource(panelBackground);
        if (tvAboutAppSummary != null) tvAboutAppSummary.setTextColor(bodyTextColor);
        if (tvAboutCreditsLabel != null) {
            tvAboutCreditsLabel.setBackgroundResource(badgeBackground);
            tvAboutCreditsLabel.setTextColor(badgeTextColor);
        }
        if (tvAboutRuntimeLabel != null) {
            tvAboutRuntimeLabel.setBackgroundResource(badgeBackground);
            tvAboutRuntimeLabel.setTextColor(badgeTextColor);
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            TextView tvWebpage = dialog.findViewById(R.id.TVWebpage);
            tvWebpage.setText(Html.fromHtml("<a href=\"https://github.com/kosoymiki/aesolator\">" + getString(R.string.about_project_url) + "</a>", Html.FROM_HTML_MODE_LEGACY));
            tvWebpage.setMovementMethod(LinkMovementMethod.getInstance());

            ((TextView) dialog.findViewById(R.id.TVAppVersion)).setText(getString(R.string.about_version_format, pInfo.versionName));

            String creditsAndThirdPartyAppsHTML = String.join("<br />",
                    "<b>Ae.solator</b> by Ae team",
                    "<b>Product line:</b> Ae.solator + FreeWine11 + WCP Archive",
                    "---",
                    "<b>Primary Wine / Proton source lines</b>",
                    "WineHQ / Wine upstream (<a href=\"https://www.winehq.org\">winehq.org</a>)",
                    "Valve Proton / proton-wine 11 (<a href=\"https://github.com/ValveSoftware/wine\">github.com/ValveSoftware/wine</a>)",
                    "AndreRH Wine ARM64EC / Hangover references (<a href=\"https://github.com/AndreRH/wine\">github.com/AndreRH/wine</a>)",
                    "GameNative Wine / Proton Android-facing line (<a href=\"https://github.com/GameNative/wine\">github.com/GameNative/wine</a>)",
                    "Valve Proton packaging context (<a href=\"https://github.com/ValveSoftware/Proton\">github.com/ValveSoftware/Proton</a>)",
                    "---",
                    "<b>Android app and runtime donor lines</b>",
                    "BrunoSX / Winlator and WFM (<a href=\"https://github.com/brunodev85/winlator\">github.com/brunodev85/winlator</a>)",
                    "SEGAINDEED / winlator-bionic-vortek (<a href=\"https://github.com/SEGAINDEED/winlator-bionic-vortek\">github.com/SEGAINDEED/winlator-bionic-vortek</a>)",
                    "Xnick417x / Winlator-Bionic-Nightly-wcp (<a href=\"https://github.com/Xnick417x/Winlator-Bionic-Nightly-wcp\">github.com/Xnick417x/Winlator-Bionic-Nightly-wcp</a>)",
                    "Pipetto-crypto / gladiorenderer (<a href=\"https://github.com/Pipetto-crypto/gladiorenderer\">github.com/Pipetto-crypto/gladiorenderer</a>)",
                    "leegao / bionic-vulkan-wrapper (<a href=\"https://github.com/leegao/bionic-vulkan-wrapper\">github.com/leegao/bionic-vulkan-wrapper</a>)",
                    "---",
                    "<b>Runtime components and graphics providers</b>",
                    "Mesa / Panfrost / PanVK / Lima / Turnip / Zink / VirGL (<a href=\"https://gitlab.freedesktop.org/mesa/mesa\">gitlab.freedesktop.org/mesa/mesa</a>)",
                    "VirGLRenderer (<a href=\"https://gitlab.freedesktop.org/virgl/virglrenderer\">gitlab.freedesktop.org/virgl/virglrenderer</a>)",
                    "DXVK (<a href=\"https://github.com/doitsujin/dxvk\">github.com/doitsujin/dxvk</a>)",
                    "VKD3D-Proton (<a href=\"https://github.com/HansKristian-Work/vkd3d-proton\">github.com/HansKristian-Work/vkd3d-proton</a>)",
                    "dgVoodoo2 (<a href=\"https://github.com/dege-diosg/dgVoodoo2\">github.com/dege-diosg/dgVoodoo2</a>)",
                    "FEX-Emu (<a href=\"https://github.com/FEX-Emu/FEX\">github.com/FEX-Emu/FEX</a>)",
                    "Box64 (<a href=\"https://github.com/ptitSeb/box64\">github.com/ptitSeb/box64</a>)",
                    "libadrenotools (<a href=\"https://github.com/bylaws/libadrenotools\">github.com/bylaws/libadrenotools</a>)",
                    "Termux Packages (<a href=\"https://github.com/termux/termux-packages\">github.com/termux/termux-packages</a>)",
                    "---",
                    "<b>Graphics package feeds</b>",
                    "GameNative Drivers (<a href=\"https://gamenative.app/drivers/\">gamenative.app/drivers</a>)",
                    "StevenMXZ Turnip CI (<a href=\"https://github.com/StevenMXZ/freedreno_turnip-CI\">github.com/StevenMXZ/freedreno_turnip-CI</a>)",
                    "whitebelyash Turnip CI (<a href=\"https://github.com/whitebelyash/freedreno_turnip-CI\">github.com/whitebelyash/freedreno_turnip-CI</a>)",
                    "MrPurple Turnip (<a href=\"https://github.com/MrPurple666/purple-turnip\">github.com/MrPurple666/purple-turnip</a>)"
            );

            TextView tvCreditsAndThirdPartyApps = dialog.findViewById(R.id.TVCreditsAndThirdPartyApps);
            tvCreditsAndThirdPartyApps.setText(Html.fromHtml(creditsAndThirdPartyAppsHTML, Html.FROM_HTML_MODE_LEGACY));
            tvCreditsAndThirdPartyApps.setMovementMethod(LinkMovementMethod.getInstance());
            tvCreditsAndThirdPartyApps.setTextColor(bodyTextColor);

            String glibcExpVersionForkHTML = String.join("<br />",
                    getString(R.string.about_runtime_summary),
                    getString(R.string.about_runtime_detail));
            TextView tvGlibcExpVersionFork = dialog.findViewById(R.id.TVGlibcExpVersionFork);
            tvGlibcExpVersionFork.setText(Html.fromHtml(glibcExpVersionForkHTML, Html.FROM_HTML_MODE_LEGACY));
            tvGlibcExpVersionFork.setMovementMethod(LinkMovementMethod.getInstance());
            tvGlibcExpVersionFork.setTextColor(bodyTextColor);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        dialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_IMAGE_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap bitmap = ImageUtils.getBitmapFromUri(this, data.getData(), 1280);
            if (bitmap == null) return;
            Callback<Bitmap> callback = consumeImagePickerCallback();
            if (callback != null) {
                callback.call(bitmap);
                return;
            }
            File userWallpaperFile = WineThemeManager.getUserWallpaperFile(this);
            ImageUtils.save(bitmap, userWallpaperFile, Bitmap.CompressFormat.PNG, 100);
        } else if (requestCode == OPEN_IMAGE_REQUEST_CODE) {
            consumeImagePickerCallback();
        } else if (requestCode == OPEN_DRIVER_PACKAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Callback<Uri> callback = consumeDriverPackagePickerCallback();
            if (callback != null) callback.call(data.getData());
        } else if (requestCode == OPEN_DRIVER_PACKAGE_REQUEST_CODE) {
            consumeDriverPackagePickerCallback();
        }
    }

    public static void setImagePickerCallback(@Nullable Callback<Bitmap> callback) {
        imagePickerCallback = callback;
    }

    @Nullable
    public static Callback<Bitmap> consumeImagePickerCallback() {
        Callback<Bitmap> callback = imagePickerCallback;
        imagePickerCallback = null;
        return callback;
    }

    public static void setDriverPackagePickerCallback(@Nullable Callback<Uri> callback) {
        driverPackagePickerCallback = callback;
    }

    @Nullable
    public static Callback<Uri> consumeDriverPackagePickerCallback() {
        Callback<Uri> callback = driverPackagePickerCallback;
        driverPackagePickerCallback = null;
        return callback;
    }
}
