/**
 ServerGUI.java

 Contains everything to do with the Server GUI

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


        //select the first screen
        TotalScreen();

        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.



        //Show it.
        frame.setVisible(true);
    }


    public void ListScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel instructions = new JLabel("Encoded Vote Lookup", SwingConstants.CENTER);
        instructions.setBounds(100,80,500, 40);
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
        frame.setSize(715, 800);

        //Size the frame.


        //Show it.
        frame.setVisible(true);
    }

    public void TotalScreen() {
        frame.setContentPane(new JPanel(new BorderLayout()));

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel instructions = new JLabel("Voting Results", SwingConstants.CENTER);
        instructions.setBounds(100,80,500, 40);
        instructions.setFont(new Font("Tacoma",Font.BOLD, 20));


        //repeat this section for each vote
        JLabel section1 = new JLabel("1) Senator", SwingConstants.LEFT);
        section1.setBounds(100,130,200, 70);
        section1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JLabel option1 = new JLabel("Inn C. Umbent", SwingConstants.LEFT);
        JLabel option2 = new JLabel("Sal Enger", SwingConstants.LEFT);
        option1.setBounds(100, 180, 150, 40);
        option1.setFont(new Font("Tacoma",Font.PLAIN, 18));
        option2.setBounds(375, 180, 150, 40);
        option2.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JLabel number1 = new JLabel("54.8%", SwingConstants.LEFT);
        number1.setBounds(250, 180, 75, 40);
        number1.setFont(new Font("Tacoma",Font.PLAIN, 18));

        JLabel number2 = new JLabel("45.2%", SwingConstants.LEFT);
        number2.setBounds(525, 180, 75, 40);
        number2.setFont(new Font("Tacoma",Font.PLAIN, 18));

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
        frame.getContentPane().add(section1);

        frame.getContentPane().add(option1);
        frame.getContentPane().add(option2);
        frame.getContentPane().add(number1);
        frame.getContentPane().add(number2);

        frame.getContentPane().add(b);

        frame.setLayout(null);
        frame.setSize(715, 800);

        //Size the frame.


        //Show it.
        frame.setVisible(true);
    }

}
