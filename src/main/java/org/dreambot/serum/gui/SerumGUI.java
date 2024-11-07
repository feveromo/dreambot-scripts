package org.dreambot.serum.gui;

import org.dreambot.serum.SanfewSerumScript;
import org.dreambot.serum.config.SerumConfig;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class SerumGUI extends JFrame {
    private final SerumConfig config = SerumConfig.getInstance();
    private final SanfewSerumScript script;

    public SerumGUI(SanfewSerumScript script) {
        super("Sanfew Serum Maker Settings");
        this.script = script;
        setLayout(new BorderLayout());
        initComponents();
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
    }

    private void initComponents() {
        // Main panel with grid layout
        JPanel mainPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Checkboxes - default to unchecked for optional tasks
        JCheckBox makeSerumsBox = new JCheckBox("Make Sanfew Serums", false);
        JCheckBox cleanHerbsBox = new JCheckBox("Clean Herbs", false);
        JCheckBox tickManipBox = new JCheckBox("Use Tick Manipulation", true);
        JCheckBox sellGEBox = new JCheckBox("Sell on Grand Exchange", false);

        // Update config with default values
        config.setMakeSerums(false);
        config.setCleanHerbs(false);
        config.setUseTickManipulation(true);
        config.setSellOnGE(false);

        // Start button
        JButton startButton = new JButton("Start Script");
        startButton.addActionListener(e -> {
            if (script != null) {
                // Save final config values
                config.setMakeSerums(makeSerumsBox.isSelected());
                config.setCleanHerbs(cleanHerbsBox.isSelected());
                config.setUseTickManipulation(tickManipBox.isSelected());
                config.setSellOnGE(sellGEBox.isSelected());
                
                script.setStarted(true);
                dispose();
            }
        });

        // Add action listeners for real-time config updates
        makeSerumsBox.addActionListener(e -> config.setMakeSerums(makeSerumsBox.isSelected()));
        cleanHerbsBox.addActionListener(e -> config.setCleanHerbs(cleanHerbsBox.isSelected()));
        tickManipBox.addActionListener(e -> config.setUseTickManipulation(tickManipBox.isSelected()));
        sellGEBox.addActionListener(e -> config.setSellOnGE(sellGEBox.isSelected()));

        // Handle window closing
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (script != null) {
                    script.setStarted(false);
                }
                dispose();
            }
        });

        // Add components to panel
        mainPanel.add(makeSerumsBox);
        mainPanel.add(cleanHerbsBox);
        mainPanel.add(tickManipBox);
        mainPanel.add(sellGEBox);
        mainPanel.add(startButton);

        // Add panel to frame
        add(mainPanel, BorderLayout.CENTER);
    }
} 