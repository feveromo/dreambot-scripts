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
        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Checkboxes - cleaning unchecked by default
        JCheckBox cleanHerbsBox = new JCheckBox("Clean Herbs", false);
        JCheckBox makeSerumsBox = new JCheckBox("Make Sanfew Serums", false);

        // Start button
        JButton startButton = new JButton("Start Script");
        startButton.addActionListener(e -> {
            if (script != null) {
                // Save final config values
                config.setCleanHerbs(cleanHerbsBox.isSelected());
                config.setMakeSerums(makeSerumsBox.isSelected());
                
                script.setStarted(true);
                dispose();
            }
        });

        // Add components to panel
        mainPanel.add(cleanHerbsBox);
        mainPanel.add(makeSerumsBox);
        mainPanel.add(startButton);

        // Add panel to frame
        add(mainPanel, BorderLayout.CENTER);
    }
} 