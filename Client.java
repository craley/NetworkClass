/**
 *
 * Client.java
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author chris
 */
public class Client {

    public static final int DEFAULT_THREAD_COUNT = 1;

    int numThreads = 1;
    //Time in milliseconds of task
    long[] durations;
    //Records the average time of thread tasks.
    ArrayList<Double> avgs = new ArrayList<>();

    String hostname;
    int port;

    public Client(int port) {
        this("localhost", port, DEFAULT_THREAD_COUNT);
    }

    public Client(int port, int numberOfThreads) {
        this("localhost", port, numberOfThreads);
    }

    public Client(String hostname, int port, int numberOfThreads) {
        this.hostname = hostname;
        this.port = port;
        if (numberOfThreads > 0 && numberOfThreads < 100) {
            this.numThreads = numberOfThreads;
        }
        durations = new long[this.numThreads];
    }

    private Scanner in = new Scanner(System.in);
    private static final String SEPARATOR = "------------------------------";
    private static final String MAIN_MENU = "\nMain Menu\n" + SEPARATOR
            + "\n[1] Host current Date and Time\n"
            + "[2] Host uptime\n"
            + "[3] Host memory use\n"
            + "[4] Host netstat\n"
            + "[5] Host current users\n"
            + "[6] Host running processes\n"
            + "[7] Quit\n";

    /**
     * Retrieves user's choice following a menu. The provided value indicates
     * the last valid menu choice. Will keep prompting user on non-numeric data
     * or a value higher than last. Menu Selection: 1 <= choice <= last
     *
     * @param last valid menu selection
     * @return selection as integer
     */
    private int getChoice(int last) {
        String selection = null;
        do {
            System.out.print(">>  ");
            selection = in.next();
            try {
                int num = Integer.parseInt(selection);
                if (num > 0 && num <= last) {
                    return num;
                }
            } catch (Exception e) {
                /* Politely ignore */ }
            System.out.println("Must be an integer between 1 and 7 inclusive. Please try again.");
        } while (true);
    }

    /**
     *
     */
    public void showMainMenu() {
        try {
            int choice = 0;
            do {
                System.out.println(MAIN_MENU);
                choice = getChoice(7);
                if (choice != 7) {
                    //Create all threads(indexed from id 1 to numThreads)
                    Thread[] ts = new Thread[numThreads];
                    for (int x = 0; x < numThreads; x++) {
                        ts[x] = new Thread(new ClientTask(x + 1, choice), "ClientThread" + (x + 1));
                    }
                    //Launch Threads
                    for (int x = 0; x < numThreads; x++) {
                        ts[x].start();
                    }
                    //Main waits on all threads
                    for (int x = 0; x < numThreads; x++) {
                        ts[x].join();
                    }
                    //Have thread data
                    calculateAverage();
                }
            } while (choice != 7);

            int numTrials = avgs.size();
            for (int x = 0; x < numTrials; x++) {
                System.out.printf("Trial %2d: %.3f milliseconds\n", x, avgs.get(x));
            }
        } catch (Exception e) {
            System.out.println("Client Socket Failed");
        }
        System.out.println("Have a nice day!");
    }

    class ClientTask implements Runnable {

        int id;
        int tasknum;

        public ClientTask(int id, int selection) {
            this.id = id;
            tasknum = selection;
        }

        @Override
        public void run() {
            long start = 0L, end = 0L;
            try {
                Socket socket = new Socket(hostname, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //Start Timing
                start = System.currentTimeMillis();
                out.println(tasknum);
                out.flush();
                String line = "";
                StringBuilder result = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                end = System.currentTimeMillis();
                //End Timing
                durations[id - 1] = end - start;

                System.out.printf("Client Thread %2d: %s\n", id, result.toString());
                socket.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    private void calculateAverage() {
        long sum = 0L;
        for (int x = 0; x < numThreads; x++) {
            sum += durations[x];
        }
        avgs.add(sum / (double) numThreads);
    }

    public static void main(String[] args) {
        //Development defaults
        String hostname = "localhost";
        int numThreads = 5;
        int port = 8125;
        
        if(args.length == 0){
            //Production
            System.out.println("You must provide the server hostname and optional number of threads. Exiting");
            System.exit(1);
            
            //Development
            //Client client = new Client(hostname, port, numThreads);
            //client.showMainMenu();
            //System.exit(0);
        }
        if(args.length > 0){
            hostname = args[0];
        }
        if(args.length > 1){
            try {
                numThreads = (int)Float.parseFloat(args[1]);
            } catch (Exception e) {
                System.out.println("Invalid number of threads.");
                System.exit(1);
            }
        }
        Client client = new Client(hostname, port, numThreads);
        client.showMainMenu();
    }
}
