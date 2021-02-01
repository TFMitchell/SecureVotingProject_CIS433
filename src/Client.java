/**
 Client.java
 Contains the main function/entry point for the program to be run at polling place.

 Receives the public key from the Tally Authority in Server.java.

 Will implement functionality to perform verification on TA.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;

public class Client
{
    public static void main(String args[])
    {
        ClientGUI myGUI = new ClientGUI(); //set up the GUI



        BigInteger r = new BigInteger(512, new Random()); //generate r

        BigInteger[] pk = getPK();

        System.out.printf("%s\n", pk[0].toString());


        BigInteger vote1 = new BigInteger("2"); //using numbers for voting right now

        BigInteger encryptedVote1 = Crypto.encrypt(vote1, r, pk); //encrypting vote1

        BigInteger vote2 = new BigInteger("5"); //another vote

        BigInteger encryptedVote2 = Crypto.encrypt(vote2, r, pk); //encrypt the 2nd vote

        //do some math
        BigInteger sum = Crypto.addEncrypted(encryptedVote1, encryptedVote2, pk[0]);

        //get sk
        BigInteger[] sk = getSK();
        System.out.printf("%s\n", sk[0].toString());

        //decrypt the sum and print
        System.out.printf("%d\n", Crypto.decrypt(sum, sk, pk[0]));

    }

    private static BigInteger[] getPK()
    {
        BigInteger pk[] =  new BigInteger[2];

        try
        {
            //getting connected
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), 1337);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());


            os.writeUTF("getPK");
            os.flush();

            pk[0] = new BigInteger(is.readUTF());
            pk[1] = new BigInteger(is.readUTF());


        } catch(Exception e) {System.out.printf("Client couldn't start connection\n"); return null;}

        return pk;
    }

    private static BigInteger[] getSK()
    {
        BigInteger sk[] =  new BigInteger[2];

        try
        {
            //getting connected
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), 1337);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());


            os.writeUTF("getSK");
            os.flush();

            sk[0] = new BigInteger(is.readUTF());
            sk[1] = new BigInteger(is.readUTF());


        } catch(Exception e) {System.out.printf("Client couldn't start connection\n"); return null;}

        return sk;
    }

}