package com.winlator.cmod.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SocClassifierTest {
    @Test
    public void detectsAdreno730FromRenderer() {
        assertEquals(
                SocClassifier.Tier.ADRENO_7XX,
                SocClassifier.detect("Adreno (TM) 730", "", "", "", "")
        );
    }

    @Test
    public void fallsBackToSocModelForSm8475() {
        assertEquals(
                SocClassifier.Tier.ADRENO_7XX,
                SocClassifier.detect("Adreno", "SM8475", "qcom", "taro", "taro")
        );
    }

    @Test
    public void detectsAdreno650As6xx() {
        assertEquals(
                SocClassifier.Tier.ADRENO_6XX,
                SocClassifier.detect("Adreno (TM) 650", "", "", "", "")
        );
    }

    @Test
    public void detectsMaliG715() {
        assertEquals(
                SocClassifier.Tier.MALI_G7XX_OR_NEWER,
                SocClassifier.detect("Mali-G715", "", "", "", "")
        );
    }

    @Test
    public void detectsXclipseFromRenderer() {
        assertEquals(
                SocClassifier.Tier.XCLIPSE_RDNA_MOBILE,
                SocClassifier.detect("Xclipse 920", "", "", "", "")
        );
    }
}
