package org.dreambot.serum;

import org.dreambot.api.script.TaskNode;

/**
 * Task.java
 * Purpose: Base abstract class for all script tasks, providing common functionality
 * Key functionality:
 * - Defines task structure through abstract methods
 * - Provides common utility methods for all tasks
 * - Ensures consistent task behavior across the script
 * 
 * Implementation notes:
 * 1. All tasks must implement accept() and execute()
 * 2. accept() determines if task should run
 * 3. execute() contains task logic and returns sleep time
 * 4. sleepMod() provides randomized delays
 */
public abstract class Task extends TaskNode {
    
    /**
     * Determines if this task should be executed
     * Critical for task selection logic
     * 
     * @return true if task should execute, false otherwise
     */
    @Override
    public abstract boolean accept();

    /**
     * Contains the main logic for the task
     * Called when accept() returns true
     * 
     * @return sleep time in milliseconds before next execution
     */
    @Override 
    public abstract int execute();

    /**
     * Provides randomized sleep duration to prevent pattern detection
     * Used for human-like delays between actions
     * 
     * @return random sleep duration between 300-800ms
     */
    protected int sleepMod() {
        return (int)(Math.random() * 500) + 300; // Random sleep 300-800ms
    }
} 