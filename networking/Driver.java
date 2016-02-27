/*
 * Testing: main thread is client, background thread is server.
 */
package networking;

/**
 *
 * @author chris
 */
public class Driver {
    
    /**
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int port = 8125;
        int numThreads = 20;
        Server server = new Server(port);
        Client client = new Client(port, numThreads);
        
        //Start background server thread
        server.startServer(false);//use iterative version
        client.showMainMenu();
        
        //User has selected quit -- Shut it down.
        server.stopServer();
        System.exit(0);
    }
    
}
