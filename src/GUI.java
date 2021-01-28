/**
 GUI.java

 Contains everything to do with the GUI

 Authors: Kevin Kincaid, Thomas Mitchell

 Referenced: https://www.codejava.net/java-se/swing/jlabel-basic-tutorial-and-examples#CreateJLabel

 **/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class GUI
{
    public GUI()
    {
        //Create the frame.
        JFrame frame = new JFrame("Secure Voting App");

        //Exit when closed.
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create components and put them in the frame.
        JLabel instructions = new JLabel("You are about to place your vote. But first, please enter your given information here:", SwingConstants.LEFT);
        frame.getContentPane().add(instructions, BorderLayout.CENTER);


        //Size the frame.
        frame.pack();

        //Show it.
        frame.setVisible(true);
    }
}
