package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.serum.config.SerumConfig;

/**
 * Handles only the collection of snakeweed using tick manipulation.
 */
public class CollectSnakeweedTask extends Task {
    private static final String VINE_NAME = "Marshy jungle vines";
    private static final int VINE_ID = 2575;
    private static final int SWAMP_TAR = 1939;
    private static final int GUAM_LEAF = 249;
    private static final int SNAKEWEED_UNCLEANED = 3051;
    
    // Area that covers the vine spawns
    private static final Area VINE_AREA = new Area(
        new Tile(2757, 3014),
        new Tile(2776, 3044)
    );

    // Area that covers the path from CKR to vines
    private static final Area PATH_AREA = new Area(
        new Tile(2801, 3005), // CKR fairy ring
        new Tile(2778, 3018),
        new Tile(2778, 3033),
        new Tile(2757, 3014)  // Connect to vine area
    );

    @Override
    public boolean accept() {
        // Check if we have required items
        if (!hasRequiredItems()) {
            log("Missing required items for vine collection!");
            return false;
        }

        // Check if we're in the correct area (either vine area or on the path to vines)
        if (!isInCollectionArea()) {
            log("Not in collection area!");
            return false;
        }

        // Check if we have space and don't already have snakeweed
        return !Inventory.isFull() && !Inventory.contains(SNAKEWEED_UNCLEANED);
    }

    private boolean hasRequiredItems() {
        return Inventory.contains(SWAMP_TAR) && 
               Inventory.contains(GUAM_LEAF);
    }

    private boolean isInCollectionArea() {
        Tile playerTile = Players.getLocal().getTile();
        return VINE_AREA.contains(playerTile) || PATH_AREA.contains(playerTile);
    }

    @Override
    public int execute() {
        // If we're in the path area but not at vines yet, let TravelTask handle it
        if (PATH_AREA.contains(Players.getLocal()) && !VINE_AREA.contains(Players.getLocal())) {
            return 100;
        }

        GameObject vine = GameObjects.closest(obj -> 
            obj != null &&
            obj.getID() == VINE_ID && 
            VINE_AREA.contains(obj)
        );
        
        if (vine != null && vine.exists()) {
            if (vine.distance() > 5) {
                log("Walking closer to vine at " + vine.getTile().toString());
                Walking.walk(vine);
                Sleep.sleepUntil(() -> vine.distance() < 5, 1000);
                return 200;
            }

            // Only perform tick manipulation if enabled in config
            if (SerumConfig.getInstance().shouldUseTickManipulation()) {
                if (Inventory.interact(SWAMP_TAR, "Use")) {
                    sleep(100);
                    if (Inventory.interact(GUAM_LEAF, "Use")) {
                        sleep(100);
                        if (vine.interact("Search")) {
                            Sleep.sleepUntil(() -> Players.getLocal().isAnimating() 
                                || Inventory.contains(SNAKEWEED_UNCLEANED), 1000);
                        }
                    }
                }
            } else {
                // Regular collection without tick manipulation
                if (vine.interact("Search")) {
                    Sleep.sleepUntil(() -> Players.getLocal().isAnimating() 
                        || Inventory.contains(SNAKEWEED_UNCLEANED), 1000);
                }
            }
        } else {
            log("No vines found in area! Position: " + Players.getLocal().getTile().toString());
        }
        return 200;
    }
} 