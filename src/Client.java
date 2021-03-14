/**
 Client.java
 Contains the main function/entry point for the program to be run at polling place.

 Receives the public key from the Polling place server in Server.java.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;

public class Client
{
    public static int count = 0; //how many votes this voting machine has received
    private static String password; //storing the typed password
    private static BigInteger n; //storing the public key
    private static int totalVoters; //storing the total eligible voters, for encoding
    public static HashMap<String, ArrayList<String>> officesAndCandidates; //list of offices and their candidates
    private static int portNum; //the port this client uses to connect to a server
    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;

    public static void main(String args[]) throws Exception //args[0] is port
    {
        portNum = Integer.parseInt(args[0]);

        ClientGUI myGUI = new ClientGUI(); //set up the GUI

        boolean connected = false;
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
        getCandidatesAndOffices(); //get candidates and offices from server

        while(!isServerReadyToSupplyPK()) //wait until n can be supplied from server
        {
            Thread.sleep(1000);
        }

        getPK(); //get n from Server
        myGUI.WelcomeScreen(); //display the welcome screen, since we're ready
    }

    //ClientGUI calls this when a vote needs to be cast
    public static void CastVote(int selected[]) throws Exception
    {
        int i = 0; //keep track of which office
        BigInteger r, biVote; //random for encryption, and the encoded vote
        os.writeUTF("sendingBallot"); //letting server know what we're sending
        os.writeUTF(password); //giving it the remembered password

        for (HashMap.Entry<String, ArrayList<String>> entry : officesAndCandidates.entrySet())
        {
            do
            {
                r = new BigInteger(n.bitLength() - 1, new Random()); //generate r
            } while (!r.gcd(n).equals(BigInteger.ONE)); //must be coprime to b

            if (selected[i] == 0)
                biVote = new BigInteger("0");
            else if (selected[i] == 1)
                biVote = new BigInteger("1");

            //for candidates past 2, we need to do a special encoding involving the total eligible voters
            else
                biVote = new BigInteger(Integer.toString((int) Math.pow(totalVoters + 1, selected[i] - 1))); //convert the vote to how it should be encrypted for homomorphic encryption to work

            BigInteger encryptedVote = Crypto.encrypt(biVote, r, n); //encrypt the encoded vote
            os.writeUTF(entry.getKey() + ":" + encryptedVote.toString()); //send it over to the server

            count++; //increment the times this machine has been used
            i++; //next office
        }
        os.writeUTF("END"); //let server know to send us confirmation, then close the connection
        os.flush();

        if (is.readUTF().equals("counted")) //let GUI know it worked
            ClientGUI.counted = true;
    }

    //routine to get the public key from the server
    private static void getPK() throws Exception
    {
        //give server command
        os.writeUTF("getN");
        os.flush();

        //get information back from server
        n = new BigInteger(is.readUTF());
    }

    //routine to get the offices/candidates list from server
    private static void getCandidatesAndOffices() throws Exception
    {
        officesAndCandidates = new HashMap<>();

        //ask for candidates
        os.writeUTF("getOfficesAndCandidates");
        os.flush();

        //read the first line of candidate file on server (each line represents one office)
        String line = is.readUTF();

        //until the server indicates that it is ready to inform us of the total number of voters, continue appending offices (and their candidates) to the officesAndCandidates hashmap
        while (!line.equals("Total Voters:"))
        {
            String splitLine[] = line.split(", "); //each line uses the following form: office, name1, name2  So we split on commas

            ArrayList<String> candidateList = new ArrayList<>(); //making an arraylist of candidates per office to eventually add to the hashmap entry
            Collections.addAll(candidateList, Arrays.copyOfRange(splitLine, 1, splitLine.length)); //adding the candidates to the candidateList that was just initialized

            officesAndCandidates.put(splitLine[0], candidateList); //add the entry to OfficesAndCandidates

            line = is.readUTF(); //continue reading from file
        }
        totalVoters = Integer.parseInt(is.readUTF()); //recording the total number of voters
    }

    //allows the GUI to test a password before presenting a voter with options
    public static boolean testPassword(char passwordChars[]) throws Exception //takes a char array of the password
    {
        //convert the char array to a string
        password = "";
        for (char letter : passwordChars)
        {
            password += letter;
        }

        //send the server the password after telling it what we're sending
        os.writeUTF("passwordCorrect?");
        os.writeUTF(password);
        os.flush();

        return is.readUTF().equals("yes");
    }

    //return whether or not the server has public key n ready to supply
    private static boolean isServerReadyToSupplyPK() throws Exception
    {
        //ask server if it needs a key to be generated
        os.writeUTF("haveKey?");
        os.flush();

        return is.readUTF().equals("yes");
    }
}