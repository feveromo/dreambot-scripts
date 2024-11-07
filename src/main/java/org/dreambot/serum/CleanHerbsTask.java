package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Sleep;

public class CleanHerbsTask extends Task {
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