/**
 Server.java
 Contains the main function/entry point for the program to be run by the polling place's main computer, networked to the voting machines running the client software.

 Generates the keypair and distributes the public key to the polling places' client software.

 Authors: Kevin Kincaid, Thomas Mitchell
 **/

import java.io.*;
import java.net.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.*;

public class Server
{
    public static HashMap<String, Boolean> approvedPasswords = new HashMap<>(); //list of acceptable passwords in order for a vote to be counted
    public static ArrayList<String> CandidateNamesByOffice = new ArrayList<>(); //list of offices in this format <office>, <candidate1>, <candidate2>, etc
    public static HashMap<String, BigInteger> encryptedSubtotals; //each office has one set of votes, division/remainders are used to determine each candidate's totals

    public static boolean hasN = false; //do we have an N value yet? used for communicating with client
    public static boolean myTurn = false; //ensures synchronization and eliminates race conditions

    public static int bitLength = 45; //bitlength of p and q values. The n is a little more than twice this bitlength as it is pSum * qSum. Set to a low value for quick testing
    public static int myListeningPort; //the port this server listens on for other servers
    public static int serverPortNums[]; //a list of all servers' ports
    public static int clientPortNums[]; //a list of my clients' ports
    private static int myIndex, numServers, numClients;
    public static int totalVoters; //this is used for encoding more than two candidates for each office (we mod by this^candidateNumber), as well as knowing how many passwords to generate
    public static int currentClientNum = 0; //used for enumerating the clients, so they know which portNum to use
    private static int nextServerPort; //the port of the server at the next index (or first index for last server)
    private static int iterationNum = 0; //keeps track of how many iterations to find a biprimal N
    private static int passwordAuthPort; //port of authority that is used to compile the password stubs from each server into passwords voters can use

    private static final Random rand = new Random();

    private static Socket socket;
    private static ObjectOutputStream os;
    private static ObjectInputStream is;

    public static BigInteger n, gg; //public key and coprime
    private static BigInteger lambda, delta; //decryption key and delta, which is numServers! (used because interpolation divides by numServers, numservers-1, ..., but needs to be integers
    private static BigInteger pShareSum, qShareSum; //sums of p and q shares, to find the share of N, which can be revealed with lagrange interpolation
    private static BigInteger pq[] = new BigInteger[2]; //p and q for this server
    public static BigInteger pShares[], qShares[], nShares[], lambdaShares[], Qi[]; //shares come from each of the Servers, including itself

    public static float candidate_counts[][]; //this is used for displaying the results in the GUI

    public static void main(String args[]) throws Exception //arguments are explained in the readme.md
    {
        readCandidatesFromFile(); //read the candidate file and load it into the array
        ServerGUI myGUI = new ServerGUI(); //set up the GUI
        ExecutorService executor = Executors.newCachedThreadPool(); //thread pool for listeners

        //interpret parameters
        myIndex = Integer.parseInt(args[0]);
        numServers = Integer.parseInt(args[1]);

        serverPortNums = new int[numServers];
        for (int i = 2; i < 2 + numServers; i++)
            serverPortNums[i-2] = Integer.parseInt(args[i]);

        numClients = Integer.parseInt(args[numServers + 2]);

        clientPortNums = new int[numClients];
        for (int i = numServers + 3; i < numServers + 3 + numClients; i++)
        {
            clientPortNums[i - numServers - 3] = Integer.parseInt(args[i]);
            executor.execute(new ClientComm()); //establish connection with each client right away
        }

        passwordAuthPort = Integer.parseInt(args[numServers + 3 + numClients]);

        totalVoters = Integer.parseInt(args[numServers + 4 + numClients]);

        //setting up arrays whose length we now know
        pShares = new BigInteger[numServers];
        qShares = new BigInteger[numServers];
        nShares = new BigInteger[numServers];
        lambdaShares = new BigInteger[numServers];
        Qi = new BigInteger[numServers];

        //we also know these now
        myListeningPort = serverPortNums[myIndex - 1]; //the port that actually belongs to this server
        delta = Crypto.factorial(numServers); //delta is the factorial of numServers. This is used later for interpolation to avoid decimals
        readKeysFromFile(); //read the stored n, p, and q values into their variable
        readResultsFromFile(); //read the stored encrypted results

        //server index one starts out with the turn belonging to it
        if (myIndex == 1)
            myTurn = true;

        //setting up the next server to pass a turn to. If this server's index is last in the sequence, loop around. Else, it's the next higher index
        if (myIndex == numServers)
            nextServerPort = serverPortNums[0];
        else
            nextServerPort = serverPortNums[myIndex];

        executor.execute(new Listening()); //listening port is on its own thread

        //if file is empty, make new passwords and write them to the file
        if (!readApprovedPasswordsFromFile())
            generateAndWriteApprovedPasswordsToFile();

        //if the routine to read the key from the file failed, hasN will be false, so we have to generate one (cooperating with other servers)
        if (!hasN)
            genBiprimalN();

        System.out.printf("Biprimal n: %s\n", n); //displaying our n in terminal

        myGUI.PressScreen(false); //options screen for the server operator (getting voter passwords or displaying results by key sharing
    }

    //called by the GUI. Shares our generated password stubs with the authority responsible for concatenating them with other servers and getting them to voters
    public static void sharePasswordsWithPasswordAuth()
    {
        //keep trying until we are able to share successfully
        boolean successfulShare = false;
        while (!successfulShare)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), passwordAuthPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());

                os.writeUTF(Integer.toString(myIndex));

                for (String password: approvedPasswords.keySet())
                {
                    os.writeUTF(password);
                }
                os.flush();
                successfulShare = true;
            } catch (Exception e) {}
        }
    }

    //read the stored voter password stubs into the approvedPasswords hashmap
    private static boolean readApprovedPasswordsFromFile() throws Exception
    {
        File file = new File("approvedPasswords" + myIndex + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        String splitLine[];

        //every line of the file is an office and its votes, which are encoded and encrypted

        //if first line is blank, the file is empty
        line = br.readLine();
        if (line == null)
            return false;

        //read the passwords and their use statuses into the approvedPasswords hashmap
        do
        {
            splitLine = line.split(" ");
            if (splitLine[1].equals("true"))
                approvedPasswords.put(splitLine[0], true);
            else
                approvedPasswords.put(splitLine[0], false);

            line = br.readLine();
        } while (line != null);

        return true;
    }

    //this generates password stubs and calls the function for writing them to file afterward
    private static void generateAndWriteApprovedPasswordsToFile() throws Exception
    {
        String possibleChars = "abcdefghijklmnopqrstuvwxyz0123456789"; //all letters and numbers

        String digits;
        for (int i = 0; i < totalVoters; i++) //for each eligible voter
        {
            do
            {
                digits = "";
                for (int j = 0; j < 3; j++) //three random letters/numbers
                {
                    digits += possibleChars.charAt(rand.nextInt(possibleChars.length()));
                }
            } while (approvedPasswords.get(digits) != null); //loop around if someone else already has the same password stub

            approvedPasswords.put(digits, false); //append to hashmap, they all haven't been used yet
        }
        //call function to save them to disk
        writeApprovedPasswordsToFile();
    }

    //save approved password stubs and their statuses to disk from the hashmap
    public static void writeApprovedPasswordsToFile() throws Exception
    {
        FileWriter file = new FileWriter("approvedPasswords" + myIndex + ".txt");

        for (Map.Entry<String, Boolean> passwordAndStatus : approvedPasswords.entrySet())
        {
            if (passwordAndStatus.getValue())
                file.write(passwordAndStatus.getKey() + " true\n");
            else
                file.write(passwordAndStatus.getKey() + " false\n");
        }
        file.close();
    }

    //generate a biprimal public key n
    public static void genBiprimalN() throws Exception
    {
        //continue trying until we get one that is biprimal
        do
        {
            pq = Crypto.genPQ(myIndex, bitLength, rand); //gen random p and q for this server

            //make a polynomial each to share p, and another to share q
            Polynomial pSharing = new Polynomial(2, pq[0], bitLength, rand);
            Polynomial qSharing = new Polynomial(2, pq[1], bitLength, rand);

            //make sure others have had time to generate a p and q/update them from the prior iteration
            while (!myTurn)
                Thread.sleep(1);

            for (int i = 0; i < numServers; i++) //send a share of p and q to each server. Each server gets the y value of x = its index (starting at 1) for the p and q polynomials of this server
            {
                //keep trying until the other server receives it
                boolean successfullySentPQShare = false;
                while (!successfullySentPQShare)
                {
                    try {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingPQ");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(pSharing.getValueAt(i + 1).toString());
                        os.writeUTF(qSharing.getValueAt(i + 1).toString());
                        os.flush();

                        successfullySentPQShare = true;
                    } catch (Exception e) { }
                }
            }

            //waiting so that we know all shares have been updated
            passTurn();
            while (!myTurn)
                Thread.sleep(1);

            //add all the shares together to make this server's share of n
            pShareSum = BigInteger.ZERO;
            qShareSum = BigInteger.ZERO;
            for (int i = 0; i < numServers; i++)
            {
                pShareSum = pShareSum.add(pShares[i]);
                qShareSum = qShareSum.add(qShares[i]);
            }
            BigInteger nShare = pShareSum.multiply(qShareSum);

            for (int portNum : serverPortNums) //send the share of n to each server
            {
                boolean successfullySentNShare = false;
                while (!successfullySentNShare)
                {
                    try
                    {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingNShare");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(nShare.toString());
                        os.flush();

                        successfullySentNShare = true;

                    } catch (Exception e) { }
                }
            }

            //waiting for other servers
            passTurn();
            while (!myTurn)
                Thread.sleep(1);

            //get ready to plug the sum of all nShares into the secret finder, which takes an array of points in the format [x, y]
            BigInteger tmp[][] = new BigInteger[numServers][2];
            for (int i = 0; i < tmp.length; i++)
            {
                tmp[i][0] = new BigInteger(Integer.toString(i + 1));
                tmp[i][1] = nShares[i].multiply(delta); //multiplying by delta to prevent decimals in the lagrange algorithm's division
            }

            n = Crypto.lagrangeGetSecret(tmp).divide(delta); //find n with interpolation. Divide by delta to un-do our multiplication done in the loop above

            if (myIndex == 1) //server index one is the one that chooses a gg (coprime of n)
            {
                //generate one, then share it with others
                gg = Crypto.getGG(n, rand);
                for (int portNum : serverPortNums)
                {
                    boolean sentGG = false;
                    while (!sentGG)
                    {
                        try
                        {
                            socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                            os = new ObjectOutputStream(socket.getOutputStream());
                            is = new ObjectInputStream(socket.getInputStream());
                            os.writeUTF("sendingGG");
                            os.writeUTF(gg.toString());
                            os.flush();

                            sentGG = true;
                        } catch (Exception e) { }
                    }
                }
            }

            //generate this server's share of Q (Qi) and distribute it to others
            BigInteger Qi = Crypto.getQi(n, gg, pq, myIndex);
            for (int portNum : serverPortNums)
            {
                boolean sentQi = false;
                while (!sentQi)
                {
                    try
                    {
                        socket = new Socket(InetAddress.getLocalHost().getHostAddress(), portNum);
                        os = new ObjectOutputStream(socket.getOutputStream());
                        is = new ObjectInputStream(socket.getInputStream());
                        os.writeUTF("sendingQi");
                        os.writeUTF(Integer.toString(myIndex - 1));
                        os.writeUTF(Qi.toString());
                        os.flush();

                        sentQi = true;
                    } catch (Exception e) { }
                }
            }

            System.out.printf("n: %s iteration %d\n", n, iterationNum++); //allows for the generation of n candidates to be observed in terminal

            //wait until others have caught up before testing biprimality
            passTurn();
            while (!myTurn)
                Thread.sleep(1);

        } while (!Crypto.isBiprimal(n, rand, Qi)); //test biprimality. Loop if the n candidate is not

        passTurn();

        //we have an n, which can be saved
        hasN = true;
        writeKeysToFile(); //saving n, p, and q to file now that we have a biprimal n
    }

    //called by the server whose client voted, and it used to distribute the correct stub to each server to determine if the password as a whole is genuine
    public static boolean distributeAndCheckPassword(String password)
    {
        //if the password isn't the right length, don't even try checking with others
        if (password.length() != 3 * numServers)
            return false;

        boolean rv = true; //return value because a direct return can't be used in a try/catch
        for (int i = 0; i < numServers; i++) //for each server
        {
            boolean sentPasswordStub = false;
            while (!sentPasswordStub && rv) //while the password stub hasn't been sent and the password hasn't been proven wrong yet
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("checkPartialPassword");
                    os.writeUTF(password.substring(3*i, 3*i + 3)); //get stub from whole password
                    os.flush();

                    sentPasswordStub = true;

                    if (is.readUTF().equals("no")) //we know that server wouldn't accept the ballot, so we need to let the user know
                        rv = false;

                } catch (Exception e) {}
            }
        }
        return rv; //no server had on objection to their stub
    }

    //send out shares of the server's secret key to the other servers. Used when election is over
    public static void shareDecryptionKey() throws Exception
    {
        BigInteger myLambda = n.add(BigInteger.ONE).subtract(pShareSum).subtract(qShareSum); //my share of lambda

        //waiting for cooperation
        while (!myTurn)
        {
            System.out.printf("Waiting for other servers to share decryption key.\n");
            Thread.sleep(500);
        }

        for (int i = 0; i < numServers; i ++) //send the appropriate share to each server
        {
            boolean sentThetaShare = false;
            while (!sentThetaShare)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingLambda");
                    os.writeUTF(Integer.toString(myIndex - 1));
                    os.writeUTF(myLambda.toString());
                    os.flush();

                    sentThetaShare = true;
                } catch (Exception e) {}
            }
        }

        //wait for other servers to send/receive theirs
        passTurn();
        while (!myTurn)
            Thread.sleep(1);

        //adding points to a Biginteger array of x, y points for the lagrange function
        BigInteger tmp[][] = new BigInteger[numServers][2];
        for (int i = 0; i < tmp.length; i++)
        {
            tmp[i][0] = new BigInteger(Integer.toString( i + 1));
            tmp[i][1] = lambdaShares[i].multiply(delta); //multiply by delta to prevent decimals
        }

        lambda = Crypto.lagrangeGetSecret(tmp).divide(delta); //undo multiplicaiton done in above loop

        passTurn();
    }

    //pass turn to next server in series
    private static void passTurn()
    {
        myTurn = false;

        boolean turnSent = false;
        while (!turnSent)
        {
            try
            {
                socket = new Socket(InetAddress.getLocalHost().getHostAddress(), nextServerPort);
                os = new ObjectOutputStream(socket.getOutputStream());
                is = new ObjectInputStream(socket.getInputStream());
                os.writeUTF("yourTurn");
                os.flush();

                turnSent = true;
            } catch (Exception e) {}
        }
    }

    //file reader for candidate list
    private static void readCandidatesFromFile() throws Exception
    {
        File file = new File("candidate_list.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        while (line != null)
        {
            CandidateNamesByOffice.add(line);
            line = br.readLine();
        }
    }

    //called by the GUI to update the candidate_counts array
    public static void getResults()
    {
        candidate_counts = new float [CandidateNamesByOffice.size()][];

        //for each office. Represented by rows
        int row = 0;
        for (String office : CandidateNamesByOffice)
        {
            String names[] = office.split(", ");

            encryptedSubtotals.get(names[0]); //the first after splitting is the office name
            candidate_counts[row] = new float[names.length - 1]; //since one of the names is of the office, we have lenth - 1 candidates to represent

            BigInteger decryptedOffice = Crypto.decrypt(encryptedSubtotals.get(names[0]), n, lambda); //the decrypted, but still encoded results for this office

            //decoding algorithm using total voters
            BigInteger remainder = decryptedOffice;
            for (int i = names.length - 1; i > 0; i--)
            {
                //the whole is the office's value, which we need to save to candidate_counts
                BigInteger whole = remainder.divide(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));
                candidate_counts[row][i-1] = whole.floatValue();

                //remainder is used next iteration
                remainder = decryptedOffice.mod(new BigInteger( Integer.toString( (int) Math.pow(totalVoters + 1, i - 1)) ));
            }
            row++;
        }
    }

    //read the n, p, and q from file
    private static void readKeysFromFile() throws Exception
    {
        File file = new File("serverKeys" + myIndex+ ".txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        line = br.readLine();

        //quit if the file is empty
        if (line == null)
            return;

        //else record the data
        n = new BigInteger(line);
        pShareSum = new BigInteger(br.readLine());
        qShareSum = new BigInteger(br.readLine());

        hasN = true; //update flag
    }

    //save N, p, and q to file
    private static void writeKeysToFile() throws Exception
    {
        FileWriter file  = new FileWriter("serverKeys" + myIndex + ".txt");

        file.write(n.toString() + "\n");
        file.write(pShareSum.toString() + "\n");
        file.write(qShareSum.toString() + "\n");

        file.close();
    }

    //reads existing subtotals from the appropriately-named file in the working directory
    private static void readResultsFromFile() throws Exception
    {
        encryptedSubtotals = new HashMap<>(); //fresh value for the class variable

        File file = new File("encryptedSubtotals" + myIndex + ".txt");
        BufferedReader br = new BufferedReader(new FileReader(file));

        String nameVotes[]; //[0]becomes a candidate's name and [1] is their encrypted subtotal

        //every line of the file is an office and its votes, colon separated
        String line = br.readLine();
        while (line != null)
        {
            nameVotes = line.split(": ");
            encryptedSubtotals.put(nameVotes[0], new BigInteger(nameVotes[1]));
            line = br.readLine();
        }
    }

    //writes to the subtotal file after the encryptedSubtotals hashmap is updated
    public static void writeResultsToFile() throws Exception
    {
        FileWriter file = new FileWriter("encryptedSubtotals" + myIndex + ".txt");

        //for every office, write the key (office title) and its encrypted subtotal, separated by a colon
        for (HashMap.Entry<String, BigInteger> entry : encryptedSubtotals.entrySet())
        {
            file.write(entry.getKey() + ": " + entry.getValue() + "\n");
        }

        file.close();
    }

    //called by the server whose client just voted and forwards the password and encrypted vote to other servers
    public static boolean distributeBallot(String password, HashMap<String, BigInteger> ballot)
    {
        for (int i = 0; i < serverPortNums.length; i++) //for each server
        {
            boolean sentBallot = false;
            while (!sentBallot)
            {
                try
                {
                    socket = new Socket(InetAddress.getLocalHost().getHostAddress(), serverPortNums[i]);
                    os = new ObjectOutputStream(socket.getOutputStream());
                    is = new ObjectInputStream(socket.getInputStream());
                    os.writeUTF("sendingBallot");
                    os.writeUTF(password.substring(3*i, 3*i + 3));

                    for (Map.Entry<String, BigInteger> officeResult : ballot.entrySet())
                    {
                        os.writeUTF(officeResult.getKey() + ":" + officeResult.getValue());
                    }
                    os.writeUTF("END");
                    os.flush();

                    //if there was an error, we can stop right now
                    if (is.readUTF().equals("error"))
                        return false;

                    sentBallot = true;

                } catch (Exception e) { }
            }
        }
        return true;
    }
}

//Class for the listener for clients. One thread is run for each client the server has
class ClientComm implements Runnable
{
    public void run()
    {
        int myIndex = Server.currentClientNum++; //assign a number on thread's startup so that it knows which port in the list of client ports to use

        try
        {
            //getting ready to receive input from the client
            ServerSocket serverSocket = new ServerSocket(Server.clientPortNums[myIndex]);
            Socket clientSocket = serverSocket.accept();
            ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());
            String line;

            while (true) //keep running until the main program is closed
            {
                line = is.readUTF();

                if (line.equals("getN"))  //we need to send the client public key n
                    os.writeUTF(Server.n.toString());

                else if (line.equals("getOfficesAndCandidates"))  //send offices and candidates
                {
                    for (String name : Server.CandidateNamesByOffice)  //the server stores these as officeName, candidate1, candidate2, etc. Each iteration is for one office and has these comma-separated values
                        os.writeUTF(name);

                    os.writeUTF("Total Voters:"); //tells client to expect total voters now
                    os.writeUTF(Integer.toString(Server.totalVoters));
                }

                else if (line.equals("haveKey?")) //client asking if we have the public key n ready
                {
                    if (!Server.hasN)
                        os.writeUTF("no");
                    else
                        os.writeUTF("yes");
                }

                else if (line.equals("sendingBallot")) //get ready to receive a ballot from the client
                {
                    String password = is.readUTF(); //password is sent first
                    String officeAndVote[] = is.readUTF().split(":"); //then the office:encryptedVote

                    //for each office/vote pair, consolidate to one ballot (the hashmap). We read the first one while initializing the officeAndVote array
                    HashMap<String, BigInteger> ballot = new HashMap<>();
                    while (!officeAndVote[0].equals("END"))
                    {
                        ballot.put(officeAndVote[0], new BigInteger(officeAndVote[1]));
                        officeAndVote = is.readUTF().split(":");
                    }

                    //use function to split the password into stubs and distribute the ballot
                    if (Server.distributeBallot(password, ballot))
                        os.writeUTF("counted");
                    else
                        os.writeUTF("error");
                }

                else if (line.equals("passwordCorrect?")) //client is checking if password is right (using external function) before letting voter place their vote
                {
                    if (Server.distributeAndCheckPassword(is.readUTF()))
                        os.writeUTF("yes");
                    else
                        os.writeUTF("no");
                }
                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                os.flush();
            }

        }catch(Exception e) {}
    }
}

//Class for the server's listening thread. Other servers try to connect to this when they need to
class Listening implements Runnable
{
    public void run()
    {
        while (true) //continue until main program is terminated
        {
            try
            {
                //getting ready to receive input on specified listening port
                ServerSocket serverSocket = new ServerSocket(Server.myListeningPort);
                Socket clientSocket = serverSocket.accept();
                ObjectInputStream is = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream os = new ObjectOutputStream(clientSocket.getOutputStream());

                String line = is.readUTF();

                if (line.equals("sendingPQ")) //receive shares of p and q from other server, which tells us its index - 1 first
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.pShares[index] = new BigInteger(is.readUTF());
                    Server.qShares[index] = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingGG")) //receive gg from server index 1
                    Server.gg = new BigInteger(is.readUTF());

                else if (line.equals("sendingQi")) //receive server i's Q share
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.Qi[index] = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingNShare")) //receive other server's nShare
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.nShares[index] = new BigInteger(is.readUTF());
                }
                else if (line.equals("sendingLambda")) //receive other's lambda share
                {
                    int index = Integer.parseInt(is.readUTF());
                    Server.lambdaShares[index] = new BigInteger(is.readUTF());
                }

                else if (line.equals("yourTurn")) //it's now this server's "turn"
                    Server.myTurn = true;

                else if (line.equals("sendingBallot")) //receive ballot from other server
                {
                    String myPassword = is.readUTF(); //get password
                    boolean passwordCorrect = false; //assume password is wrong

                    //for each approved password stub for this server,
                    for (String password : Server.approvedPasswords.keySet())
                    {
                        //...check if the password we're checking matches the provided one, if so, make sure it hasn't been used yet
                        if (myPassword.equals(password)
                                && !Server.approvedPasswords.get(password))
                        {
                            passwordCorrect = true; //the password was right
                            Server.approvedPasswords.put(password, true); //mark password as being used now

                            Server.writeApprovedPasswordsToFile(); //update the file
                            break; //no need to check other approved password stubs
                        }
                    }

                    BigInteger officeSubTotal; //store (encrypted) subtotal for each office
                    String officeAndVote[] = is.readUTF().split(":"); //sent over as office:encryptedVote

                    //continue getting other officeAndVote pairs until the ballot is complete (other server sends END), even if password is wrong
                    while (!officeAndVote[0].equals("END"))
                    {
                        if (Server.encryptedSubtotals.get(officeAndVote[0]) == null) //set the subtotal to zero if no one else has voted yet
                            officeSubTotal = new BigInteger("0");
                        else
                            officeSubTotal = Server.encryptedSubtotals.get(officeAndVote[0]);

                        officeSubTotal = Crypto.addEncrypted(officeSubTotal, new BigInteger(officeAndVote[1]), Server.n); //add the new vote to subtotal

                        //update the hashmap if the password is correct
                        if (passwordCorrect)
                            Server.encryptedSubtotals.put(officeAndVote[0], officeSubTotal);

                        officeAndVote = is.readUTF().split(":"); //continue reading
                    }

                    //send the result, then save it if necessary
                    if (passwordCorrect)
                    {
                        Server.writeResultsToFile();
                        os.writeUTF("success");
                    }
                    else
                        os.writeUTF("error");
                }
                else if (line.equals("checkPartialPassword")) //let server know that its client's password stub is acceptable
                {
                    //read the password and assume it's wrong until proven otherwise
                    String givenPassword = is.readUTF();
                    boolean passwordCorrect = false;

                    for (String possiblePassword : Server.approvedPasswords.keySet()) //for each accepted password
                    {
                        //...if the accepted password matches the supplied one and it hasn't been used yet
                        if (givenPassword.equals(possiblePassword)
                            && !Server.approvedPasswords.get(possiblePassword))
                        {
                            passwordCorrect = true; //mark as correct, then stop checking other accepted passwords
                            break;
                        }
                    }
                    if (passwordCorrect)
                        os.writeUTF("yes");
                    else
                        os.writeUTF("no");
                }
                else
                    System.out.printf("Unexpected message arrived at server: %s\n", line);

                os.flush();
                serverSocket.close();
            } catch (Exception e) { }
        }
    }
}