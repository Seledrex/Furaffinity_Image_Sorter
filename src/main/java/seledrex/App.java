package seledrex;

//======================================================================================================================
// Imports
//======================================================================================================================

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.SwingUtilities;

//======================================================================================================================
// App
//======================================================================================================================

/**
 * This class implements the GUI for the Furaffinity Image Sorter.
 *
 * The application window is created in the theme of the user's operating
 * system, and then the window is show in the center of the user's
 * primary screen. A log is created in the center below a panel that holds
 * all the buttons.
 *
 * @author Eric Auster
 */
public class App extends JPanel implements ActionListener
{
    //==================================================================================================================
    // Properties
    //==================================================================================================================

    private JButton addFolderButton, clearInputButton, setOutputButton, sortButton; // Buttons
    private JTextArea log; // Log
    private JFileChooser fc; // File chooser
    private ImageSorting sorter; // Image sorter

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    private App()
    {
        super(new BorderLayout());

        // Create the log object and make it scrollable
        log = new JTextArea(20,100);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        // Create the file chooser
        fc = new JFileChooser();

        // Create the image sorter
        sorter = new ImageSorting();

        // Set the image sorter so that it will only show directories
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Create the 'Add input folder' button
        addFolderButton = new JButton("Add input folder");
        addFolderButton.addActionListener(this);

        // Create the 'Sort images' button
        sortButton = new JButton("Sort images");
        sortButton.addActionListener(this);

        // Create the 'Set output folder' button
        setOutputButton = new JButton("Set output folder");
        setOutputButton.addActionListener(this);

        // Create the 'Remove input folder(s)' button
        clearInputButton = new JButton("Remove input folder(s)");
        clearInputButton.addActionListener(this);

        // Create a new panel to hold all the buttons
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(addFolderButton);
        buttonPanel.add(clearInputButton);
        buttonPanel.add(setOutputButton);
        buttonPanel.add(sortButton);

        // Add the button panel and log to the main panel
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
    }

    //==================================================================================================================
    // Methods
    //==================================================================================================================

    /**
     * Picks up events created when a button is pressed. Based on
     * which button is pressed, a different action will be performed.
     *
     * @param e  event created
     */
    public void actionPerformed(ActionEvent e)
    {
        // Handles the 'Add input folder' button
        if (e.getSource() == addFolderButton)
        {
            int returnVal = fc.showOpenDialog(App.this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                log.append("Added folder to be sorted: " + file.getAbsolutePath() + "\n");
                sorter.addInputFolder(file.getAbsolutePath());
            }

            log.setCaretPosition(log.getDocument().getLength());
        }
        // Handles the 'Set output folder' button
        else if (e.getSource() == setOutputButton)
        {
            int returnVal = fc.showOpenDialog(App.this);

            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                log.append("Set output folder: " + file.getAbsolutePath() + "\n");
                sorter.setOutputFolder(file);
            }

            log.setCaretPosition(log.getDocument().getLength());
        }
        // Handles the 'Sort images' button
        else if (e.getSource() == sortButton)
        {
            log.append("Sorting images..." + "\n");
            sorter.sortImages(log);
            log.setCaretPosition(log.getDocument().getLength());
        }
        // Handles the 'Remove input folder(s)' button
        else if (e.getSource() == clearInputButton)
        {
            sorter.clearInputFolders();
            log.append("Removed input folder(s)\n");
        }
    }

    /**
     * Builds the GUI and then shows the window.
     */
    private static void createAndShowGUI()
    {
        // Creates the JFrame
        JFrame frame = new JFrame("Furaffinity Image Sorter");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(new App());

        // Set size and center window
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

        // Create and show window
        frame.pack();
        frame.setVisible(true);
    }

    //==================================================================================================================
    // Main
    //==================================================================================================================

    /**
     * Main entry point for the program. Sets the look and feel of
     * the GUI to whichever operating system the user is using.
     *
     * @param args  command line arguments
     */
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                createAndShowGUI();
            }
        });
    }
}