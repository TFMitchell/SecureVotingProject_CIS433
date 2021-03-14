/**
 GUI.java

 Contains everything to do with the Client GUI.

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;

public class ClientGUI
{
    public JFrame frame;
    public static boolean counted;

    public ClientGUI()
    {
        //Create the frame.
        frame = new JFrame("Secure Voting App");

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Size the frame.
        frame.setSize(1200, 800);

        //select the first screen
        InitialScreen();
    }

    //displayed screen the system is set up, Client.java has the call to advance to welcome screen
    public void InitialScreen(){
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //Welcome screen instructions
        JLabel instructions = new JLabel("Please wait while the system initializes", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);

        frame.setVisible(true);
    }

    //Initial Screen. Used to select Language (only english currently). Called by Client.java's class when ready
    public void WelcomeScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);
        counted = false;

        //Welcome screen instructions
        JLabel instructions = new JLabel("Welcome! Please Select Your Language", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);

        //single language option, English
        JButton b = new JButton("English Ballot");
        b.setBounds(250,150,270, 90);
        b.setFont(new Font("Tacoma",Font.PLAIN, 22));
        frame.getContentPane().add(b);

        //once English is clicked, go to PasswordScreen (method below this one)
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                PasswordScreen(false); //prompt for password, without password incorrect text
            }
        });

        frame.setVisible(true);
    }

    //Meant for users to enter their pre-assigned password for identification
    public void PasswordScreen(boolean failure) {
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //Welcome screen instructions
        JLabel instructions = new JLabel("Enter your Secure Voting Password", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);

        //only display this if they got their password wrong last time
        if(failure){
            JLabel success = new JLabel("Password Incorrect: please try again", SwingConstants.LEFT);
            success.setBounds(400,130,500, 40);
            success.setFont(new Font("Tacoma",Font.BOLD, 24));
            frame.getContentPane().add(success);
        }

        //password text field
        JPasswordField pass = new JPasswordField(40);
        pass.setBounds(340,200,500, 40);
        pass.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(pass);

        JButton cb = new JButton("Confirm");
        cb.setBounds(400, 650, 180, 50);
        cb.setFont(new Font("Tacoma", Font.BOLD, 18));
        frame.getContentPane().add(cb);

        int nothingSelected[] = new int[Client.officesAndCandidates.size()]; //the voting screen should be default have nothing selected, but takes selection as an argument, so we need to make an empty selection

        cb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                char[] password = pass.getPassword();

                try {
                    if (Client.testPassword(password))
                        VoteScreen(nothingSelected); //proceed to the vote screen, presenting user with voting options
                    else
                        PasswordScreen(true); //try again: display this screen again, but with "password incorrect" text
                }
                catch (Exception e) {}
            }
        });

        frame.setVisible(true);
    }

    //User chooses from the available candidates for each office/measure.
    public void VoteScreen(int selected[]){
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //instructions
        JLabel instructions = new JLabel("Choose a candidate from each position", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        JRadioButton options[][] = new JRadioButton[Client.officesAndCandidates.size()][10]; //array of an array of radio buttons for the candidates. First dimension is for the particular office

        //lay out the voting options
        int i = 0; //keep track of how many Offices we've listed so far
        for (HashMap.Entry<String, ArrayList<String>> entry : Client.officesAndCandidates.entrySet()) //repeat this section for each office up for election
        {
            //display the name of the office e.g. "President"
            JLabel office = new JLabel(entry.getKey(), SwingConstants.LEFT);
            office.setBounds(100, 130 + 100 * i, 200, 70);
            office.setFont(new Font("Tacoma", Font.PLAIN, 18));
            frame.getContentPane().add(office);

            ButtonGroup group = new ButtonGroup(); //radio buttons for the candidates for each office

            int n = 0; //keep track of how many candidates for this office we've listed
            for (String candidate : entry.getValue()) //for each candidate in this office
            {
                //add their name to the radio options
                options[i][n] = new JRadioButton(candidate);
                options[i][n].setBounds(100 + 225 * n, 180 + 100 * i, 150, 40);
                options[i][n].setFont(new Font("Tacoma", Font.PLAIN, 16));
                group.add(options[i][n]);
                frame.getContentPane().add(options[i][n]);

                //click the candidate they have already selected (if they want to make changes, their last attempt should be auto-filled)
                if (selected[i] == n + 1) //0 means no click, so + 1 for correct index
                    options[i][n].doClick();

                n++;
            }
            i++;
        }

        //confirmation button
        JButton cb = new JButton("Confirm");
        cb.setBounds(400, 650, 180, 50);
        cb.setFont(new Font("Tacoma", Font.BOLD, 18));
        frame.getContentPane().add(cb);

        //confirmation button's action
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0)
            { //this function looks at what's been clicked

                int selected[] = new int[Client.officesAndCandidates.size()]; //this is the value that will be passed on

                for (int i = 0; i < selected.length; i++) //for each office
                {
                    for (int c = 0; c < options[i].length; c++) //for each candidate in each office
                    {
                        if (options[i][c] == null) //if there isn't a candidate at this index, continue
                            continue;

                        else if (options[i][c].isSelected()) //else if that radio button is selected, take note of it. Since 0 is no candidate selected, it's c + 1
                            selected[i] = c + 1;
                    }
                }
                try
                {ConfirmScreen(selected);} //go to confirmation screen
                catch(Exception e) {}
            }
        });

        frame.setVisible(true);
    }

    //Used to confirm the correct candidates (inferred from selected[]), or user can reselect
    public void ConfirmScreen(int selected[]) throws Exception {
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //instructions for this page
        JLabel instructions = new JLabel("Please review the information before Submitting", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        //lay out the list of offices and their selections
        int i = 0; //keep track of how many Offices we've listed so far
        for (HashMap.Entry<String, ArrayList<String>> entry : Client.officesAndCandidates.entrySet())//repeat this section for each office up for election
        {
            //the name of each office
            JLabel office = new JLabel(entry.getKey(), SwingConstants.LEFT);
            office.setBounds(150, 130 + 70 * i, 200, 70);
            office.setFont(new Font("Tacoma", Font.PLAIN, 18));
            frame.getContentPane().add(office);

            //the candidate we selected for this office (or lack thereof)
            JLabel candidate;
            if (selected[i] == 0)
                candidate = new JLabel("No Candidate Selected", SwingConstants.LEFT);

            else
                candidate = new JLabel(entry.getValue().get(selected[i] - 1), SwingConstants.LEFT);

            candidate.setBounds(350, 130 + 70 * i, 200, 70);
            candidate.setFont(new Font("Tacoma", Font.PLAIN, 18));
            frame.getContentPane().add(candidate);

            i++;
        }

        //laying out Make Changes and Confirm buttons now
        JButton mkChgsB = new JButton("Make Changes");
        mkChgsB.setBounds(100,650,180, 50);
        mkChgsB.setFont(new Font("Tacoma",Font.BOLD, 18));
        frame.getContentPane().add(mkChgsB);

        JButton cfrmB = new JButton("Confirm");
        cfrmB.setBounds(400,650,180, 50);
        cfrmB.setFont(new Font("Tacoma",Font.BOLD, 18));
        frame.getContentPane().add(cfrmB);

        //go back and re-vote with selections used last time pre-selected
        mkChgsB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                VoteScreen(selected);
            }
        });

        //advance to waiting screen (below)
        cfrmB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {Client.CastVote(selected);}
                catch(Exception e) {}
                WaitScreen();
            }
        });
        frame.setVisible(true);
    }

    //Waiting while the vote is encrypted and transmitted to the servers
    public void WaitScreen(){
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        JLabel instructions = new JLabel("Please Wait While your Vote is Confirmed", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        frame.setVisible(true);

        //check when if the vote was processed in 3 seconds
        Timer delay = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Checkdone();
            }
        });
        delay.setRepeats(false);
        delay.start();
    }

    //if vote has been counted, exit. Else, circle back to waiting screen
    public void Checkdone() {
        if(counted){
            //the system sets counted == true after collecting and counting the vote
            ExitScreen();
        }else{
            //wait until client collects the vote
            WaitScreen();
        }
    }

    //final screen, display a thank you for a few seconds, then reset the machine for someone else
    public void ExitScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        JLabel instructions = new JLabel("Thank you for voting!", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        JLabel instructions2 = new JLabel("Please Leave the voting area", SwingConstants.CENTER);
        instructions2.setBounds(100,470,500, 40);
        instructions2.setFont(new Font("Tacoma",Font.PLAIN, 20));
        frame.getContentPane().add(instructions2);

        frame.setVisible(true);

        Timer delay = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WelcomeScreen();
            }
        });
        delay.setRepeats(false);
        delay.start();
    }
}