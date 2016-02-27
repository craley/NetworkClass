/**
 *
 *
 */
package networking;

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
    
    int port;
    
    public Client(int port) {
        this(port, DEFAULT_THREAD_COUNT);
    }
    public Client(int port, int numberOfThreads){
        this.port = port;
        if(numberOfThreads > 0 && numberOfThreads < 100){
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
            System.out.println("Invalid selection. Please try again.");
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
                    //Launch threads(indexed from id 1 to numThreads)
                    Thread[] ts = new Thread[numThreads];
                    for (int x = 0; x < numThreads; x++) {
                        ts[x] = new Thread(new ClientTask(x + 1, choice), "ClientThread" + (x + 1));
                        ts[x].start();
                    }
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
                Socket socket = new Socket("localhost", port);
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                start = System.currentTimeMillis();
                out.println(tasknum);
                out.flush();
                String line = "";
                StringBuilder result = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    result.append(line);
                }
                end = System.currentTimeMillis();
                durations[id - 1] = end - start;
                
                System.out.printf("Client Thread %2d: %s\n", id, result.toString());
                socket.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }
    private void calculateAverage(){
        long sum = 0L;
        for (int x = 0; x < numThreads; x++) {
            sum += durations[x];
        }
        avgs.add(sum / (double)numThreads);
    }
}
