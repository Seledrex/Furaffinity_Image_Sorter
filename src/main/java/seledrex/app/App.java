package seledrex.app;

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
import java.util.HashSet;
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

    private JButton importArtworkButton, setStashButton, sortButton;
    private JButton loginButton, logoutButton, dlArtworkButton;
    private JLabel statusLabel, stashLabel;
    private JTextArea log;
    private JFileChooser fc;
    private ArtworkSorter sorter;
    private File stashFolder;
    private File downloadFolder;
    private WebClient webClient;
    private Set<String> artworkSet;
    private static JFrame frame;
    private PropertiesConfiguration properties;
    private String username;
    private boolean loggedIn;

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
        webClient.getOptions().setTimeout(30000);

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
        sorter = new ArtworkSorter(this);

        // Create the artwork set
        artworkSet = new HashSet<String>();

        // Set the image sorter so that it will only show directories
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        statusLabel = new JLabel("Not logged in");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setVerticalTextPosition(SwingConstants.CENTER);
        statusLabel.setPreferredSize(new Dimension(145, 25));

        stashLabel = new JLabel("Stash not set");
        stashLabel.setHorizontalAlignment(SwingConstants.LEFT);
        stashLabel.setPreferredSize(new Dimension(300, 25));
        stashLabel.setBorder(BorderFactory.createEmptyBorder(0, 10,0,0));

        loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(75, 25));
        loginButton.addActionListener(this);

        logoutButton = new JButton("Logout");
        logoutButton.setPreferredSize(new Dimension(75, 25));
        logoutButton.setEnabled(false);
        logoutButton.addActionListener(this);

        dlArtworkButton = new JButton("Download artwork");
        dlArtworkButton.setPreferredSize(new Dimension(150, 25));
        dlArtworkButton.setEnabled(false);
        dlArtworkButton.addActionListener(this);

        // Create the 'Add input folder' button
        importArtworkButton = new JButton("Import artwork");
        importArtworkButton.setPreferredSize(new Dimension(150, 25));
        importArtworkButton.addActionListener(this);

        // Create the 'Sort images' button
        sortButton = new JButton("Sort artwork");
        sortButton.setPreferredSize(new Dimension(150, 25));
        sortButton.addActionListener(this);

        // Create the 'Set output folder' button
        setStashButton = new JButton("Set stash");
        setStashButton.setPreferredSize(new Dimension(150, 25));
        setStashButton.addActionListener(this);

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
        topPanel.add(dlArtworkButton, cs);

        cs.gridx = 2;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(importArtworkButton, cs);

        cs.gridx = 3;
        cs.gridy = 0;
        cs.gridwidth = 1;
        topPanel.add(setStashButton, cs);

        cs.gridx = 3;
        cs.gridy = 1;
        cs.gridwidth = 1;
        topPanel.add(sortButton, cs);

        // Add the button panel and log to the main panel
        add(topPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
        add(stashLabel, BorderLayout.PAGE_END);

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
                            dlArtworkButton.setEnabled(true);
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
            properties.addProperty("stash", "");
        }
    }

    //==================================================================================================================
    // GUI Methods
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
        if (e.getSource() == importArtworkButton)
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
        else if (e.getSource() == setStashButton)
        {
            int returnVal = fc.showOpenDialog(App.this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                setStash(fc.getSelectedFile());
                log.append("Stash set: " + stashFolder.getAbsolutePath() + "\n");
            }

            log.setCaretPosition(log.getDocument().getLength());
        }
        // Handles the 'Sort images' button
        else if (e.getSource() == sortButton)
        {
            SwingWorker<String, Object> worker = new SwingWorker<String, Object>() {
                @Override
                protected String doInBackground() {
                    log.append("Sorting images..." + "\n");
                    sorter.sortInputFolders();
                    return null;
                }

                @Override
                protected void done() {
                    log.setCaretPosition(log.getDocument().getLength());
                }
            };
            worker.execute();
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
                dlArtworkButton.setEnabled(true);
            }
        }
        // Handles 'Logout' button
        else if (e.getSource() == logoutButton) {
            // Clear user data
            setUsername("");
            setStatus("Not logged in");
            loggedIn = false;
            logoutButton.setEnabled(false);
            dlArtworkButton.setEnabled(false);
            loginButton.setEnabled(true);

            // Show dialog
            JOptionPane.showMessageDialog(
                    this,
                    "You have successfully logged out.",
                    "Logout Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        // Handles 'Download artwork' button
        else if (e.getSource() == dlArtworkButton) {
            // Create and show new dialog
            DownloadDialog dlDialog = new DownloadDialog(frame, this);
            dlDialog.setVisible(true);
        }
    }

    private void close()
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
            writeCookies();
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
    }

    /**
     * Performs initialization steps to be carried out after the main window
     * has opened.
     */
    private void initialize()
    {
        // Check for stash setting in the properties file
        String stash = properties.getString("stash");

        // Set the stash
        if (stash != null) {
            stashFolder = new File(stash);
            if (stashFolder.exists()) {
                setStash(stashFolder);
                return;
            }
        }

        // Tell user to set their stash
        JOptionPane.showMessageDialog(
                this,
                "Please select a folder to be your art stash.",
                "Set Stash",
                JOptionPane.INFORMATION_MESSAGE);

        int returnVal = fc.showOpenDialog(App.this);

        // Set the selected folder as the stash
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            setStash(fc.getSelectedFile());
        } else {
            System.exit(5);
        }
    }

    /**
     * Builds the GUI and then shows the window.
     */
    private static void createAndShowGUI()
    {
        final App app = new App();

        // Creates the JFrame
        frame = new JFrame("Furaffinity Image Sorter");
        frame.add(app);

        frame.addWindowListener(new WindowAdapter(){
            // Override window closing event
            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
                app.close();
                super.windowClosing(windowEvent);
                System.exit(0);
            }

            // Override window opening event
            @Override
            public void windowOpened(WindowEvent e) {
                app.initialize();
            }
        });

        // Set size and center window
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

        // Create and show window
        frame.pack();
        frame.setVisible(true);
    }

    //==================================================================================================================
    // Helper Methods
    //==================================================================================================================

    /**
     * Appends to the application's log.
     *
     * @param message  message to append
     */
    void appendToLog(String message)
    {
        log.append(message);
        log.setCaretPosition(log.getText().length() - 1);
    }

    /**
     * Sets the application's status label.
     *
     * @param status  status message to set
     */
    private void setStatus(String status)
    {
        statusLabel.setText(status);
    }

    private void setStash(File file)
    {
        stashFolder = file;
        properties.setProperty("stash", stashFolder.getAbsolutePath());
        stashLabel.setText("Stash location: " + stashFolder.getAbsolutePath());
        findAllArtwork(stashFolder);
        sorter.setOutputFolder(file);
        downloadFolder = new File(stashFolder.getAbsolutePath() + "/download");
        if (!downloadFolder.exists()) {
            if (downloadFolder.mkdir()) {
                appendToLog("Download folder created: " + downloadFolder.getAbsolutePath() + "\n");
            }
        }
    }

    /**
     * Recursive function that scans the stash and adds all pieces of artwork
     * to the artwork stash set.
     *
     * @param src  stash folder to search from
     */
    private void findAllArtwork(File src)
    {
        if (src.isFile()) {
            artworkSet.add(src.getName());
        } else if (src.isDirectory()) {
            File[] files = src.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    findAllArtwork(file);
                }
            }
        }
    }

    /**
     * Simple getter for the Web Client.
     *
     * @return  the current web client instance
     */
    public WebClient getWebClient()
    {
        return webClient;
    }

    /**
     * Writes the cookies in the Web Client to a file.
     */
    public void writeCookies()
    {
        try {
            ObjectOutput out = new ObjectOutputStream(new FileOutputStream("cookie.file"));
            out.writeObject(webClient.getCookieManager().getCookies());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the username currently being used.
     *
     * @param username  username to set
     */
    private void setUsername(String username)
    {
        this.username = username;
        properties.setProperty("username", username);
    }

    /**
     * Checks if the stash contains a certain file.
     *
     * @param filename  file to check
     * @return          true if it is inside the stash
     */
    public boolean stashContains(String filename)
    {
        return artworkSet.contains(filename);
    }

    /**
     * Adds a file to the stash.
     *
     * @param artwork  file to add
     */
    public void addToStash(File artwork)
    {
        sorter.sortFile(artwork);
        artworkSet.add(artwork.getName());
    }

    /**
     * Getter for returning the download folder.
     *
     * @return  current download folder
     */
    public File getDownloadFolder()
    {
        return downloadFolder;
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