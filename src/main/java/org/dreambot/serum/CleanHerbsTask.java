package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Sleep;

/**
 * CleanHerbsTask.java
 * Purpose: Handles cleaning of collected grimy snakeweed
 * Key functionality:
 * - Cleans grimy snakeweed in inventory
 * - Manages inventory state tracking
 * - Provides proper delays between actions
 * 
 * Implementation notes:
 * 1. Only accepts when grimy herbs are present
 * 2. Uses sleep conditions to verify cleaning completion
 * 3. Returns quickly to allow other tasks to execute
 */
public class CleanHerbsTask extends Task {
    // Item identifier
    private static final int GRIMY_SNAKEWEED = 1525;

    @Override
    public boolean accept() {
        return Inventory.contains(GRIMY_SNAKEWEED);
    }

    @Override
    public int execute() {
        if (Inventory.interact(GRIMY_SNAKEWEED, "Clean")) {
            Sleep.sleepUntil(() -> !Inventory.contains(GRIMY_SNAKEWEED), 2000);
        }
        return 200;
    }
} 