package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;

public class CleanHerbsTask extends Task {
    private static final int SNAKEWEED_UNCLEANED = 3051;  // Correct ID for uncleaned snakeweed
    private static final int SNAKEWEED_CLEANED = 3052;    // Correct ID for cleaned snakeweed
    
    @Override
    public boolean accept() {
        return Inventory.contains(SNAKEWEED_UNCLEANED) &&
               !Inventory.contains(SNAKEWEED_CLEANED);
    }

    @Override
    public int execute() {
        if (Inventory.interact(SNAKEWEED_UNCLEANED, "Clean")) {
            sleep(sleepMod());
        }
        return 600;
    }
} 