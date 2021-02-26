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
    public static BigInteger pk[], sk[];
    public static BigInteger PQCollection[][];
    public static ArrayList<String> CandidateNames = new ArrayList<>();
    public static HashMap<String, BigInteger> officesAndVotes = new HashMap<>();
    public static boolean hasPQ = false;
    public static int bitLength = 1024;
    public static int certainty = 64;
    public static ArrayList<Integer> portNums = new ArrayList<>();
    public static boolean running = true;
    public static HashMap<String, BigInteger> encryptedSubtotals; //keeps track of subtotals by office
    public static int serverPort;
    public static boolean admin = true;
    public static Random rand = new Random();
    public static Socket socket;
    public static ObjectOutputStream os;
    public static ObjectInputStream is;
    public static int firstPort = 0; //that will be the admin's 0 index partner
    public static int shareCounter = 0;


    public static float candidate_counts[][];

    public static void main(String args[]) throws Exception // [0] should say "admin:" [1] = the port of the admin server [1-2] are fellow server ports and [3+] are client ports.

    {
        ServerGUI myGUI = new ServerGUI(); //set up the GUI
        ExecutorService executor = Executors.newCachedThreadPool();

        if (args[0].equals("admin:"))
            admin = false;

        if (admin)
        {
            firstPort = Integer.parseInt(args[0]);
            for (int i = 0; i < args.length; i++) //serving all of them
            {
                portNums.add(Integer.parseInt(args[i]));
                executor.execute(new ServerComm());
            }
        }
        else
        {

            for (int i = 2; i < args.length; i++)
            {
                portNums.add(Integer.parseInt(args[i]));
                executor.execute(new ServerComm());
            }

            serverPort = Integer.parseInt(args[1]);
        }


        //initializing the PQCollection
        PQCollection = new BigInteger[3][2];
        for (BigInteger[] PQ : PQCollection)
        {
            PQ[0] = BigInteger.ZERO;
            PQ[1] = BigInteger.ZERO;
        }

        readCandidatesFromFile(); //read the candidate file and load it into the array
        retreiveKeysFromFile();
        readFile();


        if (!admin)
        {
            boolean connected = false;

            while (!connected)
            {
                try
                {

                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPort);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    connected = true;
                } catch (Exception e){System.out.printf("Waiting on other server...\n");};
            }

            os.writeUTF("haveKey?");

            while (is.readUTF().equals("no"))
            {
                PQGen();
                Thread.sleep(300);
                os.writeUTF("haveKey?");
            }
        }

        pkGen();

        while(true)
        {
            Thread.sleep(5000);
            //getResults();
        }

    }

    public static void PQGen() throws Exception
    {
        BigInteger pq[] = new BigInteger[2];

        pq[0] = new BigInteger(bitLength / 2 / 3, certainty, rand);
        pq[1] = new BigInteger(bitLength / 2 / 3, certainty, rand);

        os.writeUTF("sendingPQs");

        os.writeUTF(Integer.toString(serverPort));

        os.writeUTF(pq[0].toString());
        os.writeUTF(pq[1].toString());
        os.flush();

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

    public static void pkGen() //public key gen
    {
        BigInteger pqSums[] = new BigInteger[2];
        pqSums[0] = BigInteger.ZERO;
        pqSums[1] = BigInteger.ZERO;


        for(BigInteger[] individPQ : PQCollection)
        {
            pqSums[0] = pqSums[0].add(individPQ[0]);
            pqSums[1] = pqSums[1].add(individPQ[1]);
        }

        pk = Crypto.getPK(pqSums);

        if (pk[0].signum() != 0) //this means it worked and we can say we don't need to make a PQ anymore
        {
            hasPQ = true;
        }
        else
            shareCounter = 0;
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

            BigInteger decryptedOffice = Crypto.decrypt(officesAndVotes.get(names[0]), pk[0], sk[0]);

            BigInteger remainder = decryptedOffice;

            for (int i = names.length - 1; i > 0; i--)
            {
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));
                System.out.printf("%s got %d votes.\n", names[i], whole);

                //save the count for access by GUI
                candidate_counts[row][i-1] = whole.floatValue();


                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(Client.totalVoters + 1, i - 1)) ));

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
        pk[0] = new BigInteger(line);
        line = br.readLine();
        pk[1] = new BigInteger(line);

        sk[0] = new BigInteger(line);
        line = br.readLine();
        sk[1] = new BigInteger(line);

        hasPQ = true;
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

class ServerComm implements Runnable
{
    public void run()
    {

        try
        {
            //getting ready to receive input
            ServerSocket serverSocket = new ServerSocket(Server.portNums.remove(0));
            String line;
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

            while (Server.running)
            {
                line = is.readUTF();

                if (line.equals("getPK"))  //send public key
                {
                    os.writeUTF(Server.pk[0].toString());
                    os.writeUTF(Server.pk[1].toString());

                    os.flush();

                } else if (line.equals("getSK"))  //send secret key
                {
                    os.writeUTF(Server.pk[0].toString());
                    os.writeUTF(Server.pk[1].toString());

                    os.flush();

                } else if (line.equals("getCandidates"))  //send candidates
                {

                    for (String name : Server.CandidateNames) {
                        os.writeUTF(name);
                    }

                    os.writeUTF("END"); //tells client that the list is done

                    os.flush();

                }

                else if (line.equals("haveKey?"))
                {

                    if (!Server.hasPQ)
                        os.writeUTF("no");
                    else
                    {
                        os.writeUTF(Integer.toString(Server.bitLength));
                        os.writeUTF(Integer.toString(Server.certainty));
                        os.writeUTF("yes");
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

                    officeSubTotal = Crypto.addEncrypted(officeSubTotal, encryptedVote, Server.pk[0]); //add the new vote to it

                    Server.encryptedSubtotals.put(index, officeSubTotal); //update the hashmap

                }
                else if (line.equals("sendingPQs"))
                {
                    int index;
                    if (Integer.parseInt(is.readUTF()) == Server.firstPort)
                        index = 0;
                    else
                        index = 1;

                    Server.PQCollection[index][0] = new BigInteger(is.readUTF());
                    Server.PQCollection[index][1] = new BigInteger(is.readUTF());

                    if (++Server.shareCounter == 2)
                        Server.pkGen();
                }
                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);
            }

        } catch(Exception e) {System.out.printf("Server couldn't start connection. %s\n", e);}


    }
}