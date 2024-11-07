package org.dreambot.serum;

import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.impl.TaskScript;
import org.dreambot.api.script.listener.PaintListener;
import org.dreambot.serum.config.SerumConfig;
import org.dreambot.serum.gui.SerumGUI;

import java.awt.Color;
import java.awt.Graphics;

/**
 * SanfewSerumScript.java
 * Purpose: Main script class for Sanfew Serum component collection and creation
 * Key functionality:
 * - Manages task initialization and execution
 * - Handles GUI configuration
 * - Tracks script statistics
 * - Provides paint overlay
 * 
 * Implementation notes:
 * 1. Uses TaskScript framework for task management
 * 2. Implements PaintListener for statistics display
 * 3. Uses SerumConfig for script settings
 * 4. Manages task state through script lifecycle
 */
@ScriptManifest(
    name = "Sanfew Serum Maker",
    description = "Makes Sanfew Serums for profit",
    author = "Your Name",
    version = 1.0,
    category = Category.MONEYMAKING
)
public class SanfewSerumScript extends TaskScript implements PaintListener {
    // Configuration and GUI components
    private final SerumConfig config = SerumConfig.getInstance();
    private SerumGUI gui;
    private volatile boolean started = false;
    private volatile boolean guiComplete = false;

    // Statistics tracking
    private long startTime;
    private int herbsCollected = 0;
    private String currentState = "Starting...";

    /**
     * Initializes the script
     * - Shows configuration GUI
     * - Waits for user input
     * - Initializes tasks based on configuration
     */
    @Override
    public void onStart() {
        startTime = System.currentTimeMillis();
        
        log("Script initializing...");
        
        // Show GUI on Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            gui = new SerumGUI(this);
            gui.setVisible(true);
        });
        
        // Wait for GUI to complete
        while (!guiComplete) {
            sleep(500);
        }
        
        // If user clicked start, initialize tasks
        if (started) {
            log("Starting script with configuration:");
            log("Make Serums: " + config.shouldMakeSerums());
            log("Clean Herbs: " + config.shouldCleanHerbs());
            log("Sell on GE: " + config.shouldSellOnGE());

            // Initialize tasks
            initializeTasks();
        } else {
            log("Script start cancelled by user");
            stop();
        }
    }

    /**
     * Initializes script tasks based on configuration
     * Critical for proper task execution order
     * Tasks are added in priority order
     */
    private void initializeTasks() {
        try {
            log("Adding travel task...");
            addNodes(new TravelTask(this));
            
            log("Adding collection task...");
            addNodes(new CollectSnakeweedTask(this));
            
            // Only add optional tasks if enabled
            if (config.shouldCleanHerbs()) {
                log("Adding herb cleaning task...");
                addNodes(new CleanHerbsTask());
            }
            
            log("Adding banking task...");
            addNodes(new BankingTask());
            
            if (config.shouldMakeSerums()) {
                log("Adding serum crafting task...");
                addNodes(new CraftSerumTask());
            }
            
            log("All tasks initialized successfully");
        } catch (Exception e) {
            log("Error initializing tasks: " + e.getMessage());
            stop();
        }
    }

    /**
     * Updates script start flag and GUI completion status
     * Called by GUI when configuration is complete
     * 
     * @param started true if script should start, false to cancel
     */
    public void setStarted(boolean started) {
        this.started = started;
        this.guiComplete = true;
        log("Script start flag set to: " + started);
    }

    /**
     * Cleanup method called on script exit
     * Ensures proper GUI disposal
     */
    @Override
    public void onExit() {
        log("Script exiting...");
        if (gui != null && gui.isVisible()) {
            gui.dispose();
        }
    }

    /**
     * Increments the herbs collected counter
     * Called by CollectSnakeweedTask after successful collection
     */
    public void incrementHerbsCollected() {
        herbsCollected++;
    }

    /**
     * Updates the current script state for paint display
     * Called by tasks to show current activity
     * 
     * @param state current task state description
     */
    public void updateState(String state) {
        this.currentState = state;
    }

    /**
     * Renders script statistics overlay
     * Shows runtime, herbs collected, and current state
     * 
     * @param g Graphics context for drawing
     */
    @Override
    public void onPaint(Graphics g) {
        // Create semi-transparent black background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(5, 5, 210, 90);  // Reduced height since we removed one line

        // Set text color to white
        g.setColor(Color.WHITE);

        // Draw stats
        int y = 20;
        g.drawString("Sanfew Serum Component Collector", 10, y);
        y += 20;
        g.drawString("Runtime: " + getRunTime(), 10, y);
        y += 20;
        g.drawString("Herbs collected: " + herbsCollected + " (" + getPerHour(herbsCollected) + "/hr)", 10, y);
        y += 20;
        g.drawString("State: " + currentState, 10, y);
    }

    /**
     * Formats runtime into HH:MM:SS format
     * 
     * @return formatted runtime string
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
     * @param value the current total value
     * @return calculated per hour rate
     */
    private int getPerHour(int value) {
        double timeRan = (System.currentTimeMillis() - startTime) / 3600000.0;
        return (int) (value / timeRan);
    }
} 