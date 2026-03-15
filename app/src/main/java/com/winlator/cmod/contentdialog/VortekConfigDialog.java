package com.winlator.cmod.contentdialog;

import android.view.View;

import com.winlator.cmod.R;
import com.winlator.cmod.core.GPUHelper;
import com.winlator.cmod.xenvironment.components.VortekRendererComponent;

public class VortekConfigDialog extends ContentDialog {
    public static final String DEFAULT_VK_MAX_VERSION;

    static {
        int version = VortekRendererComponent.VK_MAX_VERSION;
        DEFAULT_VK_MAX_VERSION = GPUHelper.vkVersionMajor(version) + "." + GPUHelper.vkVersionMinor(version);
    }

    public VortekConfigDialog(View anchor) {
        super(anchor.getContext(), R.layout.content_dialog);
    }
}
