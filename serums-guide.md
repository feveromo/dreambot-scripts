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

### 3. Collect Snakeweed (Regular Collection)
- **Highlighting Vines**: (optional but helpful for DreamBot navigation)  
  - identify and highlight the vine objects for easier targeting

- **Looped Collection Actions**:  
  - immediately `click on vine` to attempt snakeweed collection  
  - if herb found dialogue occurs, move to the next vine; repeat the process  
---

### 4. Clean Snakeweed & Bank Run
- **Cleaning Snakeweed**:  
  - if inventory is full of snakeweed, clean each piece for additional herblore XP  

- **Banking**:  
  - take ckr to dkr fairy ring to grand exchange and bank inventory
  - retrieve items for sandfew serum crafting  
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
