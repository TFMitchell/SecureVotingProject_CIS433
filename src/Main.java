/**
 Main.java
 Contains the main function/entry point for the program.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.math.*;
import java.util.*;

public class Main
{
    public static void main(String args[])
    {
        GUI myGUI = new GUI(); //set up the GUI

        //setting up arbitrary bitlength and certainty (for Paillier_cryptosystem)
        int bitLength = 256;
        int certainty = 68;

        BigInteger r = new BigInteger(512, new Random()); //generate r

        BigInteger keyPair[][] = Crypto.keyPairGen(bitLength, certainty); //keyPair[0] is the public key and keyPaid[1] is the secret key

        BigInteger vote1 = new BigInteger("2"); //using numbers for voting right now

        BigInteger encryptedVote1 = Crypto.encrypt(vote1, r, keyPair[0]); //encrypting vote1

        BigInteger vote2 = new BigInteger("5"); //another vote

        BigInteger encryptedVote2 = Crypto.encrypt(vote2, r, keyPair[0]); //encrypt the 2nd vote

        //do some math
        BigInteger sum = Crypto.addEncrypted(encryptedVote1, encryptedVote2, keyPair[0][0]);

        //decrypt the sum and print
        System.out.printf("%d\n", Crypto.decrypt(sum, keyPair[1], keyPair[0][0]));

    }

}