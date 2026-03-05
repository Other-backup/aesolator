package com.winlator.cmod.graphics;

public class DriverProbeResult {
    private final String requestedDriverId;
    private String selectedDriverId;
    private boolean found;
    private boolean usingSystemVulkan;
    private String driverPath = "";
    private String libraryName = "";
    private String rejectReason = "";

    public DriverProbeResult(String requestedDriverId) {
        this.requestedDriverId = requestedDriverId == null ? "" : requestedDriverId;
        this.selectedDriverId = this.requestedDriverId;
    }

    public String getRequestedDriverId() {
        return requestedDriverId;
    }

    public String getSelectedDriverId() {
        return selectedDriverId;
    }

    public void setSelectedDriverId(String selectedDriverId) {
        this.selectedDriverId = selectedDriverId == null ? "" : selectedDriverId;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public boolean isUsingSystemVulkan() {
        return usingSystemVulkan;
    }

    public void setUsingSystemVulkan(boolean usingSystemVulkan) {
        this.usingSystemVulkan = usingSystemVulkan;
    }

    public String getDriverPath() {
        return driverPath;
    }

    public void setDriverPath(String driverPath) {
        this.driverPath = driverPath == null ? "" : driverPath;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName == null ? "" : libraryName;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void reject(String reason) {
        this.rejectReason = reason == null ? "" : reason;
        this.found = false;
        this.usingSystemVulkan = false;
    }

    public boolean isUsable() {
        if (!found) return false;
        if (usingSystemVulkan) return true;
        return !driverPath.isEmpty() && !libraryName.isEmpty();
    }
}
