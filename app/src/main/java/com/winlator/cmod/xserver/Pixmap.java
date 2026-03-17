package com.winlator.cmod.xserver;

import android.graphics.Bitmap;

import com.winlator.cmod.core.WinlatorNative;

import java.nio.ByteBuffer;

public class Pixmap extends XResource {
    static {
        WinlatorNative.ensureLoaded("Pixmap");
    }

    public final Drawable drawable;

    public Pixmap(Drawable drawable) {
        super(drawable.id);
        this.drawable = drawable;
    }

    public Bitmap toBitmap(Pixmap maskPixmap) {
        ByteBuffer maskData = maskPixmap != null ? maskPixmap.drawable.getData() : null;
        Bitmap bitmap = Bitmap.createBitmap(drawable.width, drawable.height, Bitmap.Config.ARGB_8888);
        toBitmap(drawable.getData(), maskData, bitmap);
        return bitmap;
    }

    private static native void toBitmap(ByteBuffer colorData, ByteBuffer maskData, Bitmap bitmap);
}
