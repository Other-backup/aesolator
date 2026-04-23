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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WineInfo implements Parcelable {
    public static final WineInfo MAIN_WINE_VERSION = new WineInfo("wine","11.1", "arm64ec");
    private static final Pattern strictPattern = Pattern.compile("^(wine|proton)\\-([0-9\\.]+)\\-?([0-9\\.]+)?\\-(x86|x86_64|arm64ec)$");
    private static final Pattern archSuffixPattern = Pattern.compile("\\-(x86|x86_64|arm64ec)$");
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

    public boolean isArm64EC() { return arch.equals("arm64ec"); }

    public String identifier() {
        if (type.equals("proton"))
            return "proton-" + fullVersion() + "-"+ arch;
        else
            return "wine-" + fullVersion() + "-" + arch;
    }

    public String fullVersion() {
        return version+(subversion != null ? "-"+subversion : "");
    }

    @NonNull
    @Override
    public String toString() {
        if (type.equals("proton"))
            return "Proton "+fullVersion()+(this == MAIN_WINE_VERSION ? " (Custom)" : "");
        else
            return "Wine "+fullVersion()+(this == MAIN_WINE_VERSION ? " (Custom)" : "");
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
        return fromIdentifier(context, contentsManager, identifier, null);
    }

    @NonNull
    public static WineInfo fromIdentifier(Context context, ContentsManager contentsManager, String identifier, String requestedRuntimeModel) {
        ImageFs imageFs = ImageFs.find(context);
        String fallbackPath = WineUtils.resolveCanonicalRuntimeRoot(imageFs.getMainWineDir()).getPath();

        Log.d("WineInfo", "Creating WineInfo from identifier " + identifier);

        if (identifier == null || identifier.trim().isEmpty()) {
            return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, fallbackPath);
        }

        String normalizedIdentifier = identifier.toLowerCase(Locale.ENGLISH).trim();
        String effectiveRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        if (effectiveRuntimeModel.isEmpty()) {
            effectiveRuntimeModel = ContentProfile.inferRuntimeModelFromEntryName(normalizedIdentifier);
        }

        if (normalizedIdentifier.equals(MAIN_WINE_VERSION.identifier())) {
            return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, fallbackPath);
        }

        ContentProfile wineProfile = contentsManager.resolveBestRuntimeProfile(normalizedIdentifier, effectiveRuntimeModel);
        ParsedIdentifier parsed = null;
        String path = "";

        if (wineProfile != null && wineProfile.isWineProtonFamily()) {
            path = resolveInstalledRuntimePath(context, contentsManager, wineProfile);
            parsed = parseProfileIdentifier(wineProfile);
            if (parsed == null) parsed = parseIdentifier(stripRuntimeModelToken(stripEntryVersionCodeSuffix(normalizedIdentifier)));
        }

        if (parsed == null) {
            parsed = parseIdentifier(stripRuntimeModelToken(normalizedIdentifier));
        }

        if (parsed == null) {
            parsed = parseIdentifier(stripRuntimeModelToken(stripEntryVersionCodeSuffix(normalizedIdentifier)));
        }

        if (parsed != null) {
            if (path.isEmpty()) {
                path = resolveEmbeddedRuntimePath(context, imageFs, normalizedIdentifier);
            }
            return new WineInfo(parsed.type, parsed.version, parsed.arch, path);
        }

        return new WineInfo(MAIN_WINE_VERSION.type, MAIN_WINE_VERSION.version, MAIN_WINE_VERSION.arch, fallbackPath);
    }

    private static String resolveInstalledRuntimePath(Context context, ContentsManager contentsManager, ContentProfile profile) {
        if (contentsManager == null || profile == null || !contentsManager.isInstalledProfileUsable(profile)) return "";

        File runtimeRoot = contentsManager.getRuntimeRootDir(profile);
        File resolvedRoot = runtimeRoot != null ? runtimeRoot : contentsManager.getInstallDir(context, profile);
        File canonicalRoot = WineUtils.resolveCanonicalRuntimeRoot(resolvedRoot);
        return canonicalRoot != null && WineUtils.hasRuntimePayload(canonicalRoot) ? canonicalRoot.getPath() : "";
    }

    private static String resolveEmbeddedRuntimePath(Context context, ImageFs imageFs, String normalizedIdentifier) {
        if (context == null || imageFs == null || normalizedIdentifier == null || normalizedIdentifier.isEmpty()) return "";

        String[] wineVersions = context.getResources().getStringArray(R.array.wine_entries);
        for (String wineVersion : wineVersions) {
            if (wineVersion.equalsIgnoreCase(normalizedIdentifier)) {
                return resolveOptRuntimePath(imageFs, normalizedIdentifier);
            }
        }

        return resolveOptRuntimePath(imageFs, stripRuntimeModelToken(normalizedIdentifier));
    }

    private static String resolveOptRuntimePath(ImageFs imageFs, String runtimeToken) {
        if (imageFs == null || runtimeToken == null || runtimeToken.trim().isEmpty()) return "";
        File candidate = WineUtils.resolveCanonicalRuntimeRoot(new File(imageFs.getRootDir(), "/opt/" + runtimeToken.trim()));
        return candidate != null && WineUtils.hasRuntimePayload(candidate) ? candidate.getPath() : "";
    }

    public static boolean isMainWineVersion(String wineVersion) {
        return wineVersion == null ||wineVersion.equals(MAIN_WINE_VERSION.identifier());
    }

    private static ParsedIdentifier parseIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
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

    private static ParsedIdentifier parseProfileIdentifier(ContentProfile profile) {
        if (profile == null || !profile.isWineProtonFamily()) {
            return null;
        }

        String normalizedVersionName = profile.verName == null ? "" : profile.verName.trim().toLowerCase(Locale.ENGLISH);
        if (normalizedVersionName.isEmpty()) {
            return null;
        }

        String typePrefix = profile.type == ContentProfile.ContentType.CONTENT_TYPE_PROTON || profile.isProtonLike()
                ? "proton"
                : "wine";

        ParsedIdentifier parsed = parseIdentifier(typePrefix + "-" + normalizedVersionName);
        if (parsed != null) {
            return parsed;
        }

        if (normalizedVersionName.startsWith("wine-") || normalizedVersionName.startsWith("proton-")) {
            return parseIdentifier(normalizedVersionName);
        }

        return null;
    }

    private static String stripEntryVersionCodeSuffix(String identifier) {
        if (identifier == null) {
            return "";
        }
        String normalized = identifier.trim().toLowerCase(Locale.ENGLISH);
        int lastDash = normalized.lastIndexOf('-');
        if (lastDash <= 0 || lastDash >= normalized.length() - 1) {
            return normalized;
        }
        for (int i = lastDash + 1; i < normalized.length(); i++) {
            if (!Character.isDigit(normalized.charAt(i))) {
                return normalized;
            }
        }
        return normalized.substring(0, lastDash);
    }

    private static String stripRuntimeModelToken(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return "";
        String normalized = identifier.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.startsWith("wine-glibc-")) return "wine-" + normalized.substring("wine-glibc-".length());
        if (normalized.startsWith("wine-bionic-")) return "wine-" + normalized.substring("wine-bionic-".length());
        if (normalized.startsWith("proton-glibc-")) return "proton-" + normalized.substring("proton-glibc-".length());
        if (normalized.startsWith("proton-bionic-")) return "proton-" + normalized.substring("proton-bionic-".length());
        return normalized;
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
