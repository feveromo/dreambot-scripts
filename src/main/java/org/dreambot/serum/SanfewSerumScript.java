package org.dreambot.serum;

import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.impl.TaskScript;
import org.dreambot.serum.config.SerumConfig;
import org.dreambot.serum.gui.SerumGUI;

@ScriptManifest(
    name = "Sanfew Serum Maker",
    description = "Makes Sanfew Serums for profit",
    author = "Your Name",
    version = 1.0,
    category = Category.MONEYMAKING
)
public class SanfewSerumScript extends TaskScript {
    private final SerumConfig config = SerumConfig.getInstance();
    private SerumGUI gui;
    private volatile boolean started = false;
    private volatile boolean guiComplete = false;

    @Override
    public void onStart() {
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

    private void initializeTasks() {
        try {
            // Add tasks based on configuration
            log("Adding travel task...");
            addNodes(new TravelTask());
            
            log("Adding collection task...");
            addNodes(new CollectSnakeweedTask());
            
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

    public void setStarted(boolean started) {
        this.started = started;
        this.guiComplete = true;
        log("Script start flag set to: " + started);
    }

    @Override
    public void onExit() {
        log("Script exiting...");
        if (gui != null && gui.isVisible()) {
            gui.dispose();
        }
    }
} 