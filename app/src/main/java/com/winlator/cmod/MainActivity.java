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
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.ImageUtils;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.WineThemeManager;
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

    public final PreloaderDialog preloaderDialog = new PreloaderDialog(this);

    private boolean editInputControls = false;
    private int selectedProfileId;
    private SharedPreferences sharedPreferences;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences startupPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isBigPictureModeEnabled = startupPreferences.getBoolean("enable_big_picture_mode", false);
        if (isBigPictureModeEnabled) {
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

        Intent intent = getIntent();
        editInputControls = intent.getBooleanExtra("edit_input_controls", false);
        if (editInputControls) {
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            openMainMenuItem(R.id.main_menu_input_controls, false);
        } else {
            int selectedMenuItemId = intent.getIntExtra("selected_menu_item_id", 0);

            if (!requestAppPermissions()) {
                ImageFsInstaller.installIfNeeded(this);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                showAllFilesAccessDialog();
            }

            if (Build.VERSION.SDK_INT >= 33
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
            }

            if (selectedMenuItemId > 0) {
                openMainMenuItem(selectedMenuItemId, false);
            } else {
                showHomeDashboard(false);
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
            finish();
            return;
        }
        showHomeDashboard(true);
    }

    private boolean requestAppPermissions() {
        boolean hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean hasManageStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();

        if (hasWritePermission && hasReadPermission && hasManageStoragePermission) {
            return false;
        }

        if (!hasWritePermission || !hasReadPermission) {
            String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        }

        return true;
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
        if (!ImageFs.find(this).isValid()) return;
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
        boolean latestDark = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_mode", false);
        if (latestDark != isDarkMode) isDarkMode = latestDark;
        View decor = getWindow() != null ? getWindow().getDecorView() : null;
        if (decor != null && decor.getAlpha() != 1f) decor.setAlpha(1f);
        updateActionBarNavigationState();
        applyThemeChrome();
        applyThemeAssetTintPass();
    }

    private boolean isHomeDashboardVisible() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.FLFragmentContainer);
        return current instanceof MainMenuGridFragment;
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
                    "<b>Stack:</b> FreeWine 11 + lane-based graphics stack + package lanes",
                    "<b>Runtime donor lines:</b> FreeWine 11 baseline, driver archives, wrapper lanes and Android container integration",
                    "---",
                    "<b>Winlator donor bases</b>",
                    "BrunoSX / Winlator (<a href=\"https://github.com/brunodev85/winlator\">github.com/brunodev85/winlator</a>)",
                    "coffincolors / winlator (<a href=\"https://github.com/coffincolors/winlator\">github.com/coffincolors/winlator</a>)",
                    "StevenMXZ / Winlator-Ludashi (<a href=\"https://github.com/StevenMXZ/Winlator-Ludashi\">github.com/StevenMXZ/Winlator-Ludashi</a>)",
                    "---",
                    "FreeWine 11 baseline research: AndreRH + upstream Wine / Valve work",
                    "Termux Package (<a href=\"https://github.com/termux/termux-packages\">github.com/termux/termux-package</a>)",
                    "Wine (<a href=\"https://www.winehq.org\">winehq.org</a>)",
                    "Box64 (<a href=\"https://github.com/ptitSeb/box64\">github.com/ptitSeb/box64</a>)",
                    "FEX-Emu (<a href=\"https://github.com/FEX-Emu/FEX\">github.com/FEX-Emu/FEX</a>)",
                    "Mesa / Turnip / Zink (<a href=\"https://gitlab.freedesktop.org/mesa/mesa\">gitlab.freedesktop.org/mesa/mesa</a>)",
                    "DXVK (<a href=\"https://github.com/doitsujin/dxvk\">github.com/doitsujin/dxvk</a>)",
                    "VKD3D (<a href=\"https://gitlab.winehq.org/wine/vkd3d\">gitlab.winehq.org/wine/vkd3d</a>)",
                    "D8VK (<a href=\"https://github.com/AlpyneDreams/d8vk\">github.com/AlpyneDreams/d8vk</a>)",
                    "dgVoodoo2 (<a href=\"https://github.com/dege-diosg/dgVoodoo2\">github.com/dege-diosg/dgVoodoo2</a>)",
                    "libadrenotools (<a href=\"https://github.com/bylaws/libadrenotools\">github.com/bylaws/libadrenotools</a>)",
                    "---",
                    "<b>Graphics donor archives</b>",
                    "StevenMXZ Turnip CI (<a href=\"https://github.com/StevenMXZ/freedreno_turnip-CI\">github.com/StevenMXZ/freedreno_turnip-CI</a>)",
                    "whitebelyash Turnip CI (<a href=\"https://github.com/whitebelyash/freedreno_turnip-CI\">github.com/whitebelyash/freedreno_turnip-CI</a>)",
                    "MrPurple Turnip (<a href=\"https://github.com/MrPurple666/purple-turnip\">github.com/MrPurple666/purple-turnip</a>)",
                    "GameNative Drivers (<a href=\"https://gamenative.app/drivers/\">gamenative.app/drivers</a>)"
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
            File userWallpaperFile = WineThemeManager.getUserWallpaperFile(this);
            ImageUtils.save(bitmap, userWallpaperFile, Bitmap.CompressFormat.PNG, 100);
        }
    }
}
