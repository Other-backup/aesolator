package com.winlator.cmod.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.IntRange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ImageUtils {
    private static int calculateInSampleSize(BitmapFactory.Options options, int maxSize) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        int reqWidth = width >= height ? maxSize : 0;
        int reqHeight = height >= width ? maxSize : 0;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri, BitmapFactory.Options options) {
        Bitmap bitmap = null;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (options != null) {
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }
            else bitmap = BitmapFactory.decodeStream(is);
        }
        catch (IOException e) {
            Log.w("ImageUtils", "Failed to decode bitmap from uri " + uri, e);
        }
        return bitmap;
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) {
        return getBitmapFromUri(context, uri, null);
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri, int maxSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            int inSampleSize = calculateInSampleSize(options, maxSize);
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
        }
        catch (IOException e) {
            Log.w("ImageUtils", "Failed to read bitmap bounds from uri " + uri, e);
        }

        return getBitmapFromUri(context, uri, options);
    }

    public static boolean save(Bitmap bitmap, File output, Bitmap.CompressFormat compressFormat, @IntRange(from = 0, to = 100) int quality) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(output);
            return bitmap.compress(compressFormat, quality, fos);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
