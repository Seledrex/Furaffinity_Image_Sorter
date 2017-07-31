package seledrex;

//======================================================================================================================
// Imports
//======================================================================================================================

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import org.apache.commons.configuration.PropertiesConfiguration;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.SwingUtilities;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

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
    private JButton loginButton, logoutButton, dlFavsButton, dlGalleryButton;
    private JLabel statusLabel;
    private JTextArea log; // Log
    private JFileChooser fc; // File chooser
    private ImageSorting sorter; // Image sorter
    private static WebClient webClient;
    private static JFrame frame;
    private static PropertiesConfiguration properties;
    private String username;
    private static boolean loggedIn;

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    @SuppressWarnings("unchecked")
    private App()
    {
        super(new BorderLayout());

        // Create new web client
        webClient = new WebClient();
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setJavaScriptEnabled(false);

        // Enable cookies
        CookieManager manager = webClient.getCookieManager();
        manager.setCookiesEnabled(true);
        webClient.setCookieManager(manager);

        // Set logged in
        loggedIn = false;

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

        statusLabel = new JLabel("Not logged in");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setVerticalTextPosition(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(145, 25));

        loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(75, 25));
        loginButton.addActionListener(this);

        logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(75, 25));
        logoutButton.setEnabled(false);
        logoutButton.addActionListener(this);

        dlFavsButton = new JButton("Download favorites");
        dlFavsButton.setPreferredSize(new Dimension(150, 25));
        dlFavsButton.setEnabled(false);
        dlFavsButton.addActionListener(this);

        dlGalleryButton = new JButton("Download gallery");
        dlGalleryButton.setPreferredSize(new Dimension(150, 25));
        dlGalleryButton.setEnabled(false);
        dlGalleryButton.addActionListener(this);

        // Create the 'Add input folder' button
        addFolderButton = new JButton("Add input folder");
        addFolderButton.setPreferredSize(new Dimension(150, 25));
        addFolderButton.addActionListener(this);

        // Create the 'Sort images' button
        sortButton = new JButton("Sort images");
        sortButton.setPreferredSize(new Dimension(150, 25));
        sortButton.addActionListener(this);

        // Create the 'Set output folder' button
        setOutputButton = new JButton("Set output folder");
        setOutputButton.setPreferredSize(new Dimension(150, 25));
        setOutputButton.addActionListener(this);

        // Create the 'Remove input folder(s)' button
        clearInputButton = new JButton("Remove input folder(s)");
        clearInputButton.setPreferredSize(new Dimension(150, 25));
        clearInputButton.addActionListener(this);

        // Create a new panel to hold all the buttons
        JPanel topPanel = new JPanel(new GridBagLayout()); //use FlowLayout
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;

        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 2;
        topPanel.add(statusLabel, cs);

        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(loginButton, cs);

        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(logoutButton, cs);

        cs.gridx = 2;
        cs.gridy = 0;
        cs.gridwidth = 1;
        topPanel.add(dlFavsButton, cs);

        cs.gridx = 2;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(dlGalleryButton, cs);

        cs.gridx = 3;
        cs.gridy = 0;
        cs.gridwidth = 1;
        topPanel.add(addFolderButton, cs);

        cs.gridx = 3;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(clearInputButton, cs);

        cs.gridx = 4;
        cs.gridy = 0;
        cs.gridwidth = 1;
        topPanel.add(setOutputButton, cs);

        cs.gridx = 4;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(sortButton, cs);

        // Add the button panel and log to the main panel
        add(topPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);

        // Create new properties configuration
        properties = new PropertiesConfiguration();

        // Load properties and cookies if they exist
        if (new File("user.properties").exists())
        {
            try {
                // Read in properties file
                FileReader reader = new FileReader("user.properties");
                properties.load(reader);
                reader.close();

                // Check for username
                if (properties.getString("username") != null) {

                    File cookieFile = new File("cookie.file");

                    // Check if cookies exist
                    if (cookieFile.exists())
                    {
                        Set<Cookie> cookies;

                        // Read in cookies
                        ObjectInputStream in = new ObjectInputStream(new FileInputStream(cookieFile));
                        cookies = (Set<Cookie>) in.readObject();
                        in.close();

                        // Set cookies in web client
                        if (cookies != null) {
                            for (Cookie cookie : cookies) {
                                webClient.getCookieManager().addCookie(cookie);
                            }
                        }

                        // Open Furaffinity homepage
                        HtmlPage checkSuccess = webClient.getPage("http://www.furaffinity.net/");
                        String page = checkSuccess.asText();

                        // Use regular expression to check for Log Out
                        Pattern pattern = Pattern.compile("(Log Out|log out)");
                        Matcher m = pattern.matcher(page);

                        // Pattern is found
                        if (m.find()) {
                            username = properties.getString("username");
                            setStatus("Welcome " + username + "!");
                            loggedIn = true;
                            loginButton.setEnabled(false);
                            logoutButton.setEnabled(true);
                            dlFavsButton.setEnabled(true);
                            dlGalleryButton.setEnabled(true);
                        }
                    }
                }
            } catch (Exception e) {
                log.append("Error reading configuration information:\n" + getStackTrace(e));
            }
        }
        // Create new properties file otherwise
        else {
            properties.addProperty("username", "");
        }
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
            sorter.sortImages(this);
            log.setCaretPosition(log.getDocument().getLength());
        }
        // Handles the 'Remove input folder(s)' button
        else if (e.getSource() == clearInputButton)
        {
            sorter.clearInputFolders();
            log.append("Removed input folder(s)\n");
        }
        // Handles 'Login' button
        else if (e.getSource() == loginButton)
        {
            // Create and show new dialog
            LoginDialog loginDialog = new LoginDialog(frame, webClient, this);
            loginDialog.setVisible(true);

            // Check for successful login
            if (loginDialog.isSuccessful()) {
                setStatus("Welcome " + loginDialog.getUsername() + "!");
                setUsername(loginDialog.getUsername());
                loggedIn = true;
                loginButton.setEnabled(false);
                logoutButton.setEnabled(true);
                dlFavsButton.setEnabled(true);
                dlGalleryButton.setEnabled(true);
            }
        }
        // Handles 'Logout' button
        else if (e.getSource() == logoutButton) {
            // Clear user data
            setUsername("");
            setStatus("Not logged in");
            loggedIn = false;
            logoutButton.setEnabled(false);
            dlFavsButton.setEnabled(false);
            dlGalleryButton.setEnabled(false);
            loginButton.setEnabled(true);

            // Show dialog
            JOptionPane.showMessageDialog(
                    this,
                    "You have successfully logged out.",
                    "Logout Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Builds the GUI and then shows the window.
     */
    private static void createAndShowGUI()
    {
        // Creates the JFrame
        frame = new JFrame("Furaffinity Image Sorter");
        frame.add(new App());

        // Override window closing event
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
                PrintWriter writer = null;

                // Writer properties to file
                try {
                    writer = new PrintWriter("user.properties");
                    properties.save(writer);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) writer.close();
                }

                // Write cookies to file
                if (loggedIn) {
                    try {
                        ObjectOutput out = new ObjectOutputStream(new FileOutputStream("cookie.file"));
                        out.writeObject(webClient.getCookieManager().getCookies());
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    File cookieFile = new File("cookie.file");
                    if (cookieFile.exists()) {
                        if (!cookieFile.delete()) {
                            System.err.println("Could not delete cookie file");
                        }
                    }
                }

                // Close client and window
                webClient.close();
                super.windowClosing(windowEvent);
                System.exit(0);
            }
        });

        // Set size and center window
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

        // Create and show window
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * Appends to the application's log.
     *
     * @param message  message to append
     */
    void appendToLog(String message) {
        log.append(message);
    }

    /**
     * Sets the application's status label.
     *
     * @param status  status message to set
     */
    private void setStatus(String status) {
        statusLabel.setText(status);
    }

    /**
     * Sets the username currently being used.
     *
     * @param username  username to set
     */
    private void setUsername(String username) {
        this.username = username;
        properties.setProperty("username", username);
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