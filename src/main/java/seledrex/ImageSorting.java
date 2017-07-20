package seledrex;

//======================================================================================================================
// Imports
//======================================================================================================================

import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
class ImageSorting
{
    //==================================================================================================================
    // Properties
    //==================================================================================================================

    private List<Pair<String, String>> files; // Holds a list of all the files to sort
    private Map<String, List<Pair<String, String>>> gallery; // Maps artists to their respective artwork
    private List<File[]> inputFolders; // Stores the input folders supplied by user
    private File outputFolder; // Stores the output folder supplied by user

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    /**
     * Initializes the data structures and variables for the
     * image sorter.
     */
    ImageSorting()
    {
        files = new ArrayList<Pair<String, String>>();
        gallery = new HashMap<String, List<Pair<String, String>>>();
        inputFolders = new ArrayList<File[]>();
        outputFolder = null;
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
     * Clears all the input folders currently stored.
     */
    void clearInputFolders() { inputFolders.clear(); }

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
     *
     * @param log  log to print to
     */
    void sortImages(JTextArea log)
    {
        // Check if an input folder was given
        if (inputFolders.isEmpty()) {
            log.append("Input folder(s) not added\n");
            return;
        }

        // Check if an output folder was given
        if (outputFolder == null) {
            log.append("Output folder not set\n");
            return;
        }

        // Loops through each of the input folders
        for (File[] inputFolder : inputFolders)
        {
            if (inputFolder != null)
            {
                // Add each file inside of the input folder
                for (File file : inputFolder) {
                    this.files.add( new Pair<String, String>(file.getName(), file.getAbsolutePath()));
                }
            }

            // Create regular expression
            Pattern pattern = Pattern.compile("(\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d.)([^_]*)(_)(.*)");

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
            writer = new PrintWriter(outputFolder.getAbsoluteFile() + "\\output.txt");

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
            e.printStackTrace();
        } finally {
            if (writer != null) writer.close();
        }

        // Loop through all the artists
        for (String artist : artists)
        {
            // Get the artwork for the artist
            List<Pair<String, String>> artwork = gallery.get(artist);

            // Open the directory for the artist
            File artistDir = new File(outputFolder.getAbsoluteFile() + "\\" + artist);

            // If the directory does not exist, create it
            if (!artistDir.exists())
            {
                if (artistDir.mkdir()) {
                    log.append("Made new directory: " + artistDir.getAbsolutePath() + "\n");
                }
            }

            // Loop through all the files belonging to the artist
            for (Pair<String, String> filename : artwork)
            {
                // Open file inside artist directory
                File check = new File(artistDir.getAbsolutePath() + "\\" + filename.getKey());

                // If the file does not exist, copy it
                if (!check.exists())
                {
                    try {
                        FileUtils.copyFile(new File(filename.getValue()),
                                new File(artistDir.getAbsolutePath() + "\\" + filename.getKey()));
                        log.append("Copied: " + filename.getKey() + "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
