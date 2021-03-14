/**
 ServerGUI.java

 Contains everything to do with the Server GUI

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
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

        frame.setSize(1200, 800); //Size the frame.

        //select the first screen
        InitialScreen();
    }


    //displayed screen the system is set up
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


    //who exactly is pressing the button here? I'm confused
    //to Kevin: the poll worker administrator will press the button to distribute the private key when the polls close
    public void PressScreen(boolean doneSharingPasswords) {

        frame.setContentPane(new JPanel(new BorderLayout()));
        frame.setLayout(null);

        //Welcome screen instructions

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

        shareSKbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Server.shareDecryptionKey();
                    Thread.sleep(1500);
                } catch (Exception e){}

                Server.getResults(); //get results
                TotalScreen(); //show the results as totals
            }
        });

        JButton sharePasswordStubsButton = new JButton("Share Passwords");
        sharePasswordStubsButton.setBounds(550,150,270, 90);
        sharePasswordStubsButton.setFont(new Font("Tacoma",Font.PLAIN, 22));
        frame.getContentPane().add(sharePasswordStubsButton);

        sharePasswordStubsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Server.sharePasswordsWithPasswordAuth();
                    Thread.sleep(1500);
                } catch (Exception e){}

                PressScreen(true);
            }
        });

        frame.setLayout(null);
        frame.setVisible(true);
    }

    //lists the current totals of vote counts
    public void TotalScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));


        JLabel instructions = new JLabel("Voting Results", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel options[][] = new JLabel[Server.CandidateNamesByOffice.size()][20];

        int i = 0; //keep track of how many Offices we've listed so far
        for (String position : Server.CandidateNamesByOffice)//repeat this section for each office up for election
        {
            String names[] = position.split(", ");

            //the name of each office
            JLabel office = new JLabel(names[0], SwingConstants.LEFT);
            office.setBounds(100, 130 + 150 * i, 200, 70);
            office.setFont(new Font("Tacoma", Font.PLAIN, 18));
            frame.getContentPane().add(office);

            double total = 0.0;
            for (int rapid = 0; rapid < Server.candidate_counts[i].length; rapid++)
            {
                total += Server.candidate_counts[i][rapid];
                System.out.printf("candidate count %s\n", Server.candidate_counts[i][rapid]);
            }

            for (int n = names.length - 1; n > 0; n--) //for each candidate in this office
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

        JButton b=new JButton("Voting Confirmation");
        b.setBounds(200,650,280, 50);
        b.setFont(new Font("Tacoma",Font.BOLD, 18));


        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ListScreen();
            }
        });

        frame.getContentPane().add(instructions);
        frame.getContentPane().add(b);

        frame.setLayout(null);

        frame.setVisible(true);
    }


    //lists all votes recieved with their 'name' and the encoded vote
    //non-functional, static label
    //to Kevin: I don't think we really need this. I'm sorry I wasn't more clear at the start, but I also didn't really know how this stuff would work either
    public void ListScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));


        JLabel instructions = new JLabel("Encoded Vote Lookup", SwingConstants.CENTER);
        instructions.setBounds(400,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel title1 = new JLabel("Vote ID", SwingConstants.CENTER);
        JLabel title2 = new JLabel("Encoded Vote", SwingConstants.CENTER);
        title1.setBounds(150, 120, 150, 40);
        title1.setFont(new Font("Tacoma",Font.PLAIN, 20));
        title2.setBounds(400, 120, 150, 40);
        title2.setFont(new Font("Tacoma",Font.PLAIN, 20));

        JLabel vote1 = new JLabel("Location-Vote #", SwingConstants.CENTER);
        JLabel code1 = new JLabel("123049429", SwingConstants.CENTER);
        vote1.setBounds(150, 180, 150, 40);
        vote1.setFont(new Font("Tacoma",Font.PLAIN, 18));
        code1.setBounds(400, 180, 150, 40);
        code1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        frame.getContentPane().add(instructions);

        frame.getContentPane().add(title1);
        frame.getContentPane().add(title2);

        frame.getContentPane().add(vote1);
        frame.getContentPane().add(code1);

        JButton b=new JButton("Vote Totals");
        b.setBounds(200,650,280, 50);
        b.setFont(new Font("Tacoma",Font.BOLD, 18));

        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TotalScreen();
            }
        });

        frame.getContentPane().add(b);

        frame.setLayout(null);

        frame.setVisible(true);
    }

}
