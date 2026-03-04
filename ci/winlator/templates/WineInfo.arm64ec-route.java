package com.winlator.cmod.core;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.winlator.cmod.R;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.xenvironment.ImageFs;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WineInfo implements Parcelable {
    public static final WineInfo MAIN_WINE_VERSION = new WineInfo("proton", "9.0", "x86_64");
    private static final String ARM64EC_SAFE_FALLBACK_IDENTIFIER = "proton-9.0-arm64ec";

    private static final Pattern strictPattern = Pattern.compile("^(wine|proton)\\-([0-9\\.]+)\\-?([0-9\\.]+)?\\-(x86|x86_64|arm64ec)$");
    private static final Pattern archSuffixPattern = Pattern.compile("\\-(x86|x86_64|arm64ec)(?:-[0-9]+)?$");

    public final String version;
    public final String type;
    public String subversion;
    public final String path;
    private String arch;

    public WineInfo(String type, String version, String arch) {
        this.type = type;
        this.version = version;
        this.subversion = null;
        this.arch = arch;
        this.path = null;
    }

    public WineInfo(String type, String version, String subversion, String arch, String path) {
        this.type = type;
        this.version = version;
        this.subversion = subversion != null && !subversion.isEmpty() ? subversion : null;
        this.arch = arch;
        this.path = path;
    }

    public WineInfo(String type, String version, String arch, String path) {
        this.type = type;
        this.version = version;
        this.arch = arch;
        this.path = path;
    }

    private WineInfo(Parcel in) {
        type = in.readString();
        version = in.readString();
        subversion = in.readString();
        arch = in.readString();
        path = in.readString();
    }

    public String getArch() {
        return arch;
    }

    public void setArch(String arch) {
        this.arch = arch;
    }

    public boolean isWin64() {
        return arch.equals("x86_64") || arch.equals("arm64ec");
    }

    public boolean isArm64EC() {
        return arch.equals("arm64ec");
    }

    public String identifier() {
        if (type.equals("proton")) {
            return "proton-" + fullVersion() + "-" + arch;
        } else {
            return "wine-" + fullVersion() + "-" + arch;
        }
    }

    public String fullVersion() {
        return version + (subversion != null ? "-" + subversion : "");
    }

    @NonNull
    @Override
    public String toString() {
        if (type.equals("proton")) {
            return "Proton " + fullVersion() + (this == MAIN_WINE_VERSION ? " (Custom)" : "");
        } else {
            return "Wine " + fullVersion() + (this == MAIN_WINE_VERSION ? " (Custom)" : "");
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<WineInfo> CREATOR = new Parcelable.Creator<WineInfo>() {
        public WineInfo createFromParcel(Parcel in) {
            return new WineInfo(in);
        }

        public WineInfo[] newArray(int size) {
            return new WineInfo[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(version);
        dest.writeString(subversion);
        dest.writeString(arch);
        dest.writeString(path);
    }

    @NonNull
    public static WineInfo fromIdentifier(Context context, ContentsManager contentsManager, String identifier) {
        ImageFs imageFs = ImageFs.find(context);
        String defaultFallbackPath = imageFs.getRootDir().getPath() + "/opt/" + MAIN_WINE_VERSION.identifier();
        String arm64ecFallbackPath = imageFs.getRootDir().getPath() + "/opt/" + ARM64EC_SAFE_FALLBACK_IDENTIFIER;
        boolean arm64ecFallbackAvailable = new File(arm64ecFallbackPath).isDirectory();

        Log.d("WineInfo", "Creating WineInfo from identifier " + identifier);

        if (identifier == null || identifier.trim().isEmpty()) {
            return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, defaultFallbackPath);
        }

        String normalizedIdentifier = identifier.toLowerCase(Locale.ENGLISH).trim();
        if (normalizedIdentifier.equals(MAIN_WINE_VERSION.identifier())) {
            return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, defaultFallbackPath);
        }

        ContentProfile wineProfile = resolveWineProfile(contentsManager, normalizedIdentifier);
        ParsedIdentifier parsed = null;
        String path = "";

        if (wineProfile != null && wineProfile.type == ContentProfile.ContentType.CONTENT_TYPE_WINE) {
            path = contentsManager.getInstallDir(context, wineProfile).getPath();
            parsed = parseIdentifier(wineProfile.verName);
        }

        if (parsed == null) {
            parsed = parseIdentifier(normalizedIdentifier);
        }

        if (path.isEmpty()) {
            String[] wineVersions = context.getResources().getStringArray(R.array.wine_entries);
            for (String wineVersion : wineVersions) {
                if (wineVersion.equalsIgnoreCase(normalizedIdentifier)) {
                    path = imageFs.getRootDir().getPath() + "/opt/" + normalizedIdentifier;
                    break;
                }
            }
        }

        if (parsed != null) {
            if (path.isEmpty()) {
                File optDir = new File(imageFs.getRootDir(), "/opt/" + normalizedIdentifier);
                if (optDir.isDirectory()) {
                    path = optDir.getPath();
                }
            }

            if (path.isEmpty() && "arm64ec".equals(parsed.arch) && arm64ecFallbackAvailable) {
                return new WineInfo("proton", "9.0", "arm64ec", arm64ecFallbackPath);
            }

            if (path.isEmpty()) {
                path = defaultFallbackPath;
            }

            return new WineInfo(parsed.type, parsed.version, parsed.arch, path);
        }

        if (normalizedIdentifier.contains("arm64ec") && arm64ecFallbackAvailable) {
            return new WineInfo("proton", "9.0", "arm64ec", arm64ecFallbackPath);
        }

        return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, defaultFallbackPath);
    }

    public static boolean isMainWineVersion(String wineVersion) {
        return wineVersion == null || wineVersion.equals(MAIN_WINE_VERSION.identifier());
    }

    private static ParsedIdentifier parseIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }

        String normalized = identifier.toLowerCase(Locale.ENGLISH).trim();

        Matcher strictMatcher = strictPattern.matcher(normalized);
        if (strictMatcher.matches()) {
            String version = strictMatcher.group(2);
            if (strictMatcher.group(3) != null && !strictMatcher.group(3).isEmpty()) {
                version += "-" + strictMatcher.group(3);
            }
            return new ParsedIdentifier(strictMatcher.group(1), version, strictMatcher.group(4));
        }

        Matcher archMatcher = archSuffixPattern.matcher(normalized);
        if (!archMatcher.find()) {
            return null;
        }

        String arch = archMatcher.group(1);
        String base = normalized.substring(0, archMatcher.start());
        String type;
        String version;

        if (base.startsWith("proton-")) {
            type = "proton";
            version = base.substring("proton-".length());
        } else if (base.startsWith("proton")) {
            type = "proton";
            version = base.substring("proton".length());
        } else if (base.startsWith("wine-")) {
            type = "wine";
            version = base.substring("wine-".length());
        } else if (base.startsWith("wine")) {
            type = "wine";
            version = base.substring("wine".length());
        } else {
            type = "wine";
            version = base;
        }

        version = version.replaceAll("^-+", "").replaceAll("-+$", "");
        if (version.isEmpty()) {
            return null;
        }

        return new ParsedIdentifier(type, version, arch);
    }

    private static ContentProfile resolveWineProfile(ContentsManager contentsManager, String normalizedIdentifier) {
        if (contentsManager == null || normalizedIdentifier == null || normalizedIdentifier.isEmpty()) {
            return null;
        }

        ContentProfile profile = contentsManager.getProfileByEntryName(normalizedIdentifier);
        if (profile != null) {
            return profile;
        }

        String typedEntryName = ContentProfile.ContentType.CONTENT_TYPE_WINE + "-" + normalizedIdentifier;
        profile = contentsManager.getProfileByEntryName(typedEntryName);
        if (profile != null) {
            return profile;
        }

        int lastDashIndex = normalizedIdentifier.lastIndexOf('-');
        if (lastDashIndex <= 0 || lastDashIndex >= normalizedIdentifier.length() - 1) {
            return null;
        }

        String versionName = normalizedIdentifier.substring(0, lastDashIndex);
        String versionCodeToken = normalizedIdentifier.substring(lastDashIndex + 1);
        if (!versionCodeToken.matches("[0-9]+")) {
            return null;
        }

        int versionCode;
        try {
            versionCode = Integer.parseInt(versionCodeToken);
        } catch (NumberFormatException ignored) {
            return null;
        }

        List<ContentProfile> profiles = contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE);
        if (profiles == null) {
            return null;
        }

        for (ContentProfile candidate : profiles) {
            if (candidate == null) {
                continue;
            }
            if (candidate.verCode == versionCode && versionName.equalsIgnoreCase(candidate.verName)) {
                return candidate;
            }
        }
        return null;
    }

    private static class ParsedIdentifier {
        final String type;
        final String version;
        final String arch;

        ParsedIdentifier(String type, String version, String arch) {
            this.type = type;
            this.version = version;
            this.arch = arch;
        }
    }
}
