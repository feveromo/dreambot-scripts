package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.fairyring.FairyRings;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

/**
 * Handles all travel-related tasks including:
 * - Fairy ring teleports (GE <-> CKR)
 * - Walking to/from vine area
 * - Walking to bank
 */
public class TravelTask extends Task {
    private static final int DRAMEN_STAFF = 772;
    
    // Areas
    private static final Area VINE_AREA = new Area(
        new Tile(2757, 3014),
        new Tile(2776, 3044)
    );
    private static final Area GE_FAIRY_RING_AREA = new Area(3128, 3496, 3131, 3498);
    private static final Area CKR_FAIRY_RING_AREA = new Area(2798, 3000, 2804, 3006);
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489); // GE bank area

    private enum TravelState {
        WALK_TO_GE_RING,
        USE_RING_TO_CKR,
        USE_RING_TO_GE,
        WALK_TO_VINES,
        WALK_TO_BANK,
        FINISHED
    }

    private TravelState currentState = null;

    @Override
    public boolean accept() {
        // Accept if we need to move between areas
        if (!hasRequirements()) return false;
        
        if (Inventory.isFull()) {
            // Need to go to bank
            return !BANK_AREA.contains(Players.getLocal());
        } else {
            // Need to go to vine area
            return !VINE_AREA.contains(Players.getLocal());
        }
    }

    @Override
    public int execute() {
        if (currentState == null) {
            currentState = getInitialState();
        }

        switch (currentState) {
            case WALK_TO_GE_RING:
                if (walkToGERing()) {
                    currentState = Inventory.isFull() ? TravelState.USE_RING_TO_GE : TravelState.USE_RING_TO_CKR;
                }
                break;

            case USE_RING_TO_CKR:
                if (useFairyRing("CKR")) {
                    currentState = TravelState.WALK_TO_VINES;
                }
                break;

            case USE_RING_TO_GE:
                if (useFairyRing("DKR")) {
                    currentState = TravelState.WALK_TO_BANK;
                }
                break;

            case WALK_TO_VINES:
                if (walkToVines()) {
                    currentState = TravelState.FINISHED;
                }
                break;

            case WALK_TO_BANK:
                if (walkToBank()) {
                    currentState = TravelState.FINISHED;
                }
                break;
        }

        return 200;
    }

    private TravelState getInitialState() {
        if (BANK_AREA.contains(Players.getLocal())) {
            return TravelState.WALK_TO_GE_RING;
        }
        if (GE_FAIRY_RING_AREA.contains(Players.getLocal())) {
            return Inventory.isFull() ? TravelState.USE_RING_TO_GE : TravelState.USE_RING_TO_CKR;
        }
        if (CKR_FAIRY_RING_AREA.contains(Players.getLocal())) {
            return Inventory.isFull() ? TravelState.USE_RING_TO_GE : TravelState.WALK_TO_VINES;
        }
        if (VINE_AREA.contains(Players.getLocal()) && Inventory.isFull()) {
            return TravelState.WALK_TO_GE_RING;
        }
        return TravelState.WALK_TO_GE_RING;
    }

    private boolean hasRequirements() {
        return Inventory.contains(DRAMEN_STAFF) || Equipment.contains(DRAMEN_STAFF);
    }

    private boolean walkToGERing() {
        log("Walking to GE fairy ring...");
        Walking.walk(GE_FAIRY_RING_AREA.getCenter());
        return Sleep.sleepUntil(() -> GE_FAIRY_RING_AREA.contains(Players.getLocal()), 1000);
    }

    private boolean walkToVines() {
        log("Walking to vine area...");
        Tile destination = new Tile(2765, 3028);
        
        // If we're far from the destination
        if (destination.distance() > 10) {
            if (!Walking.shouldWalk()) return false;
            
            // Use regular walking but with better distance checks
            if (!Walking.walk(destination)) {
                log("Failed to walk to vine area!");
                return false;
            }
        } else {
            // If we're close, use regular walking
            Walking.walk(destination);
        }
        
        // Wait for movement to complete
        Sleep.sleepUntil(() -> {
            // Check if we're moving or have reached the area
            return !Players.getLocal().isMoving() || VINE_AREA.contains(Players.getLocal());
        }, 1000);
        
        // Add a small delay after movement stops
        if (!VINE_AREA.contains(Players.getLocal())) {
            sleep(300);
            return false;
        }
        
        return true;
    }

    private boolean walkToBank() {
        log("Walking to bank...");
        Walking.walk(BANK_AREA.getCenter());
        return Sleep.sleepUntil(() -> BANK_AREA.contains(Players.getLocal()), 1000);
    }

    private boolean useFairyRing(String code) {
        GameObject fairyRing = GameObjects.closest(obj -> 
            obj != null && 
            obj.getName() != null && 
            obj.getName().equals("Fairy ring") &&
            (GE_FAIRY_RING_AREA.contains(obj) || CKR_FAIRY_RING_AREA.contains(obj))
        );

        if (fairyRing != null && fairyRing.exists()) {
            if (!FairyRings.travelInterfaceOpen()) {
                if (fairyRing.interact("Configure")) {
                    Sleep.sleepUntil(FairyRings::travelInterfaceOpen, 1000);
                }
                return false;
            }

            if (FairyRings.travelInterfaceOpen()) {
                log("Using fairy ring code: " + code);
                String[] codes = code.split("");
                FairyRings.enterCode(0, codes[0]);
                sleep(100);
                FairyRings.enterCode(1, codes[1]);
                sleep(100);
                FairyRings.enterCode(2, codes[2]);
                sleep(100);
                
                if (FairyRings.travel(codes)) {
                    Sleep.sleepUntil(() -> Players.getLocal().isAnimating(), 1000);
                    if (Players.getLocal().isAnimating()) {
                        Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 2000);
                        sleep(600);
                        
                        // After teleport, force state change based on inventory
                        if (code.equals("CKR")) {
                            currentState = TravelState.WALK_TO_VINES;
                            log("Successfully teleported to CKR, moving to vines next");
                        } else {
                            currentState = TravelState.WALK_TO_BANK;
                            log("Successfully teleported to GE, moving to bank next");
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
} 