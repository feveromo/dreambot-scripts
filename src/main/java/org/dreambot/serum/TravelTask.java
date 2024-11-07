package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.fairyring.FairyRings;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

/**
 * TravelTask.java
 * Purpose: Manages all player movement between key locations for snakeweed collection
 * Key functionality:
 * - Handles fairy ring teleportation between GE and Karamja: South of Taw Bwo Wannai Village (CKR/DKR)
 * - Manages walking paths to/from banks and collection areas
 * - Controls state transitions based on inventory and location
 * 
 * Important implementation notes:
 * 1. State management is critical - states must flow linearly without loops
 * 2. Fairy ring interaction requires careful timing and interface handling
 * 3. Area checks must be precise to detect correct locations
 * 4. Sleep conditions prevent race conditions between actions
 */
public class TravelTask extends Task {
    // Constants for item and area tracking
    private static final int GRIMY_SNAKEWEED = 1525;

    // Critical: Area boundaries must fully contain the relevant objects
    private static final Area VINE_AREA = new Area(2757, 3014, 2776, 3044);
    private static final Area GE_FAIRY_RING_AREA = new Area(3128, 3496, 3131, 3498);
    private static final Area CKR_FAIRY_RING_AREA = new Area(2798, 3000, 2804, 3006);
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489);

    // States must progress linearly through the collection cycle
    private enum TravelState {
        WALK_TO_GE_RING,      // Initial state - walk to GE fairy ring
        USE_GE_RING,          // Use ring to teleport to Karamja: South of Taw Bwo Wannai Village (CKR)
        WALK_TO_VINES,        // Walk from CKR to vine collection area
        RETURN_TO_CKR,        // Walk back to CKR ring when inventory full
        USE_CKR_RING,         // Use ring to return to GE (DKR)
        WALK_TO_BANK,         // Walk to GE bank to deposit items
        FINISHED             // Task complete, ready for next cycle
    }

    // Start with walking to GE ring - no initial state needed
    private TravelState currentState = TravelState.WALK_TO_GE_RING;

    // Add script reference
    private final SanfewSerumScript script;

    public TravelTask(SanfewSerumScript script) {
        this.script = script;
    }

    /**
     * Critical: Accept logic must prevent state loops
     * Only accept new tasks when:
     * 1. Current task is finished AND
     * 2. We need to start collecting OR bank items
     */
    @Override
    public boolean accept() {
        if (currentState == TravelState.FINISHED) {
            if (Inventory.isEmpty() && !VINE_AREA.contains(Players.getLocal())) {
                log("Starting new collection run from GE");
                currentState = TravelState.WALK_TO_GE_RING;
                return true;
            }

            if (Inventory.isFull() && VINE_AREA.contains(Players.getLocal())) {
                log("Inventory full, starting return to bank");
                currentState = TravelState.RETURN_TO_CKR;
                return true;
            }
        }

        // Continue if we're mid-travel
        return currentState != TravelState.FINISHED;
    }

    @Override
    public int execute() {
        script.updateState(currentState.toString());
        
        log("Current state: " + currentState + ", Position: " + Players.getLocal().getTile());

        switch (currentState) {
            case WALK_TO_GE_RING:
                if (!GE_FAIRY_RING_AREA.contains(Players.getLocal())) {
                    log("Walking to GE fairy ring");
                    if (Walking.walk(GE_FAIRY_RING_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> GE_FAIRY_RING_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached GE fairy ring, switching to USE_GE_RING");
                currentState = TravelState.USE_GE_RING;
                return 100;

            case USE_GE_RING:
                log("Using GE fairy ring to teleport to CKR");
                if (useFairyRing("CKR")) {
                    log("Successfully teleported to CKR");
                    currentState = TravelState.WALK_TO_VINES;
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
                currentState = TravelState.FINISHED;
                return 100;

            case RETURN_TO_CKR:
                if (!CKR_FAIRY_RING_AREA.contains(Players.getLocal())) {
                    log("Walking back to CKR fairy ring");
                    if (Walking.walk(CKR_FAIRY_RING_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> CKR_FAIRY_RING_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached CKR fairy ring");
                currentState = TravelState.USE_CKR_RING;
                return 100;

            case USE_CKR_RING:
                log("Using CKR fairy ring to return to GE");
                if (useFairyRing("DKR")) {
                    log("Successfully teleported to GE");
                    currentState = TravelState.WALK_TO_BANK;
                }
                return 600;

            case WALK_TO_BANK:
                if (!BANK_AREA.contains(Players.getLocal())) {
                    log("Walking to GE bank");
                    if (Walking.walk(BANK_AREA.getCenter())) {
                        Sleep.sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), 5000);
                    }
                    return 600;
                }
                log("Reached bank");
                currentState = TravelState.FINISHED;
                return 100;
        }

        return 600;
    }

    /**
     * Critical fairy ring handling:
     * 1. Must find correct ring in current area
     * 2. Must wait for interface to open
     * 3. Must enter code with delays
     * 4. Must verify teleport completion
     */
    private boolean useFairyRing(String code) {
        // Ring detection must check both location and existence
        GameObject ring = GameObjects.closest(obj -> 
            obj != null && 
            obj.exists() &&
            obj.getName().equals("Fairy ring") &&
            (GE_FAIRY_RING_AREA.contains(obj.getTile()) || CKR_FAIRY_RING_AREA.contains(obj.getTile()))
        );
        
        if (ring == null || !ring.exists()) {
            log("No fairy ring found in area! Position: " + Players.getLocal().getTile());
            return false;
        }

        // Interface handling requires proper sleep conditions
        if (!FairyRings.travelInterfaceOpen()) {
            log("Opening fairy ring interface");
            if (ring.interact("Configure")) {
                Sleep.sleepUntil(FairyRings::travelInterfaceOpen, 3000);
            }
            return false;
        }

        // Code entry must have delays between digits
        if (FairyRings.travelInterfaceOpen()) {
            log("Interface open, entering code: " + code);
            String[] codes = code.split("");
            
            for (int i = 0; i < 3; i++) {
                FairyRings.enterCode(i, codes[i]);
                sleep(200); // Critical delay between inputs
            }
            
            if (FairyRings.travel(codes)) {
                log("Teleport initiated");
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                sleep(600); // Allow animation to complete
                return true;
            }
        }
        
        return false;
    }
} 