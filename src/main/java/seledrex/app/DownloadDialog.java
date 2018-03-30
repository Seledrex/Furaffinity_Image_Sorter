package seledrex.app;

//======================================================================================================================
// Imports
//======================================================================================================================

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

//======================================================================================================================
// DownloadDialog
//======================================================================================================================

/**
 * This class implements the download dialog box that allows users to download artwork of their
 * choosing. A window will be displayed, and the user can type in the username of artwork they
 * wish to download artwork from. After typing in a username, the user can download the gallery,
 * favorites, and scraps for the given username. The progress bars show the progress of the
 * current session, and the middle text area shows information about the download.
 */
public class DownloadDialog extends JDialog implements ActionListener, PropertyChangeListener {

    //==================================================================================================================
    // Properties
    //==================================================================================================================

    private final int maxTries = 3;
    private JTextField userField;
    private JButton dlFavButton, dlGalleryButton, dlScrapsButton, closeButton, stopButton;
    private JProgressBar pageProgressBar, subProgressBar;
    private JTextArea dlOutput;
    private final App app;
    private CountDownLatch latch;
    private DownloadTask task;
    private volatile boolean stop = false;

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    /**
     * Initializes the download dialog. All the user interface pieces will be
     * set to their default values, and then the window will be shown.
     *
     * @param parent  parent frame
     * @param app     parent application
     */
    DownloadDialog(Frame parent, App app)
    {
        // Intialize Dialog
        super(parent, "Download Artwork", true);
        this.app = app;

        // Create panel for download dialog
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(3, 3, 3, 3);

        // Add link label
        JLabel linkLabel = new JLabel("User: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        dialogPanel.add(linkLabel, cs);

        // Add link field
        userField = new JTextField(20);
        userField.setPreferredSize(new Dimension(120, 20));
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 1;
        dialogPanel.add(userField, cs);

        // Add progress bar
        pageProgressBar = new JProgressBar(0, 100);
        pageProgressBar.setValue(0);
        pageProgressBar.setStringPainted(true);
        pageProgressBar.setString("Page 0/0");
        pageProgressBar.setPreferredSize(new Dimension(250, 20));
        cs.gridx = 2;
        cs.gridy = 0;
        cs.gridwidth = 2;
        dialogPanel.add(pageProgressBar, cs);

        // Add progress bar
        subProgressBar = new JProgressBar(0, 100);
        subProgressBar.setValue(0);
        subProgressBar.setStringPainted(true);
        subProgressBar.setString("Submission 0/0");
        subProgressBar.setPreferredSize(new Dimension(250, 20));
        cs.gridx = 4;
        cs.gridy = 0;
        cs.gridwidth = 2;
        dialogPanel.add(subProgressBar, cs);

        // Add output text area
        dlOutput = new JTextArea(20, 80);
        dlOutput.setMargin(new Insets(5, 5, 5, 5));
        dlOutput.setEditable(false);

        // Initialize download favorites button
        dlFavButton = new JButton("Favorites");
        dlFavButton.setPreferredSize(new Dimension(90, 25));
        dlFavButton.addActionListener(this);

        // Initialize download gallery button
        dlGalleryButton = new JButton("Gallery");
        dlGalleryButton.setPreferredSize(new Dimension(90, 25));
        dlGalleryButton.addActionListener(this);

        // Initialize download scraps button
        dlScrapsButton = new JButton("Scraps");
        dlScrapsButton.setPreferredSize(new Dimension(90, 25));
        dlScrapsButton.addActionListener(this);

        // Initializes stop button
        stopButton = new JButton("Stop");
        stopButton.setPreferredSize(new Dimension(90, 25));
        stopButton.setEnabled(false);
        stopButton.addActionListener(this);

        // Initialize close button
        closeButton = new JButton("Close");
        closeButton.setPreferredSize(new Dimension(90, 25));
        closeButton.addActionListener(this);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(dlFavButton);
        buttonPanel.add(dlGalleryButton);
        buttonPanel.add(dlScrapsButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(closeButton);

        // Set content pane
        getContentPane().add(dialogPanel, BorderLayout.PAGE_START);
        getContentPane().add(new JScrollPane(dlOutput), BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.PAGE_END);

        // Finishing touches
        pack();
        setResizable(true);
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(800, 500));
        setAlwaysOnTop(false);
        this.app.writeCookies();
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
        // Handles download favorites button
        if (e.getSource() == dlFavButton) {
            if (userField.getText().trim().equals("")) {
                appendToLog("Please type in a user\n");
            } else {
                initializeDownload("favorites");
            }
        }
        // Handles download gallery button
        else if (e.getSource() == dlGalleryButton) {
            if (userField.getText().trim().equals("")) {
                appendToLog("Please type in a user\n");
            } else {
                initializeDownload("gallery");
            }
        }
        // Handles download scraps button
        else if (e.getSource() == dlScrapsButton) {
            if (userField.getText().trim().equals("")) {
                appendToLog("Please type in a user\n");
            } else {
                initializeDownload("scraps");
            }
        }
        // Handles close button
        else if (e.getSource() == closeButton) {
            if (task != null) {
                task.cancel(true);
            }
            this.dispose();
        }
        else if (e.getSource() == stopButton) {
            if (task != null) {
                task.cancel(true);
            }
            stopButton.setEnabled(false);
        }
    }

    /**
     * Initializes the download based off which type was received.
     *
     * @param type  favorites, gallery, scraps
     */
    private void initializeDownload(String type)
    {
        // Update controls
        dlFavButton.setEnabled(false);
        dlGalleryButton.setEnabled(false);
        dlScrapsButton.setEnabled(false);
        stopButton.setEnabled(true);

        // Update progress bars
        pageProgressBar.setIndeterminate(true);
        subProgressBar.setIndeterminate(true);
        pageProgressBar.setString("Initializing...");
        subProgressBar.setString("Initializing...");

        // Start new task
        stop = false;
        task = new DownloadTask(type);
        task.addPropertyChangeListener(this);
        task.execute();
    }

    /**
     * This property change listener is used to update the progress bars when
     * new progress is found.
     *
     * @param evt  event
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        // Set number of pages to download
        if (evt.getPropertyName().equals("numPages"))
        {
            int numPages = (Integer) evt.getNewValue();
            pageProgressBar.setIndeterminate(false);
            pageProgressBar.setMaximum(numPages);
            pageProgressBar.setValue(0);
            pageProgressBar.setString("Page 0/" + numPages);
        }
        // Increment current page number
        else if (evt.getPropertyName().equals("incrementPage"))
        {
            int progress = (Integer) evt.getNewValue();
            pageProgressBar.setValue(progress);
            pageProgressBar.setString("Page " + progress + "/" + pageProgressBar.getMaximum());
            if (progress != pageProgressBar.getMaximum()) {
                subProgressBar.setIndeterminate(true);
            }
        }
        // Set number of submissions on the current page
        else if (evt.getPropertyName().equals("numSubs"))
        {
            int numSubs = (Integer) evt.getNewValue();
            subProgressBar.setIndeterminate(false);
            subProgressBar.setMaximum(numSubs);
            subProgressBar.setValue(0);
            subProgressBar.setString("Submission 0/" + numSubs);
        }
        // Increment current submission number
        else if (evt.getPropertyName().equals("incrementSub"))
        {
            if (!stop) {
                int progress = (Integer) evt.getNewValue();
                subProgressBar.setValue(progress);
                subProgressBar.setString("Submission " + progress + "/" + subProgressBar.getMaximum());
            }
        }
        // Reset submission progress bar
        else if (evt.getPropertyName().equals("resetSubs"))
        {
            subProgressBar.setIndeterminate(false);
        }
        // Finished state of progress bars
        else if (evt.getPropertyName().equals("done"))
        {
            pageProgressBar.setValue(pageProgressBar.getMaximum());
            subProgressBar.setValue(subProgressBar.getMaximum());
            pageProgressBar.setString("Page " + pageProgressBar.getMaximum() + "/" + pageProgressBar.getMaximum());
            subProgressBar.setString("Submission " + subProgressBar.getMaximum() + "/" + subProgressBar.getMaximum());
            subProgressBar.setIndeterminate(false);
        }
        // Cancelled download state
        else if (evt.getPropertyName().equals("cancel"))
        {
            pageProgressBar.setValue(0);
            subProgressBar.setValue(0);
            pageProgressBar.setString("Stopped");
            subProgressBar.setString("Stopped");
            pageProgressBar.setIndeterminate(false);
            subProgressBar.setIndeterminate(false);
        }
        // Invalid user state
        else if (evt.getPropertyName().equals("invalidUser"))
        {
            pageProgressBar.setValue(0);
            subProgressBar.setValue(0);
            pageProgressBar.setString("Page 0/0");
            subProgressBar.setString("Submission 0/0");
            pageProgressBar.setIndeterminate(false);
            subProgressBar.setIndeterminate(false);
        }
    }

    /**
     * Appends a message to the log.
     *
     * @param message  message to append
     */
    private void appendToLog(String message)
    {
        dlOutput.append(message);
        dlOutput.setCaretPosition(dlOutput.getText().length() - 1);
    }

    //==================================================================================================================
    // DownloadTask
    //==================================================================================================================

    /**
     * Class that represents a single downloading task, whether it be downloading
     * all the pictures in someone's favorites, gallery, or scraps.
     */
    class DownloadTask extends SwingWorker<Void, Void>
    {
        //==============================================================================================================
        // Properties
        //==============================================================================================================

        private String type;
        private ExecutorService executor;
        private boolean invalidUser;

        //==============================================================================================================
        // Constructor
        //==============================================================================================================

        /**
         * Creates a new task.
         *
         * @param type  favorites, gallery, scraps
         */
        DownloadTask(String type)
        {
            this.type = type;
            invalidUser = false;
        }

        //==============================================================================================================
        // Methods
        //==============================================================================================================

        @Override
        protected Void doInBackground() throws Exception
        {
            // Keep track of the number of connection attempts
            int tries;

            HtmlPage userPage;
            tries = 0;

            // Try to get user's page
            while (true) {
                try {
                    userPage = app.getWebClient().getPage(
                            "http://www.furaffinity.net/user/" + userField.getText().trim() + "/");
                } catch (Exception e) {
                    tries++;
                    if (tries == maxTries) {
                        appendToLog("Error loading web page:\n" + getStackTrace(e));
                        return null;
                    }
                    continue;
                }
                break;
            }

            // Check if the user entered a valid user
            String src = userPage.asText();
            Pattern pattern = Pattern.compile("(This user cannot be found)");
            Matcher m = pattern.matcher(src);

            if (m.find()) {
                invalidUser = true;
                return null;
            }

            // Find the total number of pages
            PageScan scan = findNumPages(
                    "http://www.furaffinity.net/" + type + "/" + userField.getText().trim() + "/",
                    userField.getText().trim(),
                    type);

            assert scan != null;
            int numPages = scan.numPages;

            if (numPages == -1) {
                return null;
            }

            // Update the progress bars
            firePropertyChange("numPages", 0, numPages);
            appendToLog("Number of pages: " + numPages + "\n");

            // Record start time
            long startTime = System.nanoTime();

            // Create thread pool
            executor = newFixedThreadPool(4);
            int pageNum = 1;

            // Loop through each page
            while (!scan.queue.isEmpty())
            {
                HtmlPage currPage = scan.queue.poll();

                List<HtmlAnchor> anchors = currPage.getAnchors();
                List<HtmlAnchor> artAnchors = new ArrayList<HtmlAnchor>();
                pattern = Pattern.compile("(/view/\\d+/)");
                Set<String> set = new HashSet<String>();

                // Gather all the submission anchors
                for (HtmlAnchor anchor : anchors) {
                    m = pattern.matcher(anchor.getHrefAttribute());
                    if (m.find()) {
                        if (set.contains(anchor.getHrefAttribute())) {
                            continue;
                        } else {
                            set.add(anchor.getHrefAttribute());
                        }
                        artAnchors.add(anchor);
                    }
                }

                // Update progress bars
                firePropertyChange("numSubs", 0, artAnchors.size());
                appendToLog("Number of submission on page " + pageNum + ": " + artAnchors.size() + "\n");

                // Use a latch to synchronize threads
                latch = new CountDownLatch(artAnchors.size());

                // Create a new worker for each submission to download
                for (HtmlAnchor anchor : artAnchors) {
                    Runnable worker = new DownloadWorker("http://www.furaffinity.net" + anchor.getHrefAttribute());
                    executor.execute(worker);
                }

                // Update the progress bar every 10 milliseconds
                while (latch.getCount() > 0) {
                    firePropertyChange("incrementSub", 0,
                            (int) (artAnchors.size() - latch.getCount()));
                    Thread.sleep(10);
                }

                // Wait for latch to be ready
                latch.await();

                // Move onto the next page
                firePropertyChange("incrementPage", 0, pageNum);
                pageNum++;
            }

            // Shutdown thread pool
            executor.shutdown();

            // Wait for thread pool to shutdown
            while (!executor.isTerminated()) {
                Thread.sleep(10);
            }

            // Record end time
            long endPage = System.nanoTime();
            appendToLog("Total time: " + ((endPage - startTime) / 1000000000.0) + "\n");

            return null;
        }

        @Override
        protected void done()
        {
            // Task was cancelled
            if (isCancelled())
            {
                // Shutdown thread pool
                executor.shutdownNow();

                // Wait for thread pool to terminate
                try {
                    while (!executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    appendToLog("Error stopping download:\n" + getStackTrace(e));
                }

                appendToLog("Download stopped\n");
                stop = true;

                // Update progress bars
                firePropertyChange("cancel", 0, 5);
            }
            // Task received an invalid username
            else if (invalidUser)
            {
                // Update progress bars
                appendToLog("User '" + userField.getText().trim() + "' not found\n");
                firePropertyChange("invalidUser", 0, 5);
            }
            // Task completed properly
            else
            {
                // Update progress bars
                firePropertyChange("done", 0, 5);
                stopButton.setEnabled(false);
            }

            // Re-enable controls
            dlFavButton.setEnabled(true);
            dlGalleryButton.setEnabled(true);
            dlScrapsButton.setEnabled(true);
        }
    }

    //==================================================================================================================
    // DownloadWorker
    //==================================================================================================================

    /**
     * This implements a worker thread that will download the given submission, no matter
     * what the content may be!
     */
    @SuppressWarnings("unchecked")
    class DownloadWorker implements Runnable
    {
        //==============================================================================================================
        // Properties
        //==============================================================================================================

        private String url;

        //==============================================================================================================
        // Constructor
        //==============================================================================================================

        /**
         * Creates a new worker thread for downloading a submission.
         *
         * @param url  submissions's url
         */
        DownloadWorker(String url)
        {
            this.url = url;
        }

        //==============================================================================================================
        // Methods
        //==============================================================================================================

        /**
         * Thread's workload.
         */
        public void run()
        {
            // Create a web client
            final WebClient webClient = new WebClient();
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setTimeout(30000);

            File file = new File("cookie.file");
            Set<Cookie> cookies;

            // Read in cookies
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
                cookies = (Set<Cookie>) in.readObject();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            // Add cookies to client
            for (Cookie cookie : cookies) {
                webClient.getCookieManager().addCookie(cookie);
            }

            // Record start time
            long startTime = System.nanoTime();
            HtmlPage artView;

            int tries = 0;

            // Attempt to get submission page
            while (true) {
                try {
                    artView = webClient.getPage(url);
                } catch (Exception e) {
                    tries++;
                    if (tries == maxTries) {
                        appendToLog("Error loading web page:\n" + getStackTrace(e));
                        return;
                    }
                    continue;
                }
                break;
            }

            // Get all spans
            List<DomElement> spans = artView.getElementsByTagName("a");

            // Loop through all spans
            for (DomElement element : spans)
            {
                // Find which of these is the download button
                if (element.getAttribute("class").equals("button section-button") &&
                        (element.getTextContent() != null && element.getTextContent().equals("Download")))
                {
                    // Get the file name
                    String[] split = element.getAttribute("href").split("[/]");
                    String submission = split[split.length - 1];

                    // Check if the stash already has this artwork
                    if (app.stashContains(submission)) {
                        appendToLog("Skipped: " + submission + "\n");
                        break;
                    }

                    UnexpectedPage artSrc = null;
                    boolean isTxt = false;
                    tries = 0;

                    // Attempt to view submission source
                    while (true) {
                        try {
                            artSrc = webClient.getPage("http:" + element.getAttribute("href"));
                        } catch (Exception e) {
                            // Text page has been found
                            if (e instanceof ClassCastException)
                            {
                                isTxt = true;
                                break;
                            }
                            // Connection error so try again
                            else
                            {
                                tries++;
                                if (tries == maxTries) {
                                    appendToLog("Error loading web page:\n" + getStackTrace(e));
                                    return;
                                }
                                continue;
                            }
                        }
                        break;
                    }

                    // Submission is text
                    if (isTxt)
                    {
                        TextPage txt;
                        tries = 0;

                        // Attempt to view submission source
                        while (true) {
                            try {
                                txt = webClient.getPage("http:" + element.getAttribute("href"));
                            } catch (Exception e2) {
                                tries++;
                                if (tries == maxTries) {
                                    appendToLog("Error loading web page:\n" + getStackTrace(e2));
                                    return;
                                }
                                continue;
                            }
                            break;
                        }

                        PrintWriter writer = null;

                        // Write contents to a new text file
                        try {
                            writer = new PrintWriter(
                                    app.getDownloadFolder().getAbsolutePath() + "/" + submission);
                            writer.print(txt.getContent());
                        } catch (Exception e2) {
                            appendToLog("Error writing to file:\n" + getStackTrace(e2));
                            return;
                        } finally {
                            if (writer != null) writer.close();
                        }

                        // Add submission to the stash
                        app.addToStash(new File(
                                app.getDownloadFolder().getAbsolutePath() + "/" + submission));
                    }
                    // Submission is an image, music, animation, etc
                    else
                    {
                        // Setup input and output streams
                        InputStream inputStream = null;
                        OutputStream outputStream = null;
                        File newArtwork = new File(
                                app.getDownloadFolder().getAbsolutePath() + "/" + submission);

                        // Read content into file
                        try {
                            inputStream = artSrc.getInputStream();
                            outputStream = new FileOutputStream(newArtwork);

                            int read;
                            byte[] bytes = new byte[4096];

                            // Read in 4096 bytes at a time
                            while ((read = inputStream.read(bytes)) != -1) {
                                outputStream.write(bytes, 0, read);
                            }

                        } catch (Exception e) {
                            appendToLog("Error writing to file:\n" + getStackTrace(e));
                        } finally {
                            // Clean up input and output streams
                            try {
                                if (inputStream != null) inputStream.close();
                                if (outputStream != null) outputStream.close();
                            } catch (IOException e) {
                                appendToLog("Error writing to file:\n" + getStackTrace(e));
                            }
                        }

                        // Add submission to the stash
                        app.addToStash(newArtwork);
                    }

                    // Record end time
                    long endTime = System.nanoTime();
                    appendToLog("Downloaded: " + submission + "\n");
                    appendToLog("Download time: " + ((endTime - startTime) / 1000000000.0) + "\n");

                    break;
                }
            }

            // Decrement the latch
            latch.countDown();
        }
    }

    /**
     * Counts the number of pages of favorites, gallery, or scraps. Unfortunately, FA
     * changed its web API so this has to run in O(n) for the time being.
     *
     * @param url   start link
     * @param user  username
     * @param type  favorites, gallery, scraps
     * @return      number of pages and page queue
     */
    private PageScan findNumPages(String url, String user, String type)
    {
        PageScan scan = new PageScan();
        int tries = 0;

        // Need to count pages this way for favorites
        if (type.equals("favorites")) {

            HtmlPage startPage;

            // Try to get the user's favorites, gallery, or scraps
            while (true) {
                try {
                    startPage = app.getWebClient().getPage(url);
                    scan.queue.add(startPage);
                } catch (Exception e) {
                    tries++;
                    if (tries == maxTries) {
                        appendToLog("Error loading web page:\n" + getStackTrace(e));
                        return null;
                    }
                    continue;
                }
                break;
            }

            HtmlPage currPage = startPage;

            while (true) {

                List<HtmlAnchor> anchors = currPage.getAnchors();
                Pattern pattern = Pattern.compile("(/favorites/" + user + "/\\d+/next)");
                boolean nextPageFound = false;

                // Find the next button's link
                for (HtmlAnchor anchor : anchors) {

                    Matcher m = pattern.matcher(anchor.getHrefAttribute());

                    // Next button works!
                    if (m.find()) {

                        nextPageFound = true;
                        tries = 0;

                        while (true) {
                            try {
                                // Click next button and then add the page to the queue
                                currPage = anchor.click();
                                scan.queue.add(currPage);
                                scan.numPages++;
                            } catch (Exception e) {
                                tries++;
                                if (tries == maxTries) {
                                    appendToLog("Error loading web page:\n" + getStackTrace(e));
                                    return null;
                                }
                                continue;
                            }
                            break;
                        }

                        break;
                    }
                }

                // If there is no next page, we're done!
                if (!nextPageFound) {
                    break;
                }
            }
        } else {

            int pageNum = 1;
            scan.numPages = 0;
            boolean foundEnd = false;

            // Just go through pages one by one
            while (true) {

                HtmlPage currPage;

                while (true) {
                    try {
                        currPage = app.getWebClient().getPage(url + "/" + pageNum);

                        // Check if there there are no submissions to list
                        String src = currPage.asText();
                        Pattern pattern = Pattern.compile("(There are no submissions to list)");
                        Matcher m = pattern.matcher(src);

                        if (!m.find()) {
                            scan.queue.add(currPage);
                            scan.numPages++;
                        } else {
                            foundEnd = true;
                        }

                    } catch (Exception e) {
                        tries++;
                        if (tries == maxTries) {
                            appendToLog("Error loading web page:\n" + getStackTrace(e));
                            return null;
                        }
                        continue;
                    }
                    break;
                }

                // We're done if we found the end
                if (foundEnd) {
                    break;
                }

                pageNum++;
            }
        }

        return scan;
    }

    /**
     * Return type for the findNumPages methods. Basically just the number
     * of pages and a queue containing cached pages.
     */
    public class PageScan {

        public int numPages;
        public LinkedList<HtmlPage> queue;

        PageScan() {
            numPages = 1;
            queue = new LinkedList<HtmlPage>();
        }
    }
}
