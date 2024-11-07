package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.utilities.Sleep;

public class BankingTask extends Task {
    private static final Area BANK_AREA = new Area(3164, 3485, 3167, 3489); // GE bank area
    private static final int GRIMY_SNAKEWEED = 1525;

    private enum BankingState {
        OPEN_BANK,
        DEPOSIT_ITEMS,
        CLOSE_BANK,
        FINISHED
    }

    private BankingState currentState = BankingState.OPEN_BANK;

    @Override
    public boolean accept() {
        // Accept if we're at bank with items to deposit
        return BANK_AREA.contains(Players.getLocal()) && 
               (Inventory.isFull() || Inventory.contains(GRIMY_SNAKEWEED));
    }

    @Override
    public int execute() {
        switch (currentState) {
            case OPEN_BANK:
                if (!Bank.isOpen()) {
                    log("Opening bank...");
                    if (Bank.open()) {
                        Sleep.sleepUntil(Bank::isOpen, 2000);
                        currentState = BankingState.DEPOSIT_ITEMS;
                    }
                } else {
                    currentState = BankingState.DEPOSIT_ITEMS;
                }
                break;

            case DEPOSIT_ITEMS:
                log("Depositing items...");
                if (Bank.depositAllItems()) {
                    Sleep.sleepUntil(Inventory::isEmpty, 2000);
                    currentState = BankingState.CLOSE_BANK;
                }
                break;

            case CLOSE_BANK:
                if (Bank.close()) {
                    Sleep.sleepUntil(() -> !Bank.isOpen(), 2000);
                    currentState = BankingState.FINISHED;
                }
                break;

            case FINISHED:
                // Reset state for next banking session
                currentState = BankingState.OPEN_BANK;
                break;
        }

        return 200;
    }
} 