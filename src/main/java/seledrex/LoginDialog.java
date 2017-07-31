package seledrex;

//======================================================================================================================
// Imports
//======================================================================================================================

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;

//======================================================================================================================
// Login Dialog
//======================================================================================================================

/**
 * This class implements a login dialog for Furaffinity. A capcha image
 * will be displayed to the user, and then the user may type in the
 * username, password, and capcha message. A dialog message will be
 * displayed whether the login was successful or not.
 */
public class LoginDialog extends JDialog implements ActionListener {

    //==================================================================================================================
    // Properties
    //==================================================================================================================

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField capchaField;
    private JButton loginButton, cancelButton;
    private ImageIcon capchaImage;
    private HtmlPage faCapchaLoginPage;
    private App app;
    private boolean successful;

    //==================================================================================================================
    // Constructor
    //==================================================================================================================

    /**
     * Initializes the login dialog. The capcha image will automatically be
     * downloaded and displayed for the user.
     *
     * @param parent     parent frame
     * @param webClient  web client being used
     * @param app        parent application
     */
    LoginDialog(Frame parent, WebClient webClient, App app)
    {
        // Initialize dialog
        super(parent, "Login", true);
        this.app = app;

        try {
            // Get Furaffinity login page
            HtmlPage faDefaultLoginPage = webClient.getPage("https://www.furaffinity.net/login/");
            HtmlAnchor anchor = faDefaultLoginPage.getAnchorByHref("/login/?mode=imagecaptcha");
            faCapchaLoginPage = anchor.click();

            // Download capcha image
            HtmlImage capcha = faCapchaLoginPage.getHtmlElementById("captcha_img");
            capcha.saveAs(new File("capcha.jpg"));

        } catch (Exception e) {
            app.appendToLog("Error getting capcha image from Furaffinity's login page:\n" + getStackTrace(e));
            this.dispose();
        }

        try {
            // Read in capcha image
            BufferedImage img = ImageIO.read(new File("capcha.jpg"));
            capchaImage = new ImageIcon(img);
        } catch (Exception e) {
            app.appendToLog("Error reading capcha image:\n" + getStackTrace(e));
            this.dispose();
        }

        // Create panel for login dialog
        JPanel dialogPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        cs.insets = new Insets(3, 3, 3, 3);

        // Add username label
        JLabel usernameLabel = new JLabel("Username: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        dialogPanel.add(usernameLabel, cs);

        // Add username field
        usernameField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        dialogPanel.add(usernameField, cs);

        // Add password label
        JLabel passwordLabel = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        dialogPanel.add(passwordLabel, cs);

        // Add password field
        passwordField = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        dialogPanel.add(passwordField, cs);

        // Add capcha label
        JLabel capchaLabel = new JLabel("Capcha: ");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        dialogPanel.add(capchaLabel, cs);

        // Add capcha field
        capchaField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        dialogPanel.add(capchaField, cs);

        // Add capcha image
        JLabel capchaImageLabel = new JLabel(capchaImage);
        cs.gridx = 0;
        cs.gridy = 3;
        cs.gridwidth = 3;
        dialogPanel.add(capchaImageLabel, cs);

        // Initialize login button
        loginButton = new JButton("Login");
        loginButton.addActionListener(this);

        // Initialize cancel button
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);

        // Add panels to dialog box
        getContentPane().add(dialogPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.PAGE_END);

        // Pack and set settings
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /**
     * Picks up events created when a button is pressed. Based on
     * which button is pressed, a different action will be performed.
     *
     * @param e  event created
     */
    public void actionPerformed(ActionEvent e)
    {
        // Handles login button
        if (e.getSource() == loginButton) {
            if (authenticate(getUsername(), getPassword(), getCapcha())) {
                JOptionPane.showMessageDialog(
                        this,
                        "Welcome " + getUsername() + "! You have successfully logged in.",
                        "Login Success",
                        JOptionPane.INFORMATION_MESSAGE);
                successful = true;
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid username or password.",
                        "Login Failure",
                        JOptionPane.ERROR_MESSAGE);
                successful = false;
            }
        // Handles cancel button
        } else if (e.getSource() == cancelButton) {
            this.dispose();
        }
    }

    /**
     * Returns the text typed into the username field.
     *
     * @return  string containing text from username field
     */
    String getUsername() {
        return usernameField.getText().trim();
    }

    /**
     * Returns the text typed into the password field.
     *
     * @return  string containing text from password field
     */
    private String getPassword() {
        return new String(passwordField.getPassword());
    }

    /**
     * Returns the text typed into the capcha field.
     *
     * @return  string containing text from capcha field
     */
    private String getCapcha() {
        return capchaField.getText().trim();
    }

    /**
     * Returns whether the login was successful or not.
     *
     * @return  true on success, false otherwise
     */
    boolean isSuccessful() {
        return successful;
    }

    /**
     * Authenticates the login to Furaffinity by setting the user input to the
     * form provided by Furaffinity. The login button is then pressed, and then
     * successful login is verified by checked if the web client returned to
     * the homepage or not.
     *
     * @param username  username to login with
     * @param password  password to login with
     * @param capcha    capcha message
     * @return          true on success, false otherwise
     */
    private boolean authenticate(String username, String password, String capcha)
    {
        // Get the login form
        List<HtmlForm> formList = faCapchaLoginPage.getForms();
        HtmlForm form = formList.get(1);

        // Get the username and password fields
        HtmlTextInput usernameInput = form.getInputByName("name");
        HtmlPasswordInput passwordInput = form.getInputByName("pass");
        HtmlTextInput capchaInput = form.getInputByName("captcha");

        // Set all fields in HTML form
        usernameInput.setText(username);
        passwordInput.setText(password);
        capchaInput.setText(capcha);

        // Hit the login button
        HtmlPage afterLoginClick = null;
        try {
            HtmlInput loginButton = form.getInputByName("login");
            afterLoginClick = loginButton.click();
        } catch (Exception e) {
            app.appendToLog("Error authenticating login:\n" + getStackTrace(e));
            this.dispose();
        }

        // Check if we are returned to homepage
        if (afterLoginClick != null) {
            if (afterLoginClick.getUrl().toString().equals("http://www.furaffinity.net/")) {
                return true;
            }
        }

        return false;
    }
}
