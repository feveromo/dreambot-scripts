package org.dreambot.collector;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.fairyring.FairyRings;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.awt.Color;
import java.awt.Graphics;

/**
 * SnakeweedCollector.java
 * Purpose: Automates the collection of snakeweed using fairy rings and GE banking
 * Key functionality:
 * - Handles fairy ring transportation between GE and Karamja
 * - Manages vine detection and collection
 * - Controls banking operations
 * - Tracks collection statistics
 * 
 * Implementation notes:
 * 1. State-based design ensures clear progression
 * 2. Proper sleep conditions prevent race conditions
 * 3. Area checks confirm correct positioning
 * 4. Interaction cooldowns prevent spam clicking
 */
@ScriptManifest(
    name = "Snakeweed Collector",
    description = "Collects snakeweed using fairy rings",
    author = "Your Name",
    version = 1.0,
    category = Category.MONEYMAKING
)
public class SnakeweedCollector extends AbstractScript implements PaintListener {
    // Game object and item identifiers
    private static final int VINE_ID = 21941;          // Marshy jungle vine ID
    private static final int GRIMY_SNAKEWEED = 1525;   // Collected herb ID
    private static final int PICKING_ANIMATION = 2094; // Picking animation ID
    
    // Critical: Area boundaries must fully contain relevant objects
    private static final Area VINE_AREA = new Area(2757, 3014, 2776, 3044);
    private static final Area GE_FAIRY_RING_AREA = new Area(3128, 3496, 3131, 3498);
    private static final Area CKR_FAIRY_RING_AREA = new Area(2798, 3000, 2804, 3006);
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489);

    // State and statistics tracking
    private State currentState = State.WALK_TO_GE_RING;
    private int herbsCollected = 0;
    private long startTime;
    private long lastInteractionTime = 0;
    private static final int INTERACTION_COOLDOWN = 1200; // Minimum ms between interactions
    private int previousInventoryCount = 0;

    /**
     * States represent each stage of the collection process
     * Must progress linearly through states to maintain script flow
     */
    private enum State {
        WALK_TO_GE_RING,      // Initial walking to GE fairy ring
        USE_GE_RING,          // Using ring to teleport to Karamja
        WALK_TO_VINES,        // Walking to vine collection area
        COLLECT_HERBS,        // Collecting snakeweed from vines
        RETURN_TO_CKR,        // Walking back to CKR fairy ring
        USE_CKR_RING,         // Using ring to return to GE
        WALK_TO_BANK,         // Walking to GE bank
        BANKING              // Depositing collected herbs
    }

    /**
     * Initializes script tracking variables
     * Called once when script starts
     */
    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        log("Starting Snakeweed Collector...");
    }

    /**
     * Main script loop handling state transitions and actions
     * Returns sleep time between iterations
     */
    @Override
    public int onLoop() {
        switch (currentState) {
            case WALK_TO_GE_RING:
                if (!GE_FAIRY_RING_AREA.contains(Players.getLocal())) {
                    log("Walking to GE fairy ring");
                    if (Walking.walk(GE_FAIRY_RING_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> GE_FAIRY_RING_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached GE fairy ring");
                currentState = State.USE_GE_RING;
                return 100;

            case USE_GE_RING:
                if (useFairyRing("CKR")) {
                    log("Successfully teleported to CKR");
                    currentState = State.WALK_TO_VINES;
                }
                return 600;

            case WALK_TO_VINES:
                if (!VINE_AREA.contains(Players.getLocal())) {
                    log("Walking to vine area");
                    if (Walking.walk(VINE_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> VINE_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached vine area");
                currentState = State.COLLECT_HERBS;
                return 100;

            case COLLECT_HERBS:
                if (Inventory.isFull()) {
                    currentState = State.RETURN_TO_CKR;
                    return 100;
                }
                return collectHerbs();

            case RETURN_TO_CKR:
                if (!CKR_FAIRY_RING_AREA.contains(Players.getLocal())) {
                    log("Walking to CKR fairy ring");
                    if (Walking.walk(CKR_FAIRY_RING_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> CKR_FAIRY_RING_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached CKR fairy ring");
                currentState = State.USE_CKR_RING;
                return 100;

            case USE_CKR_RING:
                if (useFairyRing("DKR")) {
                    log("Successfully teleported to GE");
                    currentState = State.WALK_TO_BANK;
                }
                return 600;

            case WALK_TO_BANK:
                if (!BANK_AREA.contains(Players.getLocal())) {
                    log("Walking to bank");
                    if (Walking.walk(BANK_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached bank");
                currentState = State.BANKING;
                return 100;

            case BANKING:
                if (handleBanking()) {
                    currentState = State.WALK_TO_GE_RING;
                }
                return 600;
        }
        return 600;
    }

    /**
     * Handles herb collection from vines
     * Includes dialogue handling and interaction timing
     * Tracks successful collections through inventory changes
     * 
     * @return Sleep duration in milliseconds
     */
    private int collectHerbs() {
        // Check if we collected something by comparing counts
        int currentCount = Inventory.count(GRIMY_SNAKEWEED);
        if (currentCount > previousInventoryCount) {
            herbsCollected++;
            log("Herb collected! Total: " + herbsCollected);
            previousInventoryCount = currentCount;
            sleep(300);
            return 100;
        }

        // Handle dialogue
        if (Dialogues.canContinue()) {
            Dialogues.continueDialogue();
            return 100;
        }

        // Respect interaction cooldown
        if (System.currentTimeMillis() - lastInteractionTime < INTERACTION_COOLDOWN) {
            return 100;
        }

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
            Camera.rotateToTile(new Tile(2765, 3028, 0));
        }

        return 200;
    }

    /**
     * Manages banking operations
     * Opens bank, deposits herbs, and closes interface
     * 
     * @return true if banking completed successfully
     */
    private boolean handleBanking() {
        if (!Bank.isOpen()) {
            Bank.open();
            Sleep.sleepUntil(Bank::isOpen, 3000);
            return false;
        }

        if (Bank.depositAll(GRIMY_SNAKEWEED)) {
            Sleep.sleepUntil(() -> !Inventory.contains(GRIMY_SNAKEWEED), 2000);
            Bank.close();
            return true;
        }

        return false;
    }

    /**
     * Handles fairy ring transportation
     * Critical: Must handle interface properly and verify teleport
     * 
     * @param code Fairy ring code (e.g., "CKR", "DKR")
     * @return true if teleport completed successfully
     */
    private boolean useFairyRing(String code) {
        GameObject ring = GameObjects.closest("Fairy ring");
        
        if (ring == null || !ring.exists()) {
            log("No fairy ring found!");
            return false;
        }

        if (!FairyRings.travelInterfaceOpen()) {
            log("Opening fairy ring interface");
            if (ring.interact("Configure")) {
                Sleep.sleepUntil(FairyRings::travelInterfaceOpen, 3000);
            }
            return false;
        }

        if (FairyRings.travelInterfaceOpen()) {
            log("Entering code: " + code);
            String[] codes = code.split("");
            FairyRings.enterCode(0, codes[0]);
            sleep(200);
            FairyRings.enterCode(1, codes[1]);
            sleep(200);
            FairyRings.enterCode(2, codes[2]);
            sleep(200);
            
            if (FairyRings.travel(codes)) {
                log("Travel initiated");
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                sleep(600);
                return true;
            }
        }
        return false;
    }

    /**
     * Renders script statistics overlay
     * Shows runtime, herbs collected, and current state
     */
    @Override
    public void onPaint(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(5, 5, 210, 90);

        g.setColor(Color.WHITE);
        int y = 20;
        g.drawString("Snakeweed Collector", 10, y);
        y += 20;
        g.drawString("Runtime: " + getRunTime(), 10, y);
        y += 20;
        g.drawString("Herbs: " + herbsCollected + " (" + getPerHour(herbsCollected) + "/hr)", 10, y);
        y += 20;
        g.drawString("State: " + currentState, 10, y);
    }

    /**
     * Formats runtime into HH:MM:SS format
     * Used for paint display
     */
    private String getRunTime() {
        long milliseconds = System.currentTimeMillis() - startTime;
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes %= 60;
        seconds %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Calculates per hour rate for any value
     * Used for herbs collected per hour calculation
     * 
     * @param value Current total value
     * @return Calculated per hour rate
     */
    private int getPerHour(int value) {
        double timeRan = (System.currentTimeMillis() - startTime) / 3600000.0;
        return (int) (value / timeRan);
    }
} 