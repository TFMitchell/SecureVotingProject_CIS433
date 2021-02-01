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


        //Create components and put them in the frame.

        JLabel instructions = new JLabel("You are about to place your vote. But first, please enter your given information here:", SwingConstants.LEFT);
        instructions.setBounds(50,80,500, 40);

        JLabel title = new JLabel("First Name:", SwingConstants.LEFT);
        title.setBounds(50,140,500, 40);

        JTextField text = new JTextField("",10);
        text.setBounds(50,180,140, 40);

        JLabel title2 = new JLabel("Last Name:", SwingConstants.LEFT);
        title2.setBounds(50,240,500, 40);

        JTextField text2 = new JTextField("",10);
        text2.setBounds(50,280,140, 40);


        frame.getContentPane().add(instructions);
        frame.getContentPane().add(title);
        frame.getContentPane().add(title2);

        frame.getContentPane().add(text);
        frame.getContentPane().add(text2);

        frame.setSize(600, 420);
        frame.setLayout(null);
        //Size the frame.


        //Show it.
        frame.setVisible(true);
    }
}
