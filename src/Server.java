/**
 Server.java
 Contains the main function/entry point for the program to be run by the Tally Authority (TA).

 Generates the keypair and distributes the public key to the polling places' client software.


 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;

public class Server
{
    private static BigInteger keyPair[][];

    public static void main(String args[])
    {
        ServerGUI myGUI = new ServerGUI(); //set up the GUI

        //setting up arbitrary bitlength and certainty (for Paillier_cryptosystem)
        int bitLength = 256;
        int certainty = 68;

        keyPair = Crypto.keyPairGen(bitLength, certainty); //keyPair[0] is the public key and keyPair[1] is the secret key

        while(true)
        {
            System.out.printf("iterating in server\n");
            Serve();
        }

    }

    private static void Serve()
    {
        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(1337);
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

            while(true)
            {
                line = is.readUTF();

                if (line.equals("getPK"))
                {
                    os.writeUTF(keyPair[0][0].toString());
                    os.writeUTF(keyPair[0][1].toString());

                    os.flush();
                    serverSocket.close();
                    return;
                }

                else if (line.equals("getSK"))
                {
                    os.writeUTF(keyPair[1][0].toString());
                    os.writeUTF(keyPair[1][1].toString());

                    os.flush();
                    serverSocket.close();
                    return;
                }

            }

        } catch(Exception e) {System.out.printf("Server couldn't start connection\n"); return;}


    }






}
