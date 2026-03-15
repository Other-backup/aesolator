package com.winlator.cmod.box86_64.rc;

import android.content.Context;

import androidx.annotation.NonNull;

import com.winlator.cmod.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RCFile implements Comparable<RCFile> {
    public final int id;
    private String name = "";
    private final ArrayList<RCGroup> groups = new ArrayList<>();
    private final Context context;

    public RCFile(Context context, int id) {
        this.context = context;
        this.id = id;
    }

    public List<RCGroup> getGroups() {
        return groups;
    }

    public void removeGroup(RCGroup group) {
        groups.remove(group);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject profileData = new JSONObject();
        profileData.put("id", id);
        profileData.put("name", name);
        JSONArray groupsJSONArray = new JSONArray();

        for (RCGroup group : groups) {
            JSONObject groupData = new JSONObject();
            groupData.put("name", group.getGroupName());
            groupData.put("desc", group.getGroupDesc());
            groupData.put("enabled", group.isEnabled());
            JSONArray itemsJSONArray = new JSONArray();

            for (RCItem item : group.getItems()) {
                JSONObject itemData = new JSONObject();
                itemData.put("processName", item.getProcessName());
                itemData.put("desc", item.getItemDesc());
                itemData.put("vars", new JSONObject(item.getVarMap()));
                itemsJSONArray.put(itemData);
            }
            groupData.put("items", itemsJSONArray);
            groupsJSONArray.put(groupData);
        }

        profileData.put("groups", groupsJSONArray);
        return profileData;
    }

    public void save() {
        File file = getRCFile(context, id);
        try {
            FileUtils.writeString(file, toJson().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static File getRCFile(Context context, int id) {
        return new File(RCManager.getRCFilesDir(context), "box86_64rc-" + id + ".rcp");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String generateBox86_64rc() {
        TreeMap<String, TreeMap<String, String>> rcMap = new TreeMap<>();
        for (RCGroup group : groups) {
            if (!group.isEnabled()) continue;
            for (RCItem item : group.getItems()) {
                String processName = item.getProcessName();
                TreeMap<String, String> varMap = rcMap.containsKey(processName)
                        ? rcMap.get(processName)
                        : new TreeMap<>();
                rcMap.put(processName, varMap);
                varMap.putAll(item.getVarMap());
            }
        }

        StringBuilder strBuilder = new StringBuilder();
        for (Map.Entry<String, TreeMap<String, String>> entry : rcMap.entrySet()) {
            strBuilder.append('[').append(entry.getKey()).append(']').append('\n');
            for (Map.Entry<String, String> var : entry.getValue().entrySet()) {
                strBuilder.append(var.getKey()).append('=').append(var.getValue()).append('\n');
            }
            strBuilder.append('\n');
        }
        return strBuilder.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(RCFile other) {
        return Integer.compare(id, other.id);
    }
}
