# SecureVotingProject_CIS433
This readme file contains the directions for using this software, which has three components: Server (main computer at polling place), Client (voting machine at polling place, and a text-based pseudo-password distributor (PasswordAuthority). For the project to be run on a single computer, commponents communicate via localhost on port speficied by parameters. These instructions should allow one to demonstrate the software, which illustrates the concepts of our secure voting system implementation.

##Understanding the command-line parameters:
After navigating to the directory containing the Java classes, the component programs can be run with as follows with the specified parameters:

###The Servers:
javaw Server <myIndex> <N> <serverIndex1's port> <serverIndex2's port>...<serverIndexN's port> <C> <client1's port>...<clientC's port> <PasswordAuthority's port> <total number of eligible voters>

myIndex is the server's index (the first server has an index one and the last has index N).
N is the total number of servers.
C is the total number of clients.
The PasswordAuthority's port should be specified, even if the passwords have been distributed already.

Example commands with parameters for running 5 servers:
javaw Server 1 5 901 902 903 904 905 2 1001 1011 888 100
javaw Server 2 5 901 902 903 904 905 2 1002 1012 888 100
javaw Server 3 5 901 902 903 904 905 2 1003 1013 888 100
javaw Server 4 5 901 902 903 904 905 2 1004 1014 888 100
javaw Server 5 5 901 902 903 904 905 2 1005 1015 888 100

###The Clients:
javaw Client <myPort>

If  using the above example commands for 5 servers, you should start ten clients with these commands/parameters:
javaw Client 1001
javaw Client 1002
javaw Client 1003
javaw Client 1004
javaw Client 1005
javaw Client 1011
javaw Client 1012
javaw Client 1013
javaw Client 1014
javaw Client 1015


###The PasswordAuthority:
java PasswordAuthority <N> <myPort> <total number of eligible voters>
N is again the total number of servers.

If using the above example parameters for 5 servers, run the PasswordAuthority with this command/parameter:
java PasswordAuthority 5 888 100

##Formatting the offices and candidates files:
Since by default the classes share the same working directory, they can share the file that details the offices and candidates, which is named candidate_list.txt. The formatting works as follows:

<name of first office>, <first candidate name>,...<last candidate name>
...
<name of last office>, <first candidate name>,...<last candidate name>

##Initial launch and n calculation:
Both the client and servers will wait until a biprimal n has been calculated before they allow votes to be placed, or anything to be shared. This can take up to two minutes with the currently-selected keylength. 

##Generating voter passwords and pseudo-distributing them: 
With the servers running, run the PasswordAuthority. Then, on each server, click the button "Share Passwords." This will send each server's automatically-generated password stubs to the PasswordAuthority, which will write to compositePasswords.txt in its working directory once it has collected them all.

##Subsequent launches of the program:
After a biprimal n has been found, the servers retain this, as well as the encrypted results of the election and approved password stubs, in their respective .txt files. These are named as follows:

serverKeysi.txt (contains the n and the server's p and q values)
encryptedSubtotalsi.txt (contains the offices and results, encrypted)
approvedPasswordsi.txt (contains the approved password stubs for the server)
i is the server's index

None of these files need to be modified manually, but empty files should be created in the servers' working directories (the same directory by default). However, you can reset them by following the last set of instructions.


##Cleaning the server files for a fresh demonstration:
Included in the output directory is a folder named "Default txt files." This has candidate_list.txt of sample offices and candidates, a blank compositePasswords.txt, and other blank txt's for servers up to index 5.

These can be copied to the root of the working directory to overwrite the current election.