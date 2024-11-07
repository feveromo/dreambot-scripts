package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
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
 */
public class TravelTask extends Task {
    // Areas
    private static final Area VINE_AREA = new Area(
        new Tile(2757, 3014),
        new Tile(2776, 3044)
    );
    private static final Area GE_FAIRY_RING_AREA = new Area(3128, 3496, 3131, 3498);
    private static final Area CKR_FAIRY_RING_AREA = new Area(2798, 3000, 2804, 3006);
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489); // GE bank area

    private enum TravelState {
        INITIAL_SETUP,
        WALK_TO_GE_RING,
        USE_RING_TO_CKR,
        WALK_TO_VINES,
        USE_RING_TO_GE,
        WALK_TO_BANK,
        FINISHED
    }

    private TravelState currentState = TravelState.INITIAL_SETUP;

    @Override
    public boolean accept() {
        // If we're at vines with empty inventory, don't accept
        if (VINE_AREA.contains(Players.getLocal()) && !Inventory.isFull()) {
            return false;
        }

        // If inventory is full and we're not at bank, accept to handle return
        if (Inventory.isFull() && !BANK_AREA.contains(Players.getLocal())) {
            return true;
        }

        // Accept if we're not at vines and inventory isn't full
        return !VINE_AREA.contains(Players.getLocal()) && !Inventory.isFull();
    }

    @Override
    public int execute() {
        // Debug logging
        log("Current state: " + currentState);
        log("Current position: " + Players.getLocal().getTile().toString());

        // Handle initial setup based on current location
        if (currentState == TravelState.INITIAL_SETUP) {
            if (VINE_AREA.contains(Players.getLocal())) {
                if (Inventory.isFull()) {
                    currentState = TravelState.USE_RING_TO_GE;
                } else {
                    currentState = TravelState.FINISHED;
                }
            } else if (CKR_FAIRY_RING_AREA.contains(Players.getLocal())) {
                log("Starting at CKR fairy ring, walking to vines...");
                currentState = TravelState.WALK_TO_VINES;
                return 100;
            } else if (GE_FAIRY_RING_AREA.contains(Players.getLocal())) {
                currentState = TravelState.USE_RING_TO_CKR;
            } else if (BANK_AREA.contains(Players.getLocal())) {
                currentState = TravelState.WALK_TO_GE_RING;
            } else {
                // If we're near CKR area but not exactly in it
                Tile playerTile = Players.getLocal().getTile();
                if (playerTile.getX() >= 2780 && playerTile.getX() <= 2810 &&
                    playerTile.getY() >= 2990 && playerTile.getY() <= 3010) {
                    log("Near CKR area, walking to vines...");
                    currentState = TravelState.WALK_TO_VINES;
                } else {
                    currentState = TravelState.WALK_TO_GE_RING;
                }
            }
            return 100;
        }

        // Handle states
        switch (currentState) {
            case WALK_TO_GE_RING:
                if (walkToGERing()) {
                    currentState = TravelState.USE_RING_TO_CKR;
                }
                break;

            case USE_RING_TO_CKR:
                if (useFairyRing("CKR")) {
                    currentState = TravelState.WALK_TO_VINES;
                }
                break;

            case WALK_TO_VINES:
                if (walkToVines()) {
                    currentState = TravelState.FINISHED;
                }
                break;

            case USE_RING_TO_GE:
                if (useFairyRing("DKR")) {
                    currentState = TravelState.WALK_TO_BANK;
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

    private boolean walkToGERing() {
        if (!Walking.walk(GE_FAIRY_RING_AREA.getCenter())) {
            log("Failed to generate path to fairy ring!");
            return false;
        }
        return Sleep.sleepUntil(() -> GE_FAIRY_RING_AREA.contains(Players.getLocal()), 1000);
    }

    private boolean walkToVines() {
        log("Walking to vine area...");
        Walking.walk(new Tile(2765, 3028));
        return Sleep.sleepUntil(() -> VINE_AREA.contains(Players.getLocal()), 1000);
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
                        return true;
                    }
                }
            }
        }
        return false;
    }
} 