package org.dreambot.blastfurnace;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Keyboard;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.awt.Color;
import java.awt.Graphics;

@ScriptManifest(
    name = "DreamBot Adamant Bar Smelter", 
    description = "Efficient adamantite bar production at Blast Furnace", 
    author = "fever",
    version = 1.0, 
    category = Category.SMITHING
)
public class AdamantBarSmelter extends AbstractScript implements PaintListener {

    // State management flags
    private State state;
    private boolean coalBagFull = false;     // Tracks if coal bag contains coal
    private boolean needSecondCoal = true;    // Indicates if we need another coal load
    private boolean isCoalCycle = true;       // True during coal loading phase, false during adamantite
    
    // Resource IDs for items used in the script
    private static final int ADAMANTITE_ORE_ID = 449;
    private static final int COAL_ID = 453;
    private static final int ADAMANTITE_BAR_ID = 2361;
    private static final int COAL_BAG_ID = 12019;
    
    // Stamina potion IDs (4 to 1 dose)
    private static final int[] STAMINA_POTION_IDS = {12625, 12627, 12629, 12631};
    private static final int RUN_ENERGY_THRESHOLD = 30;
    
    // Blast Furnace locations
    private static final Tile CONVEYOR_BELT_TILE = new Tile(1942, 4967, 0);
    private static final Tile BAR_DISPENSER_TILE = new Tile(1939, 4963, 0);
    private static final Area BLAST_FURNACE_AREA = new Area(1934, 4958, 1954, 4974, 0);

    // Performance tracking
    private long startTime;
    private int startXP;
    private int barsMade;

    /**
     * Script states representing each stage of the adamantite bar production process.
     * The script cycles between coal and adamantite phases to maintain proper ratios.
     */
    private enum State {
        BANKING,             // Managing resources and stamina potions
        DEPOSITING_ORE,     // Putting ores on conveyor belt
        COLLECTING_BARS,    // Taking completed bars from dispenser
        WALKING_TO_BANK,    // Moving to bank location
        WALKING_TO_CONVEYOR,// Moving to conveyor belt
        WALKING_TO_COLLECTOR// Moving to bar dispenser
    }

    @Override
    public void onStart() {
        Logger.log("Starting Adamant Bar Smelter");
        if (!Equipment.contains("Ice gloves")) {
            Logger.log("Please equip ice gloves!");
            stop();
            return;
        }
        state = State.BANKING;
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.SMITHING);
        barsMade = 0;
        isCoalCycle = true;
        needSecondCoal = true;
    }

    @Override
    public int onLoop() {
        switch (state) {
            case BANKING:
                return handleBanking();
            case DEPOSITING_ORE:
                return handleOreDeposit();
            case COLLECTING_BARS:
                return handleBarCollection();
            case WALKING_TO_BANK:
                return walkToBank();
            case WALKING_TO_CONVEYOR:
                return walkToConveyor();
            case WALKING_TO_COLLECTOR:
                return walkToCollector();
        }
        return 600;
    }

    /**
     * Handles all banking operations including:
     * - Depositing finished bars
     * - Managing coal bag filling
     * - Withdrawing appropriate ores based on cycle
     * - Handling stamina potions
     * @return Sleep duration in milliseconds
     */
    private int handleBanking() {
        if (!Bank.isOpen()) {
            Bank.open();
            Sleep.sleepUntil(Bank::isOpen, 5000);
            return 600;
        }

        // Handle stamina potions
        if (Walking.getRunEnergy() <= RUN_ENERGY_THRESHOLD) {
            for (int potionId : STAMINA_POTION_IDS) {
                if (Inventory.contains(potionId)) {
                    Inventory.interact(potionId, "Drink");
                    Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                    break;
                } else if (Bank.contains(potionId)) {
                    Bank.withdraw(potionId, 1);
                    Sleep.sleepUntil(() -> Inventory.contains(potionId), 1200);
                    Inventory.interact(potionId, "Drink");
                    Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                    break;
                }
            }
        }

        // Deposit bars if we have them
        if (Inventory.contains(ADAMANTITE_BAR_ID)) {
            Bank.depositAll(ADAMANTITE_BAR_ID);
            barsMade += Inventory.count(ADAMANTITE_BAR_ID);
            isCoalCycle = true;
            needSecondCoal = true;
            return 600;
        }

        // Make sure we have coal bag
        if (!Inventory.contains(COAL_BAG_ID)) {
            Bank.withdraw(COAL_BAG_ID, 1);
            return 600;
        }

        // Clear inventory except coal bag
        if (Inventory.contains(item -> item.getID() != COAL_BAG_ID)) {
            Bank.depositAllExcept(COAL_BAG_ID);
            return 600;
        }

        // Handle coal cycle (double coal deposit)
        if (isCoalCycle) {
            if (!coalBagFull) {
                // Fill coal bag first
                if (Inventory.interact(COAL_BAG_ID, "Fill")) {
                    coalBagFull = true;
                    return 600;
                }
            } else {
                // Withdraw coal for inventory
                Bank.withdraw(COAL_ID, 27);
                needSecondCoal = true;
                Bank.close();
                state = State.WALKING_TO_CONVEYOR;
                return 100;
            }
        }
        // Handle adamantite cycle
        else {
            if (!coalBagFull) {
                // Fill coal bag
                if (Inventory.interact(COAL_BAG_ID, "Fill")) {
                    coalBagFull = true;
                    return 600;
                }
            } else {
                // Withdraw adamantite ore
                Bank.withdraw(ADAMANTITE_ORE_ID, 27);
                Bank.close();
                state = State.WALKING_TO_CONVEYOR;
                return 100;
            }
        }

        return 600;
    }

    /**
     * Manages ore deposits into the conveyor belt.
     * Handles both inventory ores and coal bag contents.
     * Controls cycle transitions between coal and adamantite phases.
     * @return Sleep duration in milliseconds
     */
    private int handleOreDeposit() {
        GameObject conveyor = GameObjects.closest("Conveyor belt");
        if (conveyor == null || !conveyor.canReach()) {
            Walking.walk(CONVEYOR_BELT_TILE);
            return 600;
        }

        // Handle inventory ore first
        if (Inventory.contains(COAL_ID) || Inventory.contains(ADAMANTITE_ORE_ID)) {
            conveyor.interact("Put-ore-on");
            Sleep.sleepUntil(() -> !Inventory.contains(COAL_ID) && !Inventory.contains(ADAMANTITE_ORE_ID), 5000);
            return 600;
        }

        // Then handle coal bag if it's full
        if (coalBagFull) {
            Inventory.interact(COAL_BAG_ID, "Empty");
            Sleep.sleepUntil(() -> Inventory.contains(COAL_ID), 1200);
            if (Inventory.contains(COAL_ID)) {
                conveyor.interact("Put-ore-on");
                Sleep.sleepUntil(() -> !Inventory.contains(COAL_ID), 2000);
                coalBagFull = false;
                
                // If we need second coal load, go back to bank
                if (needSecondCoal) {
                    needSecondCoal = false;
                    isCoalCycle = false;
                    state = State.WALKING_TO_BANK;
                } else {
                    state = State.WALKING_TO_COLLECTOR;
                }
                return 100;
            }
        }

        return 600;
    }

    /**
     * Collects completed adamantite bars from the dispenser.
     * Handles dialogue interface for bar collection.
     * Updates bar count for statistics tracking.
     * @return Sleep duration in milliseconds
     */
    private int handleBarCollection() {
        if (Inventory.contains(ADAMANTITE_BAR_ID)) {
            state = State.WALKING_TO_BANK;
            return 100;
        }

        GameObject dispenser = GameObjects.closest("Bar dispenser");
        if (dispenser != null && dispenser.interact("Take")) {
            Sleep.sleepUntil(() -> Dialogues.inDialogue(), 5000);
            if (Dialogues.inDialogue()) {
                Keyboard.type("1");
                Sleep.sleepUntil(() -> Inventory.contains(ADAMANTITE_BAR_ID), 5000);
                if (Inventory.contains(ADAMANTITE_BAR_ID)) {
                    state = State.WALKING_TO_BANK;
                }
            }
        }
        return 600;
    }

    /**
     * Handles walking to bank location and opening bank interface.
     * Includes proper distance checking and timeout handling.
     * @return Sleep duration in milliseconds
     */
    private int walkToBank() {
        if (Bank.isOpen()) {
            state = State.BANKING;
            return 600;
        }

        if (Walking.shouldWalk()) {
            Walking.walk(Bank.getClosestBankLocation());
        }

        if (Bank.open()) {
            Sleep.sleepUntil(Bank::isOpen, 5000);
            if (Bank.isOpen()) {
                state = State.BANKING;
            }
        }

        return 600;
    }

    /**
     * Controls movement to conveyor belt for ore deposits.
     * Verifies proper positioning before allowing deposits.
     * @return Sleep duration in milliseconds
     */
    private int walkToConveyor() {
        if (Walking.shouldWalk()) {
            Walking.walk(CONVEYOR_BELT_TILE);
        }
        GameObject conveyor = GameObjects.closest("Conveyor belt");
        if (conveyor != null && conveyor.canReach()) {
            state = State.DEPOSITING_ORE;
        }
        return 600;
    }

    /**
     * Manages walking to bar dispenser for collection.
     * Ensures proper positioning for bar collection.
     * @return Sleep duration in milliseconds
     */
    private int walkToCollector() {
        if (Walking.shouldWalk()) {
            Walking.walk(BAR_DISPENSER_TILE);
        }
        GameObject dispenser = GameObjects.closest("Bar dispenser");
        if (dispenser != null && dispenser.canReach()) {
            state = State.COLLECTING_BARS;
        }
        return 600;
    }

    /**
     * Renders script statistics overlay including:
     * - Runtime duration
     * - XP gained and per hour rate
     * - Bars produced and per hour rate
     * - Current script state
     */
    @Override
    public void onPaint(Graphics g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(5, 5, 250, 100);  

        g.setColor(Color.WHITE);
        int y = 20;
        g.drawString("DreamBot Adamant Bar Smelter", 10, y);
        y += 20;
        g.drawString("Time running: " + getRunTime(), 10, y);
        y += 20;
        g.drawString("XP: " + getXPGained() + " (" + getXPPerHour() + "/hr)", 10, y);
        y += 20;
        g.drawString("Bars made: " + barsMade + " (" + getBarsPerHour() + "/hr)", 10, y);
        y += 20;
        g.drawString("State: " + state.toString(), 10, y);
    }

    /**
     * Calculates and formats runtime in HH:MM:SS format
     * @return Formatted runtime string
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
     * Calculates total Smithing XP gained since script start
     * @return Total XP gained
     */
    private int getXPGained() {
        return Skills.getExperience(Skill.SMITHING) - startXP;
    }

    /**
     * Calculates XP gained per hour based on runtime
     * @return XP per hour rate
     */
    private int getXPPerHour() {
        double timeRan = (System.currentTimeMillis() - startTime) / 3600000.0;
        return (int) (getXPGained() / timeRan);
    }

    /**
     * Calculates bars produced per hour based on runtime
     * @return Bars per hour rate
     */
    private int getBarsPerHour() {
        double timeRan = (System.currentTimeMillis() - startTime) / 3600000.0;
        return (int) (barsMade / timeRan);
    }
} 