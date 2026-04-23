package com.winlator.cmod.contentdialog;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.winlator.cmod.R;
import com.winlator.cmod.XServerDisplayActivity;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerUtils;
import com.winlator.cmod.contents.Downloader;
import com.winlator.cmod.contents.PrefixPackCatalog;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.core.PreloaderDialog;
import com.winlator.cmod.xenvironment.ImageFs;
import com.winlator.cmod.xenvironment.ImageFsInstaller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrefixPackToolkitDialog extends ContentDialog {
    private static final String CATALOG_ASSET = "prefixpack/catalog.tsv";
    private static final String README_ASSET = "prefixpack/README.txt";
    private static final String TOOLKIT_ROOT = "Z:\\opt\\ae\\prefix-pack";
    private static final String CACHE_ROOT = "Z:\\opt\\ae\\prefix-pack\\cache";
    private static final String LOADER_PATH = TOOLKIT_ROOT + "\\windows\\prefix-pack-loader.cmd";
    private static final String WINDOWS_INSTALLER_ROOT = "C:\\AePrefixPack";
    private static final String WINDOWS_INSTALLER_CACHE = WINDOWS_INSTALLER_ROOT + "\\cache";
    private static final String WINDOWS_STAGE_ROOT = WINDOWS_INSTALLER_ROOT + "\\staging";
    private static final String WINDOWS_STAGE_TOOLKIT_SEGMENT = "toolkit";
    private static final String WINDOWS_SYSTEM32 = "C:\\windows\\system32";
    private static final String WINDOWS_CMD_EXE = WINDOWS_SYSTEM32 + "\\cmd.exe";
    private static final String WINDOWS_MSIEXEC_EXE = WINDOWS_SYSTEM32 + "\\msiexec.exe";
    private static final String WINDOWS_WSCRIPT_EXE = WINDOWS_SYSTEM32 + "\\wscript.exe";
    private static final String WINDOWS_START_EXE = WINDOWS_SYSTEM32 + "\\start.exe";
    private static final int TOOL_SCAN_MAX_DEPTH = 6;
    private static final int RUNTIME_DISPATCH_RETRY_MS = 300;
    private static final int RUNTIME_DISPATCH_MAX_ATTEMPTS = 150;
    private static final int RUNTIME_DISPATCH_VERIFY_DELAY_MS = 2200;
    private static final int RUNTIME_DISPATCH_VERIFY_RECHECK_MS = 1800;
    private static final int RUNTIME_DISPATCH_VERIFY_MAX_RECHECKS = 2;
    private static final int RUNTIME_DETACHED_VERIFY_DELAY_MS = 2600;
    private static final int RUNTIME_DETACHED_VERIFY_RECHECK_MS = 2000;
    private static final int RUNTIME_DETACHED_VERIFY_MAX_RECHECKS = 2;
    private static final long RUNTIME_SHELL_FALLBACK_GRACE_MS = 12000L;
    private static final int AUTO_INSTALL_RETRY_MS = 450;
    private static final int AUTO_INSTALL_MAX_ATTEMPTS = 20;
    private static final int LANE_BUILD_BATCH_SIZE = 3;
    private static final long LANE_BUILD_STEP_DELAY_MS = 12L;
    private static final long INSTALL_INFLIGHT_FRESH_MS = 15L * 60L * 1000L;
    private static final long MAX_REASONABLE_STATE_FILE_BYTES = 128L * 1024L;

    private final XServerDisplayActivity activity;
    private final ImageFs imageFs;
    private final LayoutInflater inflater;
    private final LinearLayout laneList;
    private final TextView tvSession;
    private final TextView tvPaths;
    private final TextView tvFlow;
    private final List<PrefixPackCatalog.Entry> catalogEntries;
    private final List<LaneSpec> laneSpecs;
    private final Map<String, List<PrefixPackCatalog.Entry>> laneEntryCache = new HashMap<>();
    private final Map<String, View> laneItemViews = new HashMap<>();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean statusRefreshInFlight = new AtomicBoolean(false);
    private final String autoInstallTarget;
    private File resolvedHostWinePrefixDir;
    private File resolvedHostWindowsUserDir;
    private boolean autoLaunchConsumed = false;
    private volatile boolean statusRefreshQueued = false;
    private volatile StatusIndex statusIndex = StatusIndex.empty();
    private volatile LaneUiIndex laneUiIndex = LaneUiIndex.empty();
    private boolean laneCardsBuilt = false;
    private boolean laneBuildInProgress = false;
    private int nextLaneBuildIndex = 0;
    private String activeLaneBuildSection = "";
    private boolean nextLaneBuildUsesFirstSectionSpacing = true;
    private int lastLoggedCatalogEntryCount = -1;
    private int lastLoggedCachedEntryCount = -1;
    private int lastLoggedMirroredEntryCount = -1;
    private int lastLoggedInstalledOkCount = -1;
    private int lastLoggedAttentionCount = -1;
    private int lastLoggedLaneCardCount = -1;

    private static final class LaneSpec {
        final String sectionTitle;
        final String installTarget;
        final int iconResId;
        final String title;
        final String summary;
        final boolean mayRequireGui;
        final List<String> entryIds;

        private LaneSpec(String sectionTitle, String installTarget, int iconResId, String title, String summary, boolean mayRequireGui, List<String> entryIds) {
            this.sectionTitle = sectionTitle;
            this.installTarget = installTarget;
            this.iconResId = iconResId;
            this.title = title;
            this.summary = summary;
            this.mayRequireGui = mayRequireGui;
            this.entryIds = entryIds;
        }
    }

    private static final class InstallState {
        final File stateFile;
        final String status;
        final String exitCode;
        final String updatedAt;
        final String logFile;
        final String detail;
        final String launcherFile;
        final String primaryPayload;
        final String nextAction;
        final String requestedBy;

        private InstallState(File stateFile, String status, String exitCode, String updatedAt, String logFile, String detail,
                             String launcherFile, String primaryPayload, String nextAction, String requestedBy) {
            this.stateFile = stateFile;
            this.status = status;
            this.exitCode = exitCode;
            this.updatedAt = updatedAt;
            this.logFile = logFile;
            this.detail = detail;
            this.launcherFile = launcherFile;
            this.primaryPayload = primaryPayload;
            this.nextAction = nextAction;
            this.requestedBy = requestedBy;
        }

        boolean exists() {
            return stateFile != null && stateFile.isFile();
        }
    }

    private static final class EntryStatus {
        final boolean cached;
        final boolean mirrored;

        private EntryStatus(boolean cached, boolean mirrored) {
            this.cached = cached;
            this.mirrored = mirrored;
        }
    }

    private static final class StatusIndex {
        final int totalEntries;
        final int cachedEntries;
        final int mirroredEntries;
        final Map<String, EntryStatus> entryStatusById;

        private StatusIndex(int totalEntries, int cachedEntries, int mirroredEntries, Map<String, EntryStatus> entryStatusById) {
            this.totalEntries = totalEntries;
            this.cachedEntries = cachedEntries;
            this.mirroredEntries = mirroredEntries;
            this.entryStatusById = entryStatusById;
        }

        private static StatusIndex empty() {
            return new StatusIndex(0, 0, 0, Collections.emptyMap());
        }

        private EntryStatus get(PrefixPackCatalog.Entry entry) {
            if (entry == null) return null;
            return entryStatusById.get(entry.id);
        }

        private boolean isCached(PrefixPackCatalog.Entry entry) {
            EntryStatus entryStatus = get(entry);
            return entryStatus != null && entryStatus.cached;
        }

        private boolean isMirrored(PrefixPackCatalog.Entry entry) {
            EntryStatus entryStatus = get(entry);
            return entryStatus != null && entryStatus.mirrored;
        }

        private int countCached(List<PrefixPackCatalog.Entry> entries) {
            if (entries == null || entries.isEmpty()) return 0;
            int count = 0;
            for (PrefixPackCatalog.Entry entry : entries) {
                if (isCached(entry)) count++;
            }
            return count;
        }

        private int countMirrored(List<PrefixPackCatalog.Entry> entries) {
            if (entries == null || entries.isEmpty()) return 0;
            int count = 0;
            for (PrefixPackCatalog.Entry entry : entries) {
                if (isMirrored(entry)) count++;
            }
            return count;
        }
    }

    private static final class LaneUiState {
        final int cachedCount;
        final int mirroredCount;
        final int totalCount;
        final String stateText;

        private LaneUiState(int cachedCount, int mirroredCount, int totalCount, String stateText) {
            this.cachedCount = cachedCount;
            this.mirroredCount = mirroredCount;
            this.totalCount = totalCount;
            this.stateText = stateText;
        }
    }

    private static final class LaneUiIndex {
        final int installedOkCount;
        final int attentionCount;
        final Map<String, LaneUiState> stateByTarget;

        private LaneUiIndex(int installedOkCount, int attentionCount, Map<String, LaneUiState> stateByTarget) {
            this.installedOkCount = installedOkCount;
            this.attentionCount = attentionCount;
            this.stateByTarget = stateByTarget;
        }

        private static LaneUiIndex empty() {
            return new LaneUiIndex(0, 0, Collections.emptyMap());
        }

        private LaneUiState get(String installTarget) {
            if (installTarget == null || installTarget.trim().isEmpty()) return null;
            return stateByTarget.get(installTarget);
        }
    }

    public PrefixPackToolkitDialog(@NonNull XServerDisplayActivity activity) {
        this(activity, "");
    }

    public PrefixPackToolkitDialog(@NonNull XServerDisplayActivity activity, String autoInstallTarget) {
        super(activity, R.layout.prefix_pack_toolkit_dialog);
        this.activity = activity;
        this.imageFs = ImageFs.find(activity);
        this.inflater = LayoutInflater.from(activity);
        this.laneList = findViewById(R.id.LLPrefixPackLaneList);
        this.tvSession = findViewById(R.id.TVPrefixPackSession);
        this.tvPaths = findViewById(R.id.TVPrefixPackPaths);
        this.tvFlow = findViewById(R.id.TVPrefixPackFlow);
        this.autoInstallTarget = autoInstallTarget != null ? autoInstallTarget.trim() : "";

        setTitle(R.string.prefix_pack_toolkit);
        setIcon(R.drawable.ae_icon_package);

        View cancelButton = findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        Button confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) confirmButton.setText(R.string.close);

        catalogEntries = PrefixPackCatalog.parse(FileUtils.readString(activity, CATALOG_ASSET));
        laneSpecs = Collections.unmodifiableList(new ArrayList<>(createLaneSpecs()));
        for (LaneSpec lane : laneSpecs) {
            laneEntryCache.put(lane.installTarget, Collections.unmodifiableList(resolveEntries(lane.entryIds)));
        }

        bindOverviewActions();
        updateOverview();
        applySurfaceStyle();

        setOnDismissListener(dialog -> ioExecutor.shutdownNow());
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(0));
            getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.992f),
                    Math.round(AppUtils.getScreenHeight() * 0.954f)
            );
        }
        ViewGroup.LayoutParams rootParams = getContentView().getLayoutParams();
        if (rootParams != null) {
            rootParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            rootParams.height = Math.round(AppUtils.getScreenHeight() * 0.942f);
            getContentView().setLayoutParams(rootParams);
        }
        getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.942f));
        compactDialogChrome();
        if (laneList != null) {
            startLaneCardBuild();
            refreshStatusIndexAsync();
        } else {
            getContentView().post(this::refreshStatusIndexAsync);
        }
        maybeAutoLaunchRequestedInstall();
    }

    private void compactDialogChrome() {
        getContentView().setPadding(dp(2), dp(2), dp(2), dp(2));
        getContentView().setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        View frameLayout = getContentView().findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            frameLayout.setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        }
        View titleBar = getContentView().findViewById(R.id.LLTitleBar);
        if (titleBar != null) {
            titleBar.setVisibility(View.GONE);
        }
        View bottomBar = getContentView().findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
    }

    private void bindOverviewActions() {
        // Overview remains summary-only; lane cards carry the actionable controls.
    }

    private List<PrefixPackCatalog.Entry> resolveStarterEntries() {
        return resolveEntries(Arrays.asList(
                "vcrun6sp6",
                "vcpp_aio",
                "wine_mono_11_0_0",
                "wine_gecko_2_47_4_x86",
                "wine_gecko_2_47_4_x86_64",
                "directx_jun2010",
                "xnafx31_refresh",
                "xnafx40_refresh",
                "openal_1_1"
        ));
    }

    private void refreshUi() {
        StatusIndex snapshot = statusIndex;
        LaneUiIndex laneSnapshot = laneUiIndex;
        updateOverview();
        refreshLaneCards();
        if (!laneBuildInProgress || laneCardsBuilt) {
            maybeLogDialogRender(snapshot, laneSnapshot);
        }
    }

    private void maybeLogDialogRender(StatusIndex snapshot, LaneUiIndex laneSnapshot) {
        int catalogEntryCount = snapshot.totalEntries > 0 ? snapshot.totalEntries : catalogEntries.size();
        int laneCardCount = laneList != null ? laneList.getChildCount() : -1;
        if (catalogEntryCount == lastLoggedCatalogEntryCount
                && snapshot.cachedEntries == lastLoggedCachedEntryCount
                && snapshot.mirroredEntries == lastLoggedMirroredEntryCount
                && laneSnapshot.installedOkCount == lastLoggedInstalledOkCount
                && laneSnapshot.attentionCount == lastLoggedAttentionCount
                && laneCardCount == lastLoggedLaneCardCount) {
            return;
        }
        lastLoggedCatalogEntryCount = catalogEntryCount;
        lastLoggedCachedEntryCount = snapshot.cachedEntries;
        lastLoggedMirroredEntryCount = snapshot.mirroredEntries;
        lastLoggedInstalledOkCount = laneSnapshot.installedOkCount;
        lastLoggedAttentionCount = laneSnapshot.attentionCount;
        lastLoggedLaneCardCount = laneCardCount;
        ForensicLogger.logEvent(
                activity,
                "info",
                "PREFIX_PACK_DIALOG_RENDER",
                null,
                "runtime_ui",
                "prefix_pack_dialog_render",
                ForensicLogger.fields(
                        "catalog_entry_count", catalogEntryCount,
                        "cached_entry_count", snapshot.cachedEntries,
                        "mirrored_entry_count", snapshot.mirroredEntries,
                        "installed_ok_count", laneSnapshot.installedOkCount,
                        "attention_count", laneSnapshot.attentionCount,
                        "lane_count", laneSpecs.size(),
                        "lane_card_count", laneCardCount
                )
        );
    }

    private void refreshStatusIndexAsync() {
        if (!isShowing() || ioExecutor.isShutdown()) return;
        if (!statusRefreshInFlight.compareAndSet(false, true)) {
            statusRefreshQueued = true;
            return;
        }
        ioExecutor.execute(() -> {
            StatusIndex nextIndex = buildStatusIndex();
            LaneUiIndex nextLaneUiIndex = buildLaneUiIndex(nextIndex);
            activity.runOnUiThread(() -> {
                boolean statusChanged = !sameStatusIndex(statusIndex, nextIndex);
                boolean laneUiChanged = !sameLaneUiIndex(laneUiIndex, nextLaneUiIndex);
                statusIndex = nextIndex;
                laneUiIndex = nextLaneUiIndex;
                boolean queuedRefresh = statusRefreshQueued;
                statusRefreshQueued = false;
                statusRefreshInFlight.set(false);
                if (isShowing() && (statusChanged || laneUiChanged || laneBuildInProgress || !laneCardsBuilt)) {
                    refreshUi();
                }
                if (queuedRefresh) {
                    refreshStatusIndexAsync();
                }
            });
        });
    }

    private boolean sameStatusIndex(StatusIndex left, StatusIndex right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.totalEntries != right.totalEntries
                || left.cachedEntries != right.cachedEntries
                || left.mirroredEntries != right.mirroredEntries
                || left.entryStatusById.size() != right.entryStatusById.size()) {
            return false;
        }
        for (Map.Entry<String, EntryStatus> entry : left.entryStatusById.entrySet()) {
            EntryStatus current = entry.getValue();
            EntryStatus other = right.entryStatusById.get(entry.getKey());
            if (current == null || other == null) {
                if (current != other) return false;
                continue;
            }
            if (current.cached != other.cached || current.mirrored != other.mirrored) {
                return false;
            }
        }
        return true;
    }

    private boolean sameLaneUiIndex(LaneUiIndex left, LaneUiIndex right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        if (left.installedOkCount != right.installedOkCount
                || left.attentionCount != right.attentionCount
                || left.stateByTarget.size() != right.stateByTarget.size()) {
            return false;
        }
        for (Map.Entry<String, LaneUiState> entry : left.stateByTarget.entrySet()) {
            LaneUiState current = entry.getValue();
            LaneUiState other = right.stateByTarget.get(entry.getKey());
            if (current == null || other == null) {
                if (current != other) return false;
                continue;
            }
            if (current.cachedCount != other.cachedCount
                    || current.mirroredCount != other.mirroredCount
                    || current.totalCount != other.totalCount
                    || !Objects.equals(current.stateText, other.stateText)) {
                return false;
            }
        }
        return true;
    }

    private StatusIndex buildStatusIndex() {
        if (catalogEntries == null || catalogEntries.isEmpty()) return StatusIndex.empty();
        HashMap<String, EntryStatus> entryStatusById = new HashMap<>();
        int cachedCount = 0;
        int mirroredCount = 0;
        for (PrefixPackCatalog.Entry entry : catalogEntries) {
            if (entry == null) continue;
            boolean cached = isCachedStrict(entry);
            boolean mirrored = isMirroredStrict(entry);
            if (cached) cachedCount++;
            if (mirrored) mirroredCount++;
            entryStatusById.put(entry.id, new EntryStatus(cached, mirrored));
        }
        return new StatusIndex(catalogEntries.size(), cachedCount, mirroredCount, entryStatusById);
    }

    private LaneUiIndex buildLaneUiIndex(StatusIndex nextIndex) {
        if (laneSpecs == null || laneSpecs.isEmpty()) return LaneUiIndex.empty();
        HashMap<String, LaneUiState> stateByTarget = new HashMap<>();
        int installedOkCount = 0;
        int attentionCount = 0;
        for (LaneSpec lane : laneSpecs) {
            if (lane == null) continue;
            List<PrefixPackCatalog.Entry> entries = resolveLaneEntries(lane.installTarget);
            int totalCount = entries.size();
            int cachedCount = nextIndex.countCached(entries);
            int mirroredCount = nextIndex.countMirrored(entries);
            InstallState state = readInstallState(lane.installTarget);
            stateByTarget.put(
                    lane.installTarget,
                    new LaneUiState(
                            cachedCount,
                            mirroredCount,
                            totalCount,
                            buildLaneStateText(lane, cachedCount, totalCount, mirroredCount, state)
                    )
            );
            if (state == null || !state.exists()) continue;
            String normalized = state.status != null ? state.status.trim().toLowerCase(Locale.US) : "";
            if ("success".equals(normalized)) installedOkCount++;
            else attentionCount++;
        }
        return new LaneUiIndex(installedOkCount, attentionCount, stateByTarget);
    }

    private void updateOverview() {
        StatusIndex statusSnapshot = statusIndex;
        LaneUiIndex laneSnapshot = laneUiIndex;
        int cachedCount = statusSnapshot.cachedEntries;
        int mirroredCount = statusSnapshot.mirroredEntries;
        int totalCount = catalogEntries.size();
        int installedOkCount = laneSnapshot.installedOkCount;
        int attentionCount = laneSnapshot.attentionCount;

        if (tvSession != null) {
            if (statusSnapshot.totalEntries <= 0 && laneSnapshot.stateByTarget.isEmpty()) {
                tvSession.setText("Scanning prepared cache,\nmirrored installers and\nlane state...");
            } else {
                tvSession.setText("Prepared  " + cachedCount + "/" + totalCount
                        + "\nMirror  " + mirroredCount + "/" + totalCount
                        + "\nInstalled  " + installedOkCount
                        + "\nAttention  " + attentionCount);
            }
        }
        if (tvPaths != null) {
            tvPaths.setText("Container\n"
                    + clipMiddle(
                    activity.getContainer() != null
                            ? activity.getContainer().getName()
                            : activity.getString(R.string.not_set),
                    18
            ) + "\n\nC cache\n"
                    + clipMiddle(WINDOWS_INSTALLER_CACHE, 24)
                    + "\n\nStage\n"
                    + clipMiddle(WINDOWS_STAGE_ROOT, 24));
        }
        if (tvFlow != null) {
            tvFlow.setText("Prepare\nZ cache -> C cache\n\nInstall\nstaged lane launcher\n\nProof\nsave_data\\logs");
        }
    }

    private void startLaneCardBuild() {
        if (laneList == null || laneBuildInProgress) return;
        if (laneCardsBuilt && !laneItemViews.isEmpty()) return;
        laneList.removeAllViews();
        laneItemViews.clear();
        nextLaneBuildIndex = 0;
        activeLaneBuildSection = "";
        nextLaneBuildUsesFirstSectionSpacing = true;
        laneCardsBuilt = false;
        laneBuildInProgress = true;
        appendNextLaneCardBatch();
    }

    private void appendNextLaneCardBatch() {
        if (!isShowing() || laneList == null) {
            laneBuildInProgress = false;
            return;
        }
        int appended = 0;
        while (nextLaneBuildIndex < laneSpecs.size() && appended < LANE_BUILD_BATCH_SIZE) {
            LaneSpec lane = laneSpecs.get(nextLaneBuildIndex++);
            if (!lane.sectionTitle.equals(activeLaneBuildSection)) {
                laneList.addView(createSectionHeader(lane.sectionTitle, nextLaneBuildUsesFirstSectionSpacing));
                activeLaneBuildSection = lane.sectionTitle;
                nextLaneBuildUsesFirstSectionSpacing = false;
            }
            View itemView = inflater.inflate(R.layout.prefix_pack_toolkit_lane_item, laneList, false);
            bindLaneItem(itemView, lane);
            laneList.addView(itemView);
            laneItemViews.put(lane.installTarget, itemView);
            appended++;
        }
        if (nextLaneBuildIndex < laneSpecs.size()) {
            laneList.postDelayed(this::appendNextLaneCardBatch, LANE_BUILD_STEP_DELAY_MS);
            return;
        }
        laneBuildInProgress = false;
        laneCardsBuilt = true;
        refreshLaneCards();
        maybeLogDialogRender(statusIndex, laneUiIndex);
    }

    private void refreshLaneCards() {
        if (laneItemViews.isEmpty()) return;
        for (LaneSpec lane : laneSpecs) {
            View itemView = laneItemViews.get(lane.installTarget);
            if (itemView == null) continue;
            populateLaneItem(itemView, lane, resolveLaneEntries(lane.installTarget));
        }
    }

    private View createSectionHeader(String sectionTitle, boolean firstSection) {
        TextView header = new TextView(activity);
        header.setText(sectionTitle);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.4f);
        header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        header.setPadding(10, 5, 10, 5);
        header.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_text));
        header.setBackgroundResource(R.drawable.surface_runtime_prefixpack_header_background);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = Math.round(activity.getResources().getDisplayMetrics().density * (firstSection ? 0f : 4f));
        params.bottomMargin = Math.round(activity.getResources().getDisplayMetrics().density * 3f);
        header.setLayoutParams(params);
        return header;
    }

    private List<LaneSpec> createLaneSpecs() {
        return Arrays.asList(
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_core),
                        "vcrun_full",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_vcpp_title),
                        activity.getString(R.string.prefix_pack_vcpp_summary),
                        true,
                        Arrays.asList("vcrun6sp6", "vcpp_aio")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_managed),
                        "wine_web_stack",
                        R.drawable.ae_icon_open,
                        activity.getString(R.string.prefix_pack_web_title),
                        activity.getString(R.string.prefix_pack_web_summary),
                        false,
                        Arrays.asList("wine_mono_11_0_0", "wine_gecko_2_47_4_x86", "wine_gecko_2_47_4_x86_64")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_managed),
                        "dotnet_framework",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_dotnet_title),
                        activity.getString(R.string.prefix_pack_dotnet_summary),
                        true,
                        Arrays.asList("dotnetfx35sp1", "dotnetfx40_full", "dotnetfx48")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_legacy),
                        "directx_jun2010",
                        R.drawable.ae_icon_diagnostics,
                        activity.getString(R.string.prefix_pack_directx_title),
                        activity.getString(R.string.prefix_pack_directx_summary),
                        false,
                        Collections.singletonList("directx_jun2010")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_legacy),
                        "xna",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_xna_title),
                        activity.getString(R.string.prefix_pack_xna_summary),
                        false,
                        Arrays.asList("xnafx31_refresh", "xnafx40_refresh")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_legacy),
                        "openal",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_openal_title),
                        activity.getString(R.string.prefix_pack_openal_summary),
                        false,
                        Collections.singletonList("openal_1_1")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_media),
                        "physx",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_physx_title),
                        activity.getString(R.string.prefix_pack_physx_summary),
                        true,
                        Arrays.asList("physx_system_9_21_0713", "physx_legacy_9_13_0604")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_media),
                        "lavfilters",
                        R.drawable.ae_icon_package,
                        activity.getString(R.string.prefix_pack_lavfilters_title),
                        activity.getString(R.string.prefix_pack_lavfilters_summary),
                        true,
                        Collections.singletonList("lavfilters_0_81")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_diagnostics),
                        "legacy_dx_sdk",
                        R.drawable.ae_icon_diagnostics,
                        activity.getString(R.string.prefix_pack_dxsdk_title),
                        activity.getString(R.string.prefix_pack_dxsdk_summary),
                        true,
                        Collections.singletonList("dxsdk_jun10")
                ),
                new LaneSpec(
                        activity.getString(R.string.prefix_pack_section_diagnostics),
                        "graphics_diag",
                        R.drawable.ae_icon_diagnostics,
                        activity.getString(R.string.prefix_pack_glview_title),
                        activity.getString(R.string.prefix_pack_glview_summary),
                        true,
                        Collections.singletonList("glview_6499")
                )
        );
    }

    private void bindLaneItem(View itemView, LaneSpec lane) {
        List<PrefixPackCatalog.Entry> entries = resolveLaneEntries(lane.installTarget);
        populateLaneItem(itemView, lane, entries);
        styleLaneCard(
                itemView,
                itemView.findViewById(R.id.IVLaneIcon),
                itemView.findViewById(R.id.TVLaneTitle),
                itemView.findViewById(R.id.TVLaneSection),
                itemView.findViewById(R.id.TVLaneState),
                itemView.findViewById(R.id.TVLaneSummary),
                itemView.findViewById(R.id.TVLaneSource)
        );

        Button btPrepare = itemView.findViewById(R.id.BTLanePrepare);
        Button btInstall = itemView.findViewById(R.id.BTLaneInstall);
        Button btCleanup = itemView.findViewById(R.id.BTLaneCleanup);
        if (btPrepare != null) btPrepare.setOnClickListener(v -> fetchEntries(lane.title, lane.installTarget, resolveLaneEntries(lane.installTarget)));
        if (btInstall != null) btInstall.setOnClickListener(v -> launchInstall(lane.title, lane.installTarget, resolveLaneEntries(lane.installTarget)));
        if (btCleanup != null) btCleanup.setOnClickListener(v -> cleanupLane(lane.title, lane.installTarget, resolveLaneEntries(lane.installTarget)));
        disableViewFocus(btPrepare);
        disableViewFocus(btInstall);
        disableViewFocus(btCleanup);
    }

    private void populateLaneItem(View itemView, LaneSpec lane, List<PrefixPackCatalog.Entry> entries) {
        ImageView ivIcon = itemView.findViewById(R.id.IVLaneIcon);
        TextView tvTitle = itemView.findViewById(R.id.TVLaneTitle);
        TextView tvSection = itemView.findViewById(R.id.TVLaneSection);
        TextView tvState = itemView.findViewById(R.id.TVLaneState);
        TextView tvSummary = itemView.findViewById(R.id.TVLaneSummary);
        TextView tvSource = itemView.findViewById(R.id.TVLaneSource);
        LaneUiState laneUiState = laneUiIndex.get(lane.installTarget);
        int cached = laneUiState != null ? laneUiState.cachedCount : 0;
        int total = laneUiState != null ? laneUiState.totalCount : entries.size();

        if (ivIcon != null) ivIcon.setImageResource(lane.iconResId);
        if (tvTitle != null) tvTitle.setText(lane.title);
        if (tvSection != null) {
            tvSection.setText(lane.sectionTitle);
            tvSection.setVisibility(View.GONE);
        }
        if (tvState != null) {
            tvState.setText(laneUiState != null
                    ? laneUiState.stateText
                    : "Scanning cache and staged installer state...");
        }
        if (tvSummary != null) tvSummary.setText(lane.summary);
        if (tvSource != null) tvSource.setVisibility(View.GONE);
    }

    private List<PrefixPackCatalog.Entry> resolveLaneEntries(String installTarget) {
        List<PrefixPackCatalog.Entry> cached = laneEntryCache.get(installTarget);
        return cached != null ? cached : Collections.emptyList();
    }

    private List<PrefixPackCatalog.Entry> resolveEntries(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        ArrayList<PrefixPackCatalog.Entry> entries = new ArrayList<>();
        for (String id : ids) {
            PrefixPackCatalog.Entry entry = PrefixPackCatalog.findById(catalogEntries, id);
            if (entry != null) entries.add(entry);
        }
        return entries;
    }

    private String buildLaneStateText(LaneSpec lane, int cached, int total, int mirrored, InstallState state) {
        if (state == null || !state.exists()) {
            if (total > 0 && cached == total && mirrored == total) {
                return "Ready • cache " + cached + "/" + total + " • mirror " + mirrored + "/" + total;
            }
            return "Needs prepare • cache " + cached + "/" + total + " • mirror " + mirrored + "/" + total;
        }
        return describeInstallState(state, lane.mayRequireGui)
                + " • cache " + cached + "/" + total
                + " • mirror " + mirrored + "/" + total;
    }

    private String buildLaneSourceSummary(LaneSpec lane, List<PrefixPackCatalog.Entry> entries) {
        ArrayList<String> sourceLabels = new ArrayList<>();
        int shaPinned = 0;
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null) continue;
            if (!entry.sourceLabel.isEmpty() && !sourceLabels.contains(entry.sourceLabel)) {
                sourceLabels.add(entry.sourceLabel);
            }
            if (entry.hasSha256()) shaPinned++;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Sources: ");
        if (sourceLabels.isEmpty()) {
            builder.append(activity.getString(R.string.not_set));
        } else {
            builder.append(sourceLabels.get(0));
            if (sourceLabels.size() > 1) {
                builder.append(" +").append(sourceLabels.size() - 1);
            }
        }
        builder.append(" • sha ").append(shaPinned).append("/").append(entries.size());
        return builder.toString();
    }

    private int countCached(List<PrefixPackCatalog.Entry> entries) {
        if (isMainThread()) return statusIndex.countCached(entries);
        if (entries == null || entries.isEmpty()) return 0;
        int count = 0;
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry != null && isCachedStrict(entry)) count++;
        }
        return count;
    }

    private boolean isCached(PrefixPackCatalog.Entry entry) {
        if (isMainThread()) return statusIndex.isCached(entry);
        return isCachedStrict(entry);
    }

    private boolean isCachedStrict(PrefixPackCatalog.Entry entry) {
        if (entry == null) return false;
        File cacheFile = new File(getCacheDir(), entry.fileName);
        return entry.isValidFile(cacheFile);
    }

    private boolean isCachedIndexed(PrefixPackCatalog.Entry entry) {
        if (entry == null) return false;
        return entry.isPresentFile(new File(getCacheDir(), entry.fileName));
    }

    private File getCacheDir() {
        return new File(imageFs.getRootDir(), "opt/ae/prefix-pack/cache");
    }

    private File getContainerCacheDir() {
        return new File(getDriveCRootDir(), "AePrefixPack/cache");
    }

    private File getStateDir() {
        return new File(getSaveRootDir(), "state");
    }

    private File getLogDir() {
        return new File(getSaveRootDir(), "logs");
    }

    private File getStageRootDir() {
        return new File(getDriveCRootDir(), "AePrefixPack/staging");
    }

    private File getHostWinePrefixDir() {
        if (resolvedHostWinePrefixDir != null && resolvedHostWinePrefixDir.isDirectory()) {
            return resolvedHostWinePrefixDir;
        }
        File direct = imageFs.getWinePrefixDir();
        if (new File(direct, "drive_c").isDirectory()) {
            resolvedHostWinePrefixDir = direct;
            return direct;
        }
        File homeDir = new File(imageFs.getRootDir(), "home");
        File[] homeEntries = homeDir.listFiles();
        if (homeEntries != null) {
            for (File entry : homeEntries) {
                if (entry == null || !entry.isDirectory()) continue;
                File candidate = new File(entry, ".wine");
                if (new File(candidate, "drive_c").isDirectory()) {
                    resolvedHostWinePrefixDir = candidate;
                    return candidate;
                }
            }
        }
        resolvedHostWinePrefixDir = direct;
        return direct;
    }

    private File getDriveCRootDir() {
        return new File(getHostWinePrefixDir(), "drive_c");
    }

    private File getWindowsUsersDir() {
        return new File(getDriveCRootDir(), "users");
    }

    private File getHostWindowsUserDir() {
        if (resolvedHostWindowsUserDir != null && resolvedHostWindowsUserDir.isDirectory()) {
            return resolvedHostWindowsUserDir;
        }
        File usersDir = getWindowsUsersDir();
        File preferred = new File(usersDir, ImageFs.USER);
        if (preferred.isDirectory()) {
            resolvedHostWindowsUserDir = preferred;
            return preferred;
        }
        File[] candidates = usersDir.listFiles();
        if (candidates != null) {
            for (File candidate : candidates) {
                if (candidate == null || !candidate.isDirectory()) continue;
                if ("public".equalsIgnoreCase(candidate.getName())) continue;
                if (new File(candidate, "Documents").isDirectory()) {
                    resolvedHostWindowsUserDir = candidate;
                    return candidate;
                }
            }
        }
        resolvedHostWindowsUserDir = preferred;
        return preferred;
    }

    private File getSaveRootDir() {
        return new File(new File(getHostWindowsUserDir(), "Documents"), "AePrefixPack/save_data");
    }

    private String getWindowsUserName() {
        File userDir = getHostWindowsUserDir();
        return userDir != null && userDir.getName() != null && !userDir.getName().trim().isEmpty()
                ? userDir.getName().trim()
                : ImageFs.USER;
    }

    private String getWindowsSaveRoot() {
        return "C:\\users\\" + getWindowsUserName() + "\\Documents\\AePrefixPack\\save_data";
    }

    private String getWindowsLogRoot() {
        return getWindowsSaveRoot() + "\\logs";
    }

    private String getWindowsStateRoot() {
        return getWindowsSaveRoot() + "\\state";
    }

    private File resolveWineRelativeFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) return null;
        String normalized = relativePath.trim();
        if (normalized.startsWith(ImageFs.WINEPREFIX + "/")) {
            String suffix = normalized.substring(ImageFs.WINEPREFIX.length() + 1);
            return new File(getHostWinePrefixDir(), suffix);
        }
        return new File(imageFs.getRootDir(), normalized);
    }

    private File getStateFile(String installTarget) {
        return new File(getStateDir(), installTarget + ".properties");
    }

    private InstallState readInstallState(String installTarget) {
        File stateFile = getStateFile(installTarget);
        if (!stateFile.isFile()) {
            return new InstallState(stateFile, "", "", "", "", "", "", "", "", "");
        }
        if (stateFile.length() > MAX_REASONABLE_STATE_FILE_BYTES) {
            return new InstallState(
                    stateFile,
                    "failed",
                    "",
                    "",
                    "",
                    "State marker is oversized or corrupted. Use Clean, then rerun Install so Prefix Pack can restage the lane.",
                    "",
                    "",
                    "Open Clean for this lane, then rerun Prepare or Install.",
                    ""
            );
        }

        Properties properties = parseLooseProperties(stateFile);
        if (properties == null) {
            return new InstallState(stateFile, "failed", "", "", "", "Unable to parse state marker", "", "", "", "");
        }
        return new InstallState(
                stateFile,
                properties.getProperty("status", "").trim(),
                properties.getProperty("exit_code", "").trim(),
                properties.getProperty("updated_at", "").trim(),
                properties.getProperty("log_file", "").trim(),
                properties.getProperty("detail", "").trim(),
                properties.getProperty("launcher_file", "").trim(),
                properties.getProperty("primary_payload", "").trim(),
                properties.getProperty("next_action", "").trim(),
                properties.getProperty("requested_by", "").trim()
        );
    }

    private Properties parseLooseProperties(File file) {
        if (file == null || !file.isFile()) return null;
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
            return properties;
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean isMirrored(PrefixPackCatalog.Entry entry) {
        if (isMainThread()) return statusIndex.isMirrored(entry);
        return isMirroredStrict(entry);
    }

    private boolean isMirroredStrict(PrefixPackCatalog.Entry entry) {
        if (entry == null) return false;
        return entry.isValidFile(new File(getContainerCacheDir(), entry.fileName));
    }

    private boolean isMirroredIndexed(PrefixPackCatalog.Entry entry) {
        if (entry == null) return false;
        return entry.isPresentFile(new File(getContainerCacheDir(), entry.fileName));
    }

    private int countMirrored(List<PrefixPackCatalog.Entry> entries) {
        if (isMainThread()) return statusIndex.countMirrored(entries);
        if (entries == null || entries.isEmpty()) return 0;
        int count = 0;
        for (PrefixPackCatalog.Entry entry : entries) {
            if (isMirroredStrict(entry)) count++;
        }
        return count;
    }

    private boolean mirrorEntriesToInstallerCache(List<PrefixPackCatalog.Entry> entries) {
        if (entries == null || entries.isEmpty()) return true;
        File containerCacheDir = getContainerCacheDir();
        if (!containerCacheDir.exists() && !containerCacheDir.mkdirs()) {
            return false;
        }
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || !isCachedStrict(entry)) continue;
            File source = new File(getCacheDir(), entry.fileName);
            File destination = new File(containerCacheDir, entry.fileName);
            if (entry.isValidFile(destination) && destination.length() == source.length()) continue;
            if (destination.exists() && !destination.delete()) return false;
            if (!FileUtils.copy(source, destination)) return false;
            if (!entry.isValidFile(destination)) return false;
        }
        return true;
    }

    private int importLocalDonorPayloadsIfNeeded(String installTarget, List<PrefixPackCatalog.Entry> entries) {
        if (!"xna".equalsIgnoreCase(firstNonEmpty(installTarget, "").trim())) return 0;
        if (entries == null || entries.isEmpty()) return 0;

        Container container = activity.getContainer();
        if (container == null) return 0;

        String installPath = firstNonEmpty(container.getInstallPath(), "");
        String aDrivePath = ContainerUtils.INSTANCE.getADrivePath(container.getDrives());
        Map<String, File> localPayloads = PrefixPackLocalImportResolver.resolveXnaPayloads(entries, installPath, aDrivePath);
        if (localPayloads.isEmpty()) return 0;

        File cacheDir = getCacheDir();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) return 0;

        int importedCount = 0;
        ArrayList<String> importedFiles = new ArrayList<>();
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || isCachedStrict(entry)) continue;
            File source = localPayloads.get(PrefixPackLocalImportResolver.normalizeFileKey(entry.fileName));
            if (source == null || !source.isFile()) continue;

            File destination = new File(cacheDir, entry.fileName);
            if (destination.exists() && !destination.delete()) continue;
            if (!FileUtils.copy(source, destination)) continue;
            if (!entry.isValidFile(destination)) {
                deleteIfExists(destination);
                continue;
            }
            importedCount++;
            importedFiles.add(entry.fileName);
        }

        if (importedCount > 0) {
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "PREFIX_PACK_LOCAL_DONOR_IMPORT",
                    null,
                    "runtime_ui",
                    "prefix_pack_local_donor_import",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "imported_count", importedCount,
                            "imported_files", importedFiles.toString(),
                            "install_path", installPath,
                            "a_drive_path", firstNonEmpty(aDrivePath, "")
                    )
            );
        }
        return importedCount;
    }

    private ArrayList<PrefixPackCatalog.Entry> collectStrictMissingDownloadables(List<PrefixPackCatalog.Entry> entries) {
        ArrayList<PrefixPackCatalog.Entry> missingEntries = new ArrayList<>();
        if (entries == null || entries.isEmpty()) return missingEntries;
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || !entry.isDownloadable() || isCachedStrict(entry)) continue;
            missingEntries.add(entry);
        }
        return missingEntries;
    }

    private String downloadMissingEntries(ArrayList<PrefixPackCatalog.Entry> missingEntries, PreloaderDialog preloaderDialog) {
        if (missingEntries == null || missingEntries.isEmpty()) return "";
        File cacheDir = getCacheDir();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        for (int i = 0; i < missingEntries.size(); i++) {
            PrefixPackCatalog.Entry entry = missingEntries.get(i);
            File outputFile = new File(cacheDir, entry.fileName);
            boolean downloaded = Downloader.downloadFile(entry.sourceUrl, outputFile);
            if (!downloaded) {
                return entry.fileName;
            }
            if (!entry.isValidFile(outputFile)) {
                if (outputFile.exists()) outputFile.delete();
                return entry.fileName + " (empty or checksum mismatch)";
            }
            final int progress = ((i + 1) * 100) / Math.max(1, missingEntries.size());
            activity.runOnUiThread(() -> preloaderDialog.setProgress(progress));
        }
        return "";
    }

    private String describeInstallState(InstallState state, boolean mayRequireGui) {
        if (state == null || !state.exists()) return "install not run";
        String normalized = state.status.toLowerCase(Locale.US);
        if ("success".equals(normalized)) return "installed";
        if ("scheduled".equals(normalized)) return "staged, waiting to open";
        if ("queued".equals(normalized)) return "staged, launch proof pending";
        if ("running".equals(normalized)) return mayRequireGui ? "installer running or waiting for GUI" : "installer running";
        if ("interactive".equals(normalized)) return mayRequireGui ? "launcher proved, waiting for installer GUI" : "launcher proved, follow logs";
        if ("missing_payload".equals(normalized)) return "payload missing";
        if ("failed".equals(normalized)) {
            if (!state.exitCode.isEmpty()) return "failed rc=" + state.exitCode;
            return "failed";
        }
        return normalized.isEmpty() ? "install not run" : normalized.replace('_', ' ');
    }

    private boolean isLaneLaunchable(String installTarget) {
        if (installTarget == null || installTarget.trim().isEmpty()) return false;
        String stageSegment = sanitizeTargetSegment(installTarget);
        File stageDir = new File(getStageRootDir(), stageSegment);
        File stagedDispatch = new File(stageDir, "launch-" + stageSegment + ".vbs");
        File stagedLauncher = new File(stageDir, "install-" + stageSegment + ".cmd");
        if (stagedDispatch.isFile() || stagedLauncher.isFile()) return true;
        InstallState state = readInstallState(installTarget);
        if (state == null || !state.exists()) return false;
        String normalized = state.status != null ? state.status.trim().toLowerCase(Locale.US) : "";
        return "scheduled".equals(normalized)
                || "queued".equals(normalized)
                || "interactive".equals(normalized)
                || "running".equals(normalized)
                || "failed".equals(normalized);
    }

    private void fetchEntries(String title, String installTarget, List<PrefixPackCatalog.Entry> entries) {
        fetchEntries(title, installTarget, entries, null);
    }

    private void fetchEntries(String title, String installTarget, List<PrefixPackCatalog.Entry> entries, Runnable onSuccess) {
        PreloaderDialog preloaderDialog = new PreloaderDialog(activity);
        preloaderDialog.show(R.string.prefix_pack_preparing_payloads);

        ioExecutor.execute(() -> {
            importLocalDonorPayloadsIfNeeded(installTarget, entries);
            ArrayList<PrefixPackCatalog.Entry> missingEntries = collectStrictMissingDownloadables(entries);
            boolean hadMissing = !missingEntries.isEmpty();
            String failedFile = hadMissing ? downloadMissingEntries(missingEntries, preloaderDialog) : "";
            boolean success = failedFile.isEmpty() && mirrorEntriesToInstallerCache(entries);
            activity.runOnUiThread(() -> {
                preloaderDialog.close();
                if (!isShowing()) return;
                if (success) {
                    refreshStatusIndexAsync();
                    AppUtils.showToast(activity, activity.getString(
                            hadMissing ? R.string.prefix_pack_fetch_complete : R.string.prefix_pack_everything_cached,
                            title
                    ));
                    if (onSuccess != null) onSuccess.run();
                } else {
                    ContentDialog dialog = new ContentDialog(activity);
                    dialog.setTitle(R.string.prefix_pack_toolkit);
                    dialog.setIcon(R.drawable.ae_icon_info);
                    dialog.setMessage(activity.getString(
                            R.string.prefix_pack_fetch_failed,
                            failedFile.isEmpty() ? activity.getString(R.string.prefix_pack_cache_mirror_failed) : failedFile
                    ));
                    View cancelButton = dialog.findViewById(R.id.BTCancel);
                    if (cancelButton != null) cancelButton.setVisibility(View.GONE);
                    dialog.show();
                    styleNestedDialog(dialog);
                    refreshStatusIndexAsync();
                }
            });
        });
    }

    private void launchInstall(String title, String installTarget, List<PrefixPackCatalog.Entry> entries) {
        launchInstall(title, installTarget, entries, "");
    }

    private void launchInstall(String title, String installTarget, List<PrefixPackCatalog.Entry> entries, String requestingTarget) {
        String prerequisiteTarget = resolveInstallPrerequisite(installTarget);
        if (!prerequisiteTarget.isEmpty()) {
            InstallState prerequisiteState = readInstallState(prerequisiteTarget);
            if (!isInstallSatisfiedForRequester(prerequisiteTarget, installTarget, prerequisiteState)) {
                if (isInstallInFlightForRequester(prerequisiteTarget, installTarget, prerequisiteState)) {
                    AppUtils.showToast(activity, activity.getString(
                            R.string.prefix_pack_dependency_pending,
                            title,
                            resolveLaneTitle(prerequisiteTarget)
                    ));
                    return;
                }
                AppUtils.showToast(activity, activity.getString(
                        R.string.prefix_pack_dependency_redirect,
                            title,
                            resolveLaneTitle(prerequisiteTarget)
                    ));
                launchInstall(resolveLaneTitle(prerequisiteTarget), prerequisiteTarget, resolveLaneEntries(prerequisiteTarget), installTarget);
                return;
            }
        }
        repairManagedRuntimeContractHostIfNeeded(installTarget);
        PreloaderDialog preloaderDialog = new PreloaderDialog(activity);
        preloaderDialog.show(R.string.prefix_pack_preparing_payloads);
        ioExecutor.execute(() -> {
            int localImportCount = importLocalDonorPayloadsIfNeeded(installTarget, entries);
            ArrayList<PrefixPackCatalog.Entry> missingEntries = collectStrictMissingDownloadables(entries);
            boolean hadMissing = !missingEntries.isEmpty();
            if (hadMissing) {
                ForensicLogger.logEvent(
                        activity,
                        "info",
                        "PREFIX_PACK_INSTALL_AUTO_PREPARE",
                        null,
                        "runtime_ui",
                        "prefix_pack_install_auto_prepare",
                        ForensicLogger.fields(
                                "install_target", installTarget,
                                "title", title,
                                "missing_count", missingEntries.size()
                        )
                );
            }
            String failedFile = hadMissing ? downloadMissingEntries(missingEntries, preloaderDialog) : "";
            boolean mirrorSuccess = failedFile.isEmpty() && mirrorEntriesToInstallerCache(entries);
            InstallArtifacts artifacts = mirrorSuccess ? prepareInstallArtifacts(title, installTarget, entries, requestingTarget) : null;
            ArrayList<String> runtimeDispatchCommands = artifacts != null
                    ? buildRuntimeDispatchCommands(
                            installTarget,
                            artifacts.windowsLauncherPath,
                            artifacts.windowsDispatchPath,
                            artifacts.windowsPrimaryPayload
                    )
                    : new ArrayList<>();
            activity.runOnUiThread(() -> {
                preloaderDialog.close();
                if (!isShowing()) return;
                if ((hadMissing || localImportCount > 0) && failedFile.isEmpty()) {
                    refreshStatusIndexAsync();
                }
                if (!mirrorSuccess) {
                    if (!failedFile.isEmpty()) {
                        AppUtils.showToast(activity, activity.getString(R.string.prefix_pack_fetch_failed, failedFile));
                    } else {
                        AppUtils.showToast(activity, R.string.prefix_pack_cache_mirror_failed);
                    }
                    return;
                }
                if (artifacts == null || runtimeDispatchCommands.isEmpty()) {
                    AppUtils.showToast(activity, R.string.prefix_pack_stage_failed);
                    return;
                }
                ForensicLogger.logEvent(
                        activity,
                        "info",
                        "PREFIX_PACK_INSTALL_LAUNCH",
                        null,
                        "runtime_ui",
                        "prefix_pack_install_launch",
                        ForensicLogger.fields(
                                "install_target", installTarget,
                                "entry_count", entries.size(),
                                "dispatch_command_length", runtimeDispatchCommands.get(0).length(),
                                "dispatch_command_count", runtimeDispatchCommands.size(),
                                "dispatch_route", preferDesktopShellDispatch(installTarget) ? "desktop_shell_first" : "detached_guest_first",
                                "cache_dir", CACHE_ROOT,
                                "installer_cache", WINDOWS_INSTALLER_CACHE,
                                "launcher_path", artifacts.windowsLauncherPath,
                                "dispatch_path", artifacts.windowsDispatchPath,
                                "primary_payload", artifacts.windowsPrimaryPayload,
                                "primary_payload_label", artifacts.primaryPayloadLabel,
                                "state_path", artifacts.windowsStatePath
                        )
                );
                if (!dismissAndDispatchRuntimeCommand(
                        runtimeDispatchCommands,
                        installTarget,
                        artifacts.windowsLauncherPath,
                        artifacts.windowsDispatchPath
                )) {
                    AppUtils.showToast(activity, R.string.prefix_pack_runtime_unavailable);
                    return;
                }
                AppUtils.showToast(activity, activity.getString(
                        R.string.prefix_pack_install_queued_specific,
                        title,
                        firstNonEmpty(artifacts.primaryPayloadLabel, title)
                ));
            });
        });
    }

    private boolean runCommand(String command) {
        return scheduleRuntimeCommand(command, "", 0);
    }

    private void closeAndRunCommand(String command) {
        if (!dismissAndRunCommand(command, "")) {
            AppUtils.showToast(activity, R.string.prefix_pack_runtime_unavailable);
        }
    }

    private boolean dismissAndRunCommand(String command, String installTarget) {
        if (command == null || command.trim().isEmpty()) return false;
        dismiss();
        View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
        if (anchor == null) return false;
        String normalizedCommand = command.trim();
        long dispatchStartedAt = System.currentTimeMillis();
        anchor.postDelayed(() -> attemptRuntimeCommandDispatch(
                Collections.singletonList(normalizedCommand),
                installTarget,
                "",
                dispatchStartedAt,
                0,
                0
        ), 60L);
        return true;
    }

    private boolean dismissAndDispatchRuntimeCommand(List<String> commands, String installTarget, String launcherPath, String dispatchPath) {
        ArrayList<String> normalizedCommands = normalizeDispatchCommands(commands);
        if (normalizedCommands.isEmpty()) return false;
        dismiss();
        long dispatchStartedAt = System.currentTimeMillis();
        boolean desktopShellFirst = preferDesktopShellDispatch(installTarget);
        if (!desktopShellFirst && attemptDetachedGuestDispatch(
                buildDetachedGuestDispatchCommands(installTarget, launcherPath, dispatchPath),
                installTarget,
                launcherPath,
                dispatchStartedAt,
                false
        )) {
            return true;
        }
        View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
        if (anchor == null) return false;
        anchor.postDelayed(() -> attemptRuntimeCommandDispatch(
                normalizedCommands,
                installTarget,
                launcherPath,
                dispatchStartedAt,
                0,
                0
        ), 80L);
        return true;
    }

    private void attemptRuntimeCommandDispatch(List<String> commands, String installTarget, String launcherPath, long dispatchStartedAt, int candidateIndex, int attempt) {
        ArrayList<String> normalizedCommands = normalizeDispatchCommands(commands);
        if (normalizedCommands.isEmpty()) return;
        int boundedCandidateIndex = Math.max(0, Math.min(candidateIndex, normalizedCommands.size() - 1));
        String normalizedCommand = normalizedCommands.get(boundedCandidateIndex);
        if (!isDesktopShellReady(dispatchStartedAt)) {
            View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
            if (anchor != null && attempt + 1 < RUNTIME_DISPATCH_MAX_ATTEMPTS) {
                anchor.postDelayed(
                        () -> attemptRuntimeCommandDispatch(
                                normalizedCommands,
                                installTarget,
                                launcherPath,
                                dispatchStartedAt,
                                boundedCandidateIndex,
                                attempt + 1
                        ),
                        RUNTIME_DISPATCH_RETRY_MS
                );
                return;
            }
        }
        if (scheduleRuntimeCommand(normalizedCommand, installTarget, attempt)) {
            InstallState dispatchState = readInstallState(installTarget);
            boolean interactiveGuiLane = isInteractiveGuiLane(
                    installTarget,
                    dispatchState != null ? dispatchState.primaryPayload : ""
            );
            markRuntimeDispatchQueuedState(
                    installTarget,
                    launcherPath,
                    interactiveGuiLane
                            ? "Desktop shell accepted the GUI installer hand-off. Waiting for a visible installer window or a fresh lane proof before treating the launch as started."
                            : "Desktop shell accepted the staged launcher command. Wait for the installer window or the lane logs to update."
            );
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "PREFIX_PACK_RUNTIME_WINHANDLER_DISPATCHED",
                    null,
                    "runtime_ui",
                    "prefix_pack_runtime_winhandler_dispatched",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "launcher_path", firstNonEmpty(launcherPath, ""),
                            "command", normalizedCommand,
                            "attempt", attempt,
                            "candidate_index", boundedCandidateIndex,
                            "candidate_count", normalizedCommands.size()
                    )
            );
            scheduleRuntimeDispatchVerification(
                    normalizedCommands,
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    boundedCandidateIndex
            );
            return;
        }

        boolean canRetryWinHandler = activity.getWinHandler() != null
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && attempt + 1 < RUNTIME_DISPATCH_MAX_ATTEMPTS;
        if (canRetryWinHandler) {
            View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
            if (anchor != null) {
                anchor.postDelayed(
                        () -> attemptRuntimeCommandDispatch(
                                normalizedCommands,
                                installTarget,
                                launcherPath,
                                dispatchStartedAt,
                                boundedCandidateIndex,
                                attempt + 1
                        ),
                        RUNTIME_DISPATCH_RETRY_MS
                );
                return;
            }
        }

        ForensicLogger.logEvent(
                activity,
                "warning",
                "PREFIX_PACK_RUNTIME_WAIT_TIMEOUT",
                null,
                "runtime_ui",
                "prefix_pack_runtime_wait_timeout",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "attempt", attempt,
                        "winhandler_command", normalizedCommand,
                        "max_attempts", RUNTIME_DISPATCH_MAX_ATTEMPTS,
                        "retry_ms", RUNTIME_DISPATCH_RETRY_MS
                )
        );
        refreshRuntimeWaitState(installTarget, launcherPath, dispatchStartedAt);
        AppUtils.showToast(activity, R.string.prefix_pack_runtime_waiting_shell);
    }

    private boolean isDesktopShellReady(long dispatchStartedAt) {
        if (activity.isDesktopShellCommandReady()) return true;
        boolean explorerReady = false;
        boolean wfmReady = false;
        String[] entries = new File("/proc").list();
        if (entries != null) {
            for (String entry : entries) {
                if (entry == null || entry.isEmpty()) continue;
                for (int i = 0; i < entry.length(); i++) {
                    if (!Character.isDigit(entry.charAt(i))) {
                        entry = null;
                        break;
                    }
                }
                if (entry == null) continue;
                String commandLine = readProcCmdline(Integer.parseInt(entry)).toLowerCase(Locale.US);
                if (commandLine.isEmpty()) continue;
                if (commandLine.contains("explorer.exe")) explorerReady = true;
                if (commandLine.contains("wfm.exe")) wfmReady = true;
                if (explorerReady && wfmReady) return true;
            }
        }
        if (explorerReady && wfmReady) return true;
        if (!explorerReady) return false;
        return System.currentTimeMillis() - dispatchStartedAt >= RUNTIME_SHELL_FALLBACK_GRACE_MS;
    }

    private String readProcCmdline(int pid) {
        File cmdlineFile = new File(String.format(Locale.US, "/proc/%d/cmdline", pid));
        if (!cmdlineFile.isFile()) return "";
        try (FileInputStream inputStream = new FileInputStream(cmdlineFile)) {
            byte[] buffer = new byte[4096];
            int count = inputStream.read(buffer);
            if (count <= 0) return "";
            StringBuilder builder = new StringBuilder(count);
            for (int i = 0; i < count; i++) {
                char ch = (char) buffer[i];
                builder.append(ch == '\0' ? ' ' : ch);
            }
            return builder.toString().trim().replaceAll("\\s+", " ");
        } catch (IOException ignored) {
            return "";
        }
    }

    private void scheduleRuntimeDispatchVerification(List<String> commands, String installTarget, String launcherPath, long dispatchStartedAt, int candidateIndex) {
        scheduleRuntimeDispatchVerification(commands, installTarget, launcherPath, dispatchStartedAt, candidateIndex, 0);
    }

    private void scheduleRuntimeDispatchVerification(List<String> commands, String installTarget, String launcherPath, long dispatchStartedAt, int candidateIndex, int verifyAttempt) {
        View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
        if (anchor == null) return;
        ArrayList<String> normalizedCommands = normalizeDispatchCommands(commands);
        if (normalizedCommands.isEmpty()) return;
        ForensicLogger.logEvent(
                activity,
                verifyAttempt == 0 ? "info" : "warning",
                "PREFIX_PACK_RUNTIME_VERIFY_ARMED",
                null,
                "runtime_ui",
                "prefix_pack_runtime_verify_armed",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "candidate_index", candidateIndex,
                        "candidate_count", normalizedCommands.size(),
                        "verify_attempt", verifyAttempt
                )
        );
        anchor.postDelayed(() -> verifyRuntimeDispatch(
                normalizedCommands,
                installTarget,
                launcherPath,
                dispatchStartedAt,
                candidateIndex,
                verifyAttempt
        ), verifyAttempt == 0 ? RUNTIME_DISPATCH_VERIFY_DELAY_MS : RUNTIME_DISPATCH_VERIFY_RECHECK_MS);
    }

    private void verifyRuntimeDispatch(List<String> commands, String installTarget, String launcherPath, long dispatchStartedAt, int candidateIndex, int verifyAttempt) {
        ArrayList<String> normalizedCommands = normalizeDispatchCommands(commands);
        if (normalizedCommands.isEmpty()) return;
        ForensicLogger.logEvent(
                activity,
                verifyAttempt == 0 ? "info" : "warning",
                "PREFIX_PACK_RUNTIME_VERIFY_TICK",
                null,
                "runtime_ui",
                "prefix_pack_runtime_verify_tick",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "candidate_index", candidateIndex,
                        "candidate_count", normalizedCommands.size(),
                        "verify_attempt", verifyAttempt
                )
        );
        if (hasFreshDispatchProof(installTarget, dispatchStartedAt)) {
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "PREFIX_PACK_RUNTIME_DISPATCH_CONFIRMED",
                    null,
                    "runtime_ui",
                    "prefix_pack_runtime_dispatch_confirmed",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "launcher_path", firstNonEmpty(launcherPath, ""),
                            "candidate_index", candidateIndex,
                            "candidate_count", normalizedCommands.size()
                    )
            );
            return;
        }

        if (verifyAttempt < RUNTIME_DISPATCH_VERIFY_MAX_RECHECKS) {
            scheduleRuntimeDispatchVerification(
                    normalizedCommands,
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    candidateIndex,
                    verifyAttempt + 1
            );
            return;
        }

        if (candidateIndex + 1 >= normalizedCommands.size()
                && maybeAttemptDetachedGuestFallback(
                installTarget,
                launcherPath,
                buildWindowsDispatchPath(installTarget),
                dispatchStartedAt
        )) {
            return;
        }

        if (candidateIndex + 1 < normalizedCommands.size()) {
            int nextCandidate = candidateIndex + 1;
            ForensicLogger.logEvent(
                    activity,
                    "warning",
                    "PREFIX_PACK_RUNTIME_DISPATCH_RETRY",
                    null,
                    "runtime_ui",
                    "prefix_pack_runtime_dispatch_retry",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "launcher_path", firstNonEmpty(launcherPath, ""),
                            "candidate_index", candidateIndex,
                            "next_candidate_index", nextCandidate,
                            "candidate_count", normalizedCommands.size()
                    )
            );
            attemptRuntimeCommandDispatch(
                    normalizedCommands,
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    nextCandidate,
                    0
            );
            return;
        }

        ForensicLogger.logEvent(
                activity,
                "warning",
                "PREFIX_PACK_RUNTIME_DISPATCH_UNPROVEN",
                null,
                "runtime_ui",
                "prefix_pack_runtime_dispatch_unproven",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "candidate_count", normalizedCommands.size()
                )
        );
        refreshRuntimeWaitState(installTarget, launcherPath, dispatchStartedAt);
        AppUtils.showToast(activity, R.string.prefix_pack_runtime_waiting_shell);
    }

    private boolean scheduleRuntimeCommand(String command, String installTarget, int attempt) {
        if (command == null || command.trim().isEmpty()) return false;
        if (activity.isFinishing() || activity.isDestroyed()) return false;
        if (activity.getWinHandler() == null) return false;
        if (activity.getWinHandler().isReady()) {
            activity.getWinHandler().exec(command);
            markRuntimeDispatchQueuedState(
                    installTarget,
                    "",
                    "Runtime command was dispatched into the desktop shell. Wait for the target window or lane logs to update."
            );
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    "PREFIX_PACK_RUNTIME_EXEC_DISPATCHED",
                    null,
                    "runtime_ui",
                    "prefix_pack_runtime_exec_dispatched",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "command_length", command.length(),
                            "attempt", attempt
                    )
            );
            return true;
        }
        return false;
    }

    private boolean maybeAttemptDetachedGuestFallback(String installTarget, String launcherPath, String dispatchPath, long dispatchStartedAt) {
        return attemptDetachedGuestDispatch(
                buildDetachedGuestDispatchCommands(installTarget, launcherPath, dispatchPath),
                installTarget,
                launcherPath,
                dispatchStartedAt,
                true
        );
    }

    private boolean attemptDetachedGuestDispatch(List<String> guestCommands, String installTarget, String launcherPath, long dispatchStartedAt, boolean fallback) {
        ArrayList<String> normalized = normalizeDispatchCommands(guestCommands);
        if (normalized.isEmpty()) return false;
        return attemptDetachedGuestDispatchCandidate(
                normalized,
                installTarget,
                launcherPath,
                dispatchStartedAt,
                0,
                fallback
        );
    }

    private boolean attemptDetachedGuestDispatchCandidate(List<String> guestCommands, String installTarget, String launcherPath, long dispatchStartedAt, int candidateIndex, boolean fallback) {
        if (guestCommands == null || guestCommands.isEmpty()) return false;
        if (candidateIndex < 0 || candidateIndex >= guestCommands.size()) return false;
        String guestCommand = guestCommands.get(candidateIndex);
        ForensicLogger.logEvent(
                activity,
                fallback ? "warning" : "info",
                fallback ? "PREFIX_PACK_RUNTIME_DETACHED_FALLBACK_ATTEMPT" : "PREFIX_PACK_RUNTIME_DETACHED_PRIMARY_ATTEMPT",
                null,
                "runtime_ui",
                fallback ? "prefix_pack_runtime_detached_fallback_attempt" : "prefix_pack_runtime_detached_primary_attempt",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "guest_command", guestCommand,
                        "candidate_index", candidateIndex,
                        "candidate_count", guestCommands.size()
                )
        );
        if (!activity.launchDetachedGuestProgram(
                guestCommand,
                fallback ? "prefix_pack_runtime_fallback" : "prefix_pack_runtime_primary",
                installTarget
        )) {
            return attemptDetachedGuestDispatchCandidate(
                    guestCommands,
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    candidateIndex + 1,
                    fallback
            );
        }
        InstallState dispatchState = readInstallState(installTarget);
        boolean interactiveGuiLane = isInteractiveGuiLane(
                installTarget,
                dispatchState != null ? dispatchState.primaryPayload : ""
        );
        markRuntimeDispatchQueuedState(
                installTarget,
                launcherPath,
                interactiveGuiLane
                        ? "Detached guest accepted the GUI installer hand-off. Waiting for a visible installer window or a fresh lane proof before treating the launch as started."
                        : "Detached guest launch started. Follow the installer window or the lane logs to confirm progress."
        );
        scheduleDetachedGuestVerification(
                guestCommands,
                installTarget,
                launcherPath,
                guestCommand,
                dispatchStartedAt,
                candidateIndex,
                0,
                fallback
        );
        return true;
    }

    private ArrayList<String> buildDetachedGuestDispatchCommands(String installTarget, String launcherPath, String dispatchPath) {
        ArrayList<String> commands = new ArrayList<>();
        InstallState state = readInstallState(installTarget);
        String normalizedLauncherPath = firstNonEmpty(launcherPath, "").trim();
        String normalizedDispatchPath = firstNonEmpty(dispatchPath, "").trim();
        String normalizedPrimaryPayload = firstNonEmpty(state != null ? state.primaryPayload : "", "").trim();
        boolean preferPrimaryPayload = shouldPreferPrimaryPayloadDispatch(installTarget, normalizedPrimaryPayload);
        if (preferPrimaryPayload && !normalizedPrimaryPayload.isEmpty()) {
            appendPrimaryPayloadCandidates(commands, normalizedPrimaryPayload);
        }
        if (!normalizedLauncherPath.isEmpty()) {
            appendStagedCommandCandidates(commands, normalizedLauncherPath);
        }
        if (!normalizedDispatchPath.isEmpty()) {
            appendStagedCommandCandidates(commands, normalizedDispatchPath);
        }
        if (!preferPrimaryPayload && !normalizedPrimaryPayload.isEmpty()) {
            appendPrimaryPayloadCandidates(commands, normalizedPrimaryPayload);
        }
        return commands;
    }

    private boolean preferDesktopShellDispatch(String installTarget) {
        InstallState state = readInstallState(installTarget);
        if (shouldDispatchPrimaryPayloadDirectly(
                installTarget,
                state != null ? state.primaryPayload : ""
        )) {
            return false;
        }
        LaneSpec lane = findLaneSpec(installTarget);
        if (lane != null && lane.mayRequireGui) return false;
        return false;
    }

    private LaneSpec findLaneSpec(String installTarget) {
        if (installTarget == null || installTarget.trim().isEmpty()) return null;
        for (LaneSpec lane : laneSpecs) {
            if (installTarget.equals(lane.installTarget)) {
                return lane;
            }
        }
        return null;
    }

    private void scheduleDetachedGuestVerification(List<String> guestCommands, String installTarget, String launcherPath, String guestCommand, long dispatchStartedAt, int candidateIndex, int verifyAttempt, boolean fallback) {
        View anchor = activity.getWindow() != null ? activity.getWindow().getDecorView() : getContentView();
        if (anchor == null) return;
        anchor.postDelayed(
                () -> verifyDetachedGuestDispatch(guestCommands, installTarget, launcherPath, guestCommand, dispatchStartedAt, candidateIndex, verifyAttempt, fallback),
                verifyAttempt == 0 ? RUNTIME_DETACHED_VERIFY_DELAY_MS : RUNTIME_DETACHED_VERIFY_RECHECK_MS
        );
    }

    private void verifyDetachedGuestDispatch(List<String> guestCommands, String installTarget, String launcherPath, String guestCommand, long dispatchStartedAt, int candidateIndex, int verifyAttempt, boolean fallback) {
        if (hasFreshDispatchProof(installTarget, dispatchStartedAt)) {
            refreshRuntimeWaitState(installTarget, launcherPath, dispatchStartedAt);
            ForensicLogger.logEvent(
                    activity,
                    "info",
                    fallback ? "PREFIX_PACK_RUNTIME_DETACHED_FALLBACK_CONFIRMED" : "PREFIX_PACK_RUNTIME_DETACHED_PRIMARY_CONFIRMED",
                    null,
                    "runtime_ui",
                    fallback ? "prefix_pack_runtime_detached_fallback_confirmed" : "prefix_pack_runtime_detached_primary_confirmed",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "launcher_path", firstNonEmpty(launcherPath, ""),
                            "guest_command", guestCommand
                    )
            );
            return;
        }
        if (verifyAttempt < RUNTIME_DETACHED_VERIFY_MAX_RECHECKS) {
            scheduleDetachedGuestVerification(guestCommands, installTarget, launcherPath, guestCommand, dispatchStartedAt, candidateIndex, verifyAttempt + 1, fallback);
            return;
        }
        if (guestCommands != null && candidateIndex + 1 < guestCommands.size()) {
            ForensicLogger.logEvent(
                    activity,
                    "warning",
                    fallback ? "PREFIX_PACK_RUNTIME_DETACHED_FALLBACK_RETRY" : "PREFIX_PACK_RUNTIME_DETACHED_PRIMARY_RETRY",
                    null,
                    "runtime_ui",
                    fallback ? "prefix_pack_runtime_detached_fallback_retry" : "prefix_pack_runtime_detached_primary_retry",
                    ForensicLogger.fields(
                            "install_target", firstNonEmpty(installTarget, ""),
                            "launcher_path", firstNonEmpty(launcherPath, ""),
                            "guest_command", guestCommand,
                            "candidate_index", candidateIndex,
                            "next_candidate_index", candidateIndex + 1,
                            "candidate_count", guestCommands.size()
                    )
            );
            attemptDetachedGuestDispatchCandidate(
                    guestCommands,
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    candidateIndex + 1,
                    fallback
            );
            return;
        }
        ForensicLogger.logEvent(
                activity,
                "warning",
                fallback ? "PREFIX_PACK_RUNTIME_DETACHED_FALLBACK_UNPROVEN" : "PREFIX_PACK_RUNTIME_DETACHED_PRIMARY_UNPROVEN",
                null,
                "runtime_ui",
                fallback ? "prefix_pack_runtime_detached_fallback_unproven" : "prefix_pack_runtime_detached_primary_unproven",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "launcher_path", firstNonEmpty(launcherPath, ""),
                        "guest_command", guestCommand
                )
        );
        if (!fallback) {
            InstallState installState = readInstallState(installTarget);
            attemptRuntimeCommandDispatch(
                    buildRuntimeDispatchCommands(
                            installTarget,
                            launcherPath,
                            buildWindowsDispatchPath(installTarget),
                            firstNonEmpty(installState != null ? installState.primaryPayload : "", "")
                    ),
                    installTarget,
                    launcherPath,
                    dispatchStartedAt,
                    0,
                    0
            );
            return;
        }
        refreshRuntimeWaitState(installTarget, launcherPath, dispatchStartedAt);
    }

    private void closeAndRunResolvedTool(String expectedName, String[] candidates, int missingMessageId) {
        File resolved = resolveRuntimeTool(expectedName, candidates);
        if (resolved == null || !resolved.isFile()) {
            AppUtils.showToast(activity, missingMessageId);
            return;
        }
        closeAndRunCommand(buildWindowsStartCommand(toWinePath(resolved)));
    }

    private void showNotesDialog() {
        ContentDialog dialog = new ContentDialog(activity);
        dialog.setTitle(R.string.prefix_pack_notes_title);
        dialog.setIcon(R.drawable.ae_icon_about);
        dialog.setMessage(FileUtils.readString(activity, README_ASSET));
        View cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setVisibility(View.GONE);
        dialog.show();
        styleNestedDialog(dialog);
    }

    private void showDiagnosticsDialog() {
        ContentDialog dialog = new ContentDialog(activity, R.layout.prefix_pack_diagnostics_dialog);
        dialog.setTitle(R.string.prefix_pack_diagnostics_title);
        dialog.setIcon(R.drawable.ae_icon_diagnostics);
        Button confirmButton = dialog.findViewById(R.id.BTConfirm);
        if (confirmButton != null) confirmButton.setVisibility(View.GONE);
        Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setText(R.string.close);
        bindDiagnosticsButton(dialog, R.id.BTOpenDxDiag, () -> closeAndRunCommand(buildWindowsStartCommand("dxdiag.exe")));
        bindDiagnosticsButton(dialog, R.id.BTOpenTestD3D, () -> closeAndRunResolvedTool(
                "TestD3D.exe",
                buildTestD3dCandidates(),
                R.string.prefix_pack_testd3d_missing
        ));
        bindDiagnosticsButton(dialog, R.id.BTOpenGpuInfo, () -> closeAndRunResolvedTool(
                "GPUInfo.exe",
                buildGpuInfoCandidates(),
                R.string.prefix_pack_gpuinfo_missing
        ));
        bindDiagnosticsButton(dialog, R.id.BTOpenDxCaps, () -> closeAndRunResolvedTool(
                "DXCapsViewer.exe",
                buildDxCapsCandidates(),
                R.string.prefix_pack_dxcaps_missing
        ));
        bindDiagnosticsButton(dialog, R.id.BTOpenDxCpl, () -> closeAndRunResolvedTool(
                "dxcpl.exe",
                buildDxCplCandidates(),
                R.string.prefix_pack_dxcpl_missing
        ));
        bindDiagnosticsButton(dialog, R.id.BTOpenGlView, () -> closeAndRunResolvedTool(
                "openglex.exe",
                buildGlViewCandidates(),
                R.string.prefix_pack_glview_missing
        ));
        bindDiagnosticsButton(dialog, R.id.BTOpenWineCfg, () -> closeAndRunCommand(buildWindowsStartCommand("winecfg.exe")));
        dialog.show();
        styleNestedDialog(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.948f),
                    Math.round(AppUtils.getScreenHeight() * 0.628f)
            );
        }
        ViewGroup.LayoutParams params = dialog.getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = Math.round(AppUtils.getScreenHeight() * 0.604f);
            dialog.getContentView().setLayoutParams(params);
        }
        dialog.getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.604f));
        View bottomBar = dialog.findViewById(R.id.LLBottomBar);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
    }

    private void bindDiagnosticsButton(ContentDialog dialog, int buttonId, Runnable action) {
        View button = dialog.findViewById(buttonId);
        if (button == null) return;
        button.setOnClickListener(v -> {
            dialog.dismiss();
            action.run();
        });
    }

    private void openLaneState(String installTarget) {
        InstallState state = readInstallState(installTarget);
        if (state != null && state.exists()) {
            closeAndRunCommand("notepad.exe \"" + buildWindowsStatePath(installTarget) + "\"");
            return;
        }
        File stageNote = getStageNoteFile(installTarget);
        if (stageNote.isFile()) {
            closeAndRunCommand("notepad.exe \"" + toWinePath(stageNote) + "\"");
            return;
        }
        closeAndRunCommand("explorer.exe \"" + getWindowsStateRoot() + "\"");
    }

    private void openLaneLogs(String installTarget) {
        File freshLaneLog = resolveFreshLaneLogPath(installTarget, 0L);
        if (freshLaneLog != null) {
            closeAndRunCommand("notepad.exe \"" + toWinePath(freshLaneLog) + "\"");
            return;
        }
        InstallState state = readInstallState(installTarget);
        if (state != null && state.exists() && state.logFile != null && !state.logFile.trim().isEmpty()) {
            closeAndRunCommand("notepad.exe \"" + state.logFile.trim() + "\"");
            return;
        }
        String launcherLogPath = buildWindowsLogPath(installTarget, "-launcher.log");
        closeAndRunCommand("notepad.exe \"" + launcherLogPath + "\"");
    }

    private void openLaneTarget(String title, String installTarget, List<PrefixPackCatalog.Entry> entries) {
        String stageSegment = sanitizeTargetSegment(installTarget);
        File stageDir = new File(getStageRootDir(), stageSegment);
        File stagedDispatch = new File(stageDir, "launch-" + stageSegment + ".vbs");
        File stagedLauncher = new File(
                stageDir,
                "install-" + stageSegment + ".cmd"
        );
        InstallState existingState = readInstallState(installTarget);
        String primaryPayload = firstNonEmpty(
                resolvePrimaryPayloadPath(installTarget, entries),
                existingState != null ? existingState.primaryPayload : ""
        );
        if (stagedDispatch.isFile()) {
            String stagedDispatchPath = toWinePath(stagedDispatch);
            String stagedLauncherPath = stagedLauncher.isFile() ? toWinePath(stagedLauncher) : stagedDispatchPath;
            ArrayList<String> dispatchCommands = buildRuntimeDispatchCommands(
                    installTarget,
                    stagedLauncherPath,
                    stagedDispatchPath,
                    primaryPayload
            );
            dismissAndDispatchRuntimeCommand(
                    dispatchCommands,
                    installTarget,
                    stagedLauncherPath,
                    stagedDispatchPath
            );
            return;
        }
        if (stagedLauncher.isFile()) {
            String stagedLauncherPath = toWinePath(stagedLauncher);
            ArrayList<String> dispatchCommands = buildRuntimeDispatchCommands(
                    installTarget,
                    stagedLauncherPath,
                    "",
                    primaryPayload
            );
            dismissAndDispatchRuntimeCommand(
                    dispatchCommands,
                    installTarget,
                    stagedLauncherPath,
                    ""
            );
            return;
        }
        ArrayList<PrefixPackCatalog.Entry> missingEntries = new ArrayList<>();
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || !entry.isDownloadable() || isCachedStrict(entry)) continue;
            missingEntries.add(entry);
        }
        if (!missingEntries.isEmpty()) {
            AppUtils.showToast(activity, activity.getString(R.string.prefix_pack_install_preparing_missing, title));
            fetchEntries(title, installTarget, entries, () -> openLaneTarget(title, installTarget, entries));
            return;
        }
        if (!mirrorEntriesToInstallerCache(entries)) {
            AppUtils.showToast(activity, R.string.prefix_pack_cache_mirror_failed);
            return;
        }
        InstallArtifacts artifacts = prepareInstallArtifacts(title, installTarget, entries, "");
        if (artifacts == null) {
            AppUtils.showToast(activity, R.string.prefix_pack_stage_failed);
            return;
        }
        ArrayList<String> dispatchCommands = buildRuntimeDispatchCommands(
                installTarget,
                artifacts.windowsLauncherPath,
                artifacts.windowsDispatchPath,
                artifacts.windowsPrimaryPayload
        );
        dismissAndDispatchRuntimeCommand(
                dispatchCommands,
                installTarget,
                artifacts.windowsLauncherPath,
                artifacts.windowsDispatchPath
        );
    }

    private void cleanupLane(String title, String installTarget, List<PrefixPackCatalog.Entry> entries) {
        boolean changed = false;
        boolean success = true;
        if (entries != null) {
            for (PrefixPackCatalog.Entry entry : entries) {
                if (entry == null) continue;
                File backendFile = new File(getCacheDir(), entry.fileName);
                File mirroredFile = new File(getContainerCacheDir(), entry.fileName);
                changed |= backendFile.exists() || mirroredFile.exists();
                success &= deleteIfExists(backendFile);
                success &= deleteIfExists(mirroredFile);
            }
        }

        File stageDir = new File(getStageRootDir(), sanitizeTargetSegment(installTarget));
        File stateFile = getStateFile(installTarget);
        changed |= stageDir.exists() || stateFile.exists();
        success &= deleteIfExists(stageDir);
        success &= deleteIfExists(stateFile);
        success &= deleteMatchingLaneLogs(installTarget);

        refreshStatusIndexAsync();
        if (!success) {
            AppUtils.showToast(activity, R.string.prefix_pack_cleanup_failed);
            return;
        }
        AppUtils.showToast(activity, activity.getString(
                changed ? R.string.prefix_pack_cleanup_done : R.string.prefix_pack_cleanup_nothing,
                title
        ));
    }

    private void maybeAutoLaunchRequestedInstall() {
        if (autoInstallTarget.isEmpty() || autoLaunchConsumed) return;
        maybeAutoLaunchRequestedInstall(0);
    }

    private void maybeAutoLaunchRequestedInstall(int attempt) {
        if (autoInstallTarget.isEmpty() || autoLaunchConsumed || laneList == null) return;
        laneList.postDelayed(() -> {
            if (!isShowing() || autoLaunchConsumed) return;
            if ((laneBuildInProgress || !laneCardsBuilt) && attempt + 1 < AUTO_INSTALL_MAX_ATTEMPTS) {
                maybeAutoLaunchRequestedInstall(attempt + 1);
                return;
            }
            if (!activity.isDesktopShellCommandReady() && attempt + 1 < AUTO_INSTALL_MAX_ATTEMPTS) {
                maybeAutoLaunchRequestedInstall(attempt + 1);
                return;
            }
            autoLaunchConsumed = true;
            for (LaneSpec lane : laneSpecs) {
                if (!lane.installTarget.equalsIgnoreCase(autoInstallTarget)) continue;
                launchInstall(lane.title, lane.installTarget, resolveEntries(lane.entryIds));
                return;
            }
            AppUtils.showToast(activity, activity.getString(R.string.prefix_pack_auto_install_missing_target, autoInstallTarget));
        }, attempt == 0 ? 160L : AUTO_INSTALL_RETRY_MS);
    }

    private static final class InstallArtifacts {
        final String windowsLauncherPath;
        final String windowsDispatchPath;
        final String windowsPrimaryPayload;
        final String windowsStatePath;
        final String primaryPayloadLabel;

        private InstallArtifacts(String windowsLauncherPath, String windowsDispatchPath, String windowsPrimaryPayload, String windowsStatePath, String primaryPayloadLabel) {
            this.windowsLauncherPath = windowsLauncherPath;
            this.windowsDispatchPath = windowsDispatchPath;
            this.windowsPrimaryPayload = windowsPrimaryPayload;
            this.windowsStatePath = windowsStatePath;
            this.primaryPayloadLabel = primaryPayloadLabel;
        }
    }

    private InstallArtifacts prepareInstallArtifacts(String title, String installTarget, List<PrefixPackCatalog.Entry> entries, String requestingTarget) {
        if (!ensurePrefixPackSurfaceDirs()) return null;
        deleteMatchingLaneLogs(installTarget);

        String stageSegment = sanitizeTargetSegment(installTarget);
        File stageDir = new File(getStageRootDir(), stageSegment);
        if (stageDir.exists()) {
            File[] stagedFiles = stageDir.listFiles();
            if (stagedFiles != null) {
                for (File stagedFile : stagedFiles) {
                    if (!deleteIfExists(stagedFile)) return null;
                }
            }
        }
        if (!stageDir.exists() && !stageDir.mkdirs()) return null;
        File stagedToolkitDir = new File(stageDir, WINDOWS_STAGE_TOOLKIT_SEGMENT);
        if (!stageToolkitScripts(stagedToolkitDir)) return null;

        File launcherFile = new File(stageDir, "install-" + stageSegment + ".cmd");
        File dispatchFile = new File(stageDir, "launch-" + stageSegment + ".vbs");
        File noteFile = new File(stageDir, "install-" + stageSegment + "-notes.txt");
        String launcherPath = toWinePath(launcherFile);
        String dispatchPath = toWinePath(dispatchFile);
        String stagedToolkitPath = toWinePath(stagedToolkitDir);
        String statePath = buildWindowsStatePath(installTarget);
        String launcherLogPath = buildWindowsLogPath(installTarget, "-launcher.log");
        String bootstrapLogPath = WINDOWS_STAGE_ROOT + "\\" + stageSegment + "\\install-" + stageSegment + "-bootstrap.log";
        String primaryPayload = resolvePrimaryPayloadPath(installTarget, entries);
        String primaryPayloadLabel = resolvePrimaryPayloadLabel(installTarget, entries, primaryPayload);

        deleteIfExists(new File(getLogDir(), installTarget + "-launcher.log"));

        String launcherScript = buildLaneLauncherScript(
                installTarget,
                launcherPath,
                primaryPayload,
                launcherLogPath,
                bootstrapLogPath,
                stagedToolkitPath,
                requestingTarget
        );
        if (!FileUtils.writeString(launcherFile, launcherScript)) return null;
        if (!FileUtils.writeString(dispatchFile, buildLaneDispatchScript(
                installTarget,
                launcherPath,
                primaryPayload,
                launcherLogPath,
                bootstrapLogPath
        ))) return null;

        String notes = buildLaneNotes(title, installTarget, entries, launcherPath, dispatchPath, statePath, bootstrapLogPath, primaryPayload, stagedToolkitPath, requestingTarget);
        if (!FileUtils.writeString(noteFile, notes)) return null;

        if (!FileUtils.writeString(getStateFile(installTarget),
                buildScheduledStateMarker(installTarget, launcherPath, launcherLogPath, primaryPayload, requestingTarget))) {
            return null;
        }
        return new InstallArtifacts(launcherPath, dispatchPath, primaryPayload, statePath, primaryPayloadLabel);
    }

    private void refreshRuntimeWaitState(String installTarget, String launcherPath, long dispatchStartedAt) {
        if (installTarget == null || installTarget.trim().isEmpty()) return;
        InstallState existing = readInstallState(installTarget);
        File stateFile = getStateFile(installTarget);
        File launcherLog = resolveStateLogFile(existing, installTarget);
        File bootstrapLog = getStageBootstrapLogFile(installTarget);
        File freshLaneLog = resolveFreshLaneLogPath(installTarget, dispatchStartedAt);
        boolean freshWindowMapped = activity.hasFreshTrackedApplicationWindowMappedSince(dispatchStartedAt);
        boolean launcherLogPresent = stateFileIsFresh(launcherLog, dispatchStartedAt);
        String existingStatus = existing.status != null ? existing.status.trim().toLowerCase(Locale.US) : "";
        boolean freshState = stateFileIsFresh(stateFile, dispatchStartedAt);
        boolean stateProvesDispatch = freshState && isDispatchProofStatus(existingStatus);
        boolean requesterSatisfied = isInstallSatisfiedForRequester(
                installTarget,
                firstNonEmpty(existing.requestedBy, ""),
                existing
        );

        if (requesterSatisfied) {
            Properties properties = new Properties();
            properties.setProperty("install_target", installTarget);
            properties.setProperty("status", "success");
            properties.setProperty("exit_code", firstNonEmpty(existing.exitCode, ""));
            properties.setProperty("updated_at", String.valueOf(System.currentTimeMillis()));
            properties.setProperty("log_file", toWinePath(firstNonEmptyFile(freshLaneLog, firstNonEmptyFile(launcherLog, bootstrapLog))));
            properties.setProperty("detail", "The live prefix now exposes the required runtime proof for this lane. You can rerun the dependent installer or reopen the lane logs for confirmation.");
            properties.setProperty("backend_cache", CACHE_ROOT);
            properties.setProperty("installer_cache", WINDOWS_INSTALLER_CACHE);
            properties.setProperty("state_root", getWindowsStateRoot());
            properties.setProperty("log_root", getWindowsLogRoot());
            properties.setProperty("launcher_file", firstNonEmpty(launcherPath, existing.launcherFile));
            properties.setProperty("primary_payload", firstNonEmpty(existing.primaryPayload, ""));
            properties.setProperty("next_action", "The prerequisite proof is now present. Rerun the dependent installer, or inspect the newest lane log if you want a post-install trace.");
            properties.setProperty("requested_by", firstNonEmpty(existing.requestedBy, ""));
            FileUtils.writeString(stateFile, storeProperties(properties));
            return;
        }

        if (stateProvesDispatch || freshLaneLog != null || freshWindowMapped) {
            String resolvedStatus = stateProvesDispatch ? existingStatus : "interactive";
            Properties properties = new Properties();
            properties.setProperty("install_target", installTarget);
            properties.setProperty("status", resolvedStatus);
            properties.setProperty("exit_code", firstNonEmpty(existing.exitCode, ""));
            properties.setProperty("updated_at", String.valueOf(System.currentTimeMillis()));
            properties.setProperty("log_file", freshLaneLog != null
                    ? toWinePath(freshLaneLog)
                    : toWinePath(firstNonEmptyFile(launcherLog, bootstrapLog)));
            properties.setProperty("detail", freshLaneLog != null
                    ? "Guest runtime produced a fresh lane log, so the staged installer really executed. Follow the installer window if it is visible, or inspect the saved lane log for the next step."
                    : freshWindowMapped
                    ? "A fresh installer window mapped after the staged hand-off, so the launcher has definitely started. Follow the installer window or reopen Logs for the newest lane trace."
                    : firstNonEmpty(existing.detail, "Guest runtime has already advanced the lane state beyond scheduled."));
            properties.setProperty("backend_cache", CACHE_ROOT);
            properties.setProperty("installer_cache", WINDOWS_INSTALLER_CACHE);
            properties.setProperty("state_root", getWindowsStateRoot());
            properties.setProperty("log_root", getWindowsLogRoot());
            properties.setProperty("launcher_file", firstNonEmpty(launcherPath, existing.launcherFile));
            properties.setProperty("primary_payload", firstNonEmpty(existing.primaryPayload, ""));
            properties.setProperty("next_action", freshLaneLog != null
                    ? "Installer dispatch is proven. Follow the installer window, or open Logs to inspect the newest lane log."
                    : freshWindowMapped
                    ? "Installer GUI launch is proven by a fresh mapped window. Finish the GUI flow, or open Logs to inspect the newest lane trace."
                    : firstNonEmpty(existing.nextAction, "Follow the installer window or open Logs to inspect the newest lane log."));
            properties.setProperty("requested_by", firstNonEmpty(existing.requestedBy, ""));
            FileUtils.writeString(stateFile, storeProperties(properties));
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("install_target", installTarget);
        properties.setProperty("status", launcherLogPresent ? "interactive" : "queued");
        properties.setProperty("exit_code", "");
        properties.setProperty("updated_at", String.valueOf(System.currentTimeMillis()));
        properties.setProperty("log_file", toWinePath(firstNonEmptyFile(launcherLog, bootstrapLog)));
        properties.setProperty("detail", launcherLogPresent
                ? "Launcher log is present, so the staged installer was executed in the guest runtime. Follow the installer window or inspect the lane logs for the next step."
                : "Guest dispatch was accepted, but no fresh launcher or lane log proved execution yet. The staged launcher is still parked under C:\\AePrefixPack\\staging and can be rerun.");
        properties.setProperty("backend_cache", CACHE_ROOT);
        properties.setProperty("installer_cache", WINDOWS_INSTALLER_CACHE);
        properties.setProperty("state_root", getWindowsStateRoot());
        properties.setProperty("log_root", getWindowsLogRoot());
        properties.setProperty("launcher_file", firstNonEmpty(launcherPath, existing.launcherFile));
        properties.setProperty("primary_payload", firstNonEmpty(existing.primaryPayload, ""));
        properties.setProperty("next_action", launcherLogPresent
                ? "Follow the installer window if it is visible, or inspect the launcher and lane logs under AePrefixPack save_data."
                : "If no installer window appeared, rerun Install or Launch from Prefix Pack after the runtime settles.");
        properties.setProperty("requested_by", firstNonEmpty(existing.requestedBy, ""));
        FileUtils.writeString(stateFile, storeProperties(properties));
    }

    private boolean ensurePrefixPackSurfaceDirs() {
        File[] directories = new File[] {
                getContainerCacheDir(),
                getStageRootDir(),
                new File(getDriveCRootDir(), "AePrefixPack"),
                getSaveRootDir(),
                getLogDir(),
                getStateDir()
        };
        for (File directory : directories) {
            if (directory.exists()) continue;
            if (!directory.mkdirs()) return false;
        }
        return true;
    }

    private boolean stageToolkitScripts(File stagedToolkitDir) {
        if (stagedToolkitDir == null) return false;
        if (!stagedToolkitDir.exists() && !stagedToolkitDir.mkdirs()) return false;
        try {
            String[] assets = activity.getAssets().list("prefixpack/windows");
            if (assets == null || assets.length == 0) return false;
            for (String assetName : assets) {
                if (assetName == null || assetName.trim().isEmpty()) continue;
                String assetBody = FileUtils.readString(activity, "prefixpack/windows/" + assetName);
                if (assetBody == null || assetBody.isEmpty()) return false;
                if (!FileUtils.writeString(new File(stagedToolkitDir, assetName), assetBody)) return false;
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private String buildLaneLauncherScript(String installTarget, String launcherPath, String primaryPayload, String launcherLogPath,
                                           String bootstrapLogPath, String stagedToolkitPath, String requestingTarget) {
        String toolkitRoot = firstNonEmpty(
                stagedToolkitPath,
                WINDOWS_STAGE_ROOT + "\\" + sanitizeTargetSegment(installTarget) + "\\" + WINDOWS_STAGE_TOOLKIT_SEGMENT
        );
        String stagedLoaderPath = toolkitRoot + "\\prefix-pack-loader.cmd";
        String stagedCommonPath = toolkitRoot + "\\prefix-pack-common.cmd";
        StringBuilder builder = new StringBuilder();
        builder.append("@echo off\r\n");
        builder.append("setlocal EnableExtensions\r\n");
        builder.append("set \"PREFIX_PACK_ROOT=").append(escapeBatchValue(TOOLKIT_ROOT)).append("\"\r\n");
        builder.append("set \"PREFIX_PACK_SCRIPT_DIR=").append(escapeBatchValue(toolkitRoot)).append("\\\"\r\n");
        builder.append("set \"PREFIX_PACK_LAUNCHER_FILE=").append(escapeBatchValue(launcherPath)).append("\"\r\n");
        builder.append("set \"PREFIX_PACK_PRIMARY_PAYLOAD=").append(escapeBatchValue(primaryPayload)).append("\"\r\n");
        builder.append("set \"PREFIX_PACK_REQUESTING_TARGET=").append(escapeBatchValue(firstNonEmpty(requestingTarget, ""))).append("\"\r\n");
        builder.append("set \"PREFIX_PACK_NEXT_ACTION=Follow the installer window if it opens, or inspect the state and logs under AePrefixPack save_data.\"\r\n");
        builder.append("set \"PREFIX_PACK_LAUNCHER_LOG=").append(escapeBatchValue(launcherLogPath)).append("\"\r\n");
        builder.append("set \"PREFIX_PACK_BOOTSTRAP_LOG=").append(escapeBatchValue(bootstrapLogPath)).append("\"\r\n");
        builder.append("cd /d \"%~dp0\" >nul 2>&1\r\n");
        builder.append("> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo AePrefixPack launcher bootstrap\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   target: ").append(escapeBatchValue(installTarget)).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   launcher: ").append(escapeBatchValue(launcherPath)).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   primary payload: ").append(escapeBatchValue(primaryPayload)).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   requested by: ").append(escapeBatchValue(firstNonEmpty(requestingTarget, ""))).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   staged toolkit: ").append(escapeBatchValue(toolkitRoot)).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   state: ").append(escapeBatchValue(buildWindowsStatePath(installTarget))).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   logs: ").append(escapeBatchValue(launcherLogPath)).append("\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo   cwd: %CD%\r\n");
        builder.append("call \"").append(escapeBatchValue(stagedCommonPath)).append("\" :init_env >> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" 2>&1\r\n");
        builder.append("copy /Y \"%PREFIX_PACK_BOOTSTRAP_LOG%\" \"%PREFIX_PACK_LAUNCHER_LOG%\" >nul 2>&1\r\n");
        builder.append("call \"").append(escapeBatchValue(stagedCommonPath)).append("\" :mark_state \"")
                .append(escapeBatchValue(installTarget))
                .append("\" queued 0 \"%PREFIX_PACK_BOOTSTRAP_LOG%\" \"Visible staged launcher accepted from Android UI. Loader proof is still pending.\"\r\n");
        builder.append("call \"").append(escapeBatchValue(stagedLoaderPath)).append("\" install \"").append(escapeBatchValue(installTarget)).append("\" >> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" 2>&1\r\n");
        builder.append("set \"PREFIX_PACK_LAUNCHER_RC=%ERRORLEVEL%\"\r\n");
        builder.append(">> \"%PREFIX_PACK_BOOTSTRAP_LOG%\" echo loader_rc=%PREFIX_PACK_LAUNCHER_RC%\r\n");
        builder.append("copy /Y \"%PREFIX_PACK_BOOTSTRAP_LOG%\" \"%PREFIX_PACK_LAUNCHER_LOG%\" >nul 2>&1\r\n");
        builder.append("exit /b %PREFIX_PACK_LAUNCHER_RC%\r\n");
        return builder.toString();
    }

    private String buildStageLaunchCommand(String windowsDispatchPath) {
        return buildWindowsStartCommand(windowsDispatchPath);
    }

    private String buildGuestLaunchExecutable(String windowsPath) {
        return buildWindowsStartCommand(windowsPath);
    }

    private String buildWindowsStartCommand(String windowsPath) {
        String resolvedPath = firstNonEmpty(windowsPath, "").trim();
        if (resolvedPath.isEmpty()) return "";
        String lowerPath = resolvedPath.toLowerCase(Locale.US);
        if (lowerPath.endsWith(".cmd") || lowerPath.endsWith(".bat")) {
            return buildShellStartCommand(resolvedPath);
        }
        if (lowerPath.endsWith(".vbs") || lowerPath.endsWith(".js")) {
            return WINDOWS_WSCRIPT_EXE + " \"" + resolvedPath + "\"";
        }
        if (lowerPath.endsWith(".msi")) {
            return WINDOWS_MSIEXEC_EXE + " /i \"" + resolvedPath + "\"";
        }
        return "\"" + resolvedPath + "\"";
    }

    private String buildShellStartCommand(String windowsPath) {
        String resolvedPath = firstNonEmpty(windowsPath, "").trim();
        if (resolvedPath.isEmpty()) return "";
        String workingDir = "";
        int separator = Math.max(resolvedPath.lastIndexOf('\\'), resolvedPath.lastIndexOf('/'));
        if (separator > 2) workingDir = resolvedPath.substring(0, separator);

        String lowerPath = resolvedPath.toLowerCase(Locale.US);
        if (lowerPath.endsWith(".cmd") || lowerPath.endsWith(".bat")) {
            if (!workingDir.isEmpty()) {
                return WINDOWS_CMD_EXE + " /c \"cd /d \\\"" + workingDir + "\\\" && call \\\"" + resolvedPath + "\\\"\"";
            }
            return WINDOWS_CMD_EXE + " /c call \"" + resolvedPath + "\"";
        }
        StringBuilder command = new StringBuilder(WINDOWS_CMD_EXE)
                .append(" /c start \"\"");
        if (!workingDir.isEmpty()) {
            command.append(" /d \"").append(workingDir).append('"');
        }
        if (lowerPath.endsWith(".vbs") || lowerPath.endsWith(".js")) {
            command.append(" ").append(WINDOWS_WSCRIPT_EXE).append(" \"").append(resolvedPath).append('"');
            return command.toString();
        }
        if (lowerPath.endsWith(".msi")) {
            command.append(" ").append(WINDOWS_MSIEXEC_EXE).append(" /i \"").append(resolvedPath).append('"');
            return command.toString();
        }
        command.append(" \"").append(resolvedPath).append('"');
        return command.toString();
    }

    private ArrayList<String> buildRuntimeDispatchCommands(String windowsLauncherPath, String windowsDispatchPath) {
        return buildRuntimeDispatchCommands("", windowsLauncherPath, windowsDispatchPath, "");
    }

    private ArrayList<String> buildRuntimeDispatchCommands(String installTarget, String windowsLauncherPath, String windowsDispatchPath, String windowsPrimaryPayloadPath) {
        ArrayList<String> commands = new ArrayList<>();
        String launcherPath = firstNonEmpty(windowsLauncherPath, "").trim();
        String dispatchPath = firstNonEmpty(windowsDispatchPath, "").trim();
        String primaryPayloadPath = firstNonEmpty(windowsPrimaryPayloadPath, "").trim();
        boolean preferPrimaryPayload = shouldPreferPrimaryPayloadDispatch(installTarget, primaryPayloadPath);
        if (preferPrimaryPayload && !primaryPayloadPath.isEmpty()) {
            appendPrimaryPayloadCandidates(commands, primaryPayloadPath);
        }
        if (!launcherPath.isEmpty()) {
            appendStagedCommandCandidates(commands, launcherPath);
        }
        if (!dispatchPath.isEmpty()) {
            appendStagedCommandCandidates(commands, dispatchPath);
        }
        if (!preferPrimaryPayload && !primaryPayloadPath.isEmpty()) {
            appendPrimaryPayloadCandidates(commands, primaryPayloadPath);
        }
        return commands;
    }

    private void appendStagedCommandCandidates(List<String> commands, String windowsPath) {
        String normalizedPath = firstNonEmpty(windowsPath, "").trim();
        if (normalizedPath.isEmpty()) return;
        String directCommand = buildWindowsStartCommand(normalizedPath);
        addDispatchCandidate(commands, directCommand);
        String shellCommand = buildShellStartCommand(normalizedPath);
        if (!shellCommand.equals(directCommand)) {
            addDispatchCandidate(commands, shellCommand);
        }
        String lowerPath = normalizedPath.toLowerCase(Locale.US);
        if (lowerPath.endsWith(".cmd") || lowerPath.endsWith(".bat")) {
            addDispatchCandidate(commands, WINDOWS_CMD_EXE + " /c call \"" + normalizedPath + "\"");
        } else if (lowerPath.endsWith(".vbs") || lowerPath.endsWith(".js")) {
            addDispatchCandidate(commands, WINDOWS_WSCRIPT_EXE + " \"" + normalizedPath + "\"");
        } else if (lowerPath.endsWith(".msi")) {
            addDispatchCandidate(commands, WINDOWS_MSIEXEC_EXE + " /i \"" + normalizedPath + "\"");
        } else {
            addDispatchCandidate(commands, "\"" + normalizedPath + "\"");
        }
    }

    private void appendPrimaryPayloadCandidates(List<String> commands, String primaryPayloadPath) {
        String normalizedPrimaryPayload = firstNonEmpty(primaryPayloadPath, "").trim();
        if (normalizedPrimaryPayload.isEmpty()) return;
        String directCommand = buildWindowsStartCommand(normalizedPrimaryPayload);
        addDispatchCandidate(commands, directCommand);
        addDispatchCandidate(commands, "\"" + normalizedPrimaryPayload + "\"");
        String shellCommand = buildShellStartCommand(normalizedPrimaryPayload);
        if (!shellCommand.equals(directCommand)) {
            addDispatchCandidate(commands, shellCommand);
        }
    }

    private boolean shouldPreferPrimaryPayloadDispatch(String installTarget, String primaryPayloadPath) {
        return shouldDispatchPrimaryPayloadDirectly(installTarget, primaryPayloadPath);
    }

    private boolean shouldDispatchPrimaryPayloadDirectly(String installTarget, String primaryPayloadPath) {
        // Prefix Pack lanes own extraction, prerequisite choice, staging and log/state proof.
        // Running the raw payload first regresses that contract, so direct payload dispatch stays disabled.
        return false;
    }

    private boolean canUseBareWindowsPath(String windowsPath) {
        if (windowsPath == null) return false;
        String normalized = windowsPath.trim();
        return !normalized.isEmpty()
                && normalized.contains(":\\")
                && !normalized.contains(" ")
                && !normalized.contains("\t")
                && !normalized.contains("\"");
    }

    private void addDispatchCandidate(List<String> commands, String command) {
        String normalized = command != null ? command.trim() : "";
        if (normalized.isEmpty()) return;
        if (commands.contains(normalized)) return;
        commands.add(normalized);
    }

    private ArrayList<String> normalizeDispatchCommands(List<String> commands) {
        ArrayList<String> normalized = new ArrayList<>();
        if (commands == null) return normalized;
        for (String command : commands) {
            String candidate = command != null ? command.trim() : "";
            if (candidate.isEmpty()) continue;
            if (normalized.contains(candidate)) continue;
            normalized.add(candidate);
        }
        return normalized;
    }

    private String buildLaneDispatchScript(String installTarget, String windowsLauncherPath, String windowsPrimaryPayloadPath,
                                           String launcherLogPath, String bootstrapLogPath) {
        String normalizedLauncherPath = firstNonEmpty(windowsLauncherPath, "").trim();
        String normalizedPrimaryPayloadPath = firstNonEmpty(windowsPrimaryPayloadPath, "").trim();
        boolean preferPrimaryPayload = shouldDispatchPrimaryPayloadDirectly(installTarget, normalizedPrimaryPayloadPath);
        return "Option Explicit\r\n"
                + "Dim shell, fso, launcherPath, primaryPayloadPath, commandLine, dispatchMode, rc, lowerExt\r\n"
                + "Function Q(value)\r\n"
                + "  Q = Chr(34) & value & Chr(34)\r\n"
                + "End Function\r\n"
                + "launcherPath = \"" + normalizedLauncherPath.replace("\"", "\"\"") + "\"\r\n"
                + "primaryPayloadPath = \"" + normalizedPrimaryPayloadPath.replace("\"", "\"\"") + "\"\r\n"
                + "Set shell = CreateObject(\"WScript.Shell\")\r\n"
                + "Set fso = CreateObject(\"Scripting.FileSystemObject\")\r\n"
                + "commandLine = \"\"\r\n"
                + "dispatchMode = \"\"\r\n"
                + (preferPrimaryPayload
                ? "If Len(primaryPayloadPath) > 0 And fso.FileExists(primaryPayloadPath) Then dispatchMode = \"primary_payload\"\r\n"
                + "If Len(dispatchMode) = 0 And Len(launcherPath) > 0 And fso.FileExists(launcherPath) Then dispatchMode = \"launcher\"\r\n"
                : "If Len(launcherPath) > 0 And fso.FileExists(launcherPath) Then dispatchMode = \"launcher\"\r\n"
                + "If Len(dispatchMode) = 0 And Len(primaryPayloadPath) > 0 And fso.FileExists(primaryPayloadPath) Then dispatchMode = \"primary_payload\"\r\n")
                + "If dispatchMode = \"primary_payload\" Then\r\n"
                + "  lowerExt = LCase(fso.GetExtensionName(primaryPayloadPath))\r\n"
                + "  If Len(primaryPayloadPath) > 0 And fso.FileExists(primaryPayloadPath) Then shell.CurrentDirectory = fso.GetParentFolderName(primaryPayloadPath)\r\n"
                + "  If lowerExt = \"msi\" Then\r\n"
                + "    commandLine = \"C:\\windows\\system32\\msiexec.exe /i \" & Q(primaryPayloadPath)\r\n"
                + "  ElseIf lowerExt = \"vbs\" Or lowerExt = \"js\" Then\r\n"
                + "    commandLine = \"C:\\windows\\system32\\wscript.exe \" & Q(primaryPayloadPath)\r\n"
                + "  Else\r\n"
                + "    commandLine = Q(primaryPayloadPath)\r\n"
                + "  End If\r\n"
                + "ElseIf dispatchMode = \"launcher\" And Len(launcherPath) > 0 And fso.FileExists(launcherPath) Then\r\n"
                + "  shell.CurrentDirectory = fso.GetParentFolderName(launcherPath)\r\n"
                + "  commandLine = \"C:\\windows\\system32\\cmd.exe /c call \" & Q(launcherPath)\r\n"
                + "End If\r\n"
                + "If Len(commandLine) = 0 Then WScript.Quit 2\r\n"
                + "rc = shell.Run(commandLine, 0, False)\r\n"
                + "WScript.Quit rc\r\n";
    }

    private String buildLaneNotes(String title, String installTarget, List<PrefixPackCatalog.Entry> entries,
                                  String launcherPath, String dispatchPath, String statePath, String launcherLogPath,
                                  String primaryPayload, String stagedToolkitPath, String requestingTarget) {
        StringBuilder builder = new StringBuilder();
        builder.append("AePrefixPack lane\n");
        builder.append("title=").append(title).append("\n");
        builder.append("target=").append(installTarget).append("\n");
        builder.append("launcher=").append(launcherPath).append("\n");
        builder.append("dispatch=").append(dispatchPath).append("\n");
        builder.append("staged_toolkit=").append(stagedToolkitPath).append("\n");
        builder.append("state=").append(statePath).append("\n");
        builder.append("logs=").append(launcherLogPath).append("\n");
        builder.append("primary_payload=").append(primaryPayload).append("\n");
        builder.append("requested_by=").append(firstNonEmpty(requestingTarget, "")).append("\n");
        builder.append("backend_cache=").append(CACHE_ROOT).append("\n");
        builder.append("installer_cache=").append(WINDOWS_INSTALLER_CACHE).append("\n");
        builder.append("stage_root=").append(WINDOWS_STAGE_ROOT).append("\n");
        builder.append("\nEntries:\n");
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null) continue;
            builder.append("- ").append(entry.fileName)
                    .append(" | mirrored=").append(isMirrored(entry))
                    .append(" | cached=").append(isCached(entry))
                    .append(" | source=").append(entry.sourceLabel)
                    .append('\n');
        }
        builder.append("\nInstall means the visible C: stage path is executed inside the live guest runtime. GUI-heavy lanes may open the primary payload directly first, while the staged launcher stays available for rerun and inspection.\n");
        return builder.toString();
    }

    private String buildScheduledStateMarker(String installTarget, String launcherPath, String launcherLogPath, String primaryPayload, String requestingTarget) {
        Properties properties = new Properties();
        properties.setProperty("install_target", firstNonEmpty(installTarget, ""));
        properties.setProperty("status", "scheduled");
        properties.setProperty("exit_code", "");
        properties.setProperty("updated_at", String.valueOf(System.currentTimeMillis()));
        properties.setProperty("log_file", firstNonEmpty(launcherLogPath, ""));
        properties.setProperty("detail", "Android UI staged a visible launcher under C:\\AePrefixPack\\staging and is opening the staged route or its primary installer inside the live prefix.");
        properties.setProperty("backend_cache", CACHE_ROOT);
        properties.setProperty("installer_cache", WINDOWS_INSTALLER_CACHE);
        properties.setProperty("state_root", getWindowsStateRoot());
        properties.setProperty("log_root", getWindowsLogRoot());
        properties.setProperty("launcher_file", firstNonEmpty(launcherPath, ""));
        properties.setProperty("primary_payload", firstNonEmpty(primaryPayload, ""));
        properties.setProperty("next_action", "Open the staged launcher again or inspect the stage, state and logs if the installer did not appear.");
        properties.setProperty("requested_by", firstNonEmpty(requestingTarget, ""));
        return storeProperties(properties);
    }

    private boolean isDispatchProofStatus(String normalizedStatus) {
        if (normalizedStatus == null || normalizedStatus.trim().isEmpty()) return false;
        String normalized = normalizedStatus.trim().toLowerCase(Locale.US);
        return !"scheduled".equals(normalized) && !"queued".equals(normalized);
    }

    private boolean hasFreshDispatchProof(String installTarget, long dispatchStartedAt) {
        if (installTarget == null || installTarget.trim().isEmpty()) return false;
        InstallState state = readInstallState(installTarget);
        File stateFile = getStateFile(installTarget);
        File launcherLog = resolveStateLogFile(state, installTarget);
        File freshLaneLog = resolveFreshLaneLogPath(installTarget, dispatchStartedAt);
        if (freshLaneLog != null) {
            return true;
        }

        if (stateFileIsFresh(launcherLog, dispatchStartedAt)) {
            return true;
        }
        if (stateFileIsFresh(stateFile, dispatchStartedAt)) {
            String normalized = state.status != null ? state.status.trim().toLowerCase(Locale.US) : "";
            return isDispatchProofStatus(normalized);
        }
        if (activity.hasFreshTrackedApplicationWindowMappedSince(dispatchStartedAt)) {
            return true;
        }
        return false;
    }

    private boolean isInteractiveGuiLane(String installTarget, String primaryPayloadPath) {
        LaneSpec lane = findLaneSpec(installTarget);
        return shouldDispatchPrimaryPayloadDirectly(installTarget, primaryPayloadPath)
                || (lane != null && lane.mayRequireGui);
    }

    private boolean isInstallSatisfiedForRequester(String installTarget, String requesterTarget, InstallState state) {
        if ("dotnet_framework".equalsIgnoreCase(firstNonEmpty(installTarget, ""))
                && "legacy_dx_sdk".equalsIgnoreCase(firstNonEmpty(requesterTarget, ""))) {
            if (state != null && state.exists()) {
                String normalized = firstNonEmpty(state.status, "").trim().toLowerCase(Locale.US);
                if ("success".equals(normalized)) return true;
            }
            return isDotnetLegacyFrameworkReadyHost();
        }
        return isInstallSatisfied(installTarget, state);
    }

    private boolean isInstallSatisfied(String installTarget, InstallState state) {
        if ("dotnet_framework".equalsIgnoreCase(firstNonEmpty(installTarget, ""))) {
            return isDotnetFrameworkSatisfied(state);
        }
        if ("legacy_dx_sdk".equalsIgnoreCase(firstNonEmpty(installTarget, "")) && hasLegacyDxSdkToolsHost()) {
            return true;
        }
        if (state == null || !state.exists()) return false;
        String normalized = firstNonEmpty(state.status, "").trim().toLowerCase(Locale.US);
        return "success".equals(normalized);
    }

    private boolean isInstallInFlightForRequester(String installTarget, String requesterTarget, InstallState state) {
        if ("dotnet_framework".equalsIgnoreCase(firstNonEmpty(installTarget, ""))
                && "legacy_dx_sdk".equalsIgnoreCase(firstNonEmpty(requesterTarget, ""))
                && isDotnetLegacyFrameworkReadyHost()) {
            return false;
        }
        return isInstallInFlight(installTarget, state);
    }

    private boolean isInstallInFlight(String installTarget, InstallState state) {
        if ("dotnet_framework".equalsIgnoreCase(firstNonEmpty(installTarget, "")) && isDotnetFrameworkSatisfied(state)) {
            return false;
        }
        if (state == null || !state.exists()) return false;
        String normalized = firstNonEmpty(state.status, "").trim().toLowerCase(Locale.US);
        boolean inFlight = "scheduled".equals(normalized)
                || "queued".equals(normalized)
                || "running".equals(normalized)
                || "interactive".equals(normalized);
        if (!inFlight) return false;
        long updatedAtMs = parseInstallStateUpdatedAtMs(state);
        if (updatedAtMs <= 0L) return true;
        return Math.max(0L, System.currentTimeMillis() - updatedAtMs) <= INSTALL_INFLIGHT_FRESH_MS;
    }

    private String resolveInstallPrerequisite(String installTarget) {
        // Lane-owned launcher scripts now own prerequisite flow so Install always
        // stages and opens the concrete lane the user picked instead of jumping
        // Android-side into a different lane before staging.
        return "";
    }

    private boolean isDotnetFrameworkSatisfied(InstallState state) {
        if (state != null && state.exists()) {
            String normalized = firstNonEmpty(state.status, "").trim().toLowerCase(Locale.US);
            if ("success".equals(normalized)) return true;
        }
        return isDotnetLegacyFrameworkReadyHost() && isDotnetFrameworkReadyHost();
    }

    private boolean isDotnetFrameworkReadyHost() {
        return hasDotnetFrameworkFilesHost()
                && hasDotnetFrameworkRegistryProofHost()
                && hasManagedRuntimeContractHost();
    }

    private boolean isDotnetLegacyFrameworkReadyHost() {
        return hasDotnetLegacyFilesHost()
                && hasDotnetLegacyRegistryProofHost()
                && hasManagedRuntimeContractHost();
    }

    private boolean hasDotnetLegacyFilesHost() {
        File driveC = getDriveCRootDir();
        File framework20 = new File(driveC, "windows/Microsoft.NET/Framework/v2.0.50727/mscorlib.dll");
        File framework20Fusion = new File(driveC, "windows/Microsoft.NET/Framework/v2.0.50727/fusion.dll");
        return framework20.isFile() && framework20Fusion.isFile();
    }

    private boolean hasDotnetFrameworkFilesHost() {
        File driveC = getDriveCRootDir();
        File framework32 = new File(driveC, "windows/Microsoft.NET/Framework/v4.0.30319/mscorlib.dll");
        File framework64 = new File(driveC, "windows/Microsoft.NET/Framework64/v4.0.30319/mscorlib.dll");
        return framework32.isFile() && framework64.isFile();
    }

    private boolean hasDotnetFrameworkRegistryProofHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "system.reg"),
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        return anyRegistryTokenPresent(registryFiles,
                "software\\\\microsoft\\\\net framework setup\\\\ndp\\\\v4\\\\full",
                "software\\\\microsoft\\\\.netframework",
                "v4.0.30319",
                "installroot",
                "microsoft.net\\\\framework");
    }

    private boolean hasManagedRuntimeContractHost() {
        return hasWineMonoRuntimeHost() && !hasManagedRuntimeOverridesDisabledHost();
    }

    private boolean isManagedRuntimeRepairTarget(String installTarget) {
        String normalizedTarget = firstNonEmpty(installTarget, "").trim().toLowerCase(Locale.US);
        return "dotnet_framework".equals(normalizedTarget)
                || "legacy_dx_sdk".equals(normalizedTarget)
                || "xna".equals(normalizedTarget);
    }

    private boolean needsManagedRuntimeHostRepair(String installTarget) {
        if (!isManagedRuntimeRepairTarget(installTarget)) return false;
        return hasManagedRuntimeOverridesDisabledHost()
                || hasDotnetInstallRootMismatchHost();
    }

    private void repairManagedRuntimeContractHostIfNeeded(String installTarget) {
        if (!needsManagedRuntimeHostRepair(installTarget)) return;
        boolean hadDisabledOverrides = hasManagedRuntimeOverridesDisabledHost();
        boolean hadInstallRootMismatch = hasDotnetInstallRootMismatchHost();
        boolean repairedOverrides = false;
        boolean repairedInstallRoots = false;

        if (hadDisabledOverrides && hasWineMonoRuntimeHost()) {
            repairedOverrides = rewriteManagedRuntimeOverrideRegistryHost();
        }
        if (hadInstallRootMismatch) {
            repairedInstallRoots = rewriteDotnetInstallRootsHost();
        }
        if (!repairedOverrides && !repairedInstallRoots) return;

        ForensicLogger.logEvent(
                activity,
                "info",
                "PREFIX_PACK_HOST_MANAGED_CONTRACT_REPAIRED",
                null,
                "runtime_ui",
                "prefix_pack_host_managed_contract_repaired",
                ForensicLogger.fields(
                        "install_target", firstNonEmpty(installTarget, ""),
                        "repaired_overrides", repairedOverrides,
                        "repaired_install_roots", repairedInstallRoots,
                        "overrides_disabled_before", hadDisabledOverrides,
                        "install_root_mismatch_before", hadInstallRootMismatch
                )
        );
    }

    private boolean rewriteManagedRuntimeOverrideRegistryHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        boolean changedAny = false;
        for (File registryFile : registryFiles) {
            changedAny |= rewriteManagedRuntimeOverrideRegistryFile(registryFile);
        }
        return changedAny && !hasManagedRuntimeOverridesDisabledHost();
    }

    private boolean rewriteManagedRuntimeOverrideRegistryFile(File file) {
        if (file == null || !file.isFile()) return false;
        String body = FileUtils.readString(file);
        if (body == null || body.isEmpty()) return false;
        String updated = body
                .replace("\"mscoree\"=\"disabled\"", "\"mscoree\"=\"builtin\"")
                .replace("\"mscoreei\"=\"disabled\"", "\"mscoreei\"=\"builtin\"")
                .replace("\"mscorlib\"=\"disabled\"", "\"mscorlib\"=\"builtin\"")
                .replace("\"mscorwks\"=\"disabled\"", "\"mscorwks\"=\"builtin\"");
        if (updated.equals(body)) return false;
        return FileUtils.writeString(file, updated);
    }

    private boolean hasDotnetInstallRootMismatchHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "system.reg"),
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        return anyRegistryTokenPresent(registryFiles,
                "\"InstallRoot\"=\"C:\\\\windows\\\\Microsoft.NET\\\\Framework64\\\\\"");
    }

    private boolean rewriteDotnetInstallRootsHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "system.reg"),
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        boolean changedAny = false;
        for (File registryFile : registryFiles) {
            if (registryFile == null || !registryFile.isFile()) continue;
            String body = FileUtils.readString(registryFile);
            if (body == null || body.isEmpty()) continue;
            String updated = body.replace(
                    "\"InstallRoot\"=\"C:\\\\windows\\\\Microsoft.NET\\\\Framework64\\\\\"",
                    "\"InstallRoot\"=\"C:\\\\windows\\\\Microsoft.NET\\\\Framework\\\\\"");
            if (updated.equals(body)) continue;
            if (FileUtils.writeString(registryFile, updated)) {
                changedAny = true;
            }
        }
        return changedAny && !hasDotnetInstallRootMismatchHost();
    }

    private boolean hasWineMonoRuntimeHost() {
        File driveC = getDriveCRootDir();
        File[] candidates = new File[] {
                new File(driveC, "windows/mono/mono-2.0/bin/libmono-2.0-x86.dll"),
                new File(driveC, "windows/mono/mono-2.0/bin/libmono-2.0-x86_64.dll"),
                new File(driveC, "windows/mono/mono-2.0/lib/mono/4.8-api/mscorlib.dll"),
                new File(driveC, "windows/mono/mono-2.0/lib/mono/4.7.1-api/mscorlib.dll")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) return true;
        }
        return false;
    }

    private boolean hasManagedRuntimeOverridesDisabledHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        return anyRegistryTokenPresent(registryFiles,
                "\"mscoree\"=\"disabled\"",
                "\"mscoreei\"=\"disabled\"",
                "\"mscorlib\"=\"disabled\"",
                "\"mscorwks\"=\"disabled\"");
    }

    private boolean hasDotnetLegacyRegistryProofHost() {
        File prefixDir = getHostWinePrefixDir();
        File[] registryFiles = new File[] {
                new File(prefixDir, "system.reg"),
                new File(prefixDir, "user.reg"),
                new File(prefixDir, "userdef.reg")
        };
        return anyRegistryTokenPresent(registryFiles,
                "software\\\\microsoft\\\\net framework setup\\\\ndp\\\\v2.0.50727",
                "software\\\\microsoft\\\\net framework setup\\\\ndp\\\\v3.5",
                "v2.0.50727",
                "3.5.30729");
    }

    private boolean hasLegacyDxSdkToolsHost() {
        return resolveRuntimeTool("DXCapsViewer.exe", buildDxCapsCandidates()) != null
                || resolveRuntimeTool("dxcpl.exe", buildDxCplCandidates()) != null;
    }

    private boolean anyRegistryTokenPresent(File[] registryFiles, String... tokens) {
        if (registryFiles == null || tokens == null || tokens.length == 0) return false;
        for (File registryFile : registryFiles) {
            if (fileContainsAnyToken(registryFile, tokens)) return true;
        }
        return false;
    }

    private boolean fileContainsAnyToken(File file, String... tokens) {
        if (file == null || !file.isFile() || tokens == null || tokens.length == 0) return false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = line.toLowerCase(Locale.US);
                for (String token : tokens) {
                    if (token != null && !token.isEmpty() && normalized.contains(token)) {
                        return true;
                    }
                }
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private String resolveLaneTitle(String installTarget) {
        LaneSpec lane = findLaneSpec(installTarget);
        return lane != null ? lane.title : installTarget;
    }

    private long parseInstallStateUpdatedAtMs(InstallState state) {
        if (state == null) return -1L;
        String updatedAt = firstNonEmpty(state.updatedAt, "").trim();
        if (updatedAt.isEmpty()) return -1L;
        try {
            return Long.parseLong(updatedAt);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private boolean stateFileIsFresh(File file, long dispatchStartedAt) {
        if (file == null || !file.isFile()) return false;
        long freshnessFloor = dispatchStartedAt > 0L ? Math.max(0L, dispatchStartedAt - 800L) : 0L;
        return file.length() > 0L && file.lastModified() >= freshnessFloor;
    }

    private void markRuntimeDispatchQueuedState(String installTarget, String launcherPath, String detail) {
        markRuntimeDispatchState(
                installTarget,
                launcherPath,
                "queued",
                detail,
                "Wait for the installer window or a fresh launcher log. If neither appears, rerun Install or inspect C:\\AePrefixPack\\staging."
        );
    }

    private void markRuntimeDispatchInteractiveState(String installTarget, String launcherPath, String detail) {
        markRuntimeDispatchState(
                installTarget,
                launcherPath,
                "interactive",
                detail,
                "The detached installer hand-off already started. Follow the visible installer window or reopen the staged launcher if you need to retry."
        );
    }

    private void markRuntimeDispatchState(String installTarget, String launcherPath, String status, String detail, String nextAction) {
        if (installTarget == null || installTarget.trim().isEmpty()) return;
        InstallState existing = readInstallState(installTarget);
        Properties properties = new Properties();
        properties.setProperty("install_target", installTarget);
        properties.setProperty("status", firstNonEmpty(status, "queued"));
        properties.setProperty("exit_code", firstNonEmpty(existing.exitCode, ""));
        properties.setProperty("updated_at", String.valueOf(System.currentTimeMillis()));
        properties.setProperty("log_file", firstNonEmpty(existing.logFile, buildWindowsLogPath(installTarget, "-launcher.log")));
        properties.setProperty("detail", firstNonEmpty(detail, "Runtime accepted the staged installer command, but real launch proof still depends on a fresh launcher or lane log."));
        properties.setProperty("backend_cache", CACHE_ROOT);
        properties.setProperty("installer_cache", WINDOWS_INSTALLER_CACHE);
        properties.setProperty("state_root", getWindowsStateRoot());
        properties.setProperty("log_root", getWindowsLogRoot());
        properties.setProperty("launcher_file", firstNonEmpty(launcherPath, existing.launcherFile));
        properties.setProperty("primary_payload", firstNonEmpty(existing.primaryPayload, ""));
        properties.setProperty("next_action", firstNonEmpty(nextAction, "Wait for the installer window or a fresh launcher log. If neither appears, rerun Install or inspect C:\\AePrefixPack\\staging."));
        properties.setProperty("requested_by", firstNonEmpty(existing.requestedBy, ""));
        FileUtils.writeString(getStateFile(installTarget), storeProperties(properties));
    }

    private File resolveFreshLaneLogPath(String installTarget, long dispatchStartedAt) {
        File logDir = getLogDir();
        if (!logDir.isDirectory()) return null;
        ArrayList<String> hints = buildLaneLogHints(installTarget);
        File newestMatch = null;
        File[] files = logDir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file == null || !file.isFile() || file.length() <= 0L) continue;
            if (dispatchStartedAt > 0L && !stateFileIsFresh(file, dispatchStartedAt)) continue;
            if (!matchesAnyHint(file.getName(), hints)) continue;
            if (newestMatch == null || file.lastModified() > newestMatch.lastModified()) {
                newestMatch = file;
            }
        }
        return newestMatch;
    }

    private ArrayList<String> buildLaneLogHints(String installTarget) {
        ArrayList<String> hints = new ArrayList<>();
        addHint(hints, installTarget);
        addHint(hints, sanitizeTargetSegment(installTarget));
        addHint(hints, sanitizeTargetSegment(installTarget).replace('_', '-'));
        LaneSpec lane = findLaneSpec(installTarget);
        if (lane != null && lane.entryIds != null) {
            for (String entryId : lane.entryIds) {
                addHint(hints, entryId);
                addHint(hints, entryId != null ? entryId.replace('_', '-') : "");
                PrefixPackCatalog.Entry entry = PrefixPackCatalog.findById(catalogEntries, entryId);
                if (entry != null) {
                    addHint(hints, entry.fileName);
                    int dotIndex = entry.fileName.lastIndexOf('.');
                    addHint(hints, dotIndex > 0 ? entry.fileName.substring(0, dotIndex) : entry.fileName);
                }
            }
        }
        return hints;
    }

    private void addHint(ArrayList<String> hints, String value) {
        String normalized = value != null ? value.trim().toLowerCase(Locale.US) : "";
        if (normalized.isEmpty()) return;
        if (!hints.contains(normalized)) hints.add(normalized);
    }

    private boolean matchesAnyHint(String fileName, ArrayList<String> hints) {
        String normalized = fileName != null ? fileName.trim().toLowerCase(Locale.US) : "";
        if (normalized.isEmpty()) return false;
        for (String hint : hints) {
            if (hint == null || hint.isEmpty()) continue;
            if (normalized.contains(hint)) return true;
        }
        return false;
    }

    private String resolvePrimaryPayloadPath(String installTarget, List<PrefixPackCatalog.Entry> entries) {
        if (entries == null || entries.isEmpty()) return "";
        if ("dotnet_framework".equalsIgnoreCase(installTarget)) {
            String preferred = resolveEntryPayloadPath(entries, "dotNetFx40_Full_x86_x64.exe");
            if (!preferred.isEmpty()) return preferred;
            preferred = resolveEntryPayloadPath(entries, "ndp48-x86-x64-allos-enu.exe");
            if (!preferred.isEmpty()) return preferred;
        }
        if ("physx".equalsIgnoreCase(installTarget)) {
            String preferred = resolveEntryPayloadPath(entries, "PhysX_9.21.0713_SystemSoftware.exe");
            if (!preferred.isEmpty()) return preferred;
        }
        if ("legacy_dx_sdk".equalsIgnoreCase(installTarget)) {
            String preferred = resolveEntryPayloadPath(entries, "DXSDK_Jun10.exe");
            if (!preferred.isEmpty()) return preferred;
        }
        if ("graphics_diag".equalsIgnoreCase(installTarget)) {
            String preferred = resolveEntryPayloadPath(entries, "glview6499-setup.exe");
            if (!preferred.isEmpty()) return preferred;
        }
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry != null && isMirrored(entry)) return buildWindowsPayloadPath(entry);
        }
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry != null && isCached(entry)) return buildWindowsPayloadPath(entry);
        }
        PrefixPackCatalog.Entry first = entries.get(0);
        return first != null ? buildWindowsPayloadPath(first) : "";
    }

    private String resolvePrimaryPayloadLabel(String installTarget, List<PrefixPackCatalog.Entry> entries, String primaryPayloadPath) {
        String normalizedTarget = firstNonEmpty(installTarget, "").trim().toLowerCase(Locale.US);
        String normalizedPayload = firstNonEmpty(primaryPayloadPath, "").trim().toLowerCase(Locale.US);
        if ("dotnet_framework".equals(normalizedTarget)) {
            if (normalizedPayload.contains("ndp48")) return ".NET Framework 4.8";
            if (normalizedPayload.contains("dotnetfx40")) return ".NET Framework 4.0 Full";
            if (normalizedPayload.contains("dotnetfx35")) return ".NET Framework 3.5 SP1";
        }
        if ("physx".equals(normalizedTarget)) {
            if (normalizedPayload.contains("legacy")) return "NVIDIA PhysX Legacy Runtime";
            return "NVIDIA PhysX System Software";
        }
        if ("legacy_dx_sdk".equals(normalizedTarget)) return "DirectX SDK June 2010";
        if ("graphics_diag".equals(normalizedTarget)) return "GLview";
        if ("wine_web_stack".equals(normalizedTarget)) return "Wine Mono + Gecko stack";
        if ("vcrun_full".equals(normalizedTarget)) return "VC runtime stack";
        if ("xna".equals(normalizedTarget)) {
            if (normalizedPayload.contains("31")) return "XNA Framework 3.1";
            if (normalizedPayload.contains("40")) return "XNA Framework 4.0 Refresh";
        }
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null) continue;
            if (buildWindowsPayloadPath(entry).equalsIgnoreCase(primaryPayloadPath)) {
                return lastPathSegment(entry.fileName);
            }
        }
        return lastPathSegment(primaryPayloadPath);
    }

    private String resolveEntryPayloadPath(List<PrefixPackCatalog.Entry> entries, String expectedFileName) {
        if (entries == null || expectedFileName == null || expectedFileName.trim().isEmpty()) return "";
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null || !expectedFileName.equalsIgnoreCase(entry.fileName)) continue;
            if (isMirrored(entry) || isCached(entry)) return buildWindowsPayloadPath(entry);
        }
        return "";
    }

    private String buildWindowsPayloadPath(PrefixPackCatalog.Entry entry) {
        if (entry == null) return "";
        return WINDOWS_INSTALLER_CACHE + "\\" + entry.fileName;
    }

    private String buildWindowsLauncherPath(String installTarget) {
        return WINDOWS_STAGE_ROOT + "\\" + sanitizeTargetSegment(installTarget) + "\\install-" + sanitizeTargetSegment(installTarget) + ".cmd";
    }

    private String buildWindowsDispatchPath(String installTarget) {
        return WINDOWS_STAGE_ROOT + "\\" + sanitizeTargetSegment(installTarget) + "\\launch-" + sanitizeTargetSegment(installTarget) + ".vbs";
    }

    private File getStageNoteFile(String installTarget) {
        String segment = sanitizeTargetSegment(installTarget);
        return new File(new File(getStageRootDir(), segment), "install-" + segment + "-notes.txt");
    }

    private String buildWindowsLogPath(String installTarget, String suffix) {
        return getWindowsLogRoot() + "\\" + installTarget + suffix;
    }

    private String sanitizeTargetSegment(String installTarget) {
        if (installTarget == null || installTarget.trim().isEmpty()) return "lane";
        return installTarget.trim().replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private String escapeBatchValue(String value) {
        if (value == null) return "";
        return value.replace("\"", "'").replace("\r", " ").replace("\n", " ");
    }

    private String storeProperties(Properties properties) {
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException ignored) {
            return "";
        }
        String raw = writer.toString();
        int firstLineBreak = raw.indexOf('\n');
        if (firstLineBreak >= 0 && raw.startsWith("#")) {
            raw = raw.substring(firstLineBreak + 1);
        }
        return raw;
    }

    private void showLaneInfo(LaneSpec lane, List<PrefixPackCatalog.Entry> entries) {
        InstallState state = readInstallState(lane.installTarget);
        ContentDialog dialog = new ContentDialog(activity, R.layout.prefix_pack_lane_info_dialog);
        dialog.setTitle(lane.title);
        dialog.setIcon(R.drawable.ae_icon_about);
        dialog.setBottomBarText(null);
        View dialogMessage = dialog.findViewById(R.id.TVMessage);
        if (dialogMessage != null) dialogMessage.setVisibility(View.GONE);
        TextView summaryView = dialog.findViewById(R.id.TVPrefixPackLaneInfoSummary);
        TextView stateView = dialog.findViewById(R.id.TVPrefixPackLaneInfoState);
        TextView bodyView = dialog.findViewById(R.id.TVPrefixPackLaneInfoBody);
        if (summaryView != null) {
            summaryView.setText(buildLaneCoverageSummary(lane));
        }
        if (stateView != null) {
            String stateText = "State  " + describeInstallState(state, lane.mayRequireGui)
                    + "  •  Cache " + countCached(entries) + "/" + entries.size()
                    + "  •  Mirror " + countMirrored(entries) + "/" + entries.size();
            stateView.setText(stateText);
        }
        if (bodyView != null) {
            bodyView.setText(buildLaneInfoDetailBody(lane, entries, state));
        }
        Button confirmButton = dialog.findViewById(R.id.BTConfirm);
        if (confirmButton != null) confirmButton.setVisibility(View.GONE);
        Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null) cancelButton.setText(R.string.close);
        dialog.show();
        styleNestedDialog(dialog);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            dialog.getWindow().setLayout(
                    Math.round(AppUtils.getScreenWidth() * 0.948f),
                    Math.round(AppUtils.getScreenHeight() * 0.708f)
            );
        }
        ViewGroup.LayoutParams params = dialog.getContentView().getLayoutParams();
        if (params != null) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = Math.round(AppUtils.getScreenHeight() * 0.684f);
            dialog.getContentView().setLayoutParams(params);
        }
        dialog.getContentView().setMinimumHeight(Math.round(AppUtils.getScreenHeight() * 0.684f));
    }

    private String buildLaneInfoDetailBody(LaneSpec lane, List<PrefixPackCatalog.Entry> entries, InstallState state) {
        StringBuilder builder = new StringBuilder();
        builder.append("Prepare  Z/C cache").append('\n');
        builder.append("Install  ").append(lastPathSegment(buildWindowsLauncherPath(lane.installTarget))).append('\n');
        builder.append("State    ").append(lastPathSegment(buildWindowsStatePath(lane.installTarget))).append('\n');
        builder.append("Logs     ").append(lastPathSegment(getWindowsLogRoot())).append('\n');
        builder.append("Mode     ").append(lane.mayRequireGui ? "GUI may appear" : "mostly unattended").append('\n');
        if ("dotnet_framework".equals(lane.installTarget)) {
            builder.append("Legacy35 auto  skipped on Wine; 3.5 SP1 stays staged in C: cache for manual secondary path").append('\n');
        }

        if (state != null && state.exists()) {
            builder.append("\nLast\n");
            builder.append("status  ").append(describeInstallState(state, lane.mayRequireGui)).append('\n');
            if (!state.updatedAt.isEmpty()) builder.append("updated ").append(state.updatedAt).append('\n');
            if (!state.logFile.isEmpty()) builder.append("log     ").append(lastPathSegment(state.logFile)).append('\n');
            if (!state.launcherFile.isEmpty()) builder.append("launch  ").append(lastPathSegment(state.launcherFile)).append('\n');
            if (!state.primaryPayload.isEmpty()) builder.append("payload ").append(lastPathSegment(state.primaryPayload)).append('\n');
            if (!state.nextAction.isEmpty()) builder.append("next    ").append(state.nextAction).append('\n');
        }

        builder.append("\nEntries\n");
        for (PrefixPackCatalog.Entry entry : entries) {
            if (entry == null) continue;
            builder.append("- ").append(entry.fileName)
                    .append("  ").append(describeCacheState(entry))
                    .append("  mirror=").append(isMirrored(entry) ? "yes" : "no")
                    .append('\n');
        }
        builder.append("\nTip  Use Prepare, Install or Clean on the lane card. Long-press the card for the newest lane log.");
        return builder.toString().trim();
    }

    private String buildLaneCoverageSummary(LaneSpec lane) {
        if (lane == null || lane.installTarget == null) return "";
        switch (lane.installTarget) {
            case "vcrun_full":
                return "VC6 x86 bootstrap + VC++ AIO x86/x64 runtime branches";
            case "wine_web_stack":
                return "Wine Mono x86 MSI + Wine Gecko x86 + x86_64";
            case "dotnet_framework":
                return ".NET 4.0 Full first for v4 CLR repair, with 4.8 staged as an optional follow-up";
            case "directx_jun2010":
                return "Legacy D3DX, XAudio, XACT, XInput and side-by-side June 2010 CAB set";
            case "xna":
                return "XNA 3.1 + XNA 4.0 Refresh";
            case "openal":
                return "OpenAL 1.1 Windows runtime";
            case "physx":
                return "Legacy AGEIA MSI + modern NVIDIA PhysX System Software EXE";
            case "lavfilters":
                return "DirectShow splitter + audio/video filters";
            case "legacy_dx_sdk":
                return "DirectX SDK June 2010 tools, especially DXCapsViewer and dxcpl";
            case "graphics_diag":
                return "GLview installer + donor TestD3D.exe + donor GPUInfo.exe";
            default:
                return "";
        }
    }

    private String describeCacheState(PrefixPackCatalog.Entry entry) {
        if (entry == null) return "missing";
        File cacheFile = new File(getCacheDir(), entry.fileName);
        if (!cacheFile.isFile()) return "missing";
        if (!isCached(entry)) {
            return entry.hasSha256() ? "present but empty or checksum mismatch" : "present but empty";
        }
        return entry.hasSha256() ? "yes (sha256 verified)" : "yes";
    }

    private String buildWindowsStatePath(String installTarget) {
        return getWindowsStateRoot() + "\\" + installTarget + ".properties";
    }

    private String[] buildTestD3dCandidates() {
        return new String[] {
                "opt/apps/TestD3D.exe",
                "tmp/opt/apps/TestD3D.exe",
                "tmp/TestD3D.exe"
        };
    }

    private String[] buildGpuInfoCandidates() {
        return new String[] {
                "opt/apps/GPUInfo.exe",
                "tmp/opt/apps/GPUInfo.exe",
                "tmp/GPUInfo.exe"
        };
    }

    private String[] buildGlViewCandidates() {
        return new String[] {
                ImageFs.WINEPREFIX + "/drive_c/Program Files (x86)/realtech VR/OpenGL Extensions Viewer 6.4/openglex.exe",
                ImageFs.WINEPREFIX + "/drive_c/Program Files/realtech VR/OpenGL Extensions Viewer 6.4/openglex.exe"
        };
    }

    private String[] buildDxCapsCandidates() {
        return new String[] {
                ImageFs.WINEPREFIX + "/drive_c/Program Files (x86)/Microsoft DirectX SDK (June 2010)/Utilities/bin/x86/DXCapsViewer.exe",
                ImageFs.WINEPREFIX + "/drive_c/Program Files/Microsoft DirectX SDK (June 2010)/Utilities/bin/x86/DXCapsViewer.exe"
        };
    }

    private String[] buildDxCplCandidates() {
        return new String[] {
                ImageFs.WINEPREFIX + "/drive_c/Program Files (x86)/Microsoft DirectX SDK (June 2010)/Utilities/bin/x86/dxcpl.exe",
                ImageFs.WINEPREFIX + "/drive_c/Program Files/Microsoft DirectX SDK (June 2010)/Utilities/bin/x86/dxcpl.exe"
        };
    }

    private File resolveRuntimeTool(String expectedName, String[] candidates) {
        File rootDir = imageFs.getRootDir();
        if (candidates != null) {
            for (String candidate : candidates) {
                if (candidate == null || candidate.trim().isEmpty()) continue;
                File file = resolveWineRelativeFile(candidate);
                if (file.isFile()) return file;
            }
        }

        File[] roots = new File[] {
                new File(rootDir, "opt"),
                new File(rootDir, "tmp"),
                new File(getDriveCRootDir(), "AePrefixPack"),
                new File(getDriveCRootDir(), "Program Files"),
                new File(getDriveCRootDir(), "Program Files (x86)")
        };
        for (File searchRoot : roots) {
            File match = scanForFile(searchRoot, expectedName, TOOL_SCAN_MAX_DEPTH);
            if (match != null) return match;
        }
        return null;
    }

    private File scanForFile(File directory, String expectedName, int depthRemaining) {
        if (directory == null || !directory.isDirectory() || depthRemaining < 0) return null;
        File[] children = directory.listFiles();
        if (children == null || children.length == 0) return null;

        for (File child : children) {
            if (child != null && child.isFile() && expectedName.equalsIgnoreCase(child.getName())) {
                return child;
            }
        }
        if (depthRemaining == 0) return null;

        for (File child : children) {
            if (child == null || !child.isDirectory()) continue;
            File nested = scanForFile(child, expectedName, depthRemaining - 1);
            if (nested != null) return nested;
        }
        return null;
    }

    private String toWinePath(File file) {
        if (file == null) return "";
        String absolute = file.getAbsolutePath();
        String root = imageFs.getRootDir().getAbsolutePath();
        String driveC = getDriveCRootDir().getAbsolutePath();
        if (absolute.startsWith(driveC + "/")) {
            return "C:\\" + absolute.substring(driveC.length() + 1).replace('/', '\\');
        }
        if (absolute.startsWith(root + "/")) {
            return "Z:\\" + absolute.substring(root.length() + 1).replace('/', '\\');
        }
        return absolute.replace('/', '\\');
    }

    private void applySurfaceStyle() {
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_text);
        int subtleText = ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_muted);
        getContentView().setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        getContentView().setPadding(dp(8), dp(6), dp(8), dp(6));
        View frameLayout = findViewById(R.id.FrameLayout);
        if (frameLayout != null) {
            frameLayout.setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        }

        View overviewCard = findViewById(R.id.LLPrefixPackOverviewCard);
        if (overviewCard != null) overviewCard.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);
        View lanesCard = findViewById(R.id.LLPrefixPackLanesCard);
        if (lanesCard != null) lanesCard.setBackgroundResource(R.drawable.surface_runtime_taskmgr_background);

        int[] mutedTextIds = new int[] {
                R.id.TVPrefixPackIntro,
                R.id.TVPrefixPackPaths,
                R.id.TVPrefixPackFlow,
                R.id.TVPrefixPackLaneHint
        };
        for (int id : mutedTextIds) {
            TextView textView = findViewById(id);
            if (textView != null) textView.setTextColor(subtleText);
        }
        TextView sessionView = findViewById(R.id.TVPrefixPackSession);
        if (sessionView != null) sessionView.setTextColor(brightText);

        TextView titleView = findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);
        View titleBar = findViewById(R.id.LLTitleBar);
        if (titleBar instanceof LinearLayout) {
            titleBar.setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
            titleBar.setPadding(dp(8), dp(8), dp(8), dp(2));
        }
        TextView overviewBadge = findViewById(R.id.TVPrefixPackOverviewBadge);
        if (overviewBadge != null) {
            overviewBadge.setTextColor(brightText);
            overviewBadge.setBackgroundResource(R.drawable.surface_runtime_prefixpack_header_background);
        }
        TextView lanesBadge = findViewById(R.id.TVPrefixPackLanesBadge);
        if (lanesBadge != null) {
            lanesBadge.setTextColor(brightText);
            lanesBadge.setBackgroundResource(R.drawable.surface_runtime_prefixpack_header_background);
        }

        android.widget.ImageView iconView = findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(brightText);

        View titleBackButton = findViewById(R.id.BTTitleBack);
        if (titleBackButton instanceof android.widget.ImageButton) {
            ((android.widget.ImageButton) titleBackButton).setColorFilter(brightText);
            titleBackButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
        }

        Button confirmButton = findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setVisibility(View.GONE);
        }
        View bottomBar = findViewById(R.id.LLBottomBar);
        if (bottomBar != null) {
            bottomBar.setVisibility(View.GONE);
        }
    }

    private void styleNestedDialog(ContentDialog dialog) {
        if (dialog == null) return;
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_text);
        View root = dialog.getContentView();
        if (root != null) {
            root.setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        }
        TextView titleView = dialog.findViewById(R.id.TVTitle);
        if (titleView != null) titleView.setTextColor(brightText);
        TextView messageView = dialog.findViewById(R.id.TVMessage);
        if (messageView != null) messageView.setTextColor(brightText);
        ImageView iconView = dialog.findViewById(R.id.IVIcon);
        if (iconView != null) iconView.setColorFilter(brightText);
        Button confirmButton = dialog.findViewById(R.id.BTConfirm);
        if (confirmButton != null) {
            confirmButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            confirmButton.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
        Button cancelButton = dialog.findViewById(R.id.BTCancel);
        if (cancelButton != null && cancelButton.getVisibility() == View.VISIBLE) {
            cancelButton.setBackgroundResource(R.drawable.surface_runtime_button_neutral);
            cancelButton.setTextColor(ContextCompat.getColor(activity, R.color.surface_runtime_button_text));
        }
    }

    private void styleLaneCard(View itemView, ImageView ivIcon, TextView tvTitle, TextView tvSection, TextView tvState, TextView tvSummary, TextView tvSource) {
        itemView.setBackgroundResource(R.drawable.surface_runtime_prefixpack_background);
        int brightText = ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_text);
        int subtleText = ContextCompat.getColor(activity, R.color.surface_runtime_prefixpack_muted);
        if (ivIcon != null) ivIcon.setColorFilter(brightText);
        if (tvTitle != null) tvTitle.setTextColor(brightText);
        if (tvSection != null) {
            tvSection.setTextColor(brightText);
            tvSection.setBackgroundResource(R.drawable.surface_runtime_prefixpack_badge_background);
        }
        if (tvState != null) tvState.setTextColor(brightText);
        if (tvSummary != null) tvSummary.setTextColor(subtleText);
        if (tvSource != null) tvSource.setTextColor(subtleText);
    }

    private boolean deleteMatchingLaneLogs(String installTarget) {
        File logDir = getLogDir();
        if (!logDir.isDirectory()) return true;
        String normalized = sanitizeTargetSegment(installTarget).toLowerCase(Locale.US);
        String dashed = normalized.replace('_', '-');
        boolean success = true;
        File[] files = logDir.listFiles();
        if (files == null) return true;
        for (File file : files) {
            if (file == null || !file.isFile()) continue;
            String lowerName = file.getName().toLowerCase(Locale.US);
            if (!lowerName.startsWith(normalized) && !lowerName.startsWith(dashed)) continue;
            success &= deleteIfExists(file);
        }
        return success;
    }

    private boolean deleteIfExists(File file) {
        return file == null || !file.exists() || FileUtils.delete(file);
    }

    private static String firstNonEmpty(String first, String fallback) {
        return first != null && !first.trim().isEmpty() ? first : fallback;
    }

    private File firstNonEmptyFile(File first, File fallback) {
        if (first != null && first.isFile()) return first;
        if (fallback != null && fallback.isFile()) return fallback;
        return first != null ? first : fallback;
    }

    private File getStageBootstrapLogFile(String installTarget) {
        if (installTarget == null || installTarget.trim().isEmpty()) return null;
        String stageSegment = sanitizeTargetSegment(installTarget);
        return new File(getStageRootDir(), stageSegment + "/install-" + stageSegment + "-bootstrap.log");
    }

    private File resolveStateLogFile(InstallState state, String installTarget) {
        String windowsLogPath = state != null ? firstNonEmpty(state.logFile, "") : "";
        File resolved = resolveWindowsSideFile(windowsLogPath);
        File bootstrapLog = getStageBootstrapLogFile(installTarget);
        if (resolved != null && (bootstrapLog == null || !resolved.equals(bootstrapLog))) {
            return resolved;
        }
        return new File(getLogDir(), installTarget + "-launcher.log");
    }

    private File resolveWindowsSideFile(String windowsPath) {
        String normalized = firstNonEmpty(windowsPath, "");
        if (normalized.trim().isEmpty()) return null;
        String windowsValue = normalized.replace('/', '\\');
        String upperValue = windowsValue.toUpperCase(Locale.US);
        if (upperValue.startsWith("C:\\")) {
            return new File(getDriveCRootDir(), windowsValue.substring(3).replace('\\', '/'));
        }
        if (upperValue.startsWith("Z:\\")) {
            return new File(imageFs.getRootDir(), windowsValue.substring(3).replace('\\', '/'));
        }
        return new File(windowsValue.replace('\\', '/'));
    }

    private static String lastPathSegment(String path) {
        String value = firstNonEmpty(path, "");
        int slashIndex = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        if (slashIndex >= 0 && slashIndex + 1 < value.length()) {
            return value.substring(slashIndex + 1);
        }
        return value;
    }

    private static String clipMiddle(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= maxLength) return normalized;
        int head = Math.max(10, (maxLength / 2) - 2);
        int tail = Math.max(8, maxLength - head - 3);
        return normalized.substring(0, head) + "..." + normalized.substring(normalized.length() - tail);
    }

    private void disableViewFocus(View view) {
        if (view == null) return;
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                activity.getResources().getDisplayMetrics()
        ));
    }

    private boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
