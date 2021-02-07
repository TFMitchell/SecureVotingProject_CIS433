/**
 GUI.java

 Contains everything to do with the Client GUI

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

public class ClientGUI
{
    public JFrame frame;
    private ArrayList<String> candidates;
    public boolean ready;
    public boolean ended;
    public boolean counted;
    public String accessible;

    public ClientGUI()
    {
        //does not have a vote set yet
        ready = false; //set after confirmation of votes
        ended = false; //set after the process comes to an end
        counted = false; //set after the Client receives the votes

        accessible = "";

        //temporary list of candidates
        candidates = new ArrayList<String>();
        candidates.add("Inn C. Umbent");
        candidates.add("Chel Enger");




        //Create the frame.
        frame = new JFrame("Secure Voting App");

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        //select the first screen
        WelcomeScreen();

        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.



        //Show it.
        frame.setVisible(true);
    }

    //use this to set the list of candidates from the main program
    public void setCandidates(ArrayList<String> newList) {
        candidates = newList;

    }

    //Initial Screen. Used to select Language (only english currently)
    public void WelcomeScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JLabel instructions = new JLabel("Welcome! Please Select Your Language", SwingConstants.LEFT);
        instructions.setBounds(130,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));

        JButton b=new JButton("English Ballot");
        b.setBounds(100,150,200, 70);
        b.setFont(new Font("Tacoma",Font.PLAIN, 18));

        //JButton b2=new JButton("Spanish Ballot");
        //b2.setBounds(400,150,200, 70);
        //b2.setFont(new Font("Tacoma",Font.PLAIN, 18));

        //JButton b3=new JButton("French Ballot");
        //b3.setBounds(100,250,200, 70);
        //b3.setFont(new Font("Tacoma",Font.PLAIN, 18));



        JLabel label1 = new JLabel();
        label1.setBounds(10, 210, 200, 100);

        frame.getContentPane().add(b);
        //frame.getContentPane().add(b2);
        //frame.getContentPane().add(b3);
        frame.getContentPane().add(label1);
        frame.getContentPane().add(instructions);

        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                VoteScreen("");
            }
        });



        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.


        //Show it.
        frame.setVisible(true);

    }

    //second Screen. Choose from the available candidates.
    public void VoteScreen(String selected){
        //frame.removeAll();
        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //frame.removeAll();

        JLabel instructions = new JLabel("Choose a candidate from each position", SwingConstants.LEFT);
        instructions.setBounds(150,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        //Currently, this is based on 1 job being votes on, and 2 candidates for the job
        //repeat this section for each opening
        JLabel section1 = new JLabel("1) Senator", SwingConstants.LEFT);
        section1.setBounds(100,130,200, 70);
        section1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JRadioButton option1 = new JRadioButton(candidates.get(0));
        JRadioButton option2 = new JRadioButton(candidates.get(1));
        option1.setBounds(100, 180, 150, 40);
        option1.setFont(new Font("Tacoma",Font.PLAIN, 16));
        option2.setBounds(375, 180, 150, 40);
        option2.setFont(new Font("Tacoma",Font.PLAIN, 16));

        ButtonGroup group = new ButtonGroup();

        group.add(option1);
        group.add(option2);

        if(!selected.equals("")) {


            if(Integer.parseInt(selected)== 1){
                option1.doClick();
            }else if(Integer.parseInt(selected)== 2){
                option2.doClick();
            }
        }

        JButton b2=new JButton("Confirm");
        b2.setBounds(400,650,180, 50);
        b2.setFont(new Font("Tacoma",Font.BOLD, 18));

        frame.getContentPane().add(b2);

        frame.getContentPane().add(instructions);
        frame.getContentPane().add(section1);

        frame.getContentPane().add(option1);
        frame.getContentPane().add(option2);

        b2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String send = "";
                if(option1.isSelected()){
                    send = "1";
                }else if(option2.isSelected()){
                    send = "2";
                }
                ConfirmScreen(send);
            }
        });

        frame.setLayout(null);
        frame.setSize(715, 800);


        frame.setVisible(true);
    }






    //Third Screen. Used to confirm the correct candidates, or can reselect
    public void ConfirmScreen(String selected) {

        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel instructions = new JLabel("Please review the information before Submitting", SwingConstants.LEFT);
        instructions.setBounds(130,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel section1 = new JLabel("1) Senator", SwingConstants.LEFT);
        section1.setBounds(150,130,200, 70);
        section1.setFont(new Font("Tacoma",Font.PLAIN, 18));


        JLabel candidate1 = new JLabel( "No Candidate Selected", SwingConstants.LEFT);
        if(!selected.equals("")){

            candidate1.setText(candidates.get(Integer.parseInt(selected) - 1));
        }
        candidate1.setBounds(350,130,200, 70);
        candidate1.setFont(new Font("Tacoma",Font.PLAIN, 18));


        JButton b=new JButton("Make Changes");
        b.setBounds(100,650,180, 50);
        b.setFont(new Font("Tacoma",Font.BOLD, 18));

        JButton b2=new JButton("Confirm");
        b2.setBounds(400,650,180, 50);
        b2.setFont(new Font("Tacoma",Font.BOLD, 18));

        frame.getContentPane().add(instructions);
        frame.getContentPane().add(section1);
        frame.getContentPane().add(candidate1);
        frame.getContentPane().add(b);
        frame.getContentPane().add(b2);

        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.
        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                VoteScreen(selected);
            }
        });

        b2.addActionListener(new ActionListener() {

            @Override

            public void actionPerformed(ActionEvent arg0) {
                WaitScreen(selected);
            }
        });

        //Show it.
        frame.setVisible(true);

    }


    public void WaitScreen(String selected){
        //Ready = true is the signal to the client to get the vote from 'accessible'
        accessible = selected;
        ready = true;

        frame.setContentPane(new JPanel(new BorderLayout()));

        JLabel instructions = new JLabel("Please Wait While your Vote is Confirmed", SwingConstants.CENTER);
        instructions.setBounds(100,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));
        frame.getContentPane().add(instructions);

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setSize(715, 800);


        frame.setVisible(true);

        Timer delay = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Checkdone();
            }
        });
        delay.setRepeats(false);
        delay.start();
    }

    public void Checkdone() {


        if(counted == true){
            //the system sets counted == true after collecting and counting the vote
            ExitScreen();
        }else{
            //wait until client collects the vote
            counted = true; //remove this later
            WaitScreen(accessible);
        }
    }

    //final screen, display final messages
    public void ExitScreen() {
        ready = false;
        ended = true;
        //in final version, should need the reset signal from the poll worker to restart

        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        JLabel instructions = new JLabel("Thank you for voting!", SwingConstants.CENTER);
        instructions.setBounds(100,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel instructions2 = new JLabel("Receipt is printing...", SwingConstants.CENTER);
        instructions2.setBounds(100,150,500, 40);
        instructions2.setFont(new Font("Tacoma",Font.PLAIN, 20));

        JLabel instructions3 = new JLabel("Please Leave the voting area", SwingConstants.CENTER);
        instructions3.setBounds(100,270,500, 40);
        instructions3.setFont(new Font("Tacoma",Font.PLAIN, 20));
        frame.getContentPane().add(instructions);
        frame.getContentPane().add(instructions2);
        frame.getContentPane().add(instructions3);



        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.


        //Show it.
        frame.setVisible(true);

    }

}
