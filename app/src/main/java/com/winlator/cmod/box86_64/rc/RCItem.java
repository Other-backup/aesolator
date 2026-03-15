package com.winlator.cmod.box86_64.rc;

import java.util.Map;
import java.util.TreeMap;

public class RCItem implements Comparable<RCItem> {
    private String processName;
    private String itemDesc;
    private final Map<String, String> varMap;

    public RCItem() {
        this("", "", null);
    }

    public RCItem(String name, String desc, Map<String, String> vars) {
        this.processName = name;
        this.itemDesc = desc;
        this.varMap = vars == null ? new TreeMap<>() : vars;
    }

    public static RCItem copy(RCItem item) {
        return new RCItem(item.processName, item.getItemDesc(), new TreeMap<>(item.varMap));
    }

    public Map<String, String> getVarMap() {
        return varMap;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String name) {
        this.processName = name;
    }

    public String getItemDesc() {
        return itemDesc;
    }

    public void setItemDesc(String desc) {
        this.itemDesc = desc;
    }

    @Override
    public int compareTo(RCItem other) {
        return getProcessName().compareTo(other.getProcessName());
    }
}
