package com.winlator.cmod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.winlator.cmod.R;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.SpinnerAdapters;
import com.winlator.cmod.core.ThemeAssetPainter;
import com.winlator.cmod.core.UnitUtils;
import com.winlator.cmod.inputcontrols.Binding;
import com.winlator.cmod.inputcontrols.ControlsProfile;
import com.winlator.cmod.inputcontrols.ExternalController;
import com.winlator.cmod.inputcontrols.ExternalControllerBinding;
import com.winlator.cmod.inputcontrols.InputControlsManager;
import com.winlator.cmod.math.Mathf;

public class ExternalControllerBindingsActivity extends AppCompatActivity {
    private TextView emptyTextView;
    private ControlsProfile profile;
    private ExternalController controller;
    private RecyclerView recyclerView;
    private ControllerBindingsAdapter adapter;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.external_controller_bindings_activity);
        isDarkMode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_mode", false);

        Intent intent = getIntent();
        int profileId = intent.getIntExtra("profile_id", 0);
        profile = InputControlsManager.loadProfile(this, ControlsProfile.getProfileFile(this, profileId));
        String controllerId = intent.getStringExtra("controller_id");

        controller = profile.getController(controllerId);
        if (controller == null) {
            controller = profile.addController(controllerId);
            profile.save();
        }

        Toolbar toolbar = findViewById(R.id.Toolbar);
        toolbar.setTitle(controller.getName());
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);

        emptyTextView = findViewById(R.id.TVEmptyText);
        recyclerView = findViewById(R.id.RecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter = new ControllerBindingsAdapter());
        updateEmptyTextView();
        ThemeAssetPainter.apply(this, findViewById(android.R.id.content), isDarkMode);
    }

//    private void updateControllerBinding(int keyCode, Binding binding) {
//        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return;
//
//        ExternalControllerBinding controllerBinding = controller.getControllerBinding(keyCode);
//        int position;
//        if (controllerBinding == null) {
//            controllerBinding = new ExternalControllerBinding();
//            controllerBinding.setKeyCode(keyCode);
//            controllerBinding.setBinding(binding);
//
//            controller.addControllerBinding(controllerBinding);
//            profile.save();
//            adapter.notifyDataSetChanged();
//            updateEmptyTextView();
//            position = controller.getPosition(controllerBinding);
//        }
//        else animateItemView(position = controller.getPosition(controllerBinding));
//        recyclerView.scrollToPosition(position);
//    }

    private void updateControllerBinding(int keyCode, Binding binding) {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) return;

        ExternalControllerBinding controllerBinding = controller.getControllerBinding(keyCode);
        int position;
        if (controllerBinding == null) {
            controllerBinding = new ExternalControllerBinding();
            controllerBinding.setKeyCode(keyCode);
            controllerBinding.setBinding(binding);

            controller.addControllerBinding(controllerBinding);
            profile.save();
            adapter.notifyDataSetChanged();
            updateEmptyTextView();
            position = controller.getPosition(controllerBinding);
        } else {
            animateItemView(position = controller.getPosition(controllerBinding));
        }
        recyclerView.scrollToPosition(position);
    }


//    private void processJoystickInput() {
//        int keyCode = KeyEvent.KEYCODE_UNKNOWN;
//        Binding binding = Binding.NONE;
//        final int[] axes = {MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y};
//        final float[] values = {controller.state.thumbLX, controller.state.thumbLY, controller.state.thumbRX, controller.state.thumbRY, controller.state.getDPadX(), controller.state.getDPadY()};
//
//        byte sign;
//        for (int i = 0; i < axes.length; i++) {
//            if ((sign = Mathf.sign(values[i])) != 0) {
//                if (axes[i] == MotionEvent.AXIS_X || axes[i] == MotionEvent.AXIS_Z) {
//                    binding = sign > 0 ? Binding.MOUSE_MOVE_RIGHT : Binding.MOUSE_MOVE_LEFT;
//                }
//                else if (axes[i] == MotionEvent.AXIS_Y || axes[i] == MotionEvent.AXIS_RZ) {
//                    binding = sign > 0 ? Binding.MOUSE_MOVE_DOWN : Binding.MOUSE_MOVE_UP;
//                }
//                else if (axes[i] == MotionEvent.AXIS_HAT_X) {
//                    binding = sign > 0 ? Binding.KEY_D : Binding.KEY_A;
//                }
//                else if (axes[i] == MotionEvent.AXIS_HAT_Y) {
//                    binding = sign > 0 ? Binding.KEY_S : Binding.KEY_W;
//                }
//
//                keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], sign);
//                updateControllerBinding(keyCode, binding);
//            }
//        }
//    }

    private void processJoystickInput() {
        final int[] axes = {
                MotionEvent.AXIS_X, MotionEvent.AXIS_Y,
                MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
                MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y
        };
        final float[] values = {
                controller.state.thumbLX, controller.state.thumbLY,
                controller.state.thumbRX, controller.state.thumbRY,
                controller.state.getDPadX(), controller.state.getDPadY()
        };

        for (int i = 0; i < axes.length; i++) {
            float value = values[i];
            byte sign = Mathf.sign(value);
            if (sign != 0) {
                int keyCode = ExternalControllerBinding.getKeyCodeForAxis(axes[i], sign);
                updateControllerBinding(keyCode, Binding.NONE); // Or prompt the user to select a binding
            }
        }
    }


    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.getDeviceId() == controller.getDeviceId() && controller.updateStateFromMotionEvent(event)) {
            if (controller.state.isPressed(ExternalController.IDX_BUTTON_L2)) updateControllerBinding(KeyEvent.KEYCODE_BUTTON_L2, Binding.NONE);
            if (controller.state.isPressed(ExternalController.IDX_BUTTON_R2)) updateControllerBinding(KeyEvent.KEYCODE_BUTTON_R2, Binding.NONE);
            processJoystickInput();
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getDeviceId() == controller.getDeviceId() && event.getRepeatCount() == 0) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) updateControllerBinding(event.getKeyCode(), Binding.NONE);
            return true;
        }
        else return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        finish();
        return true;
    }

    private class ControllerBindingsAdapter extends RecyclerView.Adapter<ControllerBindingsAdapter.ViewHolder> {
        private class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageButton removeButton;
            private final TextView title;
            private final Spinner bindingType;
            private final Spinner binding;

            private ViewHolder(View view) {
                super(view);
                this.title = view.findViewById(R.id.TVTitle);
                this.bindingType = view.findViewById(R.id.SBindingType);
                this.binding = view.findViewById(R.id.SBinding);
                this.removeButton = view.findViewById(R.id.BTRemove);
            }
        }

        @Override
        public final ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.external_controller_binding_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final ExternalControllerBinding item = controller.getControllerBindingAt(position);
            int accent = ContextCompat.getColor(
                    ExternalControllerBindingsActivity.this,
                    isDarkMode ? R.color.colorAccentDark : R.color.colorAccent
            );
            holder.title.setText(item.toString());
            holder.title.setTextColor(accent);
            holder.itemView.setBackground(buildRowBackground(accent));
            loadBindingSpinner(holder, item);
            holder.removeButton.setOnClickListener((view) -> {
                controller.removeControllerBinding(item);
                profile.save();
                notifyDataSetChanged();
                updateEmptyTextView();
            });
        }

        @Override
        public final int getItemCount() {
            return controller.getControllerBindingCount();
        }

        private void loadBindingSpinner(ViewHolder holder, final ExternalControllerBinding item) {
            final Context $this = ExternalControllerBindingsActivity.this;

            Runnable update = () -> {
                String[] bindingEntries = null;
                switch (holder.bindingType.getSelectedItemPosition()) {
                    case 0:
                        bindingEntries = Binding.keyboardBindingLabels();
                        break;
                    case 1:
                        bindingEntries = Binding.mouseBindingLabels();
                        break;
                    case 2:
                        bindingEntries = Binding.gamepadBindingLabels();
                        break;
                }

                holder.binding.setAdapter(SpinnerAdapters.createGeneric($this, bindingEntries));
                AppUtils.setSpinnerSelectionFromValue(holder.binding, item.getBinding().toString());
            };

            holder.bindingType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    update.run();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            Binding selectedBinding = item.getBinding();
            if (selectedBinding.isKeyboard()) {
                holder.bindingType.setSelection(0, false);
            }
            else if (selectedBinding.isMouse()) {
                holder.bindingType.setSelection(1, false);
            }
            else if (selectedBinding.isGamepad()) {
                holder.bindingType.setSelection(2, false);
            }

            holder.binding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Binding binding = Binding.NONE;
                    switch (holder.bindingType.getSelectedItemPosition()) {
                        case 0:
                            binding = Binding.keyboardBindingValues()[position];
                            break;
                        case 1:
                            binding = Binding.mouseBindingValues()[position];
                            break;
                        case 2:
                            binding = Binding.gamepadBindingValues()[position];
                            break;
                    }

                    if (binding != item.getBinding()) {
                        item.setBinding(binding);
                        profile.save();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            update.run();
        }
    }

    private void updateEmptyTextView() {
        emptyTextView.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    private void animateItemView(int position) {
        final ControllerBindingsAdapter.ViewHolder holder = (ControllerBindingsAdapter.ViewHolder)recyclerView.findViewHolderForAdapterPosition(position);
        if (holder != null) {
            final int color = ContextCompat.getColor(this, isDarkMode ? R.color.colorAccentDark : R.color.colorAccent);
            final ValueAnimator animator = ValueAnimator.ofFloat(0.4f, 0.0f);
            animator.setDuration(200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener((animation) -> {
                float alpha = (float)animation.getAnimatedValue();
                holder.itemView.setBackgroundColor(Color.argb((int)(alpha * 255), Color.red(color), Color.green(color), Color.blue(color)));
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    holder.itemView.setBackground(buildRowBackground(color));
                }
            });
            animator.start();
        }
    }

    private GradientDrawable buildRowBackground(int accent) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(UnitUtils.dpToPx(14));
        background.setColor(withAlpha(accent, isDarkMode ? 50 : 20));
        background.setStroke(Math.round(UnitUtils.dpToPx(1)), withAlpha(accent, isDarkMode ? 220 : 130));
        return background;
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00ffffff) | (clamped << 24);
    }
}
