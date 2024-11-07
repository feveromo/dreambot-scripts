package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Sleep;

public class CraftSerumTask extends Task {
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