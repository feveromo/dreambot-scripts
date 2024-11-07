package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.utilities.Sleep;

public class BankingTask extends Task {
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489); // GE bank area

    @Override
    public boolean accept() {
        return Inventory.isFull() && BANK_AREA.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 2000);
            }
            return 200;
        }

        if (Bank.isOpen()) {
            log("Depositing all items...");
            Bank.depositAllItems();
            Sleep.sleepUntil(Inventory::isEmpty, 2000);
            Bank.close();
            return 200;
        }
        return 200;
    }
} 