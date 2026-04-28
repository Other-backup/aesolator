package com.winlator.cmod.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.winlator.cmod.core.ForensicLogger;
import com.winlator.cmod.renderer.GLRenderer;
import com.winlator.cmod.xserver.XServer;

@SuppressLint("ViewConstructor")
public class XServerView extends GLSurfaceView {
    private static final long[] STARTUP_RENDER_PULSE_DELAYS_MS = {0L, 48L, 120L, 240L, 480L};
    private static final long SURFACE_KEEPALIVE_RENDER_INTERVAL_MS = 1000L;
    private final GLRenderer renderer;
    private boolean startupRenderPulsesArmed;
    private boolean surfaceKeepaliveArmed;
    private int surfaceKeepaliveRenderCount;
    private final Runnable surfaceKeepaliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!surfaceKeepaliveArmed || !isAttachedToWindow()) return;
            surfaceKeepaliveRenderCount++;
            requestRenderSafely("surface_keepalive", surfaceKeepaliveRenderCount);
            postDelayed(this, SURFACE_KEEPALIVE_RENDER_INTERVAL_MS);
        }
    };

    public XServerView(Context context, XServer xServer) {
        super(context);
        setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setPreserveEGLContextOnPause(true);
        renderer = new GLRenderer(this, xServer);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        armStartupRenderPulses("constructor");
    }

    public GLRenderer getRenderer() {
        return renderer;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        armStartupRenderPulses("attached_to_window");
        armSurfaceKeepalive("attached_to_window");
        requestLifecycleRender("attached_to_window");
    }

    @Override
    protected void onDetachedFromWindow() {
        stopSurfaceKeepalive("detached_from_window");
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) requestLifecycleRender("size_changed");
    }

    @Override
    public void onResume() {
        super.onResume();
        armSurfaceKeepalive("resume");
        requestLifecycleRender("resume");
    }

    @Override
    public void onPause() {
        stopSurfaceKeepalive("pause");
        super.onPause();
    }

    public void armStartupRenderPulses(String source) {
        if (startupRenderPulsesArmed) return;
        startupRenderPulsesArmed = true;
        ForensicLogger.logEvent(
                getContext(),
                "info",
                "XSERVER_VIEW_STARTUP_RENDER_PULSES_ARMED",
                null,
                "xserver_surface",
                "startup_render_pulses_armed",
                ForensicLogger.fields(
                        "source", source,
                        "pulse_count", STARTUP_RENDER_PULSE_DELAYS_MS.length
                )
        );
        for (int i = 0; i < STARTUP_RENDER_PULSE_DELAYS_MS.length; i++) {
            final int attempt = i + 1;
            postDelayed(
                    () -> requestRenderSafely("startup_pulse", attempt),
                    STARTUP_RENDER_PULSE_DELAYS_MS[i]
            );
        }
    }

    public void requestLifecycleRender(String source) {
        post(() -> requestRenderSafely(source, 0));
    }

    private void armSurfaceKeepalive(String source) {
        if (surfaceKeepaliveArmed) return;
        surfaceKeepaliveArmed = true;
        surfaceKeepaliveRenderCount = 0;
        ForensicLogger.logEvent(
                getContext(),
                "info",
                "XSERVER_VIEW_RENDER_KEEPALIVE_ARMED",
                null,
                "xserver_surface",
                "surface_keepalive_render_armed",
                ForensicLogger.fields(
                        "source", source,
                        "interval_ms", SURFACE_KEEPALIVE_RENDER_INTERVAL_MS
                )
        );
        removeCallbacks(surfaceKeepaliveRunnable);
        postDelayed(surfaceKeepaliveRunnable, SURFACE_KEEPALIVE_RENDER_INTERVAL_MS);
    }

    private void stopSurfaceKeepalive(String source) {
        if (!surfaceKeepaliveArmed) return;
        surfaceKeepaliveArmed = false;
        removeCallbacks(surfaceKeepaliveRunnable);
        ForensicLogger.logEvent(
                getContext(),
                "info",
                "XSERVER_VIEW_RENDER_KEEPALIVE_STOPPED",
                null,
                "xserver_surface",
                "surface_keepalive_render_stopped",
                ForensicLogger.fields(
                        "source", source,
                        "render_count", surfaceKeepaliveRenderCount
                )
        );
    }

    private void requestRenderSafely(String source, int attempt) {
        try {
            requestRender();
            if (shouldLogRenderRequest(source, attempt)) {
                ForensicLogger.logEvent(
                        getContext(),
                        "info",
                        "XSERVER_VIEW_RENDER_REQUESTED",
                        null,
                        "xserver_surface",
                        "xserver_view_render_requested",
                        ForensicLogger.fields(
                                "source", source,
                                "attempt", attempt,
                                "width", getWidth(),
                                "height", getHeight(),
                                "attached_to_window", isAttachedToWindow()
                        )
                );
            }
        }
        catch (RuntimeException error) {
            ForensicLogger.error(
                    getContext(),
                    "XSERVER_VIEW_RENDER_REQUEST_FAILED",
                    null,
                    "xserver_surface",
                    "xserver_view_render_request_failed",
                    error,
                    ForensicLogger.fields(
                            "source", source,
                            "attempt", attempt,
                            "width", getWidth(),
                            "height", getHeight(),
                            "attached_to_window", isAttachedToWindow()
                    )
            );
        }
    }

    private boolean shouldLogRenderRequest(String source, int attempt) {
        if ("surface_keepalive".equals(source)) {
            return attempt == 1 || attempt % 15 == 0;
        }
        return attempt <= 1;
    }
}
