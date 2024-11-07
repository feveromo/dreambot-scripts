package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.utilities.Sleep;

/*
 * BankingTask.java
 * Purpose: Manages all banking operations at the Grand Exchange
 * Key functionality:
 * - Handles bank interface interactions
 * - Deposits collected snakeweed
 * - Controls banking state transitions
 * 
 * Important implementation notes:
 * 1. State transitions must be sequential: OPEN -> DEPOSIT -> CLOSE
 * 2. Sleep conditions ensure interface actions complete
 * 3. Area checks confirm player location before banking
 * 4. Reset state after completion for next banking cycle
 */
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
                log("Depositing snakeweed...");
                if (Bank.depositAll(GRIMY_SNAKEWEED)) {
                    Sleep.sleepUntil(() -> !Inventory.contains(GRIMY_SNAKEWEED), 2000);
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