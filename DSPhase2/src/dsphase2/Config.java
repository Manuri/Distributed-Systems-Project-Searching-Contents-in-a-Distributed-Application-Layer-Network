/*
 * CS4262 Distributed Systems Mini Project
 */

package dsphase2;

import java.util.ArrayList;

/**
 *
 * @author Amaya
 */
public class Config {
    
    //Whe  configured as normal node
    static final String MY_IP = "127.0.0.1";
    static final int MY_PORT = 5001;
    static final String MY_NAME = "Devni";
    
    
    //When configured as peer
//    static final String MY_IP = "127.0.0.2";
//    static final int MY_PORT = 5002;
//    static final String MY_NAME = "Sasikala";
    
    static final String BOOTSTRAP_IP = "127.0.0.1";
    static final int BOOTSTRAP_PORT = 9876;
    static ArrayList<String> availableFiles = new ArrayList<>(); 
    
    public void addNewFile(String fileName){
        availableFiles.add(fileName); 
    }
    
    
     
    
    
}
