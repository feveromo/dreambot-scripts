package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

/**
 * Handles only the collection of snakeweed.
 */
public class CollectSnakeweedTask extends Task {
    private static final int VINE_ID = 2575;
    private static final int SNAKEWEED_UNCLEANED = 3051;
    private static final int GRIMY_SNAKEWEED = 1525;
    
    // Area that covers the vine spawns
    private static final Area VINE_AREA = new Area(
        new Tile(2757, 3014),
        new Tile(2776, 3044)
    );

    private boolean isCollecting = false;
    private long lastVineClick = 0;
    private GameObject currentVine = null;
    private int previousInventoryCount = 0;

    @Override
    public boolean accept() {
        // Check if we're in the correct area and have space for snakeweed
        return !Inventory.isFull() && VINE_AREA.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        // Check if inventory is full of grimy snakeweed
        int currentCount = Inventory.count(GRIMY_SNAKEWEED);
        if (currentCount >= 28) {
            log("Inventory full of grimy snakeweed (" + currentCount + "), proceeding to clean...");
            return 200;
        }

        // Track actual collection by monitoring inventory changes
        if (currentCount > previousInventoryCount) {
            log("Collected snakeweed: " + currentCount);
            previousInventoryCount = currentCount;
            isCollecting = false;
            lastVineClick = 0;
            currentVine = null;
            return 200;
        }

        // If we're waiting for dialogue, just wait
        if (isCollecting) {
            if (Dialogues.canContinue()) {
                return 100;
            }
            return 100;
        }

        // If we're not collecting, find a new vine
        if (currentVine == null) {
            currentVine = GameObjects.closest(obj -> 
                obj != null &&
                obj.getID() == VINE_ID && 
                VINE_AREA.contains(obj)
            );
        }
        
        if (currentVine != null && currentVine.exists()) {
            if (currentVine.distance() > 5) {
                log("Walking closer to vine at " + currentVine.getTile().toString());
                Walking.walk(currentVine);
                Sleep.sleepUntil(() -> currentVine.distance() < 5, 1000);
                return 200;
            }

            // Prevent spam clicking
            if (System.currentTimeMillis() - lastVineClick < 1200) {
                return 100;
            }

            // Regular collection
            if (!Players.getLocal().isAnimating() && currentVine.interact("Search")) {
                lastVineClick = System.currentTimeMillis();
                isCollecting = true;
                Sleep.sleepUntil(() -> Dialogues.canContinue(), 2000);
            }
        } else {
            log("No vines found in area! Position: " + Players.getLocal().getTile().toString());
        }
        return 200;
    }
} 