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
    private static final Tile BAR_DISPENSER_TILE = new Tile(1940, 4964, 0);
    private static final Area BLAST_FURNACE_AREA = new Area(1934, 4958, 1954, 4974, 0);

    // Object IDs
    private static final int CONVEYOR_BELT_ID = 9100;
    private static final int BAR_DISPENSER_ID = 9092;
    private static final int BANK_CHEST_ID = 26707;

    // Performance tracking
    private long startTime;
    private int startXP;
    private int barsMade;

    // Add these constants near the top of the class with other constants
    private static final double MAX_XP_PER_HOUR = 101250.0;
    private static final int MAX_BARS_PER_HOUR = 2700;
    private static final double XP_PER_BAR = 37.5;

    // Current prices from the provided data
    private static final int ADAMANT_BAR_PRICE = 1877;
    private static final int ADAMANTITE_ORE_PRICE = 1051;
    private static final int COAL_PRICE = 152;
    private static final int STAMINA_POT_PRICE = 9974 / 4; // Price per dose
    private static final int HOURLY_FEE = 72000;

    // Add these inner classes for stats tracking
    private static class BlastFurnaceStats {
        private final int barsPerHour;
        private final double efficiency;
        private final int profit;
        private final int totalCosts;
        private final int revenue;
        private final Costs costs;

        public BlastFurnaceStats(int barsPerHour, double efficiency, int profit, 
                               int totalCosts, int revenue, Costs costs) {
            this.barsPerHour = barsPerHour;
            this.efficiency = efficiency;
            this.profit = profit;
            this.totalCosts = totalCosts;
            this.revenue = revenue;
            this.costs = costs;
        }

        public int barsPerHour() { return barsPerHour; }
        public double efficiency() { return efficiency; }
        public int profit() { return profit; }
        public int totalCosts() { return totalCosts; }
        public int revenue() { return revenue; }
        public Costs costs() { return costs; }
    }

    private static class Costs {
        private final int adamantOre;
        private final int coal;
        private final int stamina;
        private final int fee;

        public Costs(int adamantOre, int coal, int stamina, int fee) {
            this.adamantOre = adamantOre;
            this.coal = coal;
            this.stamina = stamina;
            this.fee = fee;
        }

        public int adamantOre() { return adamantOre; }
        public int coal() { return coal; }
        public int stamina() { return stamina; }
        public int fee() { return fee; }
    }

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

        // First priority: deposit completed bars
        if (Inventory.contains(ADAMANTITE_BAR_ID)) {
            Logger.log("Depositing adamantite bars...");
            Bank.depositAll(ADAMANTITE_BAR_ID);
            barsMade += Inventory.count(ADAMANTITE_BAR_ID);
            isCoalCycle = true;
            needSecondCoal = true;
            return 600;
        }

        // Clear inventory except coal bag before handling stamina
        if (Inventory.contains(item -> item.getID() != COAL_BAG_ID)) {
            Logger.log("Depositing all items except coal bag");
            Bank.depositAllExcept(COAL_BAG_ID);
            return 600;
        }

        // Handle stamina potions after depositing items
        if (Walking.getRunEnergy() <= RUN_ENERGY_THRESHOLD) {
            Logger.log("Run energy low (" + Walking.getRunEnergy() + "%), checking for stamina potion...");
            
            // Check inventory first
            for (int potionId : STAMINA_POTION_IDS) {
                if (Inventory.contains(potionId)) {
                    Logger.log("Drinking stamina potion...");
                    Inventory.interact(potionId, "Drink");
                    Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                    return 600;
                }
            }

            // If no stamina in inventory, try to withdraw one
            for (int potionId : STAMINA_POTION_IDS) {
                if (Bank.contains(potionId)) {
                    Logger.log("Withdrawing stamina potion...");
                    Bank.withdraw(potionId, 1);
                    Sleep.sleepUntil(() -> Inventory.contains(potionId), 1200);
                    if (Inventory.interact(potionId, "Drink")) {
                        Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                        return 600;
                    }
                }
            }
        }

        // Make sure we have coal bag
        if (!Inventory.contains(COAL_BAG_ID)) {
            Logger.log("Withdrawing coal bag...");
            Bank.withdraw(COAL_BAG_ID, 1);
            return 600;
        }

        // Handle coal cycle (need 3 coal per adamant bar)
        if (isCoalCycle) {
            if (!coalBagFull) {
                // Fill coal bag first
                if (Inventory.interact(COAL_BAG_ID, "Fill")) {
                    Logger.log("Filled coal bag");
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
                    Logger.log("Filled coal bag");
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
                if (Sleep.sleepUntil(() -> !Inventory.contains(COAL_ID), 2000)) {
                    Logger.log("Coal deposited successfully");
                    coalBagFull = false;
                    
                    // If we need second coal load, go back to bank
                    if (needSecondCoal) {
                        needSecondCoal = false;
                        isCoalCycle = false;
                        state = State.WALKING_TO_BANK;
                        return 100;
                    }
                    
                    // Otherwise try to immediately collect bars
                    GameObject dispenser = GameObjects.closest("Bar dispenser");
                    if (dispenser != null && dispenser.canReach()) {
                        if (dispenser.interact("Take")) {
                            state = State.COLLECTING_BARS;
                            return 100;
                        }
                    }
                    
                    // If we couldn't interact, walk to collector
                    state = State.WALKING_TO_COLLECTOR;
                    Walking.walk(BAR_DISPENSER_TILE);
                    return 100;
                }
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
        // Create a semi-transparent black background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(5, 5, 250, 160);  // Made taller to accommodate more stats

        // Set text color to white
        g.setColor(Color.WHITE);
        
        // Draw stats
        int y = 20;
        g.drawString("DreamBot Adamant Bar Smelter", 10, y);
        y += 20;
        g.drawString("Time running: " + getRunTime(), 10, y);
        y += 20;
        
        // XP stats with commas for readability
        String xpGained = String.format("%,d", getXPGained());
        String xpPerHour = String.format("%,d", getXPPerHour());
        g.drawString("XP: " + xpGained + " (" + xpPerHour + "/hr)", 10, y);
        y += 20;

        // Calculate and display profit stats
        BlastFurnaceStats stats = calculateStats();
        g.drawString(String.format("Profit/hr: %,d gp", stats.profit()), 10, y);
        y += 20;
        g.drawString(String.format("Efficiency: %.1f%%", stats.efficiency()), 10, y);
        y += 20;
        g.drawString(String.format("Bars/hr: %,d", stats.barsPerHour()), 10, y);
        y += 20;
        
        // Add run energy display
        g.drawString("Run Energy: " + Walking.getRunEnergy() + "%", 10, y);
        y += 20;
        g.drawString("Current state: " + state.toString(), 10, y);
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

    // Add this method to calculate stats
    private BlastFurnaceStats calculateStats() {
        double currentXpPerHour = getXPPerHour();
        
        // If we just started (XP/hr is 0), return all zeros
        if (currentXpPerHour == 0) {
            return new BlastFurnaceStats(0, 0, 0, 0, 0, new Costs(0, 0, 0, 0));
        }
        
        double efficiencyRatio = currentXpPerHour / MAX_XP_PER_HOUR;
        int barsPerHour = (int) Math.floor(MAX_BARS_PER_HOUR * efficiencyRatio);
        
        // Calculate costs (3 coal per adamant bar)
        int adamantOreCost = barsPerHour * ADAMANTITE_ORE_PRICE;
        int coalCost = barsPerHour * 3 * COAL_PRICE; // 3 coal per bar
        int staminaCost = 9 * STAMINA_POT_PRICE;
        
        Costs costs = new Costs(
            adamantOreCost,
            coalCost,
            staminaCost,
            HOURLY_FEE
        );
        
        int totalCosts = adamantOreCost + coalCost + staminaCost + HOURLY_FEE;
        int revenue = barsPerHour * ADAMANT_BAR_PRICE;
        int profit = revenue - totalCosts;
        
        double efficiency = (currentXpPerHour / MAX_XP_PER_HOUR) * 100;
        
        return new BlastFurnaceStats(
            barsPerHour,
            efficiency,
            profit,
            totalCosts,
            revenue,
            costs
        );
    }
} 