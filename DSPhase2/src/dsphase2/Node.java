/*
 * CS4262 Distributed Systems Mini Project
 */
package dsphase2;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author Amaya
 */
public class Node implements Observer {

    private final String ip;
    private final int port;
    private final String name;
    private static Node instance = null;
    private boolean superNode;
    // only available if this is a normal node
    private String supernode;  // supernode = "peer_IP:port_no"
    //only available if this is a super node
    private ArrayList<String> superPeers = new ArrayList<>();
    private ArrayList<String> clusterNodes = new ArrayList<>();
    private int inquireResponses;
    //private final Sender com;

    public static Node getInstance(String ip, int port, String name) {
        if (instance == null) {
            instance = new Node(ip, port, name);
        }
        return instance;
    }

    private Node(String ip, int port, String name) {
        this.ip = ip;
        this.port = port;
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    /*
    Register node in super node
     */
    public RegisterResponse register() {

        String message = (new Message(MessageType.REG, ip, port, name, isSuper(), 0)).getMessage();

        String response = Sender.getInstance().sendTCPMessage(message);

        System.out.println("Response:" + response);
        String[] splitted = response.split(" ");

        String noOfNodes = splitted[2];
        String[] peerIps;
        int[] peerPorts;

        System.out.println(noOfNodes);

        switch (noOfNodes.trim()) {
            case "0":
                superNode = true;
                return new RegisterResponse(MessageType.REG_SUCCESS, null, null);
            // break;
            case "1":
                superNode = true;
                peerIps = new String[1];
                peerPorts = new int[1];
                peerIps[0] = splitted[3];
                peerPorts[0] = Integer.parseInt(splitted[4]);
                //  System.out.println(joinNetwork(peerIps[0], peerPorts[0]));
                return new RegisterResponse(MessageType.REG_SUCCESS, peerIps, peerPorts);
            //  break;
            case "9996":
                System.out.println("Failed, can’t register. BS full.");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //     break;
            case "9997":
                System.out.println("Failed, registered to another user, try a different IP and port");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //  break;
            case "9998":
                System.out.println("Failed, already registered to you, unregister first");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            // break;
            case "9999":
                System.out.println("Failed, there is some error in the command");
                return new RegisterResponse(MessageType.REG_FAILURE, null, null);
            //  break;

            default:
                if (isSuper()) {
                    superNode = true;
                }
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
        String message = (new Message(MessageType.UNREG, ip, port, name, isSuper(), 0)).getMessage();

        Sender.getInstance().sendTCPMessage(message);
    }

    private void sendMessage(MessageType msgType, String peerIp, int peerPort) {
        String message = (new Message(msgType, ip, port, name, isSuper(), 0)).getMessage();
        Sender.getInstance().sendUDPMessage(message, peerIp, peerPort);
    }

    private boolean isSuper() {
        if (Math.random() >= 0.5) {
            return true;
        } else {
            return false;
        }
    }

    private int[] getRandomTwo(int number) {
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

    @Override
    public void update(Observable o, Object arg) {
        //Process incoming message
        String incoming = (String) arg;
        String[] msg = incoming.split(" ");
        MessageType msgType = MessageType.valueOf(msg[1]);
        String peerIp = msg[2];
        int peerPort = Integer.parseInt(msg[3]);
        switch (msgType) {
            // for inquire msg : <length INQUIRE IP_address port_no is_super>
            case INQUIRE:
                if (isSuper()) {
                    sendMessage(MessageType.INQUIREOK, ip, port);
                } else {
                    String[] superNodeInfo = supernode.split(":");
                    sendMessage(MessageType.INQUIREOK, superNodeInfo[0], Integer.parseInt(superNodeInfo[1]));
                }
                break;
            // for inquire reply: <length INQUIREOK IP_address port_no> 
            case INQUIREOK:
                inquireResponses--;
                sendMessage(MessageType.JOIN, peerIp, peerPort);
                String info = peerIp + ":" + peerPort;
                if (isSuper()) {
                    String superPeer = info;
                    superPeers.add(superPeer);
                } else {
                    supernode = info;
                }
                break;
            // for join req : <length JOIN IP_address port_no>
            case JOIN:
                clusterNodes.add(peerIp + ":" + peerPort);
                sendMessage(MessageType.JOINOK, ip, port);
                break;
            //for join resp length JOINOK value
            case JOINOK:
                break;

        }

    }

    public void start() {

        RegisterResponse response = register();

        if (response.isSucess()) {
            Thread reciever = new Thread(Reciever.getInstance());
            reciever.start();

            //now join the network
            String[] peerIPs = response.getPeerIps();
            int[] peerPorts = response.getpeerPorts();


            if (isSuper()) {
                //get random 2 peers to connect and check for super peer
                int[] arr = getRandomTwo(peerIPs.length);
                inquireResponses = 2;
                for (int peer : arr) {
                    sendMessage(MessageType.INQUIRE, peerIPs[peer], peerPorts[peer]);
                }
            } else {
                // get a peer to connect and check for super peer
                int peer = getRandomNo(peerIPs.length);
                inquireResponses = 1;
                sendMessage(MessageType.INQUIRE, peerIPs[peer], peerPorts[peer]);
            }

//            // wait until all responses are received for INQUIRE message
//            while (inquireResponses != 0) {
//                continue;
//            }
//            
            while(true){
                continue;
            }
        }
    }
}
