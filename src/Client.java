/**
 Client.java
 Contains the main function/entry point for the program to be run at polling place.

 Receives the public key from the Tally Authority in Server.java.

 Will implement functionality to perform verification on TA. (PVSS)

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.lang.reflect.Array;
import java.net.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

import static java.lang.String.valueOf;

public class Client
{
    public static int count = 0; //how many votes this voting machine has received
    public String name = "poll1";
    //iterating the name for the vote is     name_count

    private static HashMap<String, BigInteger> encryptedSubtotals; //keeps track of subtotals by office
    private static BigInteger[] pq = new BigInteger[2];
    private static BigInteger[] pk = new BigInteger[2];
    private static BigInteger[] sk = new BigInteger[2];
    public static int totalVoters = 100;
    public static HashMap<String, ArrayList<String>> officesAndCandidates;
    private static int bitLength, certainty; //will be updated by main
    private  static Random rand = new Random();
    private static int portNum;
    public static Socket socket;
    public static ObjectOutputStream os;
    public static ObjectInputStream is;


    public static void main(String args[]) throws Exception //args[0] is port
    {
        portNum = Integer.parseInt(args[0]);

        ClientGUI myGUI = new ClientGUI(); //set up the GUI

        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
        os = new ObjectOutputStream(socket.getOutputStream());
        is = new ObjectInputStream(socket.getInputStream());



        /**ArrayList<String> candidates = getCandidates(); //get list of candidates from server (TA)

        //printing the names out temporarily while GUI is worked on
        for (String name : candidates)
        {
            System.out.printf("Name from server: %s\n", name);
        }
        **/

        /**
        //getting and printing out the names and encrypted vote subtotals that were saved
        encryptedSubtotals = readFile();
        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            System.out.printf("From file: %s: %d\n", entry.getKey(), entry.getValue());
        }**/

        /**
         suggestion for multiple candidate naming scheme:
         role_(position)_candidate1_candidate2_candidate3_
         *
         */

        //TODO handle possible conflict between names provided by server and names in saved file

        //not random at the moment
        //BigInteger r = new BigInteger("25");//new BigInteger(512, new Random()); //generate r


        //give server some time to recover from getting pk. We'll need to have a fix for multiple clients.
        officesAndCandidates = getCandidates();
        encryptedSubtotals = readFile();

        while (!isServerReadytoSupplyPK()) //if key's not ready, start guessing PQs qith PQgen()
        {
            PQgen();
        }

        getPK();


        //simulate votes and put them in the file
        /**
        //making and encrypting a vote for Thomas
        BigInteger vote1 = new BigInteger("1");
        BigInteger encryptedVote1 = Crypto.encrypt(vote1, r, pk);
        encryptedSubtotals.put("Thomas Mitchell", Crypto.addEncrypted(encryptedSubtotals.get("Thomas Mitchell"), encryptedVote1, pk[0])); //adding to subtotal

        //making and encrypting another vote for Thomas
        BigInteger vote2 = new BigInteger("1");
        BigInteger encryptedVote2 = Crypto.encrypt(vote2, r, pk);
        encryptedSubtotals.put("Thomas Mitchell", Crypto.addEncrypted(encryptedSubtotals.get("Thomas Mitchell"), encryptedVote2, pk[0])); //adding to subtotal

        //making and encrypting a vote for Kevin
        BigInteger vote3 = new BigInteger("1");
        BigInteger encryptedVote3 = Crypto.encrypt(vote3, r, pk);
        encryptedSubtotals.put("Kevin Kincaid", Crypto.addEncrypted(encryptedSubtotals.get("Kevin Kincaid"), encryptedVote3, pk[0])); //adding to subtotal



        updateFile(encryptedSubtotals); //writing to disk

        //getting the secret key. This wouldn't happen until after the polls closed. Even then, I don't know if the client will directly touch the secret key.
        BigInteger[] sk = getSK();

        //decrypt the summed votes and print
        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            System.out.printf("%s got %d votes.\n", entry.getKey(), Crypto.decrypt(entry.getValue(), sk, pk[0]));
        }

         **/


    }

    private static void PQgen() throws Exception //starts at 1 and includes itself
    {
        BigInteger pq[] = new BigInteger[2];

        //moving this to server, I forgot
        /**

        while (true)
        {
            pq[0] = new BigInteger(bitLength / 2 / numClients, certainty, rand);
            pq[1] = new BigInteger(bitLength / 2 / numClients, certainty, rand);



            //give server command
            os.writeUTF("sendingPQ");
            os.writeUTF(Integer.toString(myNum));
            os.writeUTF(pq[0].toString());
            os.writeUTF(pq[1].toString());

            os.flush();


        }
         **/

    }


    //current idea for function to call from the GUI
    public static void CastVote(int selected[]) throws Exception
    {

        BigInteger r = new BigInteger(bitLength, rand); //generate r


        int i = 0; //keep track of which office
        for (HashMap.Entry<String, ArrayList<String>> entry : officesAndCandidates.entrySet()) {

            BigInteger biVote;
            if (selected[i] == 0)
                biVote = new BigInteger("0");
            else if (selected[i] == 1)
                biVote = new BigInteger("1");
            else
                biVote = new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, selected[i] - 1) )); //convert the vote to how it should be encrypted for homomorphic encryption to work
            BigInteger encryptedVote = Crypto.encrypt(biVote, r, pk); //encrypt it



            //get the subtotal for this particular office
            BigInteger officeSubTotal;
            if (encryptedSubtotals.get(entry.getKey()) == null)
                officeSubTotal = new BigInteger("0");
            else
                officeSubTotal = encryptedSubtotals.get(entry.getKey());

            officeSubTotal = Crypto.addEncrypted(officeSubTotal, encryptedVote, pk); //add the new vote to it

            encryptedSubtotals.put(entry.getKey(), officeSubTotal); //update the hashmap
            count++; //increment the times this machina has been used

            i++;
        }
        //temporarily asking Server to print the results we send it.
        getFinalTally();


        //now try to update the file on disk
        try
        {
            updateFile(encryptedSubtotals);
        } catch(Exception e) {System.out.printf("Client couldn't write to file.\n"); return;}

}


    //routine to get the public key from the server
    private static void getPK() throws Exception
    {


        //give server command
        os.writeUTF("getPK");
        os.flush();

        //get information back from server
        pk[0] = new BigInteger(is.readUTF());
        pk[1] = new BigInteger(is.readUTF());



    }


    //routine to get the secret key from the server. This won't be in the final copy of the program.
    private static void getSK() throws Exception
    {


        //ask server for secret key
        os.writeUTF("getSK");
        os.flush();

        //get secret key
        sk[0] = new BigInteger(is.readUTF());
        sk[1] = new BigInteger(is.readUTF());


    }

    //routine to get the candidate list from server
    public static HashMap<String, ArrayList<String>> getCandidates() throws Exception
    {
        HashMap<String, ArrayList<String>> officesAndCandidates = new HashMap<>();

        //ask for candidates
        os.writeUTF("getCandidates");
        os.flush();

        //read the first line of candidate file on server (each line represents one office)
        String line = is.readUTF();

        //continue reading from server until it sends special "END" message to signify end of list
        while (!line.equals("END"))
        {
            String splitLine[] = line.split(", "); //each line uses the following form: office, name1, name2  So we split on commas
            ArrayList<String> candidateList = new ArrayList<>(); //making an arraylist of candidates per office to eventually add to the hashmap entry
            Collections.addAll(candidateList, Arrays.copyOfRange(splitLine, 1, splitLine.length)); //the candidateList should be the splitLine values except the first entry, which is for the office name
            officesAndCandidates.put(splitLine[0],candidateList); //add the entry to the return value
            line = is.readUTF(); //continue reading from file
        }


        return officesAndCandidates;
    }

    //writes to the subtotal file after voting is done
    private static void updateFile(HashMap<String, BigInteger> records) throws Exception
    {
        FileWriter file = new FileWriter("encryptedSubtotals.txt");

        //for every office
        for (HashMap.Entry<String, BigInteger> entry : records.entrySet())
        {
            file.write(entry.getKey() + ": " + entry.getValue() + "\n");
        }

        file.close();
    }

    //reads existing subtotals from the appropriately-named file in the working directory
    private static HashMap<String, BigInteger> readFile() throws Exception
    {
        HashMap<String, BigInteger> encryptedSubtotals = new HashMap<>(); //return value

        File file = new File("encryptedSubtotals.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String nameVotes[]; //[0]is a candidate's name and [1] is their encrypted subtotal

        //every line of the file is an office and its votes
        line = br.readLine();
        while (line != null)
        {
            nameVotes = line.split(": ");
            encryptedSubtotals.put(nameVotes[0], new BigInteger(nameVotes[1]));
            line = br.readLine();
        }

        return encryptedSubtotals;
    }

    private static boolean isServerReadytoSupplyPK() throws Exception
    {
        boolean rv = false;



        //ask server if it needs a key to be generated
        os.writeUTF("needKey?");
        os.flush();

        rv = !is.readUTF().equals("yes");

        bitLength = Integer.parseInt(is.readUTF());
        certainty = Integer.parseInt(is.readUTF());



        return rv;
    }

    private static void getFinalTally() throws Exception
    {


        //ask server to print results
        os.writeUTF("sendingResults");
        os.flush();

        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            os.writeUTF(entry.getKey());
            os.flush();
            os.writeUTF(entry.getValue().toString());
            os.flush();
        }
        os.writeUTF("END");

        os.flush();

    }

}
