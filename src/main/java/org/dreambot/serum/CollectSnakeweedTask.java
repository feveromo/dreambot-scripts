package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

/**
 * CollectSnakeweedTask.java
 * Purpose: Handles snakeweed collection from marshy vines in Karamja
 * Key functionality:
 * - Locates and interacts with marshy jungle vines
 * - Manages dialogue interactions during collection
 * - Tracks inventory status and collection progress
 * - Updates script statistics
 * 
 * Implementation notes:
 * 1. Vine detection requires both ID and name checks
 * 2. Interaction cooldown prevents spam clicking
 * 3. Camera rotation helps when vines aren't visible
 * 4. Distance checks ensure proper pathing
 */
public class CollectSnakeweedTask extends Task {
    private static final int VINE_ID = 21941;
    private static final int GRIMY_SNAKEWEED = 1525;
    
    private static final Area VINE_AREA = new Area(
        new Tile(2757, 3014),
        new Tile(2776, 3044)
    );

    private int previousInventoryCount = 0;
    private long lastInteractionTime = 0;
    private static final int INTERACTION_COOLDOWN = 1200;
    
    // Reference to main script for tracking
    private final SanfewSerumScript script;

    public CollectSnakeweedTask(SanfewSerumScript script) {
        this.script = script;
    }

    @Override
    public boolean accept() {
        return !Inventory.isFull() && VINE_AREA.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        script.updateState("Collecting Snakeweed");
        
        // Handle dialogue first if it exists
        if (Dialogues.canContinue()) {
            Dialogues.continueDialogue();
            return 100;
        }

        // Check if we collected something
        int currentCount = Inventory.count(GRIMY_SNAKEWEED);
        if (currentCount > previousInventoryCount) {
            previousInventoryCount = currentCount;
            script.incrementHerbsCollected(); // Update counter through script reference
            sleep(300);
            return 100;
        }

        // Respect click cooldown
        if (System.currentTimeMillis() - lastInteractionTime < INTERACTION_COOLDOWN) {
            return 100;
        }

        // Find and interact with nearest vine
        GameObject vine = GameObjects.closest(obj -> 
            obj != null && 
            obj.exists() &&
            VINE_AREA.contains(obj) &&
            (obj.getName().equals("Marshy jungle vine") || obj.getID() == VINE_ID) &&
            obj.hasAction("Search")
        );

        if (vine != null && vine.exists()) {
            if (vine.distance() > 4) {
                Walking.walk(vine);
                return 600;
            }

            if (!Players.getLocal().isAnimating() && vine.interact("Search")) {
                lastInteractionTime = System.currentTimeMillis();
                Sleep.sleepUntil(() -> Dialogues.canContinue(), 2000);
            }
        } else {
            // If no vine found, rotate camera
            Camera.rotateToTile(new Tile(2765, 3028, 0));
        }

        return 200;
    }
} 