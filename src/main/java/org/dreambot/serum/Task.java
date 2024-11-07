package org.dreambot.serum;

import org.dreambot.api.script.TaskNode;

public abstract class Task extends TaskNode {
    
    @Override
    public abstract boolean accept();

    @Override 
    public abstract int execute();

    protected int sleepMod() {
        return (int)(Math.random() * 500) + 300; // Random sleep 300-800ms
    }
} 