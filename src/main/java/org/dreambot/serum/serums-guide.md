# DreamBot Script Guide: Sandfew Serum Money-Making Herblore Method

## Overview
this guide will walk an LLM through scripting a bot in DreamBot to automate the sandfew serum herblore method in OSRS. the script involves gathering snakeweed with tick manipulation, crafting serums, and selling them for profit. each key action is broken down with conditions, timing, and item handling details.

---

### 1. Initial Setup
- **Required Items**:  
  - `swamp tar`  
  - `pestle and mortar`  
  - `Guam leaf`  
  - `Dramen staff`  
  - teleport items or tabs for fairy rings (`CKR`) and a bank (suggest `house teletabs`)
- **Initial Conditions**:  
  - check inventory for necessary items  
  - confirm that fairy ring CKR and bank teleport access are functional  

---

### 2. Travel to Snakeweed Location
- **Teleport to CKR**: use fairy ring network to teleport to `CKR`
- **Move to Snakeweed Spawn**: navigate northwest from CKR to the marshy jungle vines location

---

### 3. Collect Snakeweed (Tick Manipulation)
- **Highlighting Vines**: (optional but helpful for DreamBot navigation)  
  - identify and highlight the vine objects for easier targeting

- **Looped Collection Actions**:  
  1. `use swamp tar on Guam leaf` to prepare for snakeweed collection  
  2. immediately `click on vine` to attempt snakeweed collection  
  3. if *bush shaking sound* occurs, move to the next vine; repeat the process  
  4. **wait** for a fixed time (adjust based on response speed), or listen for sound cue, then proceed to the next vine  

- **World-Hopping (optional, for efficiency)**:  
  - after clearing all vines, `hop to another world` and repeat the snakeweed collection loop  
  - repeat until inventory is full of snakeweed  

---

### 4. Clean Snakeweed & Bank Run
- **Cleaning Snakeweed**:  
  - if inventory is full of snakeweed, clean each piece for additional herblore XP  

- **Banking**:  
  - `teleport to Castle Wars` or use bank teleport to deposit items  
  - store cleaned snakeweed and retrieve items for sandfew serum crafting  

---

### 5. Craft Sandfew Serums
- **Required Items per Serum**:  
  - 1 `super restore`  
  - 1 `crushed unicorn horn`  
  - 1 `nail beast nail`  

- **Potion Crafting Actions**:  
  - manually `combine ingredients` for each sandfew serum, using item IDs or clicking as needed  
  - repeat until inventory is filled with crafted serums  

---

### 6. Sell Sandfew Serums (Optional - Profit Maximization)
- **Using Grand Exchange (GE)**:  
  - if banked serums, withdraw all and navigate to the GE  
  - `list sandfew serums for sale`  
  - recommended: set price slightly above default GE value to maximize profits  

---

### 7. Repeat Loop for XP & Profit
- **Loop to Step 2**:  
  - reset by returning to CKR fairy ring and repeat the entire process for continuous XP and GP gains  

---

## Notes for Bot Optimization
- **Timing Adjustments**:  
  - add slight random delays between actions to mimic human timing  
  - world-hopping can be triggered based on vine availability or timed intervals  

- **Error Handling**:  
  - include checks for out-of-stock items, failed teleports, or vine unavailability  

---
