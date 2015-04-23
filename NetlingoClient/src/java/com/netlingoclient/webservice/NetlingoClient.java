
/*
 * CS4262 Distributed Systems Mini Project
 */
package com.netlingoclient.webservice;

/**
 *
 * @author Netlingo
 */

// ******************************************************************
// NO NEED OF update() FOR WEB SERVICE: cases join, inquire, files covered: REST is due to be covered
// Added web methods for inquire [completed] join [completed], informFileList [completed], search ,leave
//  TODO: search, leave
// Port no is not needed. So I removed port related args. We can just define port for BS communication only in Config
// Compilation errors occured due to those port related changes... stopped changing further as I'm not familiar with searchcode
// In start(), the changes have redundant code : can resolve them
// BTW :-D Sorry for the mess! 
// ***************************************************Dinithi :-D***************
                                                
   
import com.netlingoclient.classes.*;
import com.netlingoclient.config.Config;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(serviceName = "NetlingoClient")
public class NetlingoClient extends Observable implements Observer {

    private final String myIp;
    private final String myName;
    private static NetlingoClient instance = null;
    private boolean isSuper;
    // only available if this is a normal node
    private String mySuperNode;  // supernode = "peer_IP:port_no"
    //only available if this is a super node
    private ArrayList<String> superPeers = new ArrayList<>();
    private ArrayList<String> childNodes = new ArrayList<>();
    private HashMap<String, String> routingTable = new HashMap<>();
    private int inquireResponses;
    //private final Sender com;
    private HashMap<String, Integer> joinRequestSentPeers;

    public static NetlingoClient getInstance(String ip, String name) {
        if (instance == null) {
            instance = new NetlingoClient(ip, name);
        }
        return instance;
    }

    private NetlingoClient(String ip, String name) {
        this.myIp = ip;
        this.myName = name;
        isSuper = Config.isSuper;
        joinRequestSentPeers = new HashMap<>();
        addMyFiles(2);
        this.addObserver(Config.CONFIG_WINDOW);
    }

    @WebMethod(operationName = "hello")
    public String hello(@WebParam(name = "name") String txt) {

        return "";
    }

    @WebMethod(operationName = "inquire")
    public String inquire(@WebParam(name = "incomingIP") String incomingIP) {

        System.out.println("Received INQUIRE message from " + incomingIP + ":" + incomingPort);
        if (isSuper) {
            return "INQUIREOK " + myIp + " " + myPort;
        } else {
            String[] superNodeInfo = mySuperNode.split(":");
            return "INQUIREOK " + superNodeInfo[0] + " " + superNodeInfo[1];
        }


    }

    @WebMethod(operationName = "join")
    public String join(@WebParam(name = "incomingIP") String incomingIP,
            @WebParam(name = "incomingPeerName") String incomingPeerName, @WebParam(name = "isSuper") boolean isSuper) {

        if (isSuper) {
            System.out.println("Added peer super node:" + incomingPeerName);
            superPeers.add(incomingIP);
        } else {
            System.out.println("Added child node:" + incomingPeerName);
            childNodes.add(incomingIP);
        }

        return "JOINOK " + myIp;
    }
    
        @WebMethod(operationName = "informFileList")
    public String informFileList(@WebParam(name = "incomingIP") String incomingIP, @WebParam(name = "myTerms") String myTerms) {
          if (!isSuper) {
                    System.out.println("Unexpected request");
                } else {
                    addChildrensFiles(myTerms, incomingIP);
                }
        return "";
    }
    

    @WebMethod(operationName = "search")
    public String search(@WebParam(name = "incomingIP") String incomingIP, @WebParam(name = "fileName") String fileName) {

        return "";
    }

    @WebMethod(operationName = "leave")
    public String leave(@WebParam(name = "incomingIP") String incomingIP, @WebParam(name = "leavingPeerName") String leavingPeerName) {
        return "";
    }

    private void addMyFiles(int numberOfNodes) {

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(new File("src/resources/FileNames")));
            String readLine = null;
            int lineNumber = 0;
            String[] ipComponents = myIp.split("\\.");
            int ipRemainder = Integer.parseInt(ipComponents[3]) % numberOfNodes;
            while ((readLine = reader.readLine()) != null) {
                if (lineNumber % numberOfNodes == ipRemainder) {
                    String[] terms = readLine.toLowerCase().split(" ");
                    String fileName = readLine.replace(" ", "_");
                    for (String term : terms) {
                        if (myFiles.containsKey(term)) {
                            (myFiles.get(term)).add(fileName);
                        } else {
                            ArrayList<String> files = new ArrayList<>();
                            files.add(fileName);
                            myFiles.put(term, files);
                        }
                    }
                }
                lineNumber++;
            }
            //myFiles.put("Windows",new String[]{"Windows XP","Windows 8"});
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NetlingoClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NetlingoClient.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(NetlingoClient.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println(myFiles);
        }
    }

    private void addChildrensFiles(String termsString, String childIp) {
        Set<String> childTerms = chilrensFiles.keySet();
        String[] terms = termsString.split(",");
        for (String term : terms) {
            if (childTerms.contains(term)) {
                ArrayList<String> childrenHavingTerm = chilrensFiles.get(term);
                childrenHavingTerm.add(childIp);
            } else {
                ArrayList<String> ipAddress = new ArrayList<>();
                ipAddress.add(childIp);
                chilrensFiles.put(term, ipAddress);
            }
        }
    }

    public String getIp() {
        return myIp;
    }

    public int getPort() {
        return myPort;
    }

    /*
    Register node in super node
     */
    public RegisterResponse register() {

        String message = (new Message(MessageType.REG, myIp, myPort, myName)).getMessage();
        UpdateTheLog("<Sending msg> " + message + " >> to BS");
        String response = Sender.getInstance().sendTCPMessage(message);

        UpdateTheLog("<Received msg> " + response + " << from BS");
        System.out.println("Response:" + response);
        String[] splitted = response.split(" ");

        String noOfNodes = splitted[2];
        String[] peerIps;
        int[] peerPorts;

        System.out.println(noOfNodes);

        switch (noOfNodes.trim()) {
            case "0":
                isSuper = true;
                return new RegisterResponse(MessageType.REG_SUCCESS, null, null);
            // break;
            case "1":
                isSuper = true;
                peerIps = new String[1];
                peerPorts = new int[1];
                peerIps[0] = splitted[3];
                peerPorts[0] = Integer.parseInt(splitted[4]);
                //  System.out.println(joinNetwork(peerIps[0], peerPorts[0]));
                return new RegisterResponse(MessageType.REG_SUCCESS, peerIps, peerPorts);
            //  break;
            case "9996":
                System.out.println("Failed, can’t register. BS full.");
                UpdateTheLog("Failed, can’t register. BS full.");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //     break;
            case "9997":
                System.out.println("Failed, registered to another user, try a different IP and port");
                UpdateTheLog("Failed, registered to another user, try a different IP and port");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //  break;
            case "9998":
                System.out.println("Failed, already registered to you, unregister first");
                UpdateTheLog("Failed, already registered to you, unregister first");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            // break;
            case "9999":
                System.out.println("Failed, there is some error in the command");
                UpdateTheLog("Failed, there is some error in the command");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //  break;

            default:
                int number = Integer.parseInt(noOfNodes);
                peerIps = new String[number];
                peerPorts = new int[number];
                System.out.println("number:" + number);
                for (int i = 1; i < number + 1; i++) {
                    peerIps[i - 1] = splitted[3 * i];
                    peerPorts[i - 1] = Integer.parseInt(splitted[3 * i + 1]);
                    System.out.println(peerIps[i - 1] + "," + peerPorts[i - 1]);
                }

                return new RegisterResponse(MessageType.REG_SUCCESS, peerIps, peerPorts);

        }
    }

    private void unregister() {
        String message = (new Message(MessageType.UNREG, myIp, myPort, myName)).getMessage();

        Sender.getInstance().sendTCPMessage(message);
    }

    //can pass another argument(eg: String arg) and pass that on to Message constructor 
    //in addition to what is already in it. Then we can replace the function below with this 
//    private void sendMessage(MessageType msgType, String peerIp, int peerPort) {
//        String message = (new Message(msgType, myIp, myPort, name)).getMessage();
//        UpdateTheLog("<Sending msg> "+message);
//        Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
//    }
    private void sendMessage(String message, String peerIp, int peerPort) {
        System.out.println("sending message: " + message + " from:" + Config.MY_IP + ":" + Config.MY_PORT + " to:" + peerIp + ":" + peerPort);
        Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
    }

    private int[] getRandomTwo(int number) {

        if (number == 2) {
            return new int[]{0, 1};
        }
        int rand1 = (int) (Math.random() * 1000 % number);
        int rand2 = (int) (Math.random() * 1000 % (number));

        while (rand1 == rand2) {
            rand2 = (int) ((Math.random() * 1000) % (number));
        }
        int[] array = {rand1, rand2};
        return array;
    }

    private int getRandomNo(int number) {
        return (int) (Math.random() * 1000 % number);
    }

    private int getRandomNo(int number, int exception) {
        if (number == 1) {
            return -1;
        }
        int value;
        while ((value = getRandomNo(number)) == exception);
        return value;
    }
    //my files
    //stored as an invereted index
    //format term:set of files
    private HashMap<String, ArrayList<String>> myFiles = new HashMap<>();
    //Store the files children have in key,peers format
    private HashMap<String, ArrayList<String>> chilrensFiles = new HashMap<>();

    public HashMap getMyFiles() {
        return myFiles;
    }

    // NO NEED OF THIS METHOD FOR WEB SERVICE: SHOULD DIVIDE LOGIC TO AMONG WEB METHODS
    // TODO: search, leave
    @Override
    public void update(Observable o, Object arg) {
        //Process incoming message
        UDPResponse receivedMessage = (UDPResponse) arg;
        String incoming = receivedMessage.getData();
        System.out.println("incoming message:" + incoming);
        String[] msg = incoming.split(" ");
        MessageType msgType = MessageType.valueOf(msg[1]);
        String requesterIp = null;
        int requesterPort = 0;

        switch (msgType) {
            case SEROK:
                String success = msg[2].trim();
                if (!success.equals("0")) {
                    requesterIp = msg[3].trim();
                    requesterPort = Integer.parseInt(msg[4].trim());
                }
                break;

            case JOINOK:
                break;

            default:
                requesterIp = msg[2].trim();
                requesterPort = Integer.parseInt(msg[3].trim());
        }
        String info = requesterIp + ":" + requesterPort;
        String outGoingMessage;
        switch (msgType) {
          
            case SER:
                String[] messageComponents = incoming.split("\"");
                String[] searcherIpPort = messageComponents[0].split(" ");
                String searcherIp = searcherIpPort[2];
                int searcherPort = Integer.parseInt(searcherIpPort[3]);
                String fileKey = messageComponents[1];
                int hopCount;
                System.out.println("Hop count length:" + messageComponents[2].length());
                try {
                    hopCount = 1 + Integer.parseInt(messageComponents[2].trim());
                } catch (NumberFormatException e) {
                    hopCount = 1;
                }
                System.out.println("Search message received for key:" + fileKey);
                //check if I have the file
                if (hopCount < Config.TTL) {
                    fileKey = fileKey.toLowerCase();
                    boolean locatable = false;
                    if (myFiles.containsKey(fileKey)) {
                        locatable = true;
                        ArrayList<String> files = myFiles.get(fileKey);
                        int noOfFiles = files.size();
                        //first send the list of files to the searcher
                        String response = (new Message(MessageType.SEROK, noOfFiles, Config.MY_IP, Config.MY_PORT, hopCount, files, fileKey)).getMessage();
                        System.out.println("Created response:" + response);
                        sendMessage(response, searcherIp, searcherPort);
                    }
                    //if I am a super peer, forward the search message to respective peers
                    if (isSuper) {
                        //forward the search query to a random peers

                        int randomPeerNumer = getRandomNo(superPeers.size(), superPeers.indexOf(searcherIp + ":" + searcherPort));
                        String[] ipPort;
                        if (randomPeerNumer != -1) {
                            locatable = true;
                            ipPort = (superPeers.get(randomPeerNumer)).split(":");
                            ////search(fileKey, searcherIp, searcherPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);
                            System.out.println("adding to routing table,key:" + ipPort[0] + fileKey + "   value:" + searcherIp + ":" + searcherPort);
                            routingTable.put(ipPort[0] + fileKey, searcherIp + ":" + searcherPort);
                            search(fileKey, myIp, myPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);
                        }

                        //next forward the search query to children having the file
                        if (chilrensFiles.containsKey(fileKey)) {
                            locatable = true;
                            ArrayList<String> peersWithFile = chilrensFiles.get(fileKey);
                            for (String peer : peersWithFile) {
                                ////search(fileKey, searcherIp, searcherPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);
                                ipPort = peer.split(":");
                                routingTable.put(ipPort[0] + fileKey, searcherIp + ":" + searcherPort);
                                search(fileKey, myIp, myPort, ipPort[0], Integer.parseInt(ipPort[1]), hopCount);

                            }
                        }

                        if (!locatable) {
                            String response = (new Message(MessageType.SEROK, 0)).getMessage();
                            System.out.println("Created response:" + response);
                            sendMessage(response, searcherIp, searcherPort);
                        }
                    }

                }
                break;
            case SEROK:
                String[] parts = incoming.split(" ");
                int noOfFiles = Integer.parseInt(parts[2].trim());
                switch (noOfFiles) {
                    case 0:
                        System.out.println("Files not found!");
                        break;
                    case 1:
                        System.out.println("Files found:");
                        System.out.println(incoming);
                        forwardSEROKToImmediateRequester(incoming, receivedMessage.getIpAddress());
                        break;
                    case 9999:
                        System.out.println("Node unreachable");
                        break;
                    case 9998:
                        System.out.println("Unknown error occured...");
                        break;

                }
                break;
            case LEAVE:
                incoming = (String) arg;
                int length = msg.length;
                //if its just a child asking to leave, remove him from the childNodes list 
                //and remove all file names from the super node which were in the leaving node but not in any other children
                if ("CHILD-LEAVING".equals(msg[length].trim())) {
                    //TO-DO: need to remove all file names from the super node which were in the leaving node but not in any other children

                    childNodes.remove(msg[2].trim() + ":" + msg[3].trim());
                } //if it is a super node that is leaving, take the ip and port it sends and send an INQUIRE message to it asking to connect
                else {
                    String[] ipPort = msg[length].split(":");
                    outGoingMessage = (new Message(MessageType.INQUIRE, myIp, myPort, "")).getMessage();
                    sendMessage(outGoingMessage, ipPort[0].trim(), Integer.parseInt(ipPort[1].trim()));
                }

            case LEAVEOK:
                break;

        }

    }

    public void start() {

        // normal just as in phase 2
        RegisterResponse response = register();

        if (response.isSucess()) {

//            Thread reciever = new Thread(Reciever.getInstance());
//            reciever.start();

            //now join the network
            String[] peerIPs = response.getPeerIps();
            System.out.println("Peer IPs");
            if (peerIPs != null) {
                for (String i : peerIPs) {
                    System.out.println(i);
                }
            }
            int[] peerPorts = response.getpeerPorts();

            if (peerIPs != null) {

                if (isSuper) {
                    System.out.println("I am Super");
                    int[] randomPeers;
                    if (peerIPs.length >= 2) {
                        //get random 2 peers to connect and check for super peer
                        randomPeers = getRandomTwo(peerIPs.length);
                        inquireResponses = 2;
                        for (int peer : randomPeers) {
                            System.out.println("random peer: " + peerIPs[peer]);

                            // [TODO]Call inquire() web method on peerIPs[peer], send myIP, myPort
                            // [TODO]get response as string
                            String inquireResponse = ""; // here goes the web service call
                            String[] msg = inquireResponse.split(" ");
                            String potentialSuperPeerIp = msg[2].trim();

                            joinRequestSentPeers.put(potentialSuperPeerIp, requesterPort);
                            // [TODO] call join method on potentialSuperPeer IP, send myIP and my name, am I super/not
                            // [TODO] get response as a string]
                             // Checking if JOINOK success
                        if (resp.split(" ")[1].equals("0")) {
                            superPeers.add(potentialSuperPeerIp);
                        System.out.println("Added peer super node: " + potentialSuperPeerIp);
                        }
                           


                        }
                    } else {
                        if (peerIPs.length == 1) {
                            // [TODO]Call inquire() web method on peerIPs[0], send myIP, myPort
                            // [TODO]get response as string
                            String inquireResponse = ""; // here goes the web service call
                            String[] msg = inquireResponse.split(" ");
                            String potentialSuperPeerIp = msg[2].trim();
                            // [TODO] call join method on potentialSuperPeer IP, send myIP and my name, am I super/not
                            // [TODO] get response as a string]
                             // Checking if JOINOK success
                        if (resp.split(" ")[1].equals("0")) {
                            superPeers.add(potentialSuperPeerIp);
                        System.out.println("Added peer super node: " + potentialSuperPeerIp);
                        }
                            
                        }
                    }
                } else {
                    System.out.println("I am Normal");
                    // get a peer to connect and check for super peer
                    if (peerIPs.length > 0) {
                        int peer;
                        if (peerIPs.length == 1) {
                            peer = 0;
                        } else {
                            peer = getRandomNo(peerIPs.length);
                        }
                        System.out.println("random peer: " + peerIPs[peer]);
                        // [TODO]Call inquire() web method on peerIPs[peer], send myIP, myPort
                        // [TODO]get response as string
                        String inquireResponse = ""; // here goes the web service call
                        String[] msg = inquireResponse.split(" ");
                        String potentialSuperPeerIp = msg[2].trim();

                        // [TODO] call join method on potentialSuperPeer IP, send myIP and my name, am I super/not
                        // [TODO] get response as a string]
                        // Checking if JOINOK success
                        if (resp.split(" ")[1].equals("0")) {
                            mySuperNode = potentialSuperPeerIp;
                            System.out.println("Added peer super node: " +potentialSuperPeerIp);
                            //Send the indexed terms for the files I have, to my super peer
                            Iterator iterator = myFiles.keySet().iterator();
                            String myTerms = "";
                            while (iterator.hasNext()) {
                                myTerms = "," + (String) iterator.next();
                            }
                            // [TODO] call informFileList() on super peer with my IP, myTerms.substring(1)).getMessage()
                            
                        }
                    }

                }

            }
            String message = "Peer " + myName + " joined the network...";
            System.out.println(message);
            //UpdateTheLog(message);
        }

    

    

    public void search(String fileName) {
        String[] ipPort;
        if (isSuper) {
            int noOfSuperPeers = superPeers.size();
            int randomSuperPeer = getRandomNo(noOfSuperPeers);
            System.out.println("Selected super peer to send search message: " + superPeers.get(randomSuperPeer));
            ipPort = (superPeers.get(randomSuperPeer)).split(":");
        } else {
            ipPort = mySuperNode.split(":");
        }
        search(fileName, ipPort[0], Integer.parseInt(ipPort[1]));
    }

    public void search(String fileName, String peerIp, int peerPort, int hopCount) {
        search(fileName, Config.MY_IP, Config.MY_PORT, peerIp, peerPort, hopCount);
    }

    public void search(String fileName, String searcherIp, int searcherPort, String peerIp, int peerPort, int hopCount) {
        String fileNameString = "\"" + fileName + "\"";
        String message = (new Message(MessageType.SER, searcherIp, searcherPort, fileNameString, hopCount)).getMessage();
        System.out.println("created message" + message);
        Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
        System.out.println("Message sent:" + message);
    }

    public void search(String fileName, String searcherIp, int searcherPort, String peerIp, int peerPort) {
        String fileNameString = "\"" + fileName + "\"";
        String message = (new Message(MessageType.SER, searcherIp, searcherPort, fileNameString)).getMessage();
        System.out.println("created message" + message);
        Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
        System.out.println("Message Sent:" + message);
    }

    public void search(String fileName, String peerIp, int peerPort) {
        search(fileName, myIp, myPort, peerIp, peerPort);
    }

    public void UpdateTheLog(String msg) {
        setChanged();
        notifyObservers(msg);
        clearChanged();
    }

    public void leave() {
        String[] ipPort;
        String message;
        //if I am a superNode 
        if (isSuper) {
            //send messages to all peers saying I am leaving and give them the ip and port of another super peer to connect with
            int noOfPeers = superPeers.size();
            for (int i = 0; i < noOfPeers; i++) {
                ipPort = superPeers.get(i).split(":");
                if (i < noOfPeers / 2) {
                    //if the index of the super peer in the super peer array list is < length/2 then direct him to join (i+1)th super peer
                    message = (new Message(MessageType.LEAVE, myIp, myPort, superPeers.get(i + 1))).getMessage();
                } else {
                    message = (new Message(MessageType.LEAVE, myIp, myPort, superPeers.get(i - 1))).getMessage();
                }
                sendMessage(message, ipPort[0].trim(), Integer.parseInt(ipPort[1].trim()));
            }

            //send messages to all children saying I am leaving and give them the ip and port of a super peer to connect with
            int noOfChildren = childNodes.size();
            for (int i = 0; i < noOfChildren; i++) {
                ipPort = childNodes.get(i).split(":");
                if (i <= noOfPeers) {
                    message = (new Message(MessageType.LEAVE, myIp, myPort, superPeers.get(i))).getMessage();
                } else {
                    message = (new Message(MessageType.LEAVE, myIp, myPort, superPeers.get(i - noOfPeers))).getMessage();
                }
                sendMessage(message, ipPort[0].trim(), Integer.parseInt(ipPort[1].trim()));
            }
        } //if I am not a super Node then just tell the super peer that I am leaving
        else {
            ipPort = mySuperNode.split(":");
            //just pass null to show that I am a normal node
            message = (new Message(MessageType.LEAVE, myIp, myPort, null)).getMessage();
            sendMessage(message, ipPort[0].trim(), Integer.parseInt(ipPort[1].trim()));
        }
    }

    private void forwardSEROKToImmediateRequester(String incoming, String senderIp) {

        String[] parts = incoming.split(" ");

        String routingTableKey, key, immediateRequesterIpPort[];

        System.out.println("Files found:");
        System.out.println(incoming);

        key = parts[6];
        routingTableKey = senderIp + ":" + key;

        if (routingTable.containsKey(key)) {
            immediateRequesterIpPort = routingTable.get(routingTableKey).split(":");
            routingTable.remove(routingTableKey);

            sendMessage(incoming, immediateRequesterIpPort[0], Integer.parseInt(immediateRequesterIpPort[1]));
        } else {
            System.out.println("Files found");
        }

    }
}