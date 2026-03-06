package com.winlator.cmod.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.winlator.cmod.R;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class CPUListView extends LinearLayout {
    private static final int CPUS_PER_ROW = 4;
    private List<String> checkedCPUList;
    private final byte numProcessors;

    public CPUListView(Context context) {
        this(context, null);
    }

    public CPUListView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CPUListView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        checkedCPUList = new ArrayList<>();
        numProcessors = (byte)Runtime.getRuntime().availableProcessors();
        refreshContent();
    }

    private void refreshContent() {
        removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        setOrientation(VERTICAL);

        LinearLayout currentRow = null;
        for (int i = 0; i < numProcessors; i++) {
            if (i % CPUS_PER_ROW == 0) {
                currentRow = new LinearLayout(getContext());
                currentRow.setOrientation(HORIZONTAL);
                currentRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                addView(currentRow);
            }

            View itemView = inflater.inflate(R.layout.cpu_list_item, currentRow, false);
            String tag = "CPU" + i;
            CheckBox checkBox = itemView.findViewById(R.id.CheckBox);
            checkBox.setTag(tag);
            checkBox.setChecked(checkedCPUList == null || checkedCPUList.contains(String.valueOf(i)));
            ((TextView) itemView.findViewById(R.id.TextView)).setText(tag);
            if (currentRow != null) currentRow.addView(itemView);
        }
    }

    public void setCheckedCPUList(String checkedCPUList) {
        if (checkedCPUList == null || checkedCPUList.trim().isEmpty()) {
            this.checkedCPUList = new ArrayList<>();
        } else {
            this.checkedCPUList = new ArrayList<>(Arrays.asList(checkedCPUList.split(",")));
        }
        refreshContent();
    }

    public void setCheckedCPUList(int from, int to) {
        if (checkedCPUList == null) checkedCPUList = new ArrayList<>();
        checkedCPUList.clear();
        for (int i = from; i < to; i++) checkedCPUList.add(String.valueOf(i));
        refreshContent();
    }

    public String getCheckedCPUListAsString() {
        String cpuList = "";

        for (int i = 0; i < numProcessors; i++) {
            CheckBox checkBox = findViewWithTag("CPU"+i);
            if (checkBox.isChecked()) cpuList += (!cpuList.isEmpty() ? "," : "")+i;
        }
        return cpuList;
    }

    public boolean[] getCheckedCPUList() {
        boolean[] cpuList = new boolean[numProcessors];
        for (int i = 0; i < numProcessors; i++) {
            CheckBox checkBox = findViewWithTag("CPU"+i);
            cpuList[i] = checkBox.isChecked();
        }
        return cpuList;
    }

    public byte getNumProcessors() {
        return numProcessors;
    }
}
