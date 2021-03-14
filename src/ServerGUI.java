/**
 ServerGUI.java

 Contains everything to do with the Server GUI.

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ServerGUI
{
    public JFrame frame;

    public ServerGUI()
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

    //displayed screen the system is setting up (generating biprimal n)
    public void InitialScreen(){
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //Welcome screen instructions
        JLabel instructions = new JLabel("Please wait while the system initializes", SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);

        JLabel instructions2 = new JLabel("Generating n and passwords", SwingConstants.LEFT);
        instructions2.setBounds(450,150,500, 40);
        instructions2.setFont(new Font("Tacoma",Font.PLAIN, 24));
        frame.getContentPane().add(instructions2);

        frame.setVisible(true);
    }

    //server calls this when it's ready
    public void PressScreen(boolean doneSharingPasswords) {
        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //Welcome screen instructions. Lets user know if password sharing (with PasswordAuthority) is done
        String instructionsText;
        if (doneSharingPasswords)
            instructionsText = "Password Sharing Finished";
        else
            instructionsText = "Server Operation Panel";

        JLabel instructions = new JLabel(instructionsText, SwingConstants.LEFT);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));
        frame.getContentPane().add(instructions);

        JButton shareSKbutton = new JButton("Share Private Key");
        shareSKbutton.setBounds(250,150,270, 90);
        shareSKbutton.setFont(new Font("Tacoma",Font.PLAIN, 22));
        frame.getContentPane().add(shareSKbutton);

        //when secret key is requested to be shared, call the appropriate function in Server.java
        shareSKbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Server.shareDecryptionKey();
                    Thread.sleep(1500);
                } catch (Exception e){}

                Server.getResults(); //call function to prepare variable this GUI uses to get results
                TotalScreen(); //show the results as totals
            }
        });

        //button to share password stubs with the Password Authority for them to be given to voters
        JButton sharePasswordStubsButton = new JButton("Share Passwords");
        sharePasswordStubsButton.setBounds(550,150,270, 90);
        sharePasswordStubsButton.setFont(new Font("Tacoma",Font.PLAIN, 22));
        frame.getContentPane().add(sharePasswordStubsButton);

        //calls the appropriate function on Server.java
        sharePasswordStubsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Server.sharePasswordsWithPasswordAuth();
                    Thread.sleep(1500);
                } catch (Exception e){}

                PressScreen(true); //let user know the sharing is done
            }
        });

        frame.setLayout(null);
        frame.setVisible(true);
    }

    //lists the current totals of vote counts. Called by the share private key button on previous screen
    public void TotalScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));

        JLabel instructions = new JLabel("Voting Results", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel options[][] = new JLabel[Server.CandidateNamesByOffice.size()][20]; //in the form [office][candidate]
        int i = 0; //keep track of how many Offices we've listed so far
        for (String position : Server.CandidateNamesByOffice)//repeat this section for each office up for election
        {
            String names[] = position.split(", ");

            //the name of each office is the names at index 0
            JLabel office = new JLabel(names[0], SwingConstants.LEFT);
            office.setBounds(100, 130 + 150 * i, 200, 70);
            office.setFont(new Font("Tacoma", Font.PLAIN, 18));
            frame.getContentPane().add(office);

            //get totals for percentages to work
            double total = 0.0;
            for (int rapid = 0; rapid < Server.candidate_counts[i].length; rapid++)
            {
                total += Server.candidate_counts[i][rapid];
                System.out.printf("candidate count %s\n", Server.candidate_counts[i][rapid]);
            }

            //for each candidate in this office, display results
            for (int n = names.length - 1; n > 0; n--)
            {
                //add the candidate names to labels
                options[i][n - 1] = new JLabel(names[n], SwingConstants.LEFT);
                options[i][n - 1].setBounds(100 + 225 * (n - 1), 180 + 150 * i, 150, 40);
                options[i][n - 1].setFont(new Font("Tacoma", Font.PLAIN, 16));

                //calculate the percentage and add to corresponding labels
                double quick =  Server.candidate_counts[i][n-1]/total*100;
                String temp = String.valueOf(quick).concat("%");
                options[i][10 + n - 1] = new JLabel(temp, SwingConstants.LEFT);
                options[i][10 + n - 1].setBounds(100 + 225 * (n - 1), 230 + 150 * i, 150, 40);
                options[i][10 + n - 1].setFont(new Font("Tacoma", Font.PLAIN, 16));

                frame.getContentPane().add(options[i][n - 1]);
                frame.getContentPane().add(options[i][10 + n - 1]);
            }
            i++;
        }

        frame.setLayout(null);
        frame.setVisible(true);
    }
}
