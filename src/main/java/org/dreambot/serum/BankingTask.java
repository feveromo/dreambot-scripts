package org.dreambot.serum;

import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.impl.Condition;

public class BankingTask extends Task {
    // Required items for collecting snakeweed
    private static final int[] COLLECTION_ITEMS = {
        1939,  // SWAMP_TAR
        249,   // GUAM_LEAF
        772    // DRAMEN_STAFF (required for fairy rings)
    };

    // Required items for making serums
    private static final int[] SERUM_ITEMS = {
        3024,  // SUPER_RESTORE_4
        235,   // CRUSHED_UNICORN_HORN
        4198   // NAIL_BEAST_NAILS
    };

    private boolean isCollectingMode = true;

    @Override
    public boolean accept() {
        return Inventory.isFull() || needsSupplies() || 
               !Bank.isOpen() && (needsSupplies() || Inventory.isFull());
    }

    @Override
    public int execute() {
        // If we're not at a bank, walk to closest bank
        if (!BankLocation.getNearest().getArea(1).contains(Players.getLocal().getTile())) {
            log("Walking to nearest bank...");
            Walking.walk(BankLocation.getNearest().getCenter());
            Sleep.sleepUntil(new Condition() {
                @Override
                public boolean verify() {
                    return BankLocation.getNearest().getArea(1).contains(Players.getLocal().getTile());
                }
            }, 5000);
            return 600;
        }

        if (!Bank.isOpen()) {
            log("Opening bank...");
            if (Bank.open()) {
                Sleep.sleepUntil(new Condition() {
                    @Override
                    public boolean verify() {
                        return Bank.isOpen();
                    }
                }, 3000);
            }
            return 600;
        }

        if (isCollectingMode) {
            log("Handling collection banking...");
            handleCollectionBanking();
        } else {
            log("Handling serum crafting banking...");
            handleSerumBanking();
        }

        return 600;
    }

    private void handleCollectionBanking() {
        log("Depositing items except collection supplies...");
        Bank.depositAllExcept(item -> {
            for (int id : COLLECTION_ITEMS) {
                if (item.getID() == id) return true;
            }
            return false;
        });
        sleep(sleepMod());

        for (int itemId : COLLECTION_ITEMS) {
            if (!Inventory.contains(itemId)) {
                if (Bank.contains(itemId)) {
                    log("Withdrawing item: " + itemId);
                    Bank.withdraw(itemId, 1);
                    Sleep.sleepUntil(new Condition() {
                        @Override
                        public boolean verify() {
                            return Inventory.contains(itemId);
                        }
                    }, 2000);
                } else {
                    log("ERROR: Missing required collection item: " + itemId);
                }
            }
        }
    }

    private void handleSerumBanking() {
        log("Depositing all items...");
        Bank.depositAllItems();
        Sleep.sleepUntil(new Condition() {
            @Override
            public boolean verify() {
                return Inventory.isEmpty();
            }
        }, 2000);

        for (int itemId : SERUM_ITEMS) {
            if (Bank.contains(itemId)) {
                log("Withdrawing serum ingredient: " + itemId);
                Bank.withdraw(itemId, 14);
                Sleep.sleepUntil(new Condition() {
                    @Override
                    public boolean verify() {
                        return Inventory.contains(itemId);
                    }
                }, 2000);
            } else {
                log("ERROR: Missing required serum ingredient: " + itemId);
            }
        }
    }

    private boolean needsSupplies() {
        if (isCollectingMode) {
            for (int itemId : COLLECTION_ITEMS) {
                if (!Inventory.contains(itemId)) {
                    return true;
                }
            }
        } else {
            for (int itemId : SERUM_ITEMS) {
                if (!Inventory.contains(itemId)) {
                    return true;
                }
            }
        }
        return false;
    }
} 