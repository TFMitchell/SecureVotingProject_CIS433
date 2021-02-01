/**
 GUI.java

 Contains everything to do with the Client GUI

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ClientGUI
{

    public ClientGUI()
    {

        //Create the frame.
        JFrame frame = new JFrame("Secure Voting App");

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //After Voting Screen
        /*  After receipt
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
        frame.getContentPane().add(instructions3);*/

        //Confirmation Screen
        /*  Confirmation Page
        JLabel instructions = new JLabel("Please review the information before Submitting", SwingConstants.LEFT);
        instructions.setBounds(130,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        JLabel votes1 = new JLabel("One listed name per vote", SwingConstants.LEFT);
        votes1.setBounds(100,150,200, 70);
        votes1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JButton b=new JButton("Make Changes");
        b.setBounds(100,650,180, 50);
        b.setFont(new Font("Tacoma",Font.BOLD, 18));

        JButton b2=new JButton("Confirm");
        b2.setBounds(400,650,180, 50);
        b2.setFont(new Font("Tacoma",Font.BOLD, 18));

        frame.getContentPane().add(instructions);
        frame.getContentPane().add(votes1);
        frame.getContentPane().add(b);
        frame.getContentPane().add(b2);*/

        //Voting Page

        JLabel instructions = new JLabel("Choose a candidate from each position", SwingConstants.LEFT);
        instructions.setBounds(150,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));

        //repeat this section for each opening
        JLabel section1 = new JLabel("1) Senator", SwingConstants.LEFT);
        section1.setBounds(100,130,200, 70);
        section1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JRadioButton option1 = new JRadioButton("Inn C. Umbent");
        JRadioButton option2 = new JRadioButton("Sal Enger");
        JRadioButton option3 = new JRadioButton("Noah Chance");
        option1.setBounds(100, 180, 150, 40);
        option1.setFont(new Font("Tacoma",Font.PLAIN, 16));
        option2.setBounds(275, 180, 150, 40);
        option2.setFont(new Font("Tacoma",Font.PLAIN, 16));
        option3.setBounds(450, 180, 150, 40);
        option3.setFont(new Font("Tacoma",Font.PLAIN, 16));

        ButtonGroup group = new ButtonGroup();

        group.add(option1);
        group.add(option2);
        group.add(option3);

        JButton b2=new JButton("Confirm");
        b2.setBounds(400,650,180, 50);
        b2.setFont(new Font("Tacoma",Font.BOLD, 18));

        frame.getContentPane().add(b2);

        frame.getContentPane().add(instructions);
        frame.getContentPane().add(section1);

        frame.getContentPane().add(option1);
        frame.getContentPane().add(option2);
        frame.getContentPane().add(option3);


        //Welcome
        /*   Start Menu
        JLabel instructions = new JLabel("Welcome! Please Select Your Language", SwingConstants.LEFT);
        instructions.setBounds(130,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 24));

        JButton b=new JButton("English Ballot");
        b.setBounds(100,150,200, 70);
        b.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JButton b2=new JButton("Spanish Ballot");
        b2.setBounds(400,150,200, 70);
        b2.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JButton b3=new JButton("French Ballot");
        b3.setBounds(100,250,200, 70);
        b3.setFont(new Font("Tacoma",Font.PLAIN, 18));



        JLabel label1 = new JLabel();
        label1.setBounds(10, 210, 200, 100);

        frame.getContentPane().add(b);
        frame.getContentPane().add(b2);
        frame.getContentPane().add(b3);
        frame.getContentPane().add(label1);
        frame.getContentPane().add(instructions);

        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                //next screen
            }
        }); */



        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.


        //Show it.
        frame.setVisible(true);
    }
}
