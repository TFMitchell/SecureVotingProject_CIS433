/**
 Server.java
 Contains the main function/entry point for the program to be run by the Tally Authority (TA).

 Generates the keypair and distributes the public key to the polling places' client software.


 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import com.sun.media.sound.SF2InstrumentRegion;

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

public class Server
{
    public static ArrayList<String> CandidateNames = new ArrayList<>();
    public static HashMap<String, BigInteger> officesAndVotes = new HashMap<>();
    public static boolean hasN = false;
    public static int bitLength = 64;
    public static  int myListeningPort;
    public static int serverPortNums[];
    public static int clientPortNums[];
    public static boolean running = true;
    public static HashMap<String, BigInteger> encryptedSubtotals; //keeps track of subtotals by office
    public static Random rand = new Random();
    public static int myIndex, numServers, numClients;
    private static int totalVoters = 100;
    public static int currentClientNum = 0;
    public static BigInteger N, theta;
    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;
    public static BigInteger pShareSum = BigInteger.ZERO;
    public static BigInteger qShareSum = BigInteger.ZERO;

    public static float candidate_counts[][];

    public static void main(String args[]) throws Exception
    {
        ServerGUI myGUI = new ServerGUI(); //set up the GUI
        ExecutorService executor = Executors.newCachedThreadPool();

        //interpret parameters
        myIndex = Integer.parseInt(args[0]);
        numServers = Integer.parseInt(args[1]);

        myListeningPort = Integer.parseInt(args[2]);


        executor.execute(new Listening());

        serverPortNums = new int[numServers];
        for (int i = 2; i < 2 + numServers; i++)
        {
            serverPortNums[i-2] = Integer.parseInt(args[i]);
        }

        numClients = Integer.parseInt(args[numServers + 2]);
        clientPortNums = new int[numClients];
        for (int i = numServers + 3; i < numServers + 3 + numClients; i++)
        {
            clientPortNums[i - numServers - 3] = Integer.parseInt(args[i]);
            executor.execute(new ClientComm());
        }


        readCandidatesFromFile(); //read the candidate file and load it into the array
        retreiveKeysFromFile();
        readFile();

        BigInteger pq[] = Crypto.genPQ(myIndex, bitLength, rand);

        System.out.printf("My P: %s Q: %s\n", pq[0], pq[1]);

        for (int portNum : serverPortNums)
        {
            while (true)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingPQ");
                    os.writeUTF(pq[0].toString());
                    os.writeUTF(pq[1].toString());
                    os.flush();

                    break;
                } catch (Exception e) { continue; }
            }

        }

        Thread.sleep(5000);

        System.out.printf("P: %s, Q: %s\n", pShareSum, qShareSum);

        N = pShareSum.multiply(qShareSum);

        hasN = true;



    }


    //file handler for candidate list
    public static void readCandidatesFromFile() throws Exception
    {
        File file = new File("candidate_list.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        while (line != null)
        {
            CandidateNames.add(line);
            line = br.readLine();
        }
    }


    public static void getResults()
    {
        System.out.printf("These are the results:\n");

        candidate_counts = new float [CandidateNames.size()][];
        int row = 0;
        for (String office : CandidateNames)
        {
            String names[] = office.split(", ");




            officesAndVotes.get(names[0]); //the first after splitting is the office name
            candidate_counts[row] = new float[names.length - 1];

            BigInteger decryptedOffice = Crypto.decrypt(officesAndVotes.get(names[0]), N, theta);

            BigInteger remainder = decryptedOffice;

            for (int i = names.length - 1; i > 0; i--)
            {
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));
                System.out.printf("%s got %d votes.\n", names[i], whole);

                //save the count for access by GUI
                candidate_counts[row][i-1] = whole.floatValue();


                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));

            }
            row++;
        }
    }

    public static void retreiveKeysFromFile() throws Exception
    {
        File file = new File("serverKeys.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        if (line == null)
        {
            return;
        }
        //else
        N = new BigInteger(line);
        theta = new BigInteger(br.readLine());

        hasN = true;
    }
    //reads existing subtotals from the appropriately-named file in the working directory
    private static void readFile() throws Exception
    {
        encryptedSubtotals = new HashMap<>(); //return value

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

        //now try to update the file on disk
        try
        {
            updateFile(encryptedSubtotals);
        } catch(Exception e) {System.out.printf("Couldn't write to file.\n");}

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

}

class ClientComm implements Runnable
{

    public void run()
    {
        int myIndex = Server.currentClientNum++;

        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(Server.clientPortNums[myIndex]);
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

            while (Server.running)
            {


                line = is.readUTF();

                if (line.equals("getN"))  //send public key
                {
                    os.writeUTF(Server.N.toString());

                    os.flush();


                }

                else if (line.equals("getCandidates"))  //send candidates
                {

                    for (String name : Server.CandidateNames) {
                        os.writeUTF(name);
                    }

                    os.writeUTF("END"); //tells client that the list is done

                    os.flush();

                }

                else if (line.equals("haveKey?"))
                {

                    if (!Server.hasN)
                        os.writeUTF("no");
                    else
                    {
                        os.writeUTF("yes");
                        os.writeUTF(Integer.toString(Server.bitLength));
                    }

                    os.flush();
                }



                else if (line.equals("sendingBallot"))
                {
                    //get the subtotal for this particular office
                    BigInteger officeSubTotal;

                    BigInteger encryptedVote = new BigInteger(is.readUTF());

                    String index = is.readUTF();

                    if (Server.encryptedSubtotals.get(index) == null)
                        officeSubTotal = new BigInteger("0");
                    else
                        officeSubTotal = Server.encryptedSubtotals.get(index);

                    officeSubTotal = Crypto.addEncrypted(officeSubTotal, encryptedVote, Server.N); //add the new vote to it

                    Server.encryptedSubtotals.put(index, officeSubTotal); //update the hashmap

                }

                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);


            }

        }catch(Exception e) {System.out.printf("Server couldn't start connection. %s\n", e);}


    }
}

class Listening implements Runnable {

    public void run() {


        while (Server.running)
        {
            try
            {
                //getting ready to receive input
                ServerSocket serverSocket = new ServerSocket(Server.myListeningPort);
                String line;
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

                line = is.readUTF();


                if (line.equals("sendingPQ"))
                {
                    Server.pShareSum = Server.pShareSum.add(new BigInteger(is.readUTF()));
                    Server.qShareSum = Server.qShareSum.add(new BigInteger(is.readUTF()));

                    os.flush();
                }
                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                serverSocket.close();
            } catch (Exception e) {
                System.out.printf("Server couldn't start connection. %s\n", e); }

        }
    }
}


class Polynomial
{
    private final BigInteger a[];

    public Polynomial(int degree, BigInteger intercept, int bitLength, Random rand)
    {
        a = new BigInteger[degree + 1];
        a[0] = intercept;
        for (int i = 1; i < degree + 1; i++) {
            a[i] = new BigInteger(bitLength, rand);
        }
    }

    public BigInteger getValueAt(int x)
    {
        BigInteger rv = a[0];

        for (int i = 1; i < a.length; i++)
        {
            rv = rv.add(a[i].multiply(new BigInteger(Integer.toString((int) Math.pow(x, i)))));
        }

        return rv;
    }


}