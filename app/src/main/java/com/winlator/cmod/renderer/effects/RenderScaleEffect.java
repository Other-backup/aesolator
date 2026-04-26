package com.winlator.cmod.renderer.effects;

import com.winlator.cmod.renderer.GLRenderer;

public interface RenderScaleEffect {
    int getRenderWidth(GLRenderer renderer, int outputWidth);
    int getRenderHeight(GLRenderer renderer, int outputHeight);
}
