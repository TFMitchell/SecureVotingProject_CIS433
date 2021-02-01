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

    public static void main(String args[]) throws Exception
    {
        ClientGUI myGUI = new ClientGUI(); //set up the GUI

        ArrayList<String> candidates = getCandidates(); //get list of candidates from server (TA)

        //printing the names out temporarily while GUI is worked on
        for (String name : candidates)
        {
            System.out.printf("Name from server: %s\n", name);
        }

        //getting and printing out the names and encrypted vote subtotals that were saved
        HashMap<String, BigInteger> encryptedSubtotals = readFile();
        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            System.out.printf("From file: %s: %d\n", entry.getKey(), entry.getValue());
        }

        //TODO handle possible conflict between names provided by server and names in saved file

        //not random at the moment
        BigInteger r = new BigInteger("25");//new BigInteger(512, new Random()); //generate r

        BigInteger[] pk = getPK(); //get the public key from the server (TA)

        //simulate votes and put them in the file

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
    }

    //routine to get the public key from the server
    private static BigInteger[] getPK()
    {
        BigInteger pk[] =  new BigInteger[2]; //return value

        try
        {
            //getting connected
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), 1337);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

            //give server command
            os.writeUTF("getPK");
            os.flush();

            //get information back from server
            pk[0] = new BigInteger(is.readUTF());
            pk[1] = new BigInteger(is.readUTF());

        } catch(Exception e) {System.out.printf("Client couldn't start connection (PK function).\n"); return null;}

        return pk;
    }

    //routine to get the secret key from the server. This won't be in the final copy of the program.
    private static BigInteger[] getSK()
    {
        BigInteger sk[] =  new BigInteger[2]; //return value

        try
        {
            //getting connected
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), 1337);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

            //ask server for secret key
            os.writeUTF("getSK");
            os.flush();

            //get secret key
            sk[0] = new BigInteger(is.readUTF());
            sk[1] = new BigInteger(is.readUTF());

        } catch(Exception e) {System.out.printf("Client couldn't start connection (SK function).\n"); return null;}

        return sk;
    }

    //routine to get the candidate list from server
    private static ArrayList<String> getCandidates()
    {
        ArrayList<String>  candidates = new ArrayList<>(); //return value

        try
        {
            //getting connected
            Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(), 1337);
            ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

            //ask for candidates
            os.writeUTF("getCandidates");
            os.flush();

            //read the first name/line of candidate file on server
            String name = is.readUTF();

            //continue reading from server until it sends special "END" message to signify end of list
            while (!name.equals("END"))
            {
                candidates.add(name);
                name = is.readUTF();
            }

        } catch(Exception e) {System.out.printf("Client couldn't start connection (getCandidates function).\n"); return null;}

        return candidates;
    }

    //writes to the subtotal file after voting is done
    private static void updateFile(HashMap<String, BigInteger> records) throws Exception
    {
        FileWriter file = new FileWriter("encryptedSubtotals.txt");

        //for every candidate
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

        //every line of the file is a candidate
        line = br.readLine();
        while (line != null)
        {
            nameVotes = line.split(": ");
            encryptedSubtotals.put(nameVotes[0], new BigInteger(nameVotes[1]));
            line = br.readLine();
        }

        return encryptedSubtotals;
    }

}