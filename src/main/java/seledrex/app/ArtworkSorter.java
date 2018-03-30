package seledrex.app;

//======================================================================================================================
// Imports
//======================================================================================================================

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

//======================================================================================================================
// Image Sorting
//======================================================================================================================

/**
 * Implements the backend of the GUI, where all the image sorting
 * actually occurs. All the files inside the specified directories
 * are read into an array. Then the filenames and paths are read
 * into the program. Afterwards, a regular expression is used on
 * each of the filenames to check if it can be sorted. If a match is
 * found, then the artist name is extracted, and the artist is added
 * to a hash map that keeps track of all the artists found. Files
 * are the associated with each artist, where each artist has a list
 * of files. An output.txt file is created to show all the files
 * sorted, and then the images are copied to their respective
 * directories. If an artist does not have a directory made for him
 * or her, then a new one is created. If the copied file already exists,
 * then it will not be copied.
 *
 * @author Eric Auster
 */
class ArtworkSorter
{
    //==================================================================================================================
    // Properties
    //==================================================================================================================

    private List<Pair<String, String>> files; // Holds a list of all the files to sort
    private Map<String, List<Pair<String, String>>> gallery; // Maps artists to their respective artwork
    private List<File[]> inputFolders; // Stores the input folders supplied by user
    private File outputFolder; // Stores the output folder supplied by user
    private static String[] validFormats = {"jpg", "jpeg", "png", "gif", "swf", "mid", "wav", "mp3", "mpeg", "txt", "docx"};
    private final App app;

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    /**
     * Initializes the data structures and variables for the
     * image sorter.
     */
    ArtworkSorter(App app)
    {
        files = new ArrayList<Pair<String, String>>();
        gallery = new HashMap<String, List<Pair<String, String>>>();
        inputFolders = new ArrayList<File[]>();
        outputFolder = null;
        this.app = app;
    }

    //==================================================================================================================
    // Methods
    //==================================================================================================================

    /**
     * Adds an input folder to the input folder list. An array of
     * files is created immediately.
     *
     * @param path  path to input folder
     */
    void addInputFolder(String path) { inputFolders.add(new File(path).listFiles()); }

    /**
     * Sets the output folder that all files will be saved to.
     *
     * @param outputFolder  path to output folder
     */
    void setOutputFolder(File outputFolder) { this.outputFolder = outputFolder; }

    //==================================================================================================================
    // Sorting
    //==================================================================================================================

    /**
     * Where all the fun really happens! Each individual file is parsed
     * using the regular expression, and then are properly sorted and
     * copied to their proper locations.
     */
    void sortInputFolders()
    {
        // Check if an input folder was given
        if (inputFolders.isEmpty()) {
            app.appendToLog("Input folder(s) not added\n");
            return;
        }

        // Check if an output folder was given
        if (outputFolder == null) {
            app.appendToLog("Output folder not set\n");
            return;
        }

        // Loops through each of the input folders
        for (File[] inputFolder : inputFolders)
        {
            if (inputFolder != null)
            {
                // Add each file inside of the input folder
                for (File file : inputFolder) {
                    // Check if file extension is valid. Add file if valid, log error if not.
                    if (formatIsValid(FilenameUtils.getExtension(file.getName()))) {
                        this.files.add(Pair.of(file.getName(), file.getAbsolutePath()));
                    }
                    else {
                        app.appendToLog("Invalid file format for file '" + file.getAbsolutePath() + "'\n");
                    }
                }
            }

            // Create regular expression
            Pattern pattern = Pattern.compile("(\\d+.)([^_]*)(_)(.*)");

            // Loop through each of the files
            for (Pair<String, String> file : this.files)
            {
                // Use regular expression
                Matcher m = pattern.matcher(file.getKey());

                // Pattern is found
                if (m.find())
                {
                    // Extract artist
                    String artist = m.group(2);

                    // Check if the artist is in the gallery yet
                    if (!gallery.containsKey(artist)) {
                        gallery.put(artist, new ArrayList<Pair<String, String>>());
                    }

                    // Add the file to artist
                    gallery.get(artist).add(file);
                }
                // Pattern not found
                else
                {
                    // Check if unsorted has been created
                    if (!gallery.containsKey("unsorted")) {
                        gallery.put("unsorted", new ArrayList<Pair<String, String>>());
                    }

                    // Add the file to unsorted
                    gallery.get("unsorted").add(file);
                }
            }
        }

        // Grab all the artists found
        Set<String> artists = gallery.keySet();
        PrintWriter writer = null;

        try {
            // Create a print writer
            writer = new PrintWriter(outputFolder.getAbsolutePath() + "/output.txt");

            // Loop through each artist
            for (String artist : artists)
            {
                // Get the artwork for the artist
                List<Pair<String, String>> artwork = gallery.get(artist);

                // Print the artist to file
                writer.format(artist + ":\n");

                int num = 1;

                // Loop through all the files belonging to the artist
                for (Pair<String, String> file : artwork)
                {
                    // Write the number and then filename to file
                    writer.format("\t" + num + ". " + file.getKey() + "\n");
                    num++;
                }
                writer.format("\n");
            }

        // Catch exceptions
        } catch (Exception e) {
            app.appendToLog("Error writing output.txt:\n" + getStackTrace(e));
        } finally {
            if (writer != null) writer.close();
        }

        // Loop through all the artists
        for (String artist : artists)
        {
            // Get the artwork for the artist
            List<Pair<String, String>> artwork = gallery.get(artist);

            // Open the directory for the artist
            File artistDir = new File(outputFolder.getAbsolutePath() + "/" + artist);

            // If the directory does not exist, create it
            if (!artistDir.exists())
            {
                if (artistDir.mkdir()) {
                    app.appendToLog("Made new directory: " + artistDir.getAbsolutePath() + "\n");
                }
            }

            // Loop through all the files belonging to the artist
            for (Pair<String, String> filename : artwork)
            {
                // Open file inside artist directory
                File check = new File(artistDir.getAbsolutePath() + "/" + filename.getKey());

                // If the file does not exist, copy it
                if (!check.exists())
                {
                    try {
                        FileUtils.copyFile(new File(filename.getValue()),
                                new File(artistDir.getAbsolutePath() + "/" + filename.getKey()));
                        app.appendToLog("Copied: " + filename.getKey() + "\n");
                    } catch (Exception e) {
                        app.appendToLog("Error copying files:\n" + getStackTrace(e));
                    }
                }
            }
        }
    }

    public void sortFile(File file)
    {
        // Create regular expression
        Pattern pattern = Pattern.compile("(\\d{10}.)([^_]*)(_)(.*)");

        // Use regular expression
        Matcher m = pattern.matcher(file.getName());

        String artist;

        // Pattern is found
        if (m.find())
        {
            // Extract artist
            artist = m.group(2);
        }
        // Pattern not found
        else
        {
            app.appendToLog("Incorrect format for file: " + file.getName() + "\n");
            return;
        }

        // Open the directory for the artist
        File artistDir = new File(outputFolder.getAbsolutePath() + "/" + artist);

        // If the directory does not exist, create it
        if (!artistDir.exists())
        {
            if (artistDir.mkdir()) {
                app.appendToLog("Made new directory: " + artistDir.getAbsolutePath() + "\n");
            }
        }

        // Open file inside artist directory
        File check = new File(artistDir.getAbsolutePath() + "/" + file.getName());

        // If the file does not exist, copy it
        if (!check.exists())
        {
            try {
                FileUtils.moveFile(new File(file.getAbsolutePath()),
                        new File(artistDir.getAbsolutePath() + "/" + file.getName()));
                app.appendToLog("Copied: " + file.getName() + "\n");
            } catch (Exception e) {
                app.appendToLog("Error copying files:\n" + getStackTrace(e));
            }
        }
    }

    /**
     * Helper function for sort. Returns true if given extension is valid.
     *
     * @param extension  extension to compare to valid extensions
     * @return           true if valid format, false otherwise
     */
    private static boolean formatIsValid(String extension)
    {
        for (String format : validFormats) {
            if (extension.toLowerCase().equals(format)) {
                return true;
            }
        }

        return false;
    }
}
