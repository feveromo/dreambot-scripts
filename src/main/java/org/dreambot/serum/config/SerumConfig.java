package org.dreambot.serum.config;

/**
 * Configuration settings for the Sanfew Serum script
 */
public class SerumConfig {
    private static SerumConfig instance;
    
    private boolean makeSerums = true;
    private boolean sellOnGE = false;
    private boolean cleanHerbs = true;
    
    // Singleton pattern
    private SerumConfig() {}
    
    public static SerumConfig getInstance() {
        if (instance == null) {
            instance = new SerumConfig();
        }
        return instance;
    }
    
    // Getters and setters
    public boolean shouldMakeSerums() {
        return makeSerums;
    }
    
    public void setMakeSerums(boolean makeSerums) {
        this.makeSerums = makeSerums;
    }
    
    public boolean shouldSellOnGE() {
        return sellOnGE;
    }
    
    public void setSellOnGE(boolean sellOnGE) {
        this.sellOnGE = sellOnGE;
    }
    
    public boolean shouldCleanHerbs() {
        return cleanHerbs;
    }
    
    public void setCleanHerbs(boolean cleanHerbs) {
        this.cleanHerbs = cleanHerbs;
    }
} 