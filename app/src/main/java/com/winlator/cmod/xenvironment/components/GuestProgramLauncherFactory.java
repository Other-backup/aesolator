package com.winlator.cmod.xenvironment.components;

import com.winlator.cmod.container.Shortcut;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.xenvironment.ImageFs;

public abstract class GuestProgramLauncherFactory {
    public static GuestProgramLauncherComponent create(
            ImageFs imageFs,
            ContentsManager contentsManager,
            ContentProfile wineProfile,
            Shortcut shortcut,
            String requestedRuntimeModel
    ) {
        String effectiveRuntimeModel = ContentProfile.normalizeRuntimeModel(requestedRuntimeModel);
        if (effectiveRuntimeModel.isEmpty() && wineProfile != null) {
            effectiveRuntimeModel = wineProfile.getRuntimeModel();
        }
        if (effectiveRuntimeModel.isEmpty() && imageFs != null) {
            effectiveRuntimeModel = imageFs.getRuntimeLibcModel();
        }
        if ("glibc".equalsIgnoreCase(effectiveRuntimeModel)) {
            return new GlibcProgramLauncherComponent(contentsManager, wineProfile, shortcut);
        }
        return new BionicProgramLauncherComponent(contentsManager, wineProfile, shortcut);
    }
}
