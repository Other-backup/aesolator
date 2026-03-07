package com.winlator.cmod.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.core.WineThemeManager;

import java.io.File;

public class ImagePickerView extends View implements View.OnClickListener {
    public ImagePickerView(Context context) {
        this(context, null);
    }

    public ImagePickerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImagePickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setTag("theme_combo_box");
        setBackgroundResource(R.drawable.combo_box);
        setClickable(true);
        setFocusable(true);
        setOnClickListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float rectSize = height - UnitUtils.dpToPx(12);
        float startX = (width - rectSize) * 0.5f - UnitUtils.dpToPx(16);
        float startY = (height - rectSize) * 0.5f;
        drawFallbackGlyph(canvas, startX, startY, rectSize);
    }

    private void drawFallbackGlyph(Canvas canvas, float startX, float startY, float rectSize) {
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(UnitUtils.dpToPx(1.6f));
        strokePaint.setColor(0xFFFFFFFF);

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x66FFFFFF);

        float radius = UnitUtils.dpToPx(4);
        RectF frame = new RectF(startX, startY, startX + rectSize, startY + rectSize);
        canvas.drawRoundRect(frame, radius, radius, strokePaint);

        float sunRadius = rectSize * 0.12f;
        canvas.drawCircle(startX + rectSize * 0.28f, startY + rectSize * 0.30f, sunRadius, fillPaint);

        Paint mountainPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mountainPaint.setStyle(Paint.Style.STROKE);
        mountainPaint.setStrokeWidth(UnitUtils.dpToPx(1.4f));
        mountainPaint.setColor(0xFFFFFFFF);
        float baseY = startY + rectSize * 0.74f;
        canvas.drawLine(startX + rectSize * 0.14f, baseY, startX + rectSize * 0.40f, startY + rectSize * 0.48f, mountainPaint);
        canvas.drawLine(startX + rectSize * 0.40f, startY + rectSize * 0.48f, startX + rectSize * 0.56f, baseY, mountainPaint);
        canvas.drawLine(startX + rectSize * 0.42f, baseY, startX + rectSize * 0.67f, startY + rectSize * 0.58f, mountainPaint);
        canvas.drawLine(startX + rectSize * 0.67f, startY + rectSize * 0.58f, startX + rectSize * 0.84f, baseY, mountainPaint);
    }

    @Override
    public void onClick(View anchor) {
        final Context context = getContext();
        final File userWallpaperFile = WineThemeManager.getUserWallpaperFile(context);

        View view = LayoutInflater.from(context).inflate(R.layout.image_picker_view, null);
        ImageView imageView = view.findViewById(R.id.ImageView);

        if (userWallpaperFile.isFile()) {
            imageView.setImageBitmap(BitmapFactory.decodeFile(userWallpaperFile.getPath()));
        }
        else imageView.setImageResource(R.drawable.wallpaper);

        final PopupWindow[] popupWindow = {null};
        View browseButton = view.findViewById(R.id.BTBrowse);
        browseButton.setOnClickListener((v) -> {
            MainActivity activity = (MainActivity)context;
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            popupWindow[0].dismiss();
            activity.startActivityForResult(intent, MainActivity.OPEN_IMAGE_REQUEST_CODE);
        });

        View removeButton = view.findViewById(R.id.BTRemove);
        if (userWallpaperFile.isFile()) {
            removeButton.setVisibility(View.VISIBLE);
            removeButton.setOnClickListener((v) -> {
                FileUtils.delete(userWallpaperFile);
                popupWindow[0].dismiss();
            });
        }

        popupWindow[0] = AppUtils.showPopupWindow(anchor, view, 200, 240);
    }
}
