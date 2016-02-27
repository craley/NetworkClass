/**
 * 
 * Server.java
 */

package proj1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author chris
 */
public class Server {

    Thread runner;
    Lock lock = new ReentrantLock();
    Condition pause = lock.newCondition();
    boolean isRunning;
    int port;
    
    //Available server commands
    public static final String[] COMMANDS = {
        "date",
        "uptime",
        "free -m",
        "netstat -a",
        "who",
        "ps -e"
    };

    public Server(int port) {
        this.port = port;
    }

    public void startServer(boolean useConcurrent) {
        isRunning = true;
        System.out.println("Starting Server Daemon Now...");
        runner = new Thread(new ServerTask(useConcurrent), "Server");
        runner.start();
    }

    public void stopServer() {
        isRunning = false;
        runner.interrupt();
        runner = null;
    }

    /**
     * Background Server Daemon.
     */
    class ServerTask implements Runnable {

        boolean useConcurrent;

        public ServerTask(boolean isConcurrent) {
            useConcurrent = isConcurrent;
        }

        @Override
        public void run() {
            if (useConcurrent) {
                concurrentServer();
            } else {
                iterativeServer();
            }
        }

        public void iterativeServer() {
            try {
                ServerSocket ssocket = new ServerSocket(port);
                System.out.println("Server bound to port: " + port);
                while (isRunning) {
                    Socket clientSocket = ssocket.accept();//blocks until connection
                    System.out.println("Server: Accepted Connection. Address: " + clientSocket.getLocalAddress() + " Port: " + clientSocket.getLocalPort());
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    //get requested operation
                    String request = in.readLine();//Blocks server until client's request comes through
                    
                    if (request != null) {
                        //perform operation
                        try {
                            //Acceptable: 1 <= input <= 6
                            int selection = Integer.parseInt(request);
                            if (selection < 1 || selection > 6) {
                                throw new IllegalArgumentException();
                            }
                            out.println(execCommand(selection - 1));
                        } catch (Exception e) {
                            out.println("Invalid Request");
                        }
                    } else {
                        //Need a response no matter what
                        out.println("Invalid Request");
                    }
                    out.flush();
                    clientSocket.close();
                }
                ssocket.close();
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }

        public void concurrentServer() {
            try {
                ServerSocket ssocket = new ServerSocket(port);
                System.out.println("Server bound to port: " + port);
                while (isRunning) {
                    Socket clientSocket = ssocket.accept();//blocks until connection
                    new Thread(new ConnectionHandler(clientSocket)).start();
                }
                ssocket.close();
            } catch (Exception e) {
            }
        }

        class ConnectionHandler implements Runnable {

            Socket socket;

            public ConnectionHandler(Socket socket) {
                this.socket = socket;
            }

            @Override
            public void run() {
                try {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //get requested operation
                    String request = in.readLine();//Blocks server until client's request comes through
                    
                    if (request != null) {
                        //perform operation
                        try {
                            //Acceptable: 1 <= input <= 6
                            int selection = Integer.parseInt(request);
                            if (selection < 1 || selection > 6) {
                                throw new IllegalArgumentException();
                            }
                            out.println(execCommand(selection - 1));
                        } catch (Exception e) {
                            out.println("Invalid Request");
                        }
                    } else {
                        //Need a response no matter what
                        out.println("Invalid Request");
                    }
                    out.flush();
                    socket.close();
                } catch (Exception e) {
                }
            }
        }
    }
    private void parseRequest(){
        //maybe
    }

    private static String execCommand(int cmd) {
        Runtime runt = Runtime.getRuntime();

        try {
            Process proc = runt.exec(COMMANDS[cmd]);
            BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line = "";
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line).append('\n');
            }
            System.out.println("Service finished read: " + result.toString());
            return result.toString();
        } catch (Exception e) {
            System.out.println("Command Execution Failed: " + e.toString());
        }
        return "Command Failed";
    }
    
    public static void main(String[] args) throws Exception {
        int port = 8125;
        Server server = new Server(port);
        server.startServer(false);
        //Have main wait on server thread
        server.runner.join();
        
        //press CTRL-C to quit
    }
}

