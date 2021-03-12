/**
 Client.java
 Contains the main function/entry point for the program to be run at polling place.

 Receives the public key from the Tally Authority in Server.java.

 Will implement functionality to perform verification on TA. (PVSS)

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;

public class Client
{
    public static int count = 0; //how many votes this voting machine has received
    public String name = "poll1"; //iterating the name for the vote is     name_count
    private static String password;
    private static BigInteger N, theta;
    private static int totalVoters = 100;
    public static HashMap<String, ArrayList<String>> officesAndCandidates;
    private static int portNum;
    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;


    public static void main(String args[]) throws Exception //args[0] is port
    {
        portNum = Integer.parseInt(args[0]);

        boolean connected = false;
        ClientGUI myGUI = new ClientGUI(); //set up the GUI
        while (!connected)
        {
            try
            {
                Thread.sleep(1000);
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                connected = true;
            } catch (Exception e){System.out.printf("Waiting on server...\n");};
        }
        getCandidates(); //getFromServer
        //ClientGUI myGUI = new ClientGUI(); //set up the GUI

        while(!isServerReadyToSupplyPK()) //wait until N can be supplied from server
        {
            Thread.sleep(1000);
        }

        getPK(); //get N from Server
        myGUI.PasswordScreen(false);


        System.out.printf("N: %s\n", N);

    }

    //current idea for function to call from the GUI
    public static void CastVote(int selected[]) throws Exception {

        int i = 0; //keep track of which office
        BigInteger r;
        for (HashMap.Entry<String, ArrayList<String>> entry : officesAndCandidates.entrySet())
        {

            do {
                r = new BigInteger(N.bitLength() - 1, new Random()); //generate r
            } while (!r.gcd(N).equals(BigInteger.ONE));

            BigInteger biVote;
            if (selected[i] == 0)
                biVote = new BigInteger("0");
            else if (selected[i] == 1)
                biVote = new BigInteger("1");
            else
                biVote = new BigInteger(Integer.toString((int) Math.pow(totalVoters + 1, selected[i] - 1))); //convert the vote to how it should be encrypted for homomorphic encryption to work

            BigInteger encryptedVote = Crypto.encrypt(biVote, r, N); //encrypt it

            os.writeUTF("sendingBallot");
            os.writeUTF(password);
            os.writeUTF(encryptedVote.toString());
            os.writeUTF(entry.getKey());
            os.flush();

            count++; //increment the times this machine has been used

            i++;
        }
    }

    //routine to get the public key from the server
    private static void getPK() throws Exception
    {

        //give server command
        os.writeUTF("getN");
        os.flush();

        //get information back from server
        N = new BigInteger(is.readUTF());

    }


    //routine to get the secret key from the server. This won't be in the final copy of the program.
    private static void getSK() throws Exception
    {

        //ask server for secret key
        os.writeUTF("getSK");
        os.flush();

        //get secret key
        theta = new BigInteger(is.readUTF());
    }

    //routine to get the candidate list from server
    public static void getCandidates() throws Exception
    {
        officesAndCandidates = new HashMap<>();

        //ask for candidates
        os.writeUTF("getCandidates");
        os.flush();

        //read the first line of candidate file on server (each line represents one office)
        String line = is.readUTF();

        //continue reading from server until it sends special "END" message to signify end of list
        while (!line.equals("Total Voters:"))
        {
            String splitLine[] = line.split(", "); //each line uses the following form: office, name1, name2  So we split on commas
            ArrayList<String> candidateList = new ArrayList<>(); //making an arraylist of candidates per office to eventually add to the hashmap entry
            Collections.addAll(candidateList, Arrays.copyOfRange(splitLine, 1, splitLine.length)); //the candidateList should be the splitLine values except the first entry, which is for the office name
            officesAndCandidates.put(splitLine[0],candidateList); //add the entry to the return value
            line = is.readUTF(); //continue reading from file
        }
        totalVoters = Integer.parseInt(is.readUTF());

    }

    public static boolean testPassword(char passwordChars[]) throws Exception
    {
        password = "";

        for (char letter : passwordChars)
        {
            password += letter;
        }

        os.writeUTF("passwordCorrect?");
        os.writeUTF(password);
        os.flush();

        if (is.readUTF().equals("yes"))
            return true;
        else
            return false;
    }


    private static boolean isServerReadyToSupplyPK() throws Exception
    {
        //ask server if it needs a key to be generated
        os.writeUTF("haveKey?");
        os.flush();

        if (is.readUTF().equals("yes"))
            return true;
        else
            return false;
    }

}
