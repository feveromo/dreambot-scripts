package org.dreambot;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;

@ScriptManifest(
    name = "My First Script", 
    description = "Basic script template", 
    author = "YourName",
    version = 1.0, 
    category = Category.MISC
)
public class MyFirstScript extends AbstractScript {

    @Override
    public void onStart() {
        Logger.log("Script started!");
    }

    @Override
    public int onLoop() {
        Logger.log("Script is running!");
        return 1000; // Sleep for 1000ms before next loop
    }

    @Override
    public void onExit() {
        Logger.log("Script ended!");
    }
}