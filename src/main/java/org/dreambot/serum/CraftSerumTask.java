package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Sleep;

/**
 * CraftSerumTask.java
 * Purpose: Handles creation of Sanfew serums from components
 * Key functionality:
 * - Combines ingredients in correct order
 * - Manages inventory state
 * - Controls action timing
 * 
 * Implementation notes:
 * 1. Requires all components before accepting
 * 2. Uses proper delays between actions
 * 3. Verifies successful creation
 * 4. Returns longer delay on failure
 */
public class CraftSerumTask extends Task {
    // Item identifiers for serum components
    private static final int SUPER_RESTORE_4 = 3024;
    private static final int CRUSHED_UNICORN_HORN = 235;
    private static final int NAIL_BEAST_NAILS = 4198;
    private static final int SANFEW_SERUM_4 = 10925;
    
    @Override
    public boolean accept() {
        return !Inventory.contains(SANFEW_SERUM_4) && 
               Inventory.contains(SUPER_RESTORE_4) &&
               Inventory.contains(CRUSHED_UNICORN_HORN) &&
               Inventory.contains(NAIL_BEAST_NAILS);
    }

    @Override
    public int execute() {
        if (Inventory.interact(SUPER_RESTORE_4, "Use")) {
            sleep(sleepMod());
            if (Inventory.interact(CRUSHED_UNICORN_HORN, "Use")) {
                sleep(sleepMod());
                if (Inventory.interact(NAIL_BEAST_NAILS, "Use")) {
                    Sleep.sleepUntil(() -> Inventory.contains(SANFEW_SERUM_4), 3000);
                    return 600;
                }
            }
        }
        return 1000; // Longer delay if we failed to interact
    }
} 