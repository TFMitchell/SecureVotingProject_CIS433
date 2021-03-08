/**
 GUI.java

 Contains everything to do with the Client GUI

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.*;

public class ClientGUI
{
    public JFrame frame;

    public boolean ready;
    public boolean ended;
    public boolean counted;
    public boolean approved;
    //public String accessible; //what should this be doing?

    public ClientGUI()
    {
        //does not have a vote set yet
        ready = false; //set after confirmation of votes
        ended = false; //set after the process comes to an end
        counted = false; //set after the Client receives the votes

        approved = false; //dummy for password checking

        //accessible = "";

        //Create the frame.
        frame = new JFrame("Secure Voting App");

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setSize(1000, 800); //Size the frame.

        //select the first screen
        PasswordScreen();

    }

    //Initial Screen. Used to select Language (only english currently)
    public void WelcomeScreen() {

        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);


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

        //JButton b2=new JButton("Spanish Ballot");
        //b2.setBounds(400,150,200, 70);
        //b2.setFont(new Font("Tacoma",Font.PLAIN, 18));
        //frame.getContentPane().add(b2);

        //JButton b3=new JButton("French Ballot");
        //b3.setBounds(100,250,200, 70);
        //b3.setFont(new Font("Tacoma",Font.PLAIN, 18));
        //frame.getContentPane().add(b3);

        //what does this section do?
        JLabel label1 = new JLabel();
        label1.setBounds(10, 210, 200, 100);

        //once English is clicked, go to VoteScreen (method below this one)
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {

                int nothingSelected[] = new int[Client.officesAndCandidates.size()]; //the voting screen should be default have nothing selected, but takes selection as an argument, so we need to make an empty selection

                /**for (int i = 0; i < nothingSelected.length; i++) {
                    nothingSelected[i] = 0;
                }**/
                PasswordScreen(); //prompt for password
            }
        });



        //Show it.
        frame.setVisible(true);
    }


    //Meant for users to enter their pre-assigned password for identification
    public void PasswordScreen() {

        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);


        //Welcome screen instructions
        JLabel instructions = new JLabel("Enter your Secure Voting Password", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);


        JPasswordField pass = new JPasswordField(40);
        pass.setBounds(340,200,500, 40);
        pass.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(pass);
        //pass.setText("password");


        //single language option, English
        JButton cb = new JButton("Confirm");
        cb.setBounds(400, 650, 180, 50);
        cb.setFont(new Font("Tacoma", Font.BOLD, 18));
        frame.getContentPane().add(cb);

        int nothingSelected[] = new int[Client.officesAndCandidates.size()]; //the voting screen should be default have nothing selected, but takes selection as an argument, so we need to make an empty selection

        //once English is clicked, go to VoteScreen (method below this one)
        cb.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                char[] password = pass.getPassword();

                try {

                    if (Client.testPassword(password))
                        VoteScreen(nothingSelected); //proceed
                    else
                        PasswordScreen(); //try again
                }
                catch (Exception e) {System.out.printf("Error on 153: %s\n", e);}

            }
        });

        frame.setVisible(true);

    }

    //second Screen. Choose from the available candidates.
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
                if (selected[i] == n + 1) { //0 means no click, so + 1 for correct index
                    options[i][n].doClick();
                }

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


    //Third Screen. Used to confirm the correct candidates, or can reselect
    public void ConfirmScreen(int selected[]) throws Exception
    {

        //frame.removeAll();

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
                WaitScreen(selected);
            }
        });

        frame.setVisible(true);

    }


    public void WaitScreen(int selected[]){
        //Ready = true is the signal to the client to get the vote from 'accessible'
        //accessible = selected;
        ready = true;

        //I moved this because the idea with the wait screen was that it would repeatedly come up until the vote is counted
        //If the vote is cast here it could be counted multiple times.
        //Client.CastVote(selected);

        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        JLabel instructions = new JLabel("Please Wait While your Vote is Confirmed", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        frame.setVisible(true);

        //check when the vote is processed
        Timer delay = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Checkdone(selected);
            }
        });
        delay.setRepeats(false);
        delay.start();
    }

    //if vote has been counted, exit, else circle back to waiting screen
    public void Checkdone(int selected[]) {

        counted = true; //TODO: should be set by the Client

        if(counted == true){
            //the system sets counted == true after collecting and counting the vote
            ExitScreen();
        }else{
            //wait until client collects the vote
            WaitScreen(selected);
        }
    }

    //final screen, display final messages
    public void ExitScreen() {
        ready = false;
        ended = true;
        //in final version, should need the reset signal from the poll worker to restart

        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);


        JLabel instructions = new JLabel("Thank you for voting!", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel instructions2 = new JLabel("Receipt is printing...", SwingConstants.CENTER);
        instructions2.setBounds(400,150,500, 40);
        instructions2.setFont(new Font("Tacoma",Font.PLAIN, 20));

        JLabel instructions3 = new JLabel("Please Leave the voting area", SwingConstants.CENTER);
        instructions3.setBounds(100,270,500, 40);
        instructions3.setFont(new Font("Tacoma",Font.PLAIN, 20));
        frame.getContentPane().add(instructions);
        frame.getContentPane().add(instructions2);
        frame.getContentPane().add(instructions3);

        // are we showing the "receipt", the encryptedVote
        //I'm basing this on the numberfile video, with the idea the server lists all the receipts
        //to Kevin: I don't really know. I think maybe we shouldn't because of the coercion argument.

        //Show it.
        frame.setVisible(true);

    }

}
