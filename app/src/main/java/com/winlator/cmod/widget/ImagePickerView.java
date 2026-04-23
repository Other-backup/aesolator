package com.winlator.cmod.widget;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.winlator.cmod.MainActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.ImageUtils;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.core.WineThemeManager;

import java.io.File;

public class ImagePickerView extends AppCompatButton implements View.OnClickListener {
    @Nullable
    private File targetFile;

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
        setText(R.string.big_picture_select_wallpaper);
        setAllCaps(false);
        setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        int horizontalPadding = Math.round(UnitUtils.dpToPx(14));
        int verticalPadding = Math.round(UnitUtils.dpToPx(10));
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        setMinHeight(Math.round(UnitUtils.dpToPx(44)));
        setClickable(true);
        setFocusable(true);
        setOnClickListener(this);
    }

    @Override
    public void onClick(View anchor) {
        final Context context = getContext();
        final File userWallpaperFile = targetFile != null ? targetFile : WineThemeManager.getUserWallpaperFile(context);

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
            MainActivity.setImagePickerCallback((Bitmap bitmap) -> {
                File parent = userWallpaperFile.getParentFile();
                if (parent != null && !parent.isDirectory()) parent.mkdirs();
                ImageUtils.save(bitmap, userWallpaperFile, Bitmap.CompressFormat.PNG, 100);
            });
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

    public void setTargetFile(@Nullable File targetFile) {
        this.targetFile = targetFile;
    }
}
