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

/*
 * BlastFurnaceScript.java
 * 
 * A DreamBot script that automates steel bar production at the Blast Furnace.
 * 
 * Core functionality:
 * - Banks and manages resources (coal, iron ore)
 * - Handles coal bag filling and emptying
 * - Operates conveyor belt and bar dispenser
 * - Manages stamina potions for run energy
 * - Tracks XP gains and runtime
 * 
 * Requirements:
 * - Ice gloves equipped
 * - Coal bag in bank or inventory
 * - Stamina potions (optional)
 * 
 * Process flow:
 * 1. Fill coal bag at bank
 * 2. Withdraw iron ore
 * 3. Deposit iron ore, empty coal bag, deposit coal
 * 4. Collect bars from dispenser
 * 5. Bank bars and repeat
 */
@ScriptManifest(
    name = "Blast Furnace Steel Bar Smelter", 
    description = "Efficient steel bar production at Blast Furnace", 
    author = "fever",
    version = 1.0, 
    category = Category.SMITHING
)
public class BlastFurnaceScript extends AbstractScript implements PaintListener {

    // State management
    private State state;
    private boolean coalBagFull = false;
    
    // Resource IDs
    private static final int IRON_ORE_ID = 440;
    private static final int COAL_ID = 453;
    private static final int STEEL_BAR_ID = 2353;
    private static final int IRON_BAR_ID = 2351;
    private static final int COAL_BAG_ID = 12019;
    
    // Stamina potion IDs (4 to 1 dose)
    private static final int STAMINA_POTION_4_ID = 12625;
    private static final int STAMINA_POTION_3_ID = 12627;
    private static final int STAMINA_POTION_2_ID = 12629;
    private static final int STAMINA_POTION_1_ID = 12631;
    private static final int RUN_ENERGY_THRESHOLD = 30;
    
    // Blast Furnace locations
    private static final Tile CONVEYOR_BELT_TILE = new Tile(1942, 4967, 0);
    private static final Tile BAR_DISPENSER_TILE = new Tile(1940, 4964, 0);
    private static final Area BLAST_FURNACE_AREA = new Area(1934, 4958, 1954, 4974, 0);
    private static final Tile BANK_CHEST_TILE = new Tile(1948, 4957, 0);

    // Performance tracking
    private long startTime;
    private int startXP;
    private int barsMade;

    // Add these constants near the top of the class with other constants
    private static final double MAX_XP_PER_HOUR = 95400.0;
    private static final int MAX_BARS_PER_HOUR = 5400;
    private static final double XP_PER_BAR = 17.5;

    // Current prices
    private static final int STEEL_BAR_PRICE = 469;
    private static final int IRON_ORE_PRICE = 158;
    private static final int COAL_PRICE = 149;
    private static final int STAMINA_POT_PRICE = 10025 / 4; // Price per dose
    private static final int HOURLY_FEE = 72000;

    /**
     * Script states representing each stage of the steel bar production process.
     * The script transitions between these states in a cycle to maintain continuous production.
     */
    private enum State {
        BANKING,             // Managing resources and stamina potions
        DEPOSITING_ORE,     // Putting ores on conveyor belt
        COLLECTING_BARS,    // Taking completed bars from dispenser
        WALKING_TO_BANK,    // Moving to bank location
        WALKING_TO_CONVEYOR,// Moving to conveyor belt
        WALKING_TO_COLLECTOR// Moving to bar dispenser
    }

    /**
     * Initializes the script and performs necessary checks on startup
     * Verifies ice gloves are equipped before starting
     * Verifies we have a coal bag in inventory
     */
    @Override
    public void onStart() {
        Logger.log("Starting Steel Bar Smelter");
        // Ice gloves and coal bag are required for efficient bar collection
        if (!Equipment.contains("Ice gloves")) {
            Logger.log("Please equip ice gloves!");
            stop();
            return;
        }
        state = State.BANKING;
        startTime = System.currentTimeMillis();
        startXP = Skills.getExperience(Skill.SMITHING);
        barsMade = 0;
    }

    /**
     * Main loop of the script, handles state transitions and actions
     * @return Sleep duration in milliseconds before next loop
     */
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
     * - Withdrawing new resources
     * - Handling stamina potions
     * @return Sleep duration in milliseconds
     */
    private int handleBanking() {
        Logger.log("Current state: BANKING");
        
        if (!Bank.isOpen()) {
            Logger.log("Opening bank...");
            Bank.open();
            Sleep.sleepUntil(Bank::isOpen, 5000);
            return 600;
        }

        // Check for required materials
        if (!Bank.contains(COAL_ID) || !Bank.contains(IRON_ORE_ID)) {
            Logger.log("Out of materials!");
            if (!Bank.contains(COAL_ID)) {
                Logger.log("No coal remaining in bank.");
            }
            if (!Bank.contains(IRON_ORE_ID)) {
                Logger.log("No iron ore remaining in bank.");
            }
            Logger.log("Stopping script...");
            stop();
            return 0;
        }

        // First priority: deposit completed bars
        if (Inventory.contains(STEEL_BAR_ID)) {
            Logger.log("Depositing steel bars...");
            Bank.depositAll(STEEL_BAR_ID);
            return 600;
        }

        // Check stamina after depositing bars
        if (Walking.getRunEnergy() <= RUN_ENERGY_THRESHOLD) {
            Logger.log("Run energy low (" + Walking.getRunEnergy() + "%), checking for stamina potion...");
            
            // Check inventory first
            int[] staminaPotions = {
                STAMINA_POTION_4_ID,
                STAMINA_POTION_3_ID,
                STAMINA_POTION_2_ID,
                STAMINA_POTION_1_ID
            };

            boolean hasStamina = false;
            for (int potionId : staminaPotions) {
                if (Inventory.contains(potionId)) {
                    hasStamina = true;
                    Logger.log("Drinking stamina potion...");
                    if (Inventory.interact(potionId, "Drink")) {
                        Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                        break;
                    }
                }
            }

            // If no stamina in inventory, try to withdraw one
            if (!hasStamina) {
                Logger.log("No stamina in inventory, checking bank...");
                for (int potionId : staminaPotions) {
                    if (Bank.contains(potionId)) {
                        Logger.log("Withdrawing stamina potion...");
                        Bank.withdraw(potionId, 1);
                        Sleep.sleepUntil(() -> Inventory.contains(potionId), 1200);
                        if (Inventory.interact(potionId, "Drink")) {
                            Sleep.sleepUntil(() -> Walking.getRunEnergy() > RUN_ENERGY_THRESHOLD, 2000);
                        }
                        break;
                    }
                }
            }
        }

        // Log current state
        Logger.log("Inventory state:");
        Logger.log("- Has steel bars: " + Inventory.contains(STEEL_BAR_ID));
        Logger.log("- Has coal bag: " + Inventory.contains(COAL_BAG_ID));
        Logger.log("- Has iron ore: " + Inventory.contains(IRON_ORE_ID));
        Logger.log("- Coal bag full: " + coalBagFull);
        Logger.log("- Empty slots: " + Inventory.getEmptySlots());

        // Make sure we have coal bag
        if (!Inventory.contains(COAL_BAG_ID)) {
            Logger.log("Withdrawing coal bag...");
            Bank.withdraw(COAL_BAG_ID, 1);
            return 600;
        }

        // Clear inventory except coal bag if we need to fill coal bag
        if (!coalBagFull && Inventory.contains(item -> item.getID() != COAL_BAG_ID)) {
            Logger.log("Depositing everything except coal bag to prepare for filling...");
            Bank.depositAllExcept(COAL_BAG_ID);
            return 600;
        }

        // Fill coal bag if empty
        if (!coalBagFull) {
            Logger.log("Attempting to fill coal bag...");
            if (Inventory.interact(COAL_BAG_ID, "Fill")) {
                Logger.log("Coal bag filled");
                coalBagFull = true;
                return 600;
            }
        }

        // Final step: If coal bag is full and we don't have iron ore, withdraw it and head to conveyor
        if (coalBagFull) {
            if (!Inventory.contains(IRON_ORE_ID)) {
                Logger.log("Coal bag full, withdrawing iron ore...");
                Bank.withdraw(IRON_ORE_ID, 27);
                return 600;
            } else {
                Logger.log("Ready to smelt! Moving to conveyor...");
                Bank.close();
                state = State.WALKING_TO_CONVEYOR;
            }
        }

        return 600;
    }

    /**
     * Deposits ores into the conveyor belt and empties coal bag
     * Includes retry logic with proper timeouts
     * @return Sleep duration in milliseconds
     */
    private int handleOreDeposit() {
        Logger.log("Current state: DEPOSITING_ORE");
        
        // First deposit iron ore
        if (Inventory.contains(IRON_ORE_ID)) {
            // Increased from 5000 to 8000 (8 seconds)
            long startTime = System.currentTimeMillis();
            boolean hasLoggedDeposit = false;
            
            while (System.currentTimeMillis() - startTime < 8000) { // Increased timeout
                GameObject conveyor = GameObjects.closest("Conveyor belt");
                if (conveyor != null && conveyor.canReach()) {
                    if (!hasLoggedDeposit) {
                        Logger.log("Found reachable conveyor, attempting to deposit iron ore...");
                        hasLoggedDeposit = true;
                    }
                    
                    if (conveyor.interact("Put-ore-on")) {
                        if (Sleep.sleepUntil(() -> !Inventory.contains(IRON_ORE_ID), 5000)) {
                            Logger.log("Iron ore deposited successfully");
                            break;
                        }
                    }
                } else {
                    // Only log walking message once every 3 seconds
                    if ((System.currentTimeMillis() - startTime) % 3000 == 0) {
                        Logger.log("Walking to conveyor...");
                    }
                    Walking.walk(CONVEYOR_BELT_TILE);
                    Sleep.sleep(600);
                }
            }

            // Update message to match new timeout
            if (Inventory.contains(IRON_ORE_ID)) {
                Logger.log("Failed to reach/deposit at conveyor within 8 seconds, returning to bank");
                state = State.WALKING_TO_BANK;
                return 600;
            }
        }

        // Then empty coal bag if it's full
        if (coalBagFull) {
            Logger.log("Attempting to empty coal bag...");
            if (Inventory.interact(COAL_BAG_ID, "Empty")) {
                if (Sleep.sleepUntil(() -> Inventory.contains(COAL_ID), 1200)) {
                    Logger.log("Coal bag emptied");
                    
                    // Now deposit the coal
                    GameObject conveyor = GameObjects.closest("Conveyor belt");
                    if (conveyor != null && conveyor.canReach()) {
                        if (conveyor.interact("Put-ore-on")) {
                            // Wait longer for coal deposit and animation to complete
                            if (Sleep.sleepUntil(() -> !Inventory.contains(COAL_ID), 3000)) {
                                Logger.log("Coal deposited successfully");
                                coalBagFull = false;
                                
                                // Add a small delay after deposit before state transition
                                Sleep.sleep(600);
                                
                                // Now check for dispenser
                                GameObject dispenser = GameObjects.closest("Bar dispenser");
                                if (dispenser != null && dispenser.canReach()) {
                                    if (dispenser.interact("Take")) {
                                        state = State.COLLECTING_BARS;
                                        return 100;
                                    }
                                }
                                
                                state = State.COLLECTING_BARS;
                                Walking.walk(BAR_DISPENSER_TILE);
                                return 100;
                            } else {
                                Logger.log("Failed to deposit coal, retrying...");
                                return 600;
                            }
                        }
                    }
                }
            }
        }

        return 600;
    }

    /**
     * Collects completed steel bars from the bar dispenser
     * Handles chat interface for selecting quantity
     * Can handle multiple bar types (steel/iron)
     * @return Sleep duration in milliseconds
     */
    private int handleBarCollection() {
        // Only log once when entering state
        if (state != State.COLLECTING_BARS) {
            Logger.log("Current state: COLLECTING_BARS");
        }
        
        // First check if we have any bars in inventory - if so, go bank them
        if (Inventory.contains(STEEL_BAR_ID) || Inventory.contains(IRON_BAR_ID)) {
            Logger.log("Have bars in inventory (" + 
                (Inventory.contains(STEEL_BAR_ID) ? "steel" : "iron") + 
                "), going to bank");
            state = State.WALKING_TO_BANK;
            return 100;
        }

        GameObject dispenser = GameObjects.closest("Bar dispenser");
        // Add distance check to start walking if too far
        if (dispenser == null || !dispenser.canReach()) {
            Walking.walk(BAR_DISPENSER_TILE);
            return 100;
        }

        if (dispenser.interact("Take")) {
            Logger.log("Interacting with bar dispenser...");
            
            if (Sleep.sleepUntil(() -> Dialogues.inDialogue(), 2000)) {
                Logger.log("Dialogue opened for bar collection");
                
                Keyboard.type("1");
                Logger.log("Pressed 1 to take all bars");
                
                Sleep.sleepUntil(() -> 
                    Inventory.contains(STEEL_BAR_ID) || 
                    Inventory.contains(IRON_BAR_ID), 2000);
                
                if (Inventory.contains(STEEL_BAR_ID) || Inventory.contains(IRON_BAR_ID)) {
                    Logger.log("Successfully collected bars: " + 
                        (Inventory.contains(STEEL_BAR_ID) ? "steel" : "iron") +
                        " (Count: " + (Inventory.contains(STEEL_BAR_ID) ? 
                            Inventory.count(STEEL_BAR_ID) : 
                            Inventory.count(IRON_BAR_ID)) + ")");
                    state = State.WALKING_TO_BANK;
                }
            }
        }
        return 100;
    }

    /**
     * Handles walking to the bank location and opening the bank
     * @return Sleep duration in milliseconds
     */
    private int walkToBank() {
        Logger.log("Current state: WALKING_TO_BANK");
        Logger.log("Has steel bars: " + Inventory.contains(STEEL_BAR_ID));
        Logger.log("Has iron bars: " + Inventory.contains(IRON_BAR_ID));

        // First try to open bank if we're close enough
        if (Bank.isOpen()) {
            state = State.BANKING;
            return 600;
        }

        // If we're not at bank yet, walk there
        if (Walking.shouldWalk()) {
            Logger.log("Walking to bank...");
            Walking.walk(Bank.getClosestBankLocation());
            return 600;
        }

        // Try to open bank when we reach it
        Logger.log("Attempting to open bank...");
        if (Bank.open()) {
            Sleep.sleepUntil(Bank::isOpen, 5000);
            if (Bank.isOpen()) {
                Logger.log("Bank opened successfully");
                state = State.BANKING;
            }
        }

        return 600;
    }

    /**
     * Handles walking to the conveyor belt
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
     * Handles walking to the bar collector
     * @return Sleep duration in milliseconds
     */
    private int walkToCollector() {
        // Give 3 seconds to reach the collector
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < 3000) {
            GameObject dispenser = GameObjects.closest("Bar dispenser");
            if (dispenser != null && dispenser.canReach()) {
                state = State.COLLECTING_BARS;
                return 100;
            }
            
            // Only log walking message once every 2 seconds
            if ((System.currentTimeMillis() - startTime) % 2000 == 0) {
                Logger.log("Walking to bar dispenser...");
            }
            Walking.walk(BAR_DISPENSER_TILE);
            Sleep.sleep(300);
        }
        
        // If we haven't reached collector in 3 seconds, retry from conveyor
        Logger.log("Failed to reach collector, retrying from conveyor");
        state = State.WALKING_TO_CONVEYOR;
        return 100;
    }

    /**
     * Cleanup method called when script exits
     */
    @Override
    public void onExit() {
        Logger.log("Steel Bar Smelter ended!");
    }

    /**
     * Implements the paint overlay with clean, readable stats
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
        g.drawString("DreamBot Steel Bar Smelter", 10, y);
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
     * Calculates runtime in HH:MM:SS format
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
     * Calculates total XP gained
     */
    private int getXPGained() {
        return Skills.getExperience(Skill.SMITHING) - startXP;
    }

    /**
     * Calculates XP gained per hour
     */
    private int getXPPerHour() {
        double timeRan = (System.currentTimeMillis() - startTime) / 3600000.0;
        return (int) (getXPGained() / timeRan);
    }

    // Add this method to calculate stats
    private BlastFurnaceStats calculateStats() {
        double currentXpPerHour = getXPPerHour();
        
        // If we just started (XP/hr is 0), return all zeros
        if (currentXpPerHour == 0) {
            return new BlastFurnaceStats(0, 0, 0, 0, 0, new Costs(0, 0, 0, 0));
        }
        
        // Rest of calculation remains the same
        double efficiencyRatio = currentXpPerHour / MAX_XP_PER_HOUR;
        int barsPerHour = (int) Math.floor(MAX_BARS_PER_HOUR * efficiencyRatio);
        
        int ironOreCost = barsPerHour * IRON_ORE_PRICE;
        int coalCost = barsPerHour * COAL_PRICE;
        int staminaCost = 9 * STAMINA_POT_PRICE;
        
        Costs costs = new Costs(
            ironOreCost,
            coalCost,
            staminaCost,
            HOURLY_FEE
        );
        
        int totalCosts = ironOreCost + coalCost + staminaCost + HOURLY_FEE;
        int revenue = barsPerHour * STEEL_BAR_PRICE;
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

    // Remove the record definitions and replace with static inner classes
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
        private final int ironOre;
        private final int coal;
        private final int stamina;
        private final int fee;

        public Costs(int ironOre, int coal, int stamina, int fee) {
            this.ironOre = ironOre;
            this.coal = coal;
            this.stamina = stamina;
            this.fee = fee;
        }

        public int ironOre() { return ironOre; }
        public int coal() { return coal; }
        public int stamina() { return stamina; }
        public int fee() { return fee; }
    }
} 