package com.winlator.cmod.xserver;

import androidx.collection.ArraySet;

import java.util.Iterator;

public class ResourceIDs {
    public static final int INVALID_ID_BASE = -1;
    private final ArraySet<Integer> idBases = new ArraySet<>();
    private final ArraySet<Integer> allocatedBases = new ArraySet<>();
    private final ArraySet<Integer> validBases = new ArraySet<>();
    public final int idMask;
    public final int maxClients;

    public ResourceIDs(int maxClients) {
        this.maxClients = maxClients;
        int clientsBits = 32 - Integer.numberOfLeadingZeros(maxClients);
        clientsBits = Integer.bitCount(maxClients) == 1 ? clientsBits - 1 : clientsBits;
        int base = 29 - clientsBits;
        idMask = (1 << base) - 1;
        for (int i = 1; i < maxClients; i++) {
            int idBase = i << base;
            idBases.add(idBase);
            validBases.add(idBase);
        }
    }

    public synchronized Integer get() {
        if (idBases.isEmpty()) return INVALID_ID_BASE;
        Iterator<Integer> iter = idBases.iterator();
        int idBase = iter.next();
        iter.remove();
        allocatedBases.add(idBase);
        return idBase;
    }

    public boolean isInInterval(int value, int idBase) {
        if (!isValidBase(idBase)) return false;
        return (value | idMask) == (idBase | idMask);
    }

    public synchronized boolean free(Integer idBase) {
        if (idBase == null || !validBases.contains(idBase)) return false;
        if (!allocatedBases.remove(idBase)) return false;
        idBases.add(idBase);
        return true;
    }

    public synchronized boolean isValidBase(Integer idBase) {
        return idBase != null && validBases.contains(idBase);
    }

    public synchronized int availableCount() {
        return idBases.size();
    }

    public synchronized int allocatedCount() {
        return allocatedBases.size();
    }
}
