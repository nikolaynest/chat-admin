package nikochat.com.ui;

import nikochat.com.server.Server;
import nikochat.com.service.Log;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created by nikolay on 25.08.14.
 */
public class ServerMenu implements Runnable {

    public ServerMenu(Server server) {
        this.server = server;
    }

    Server server;

    public static void printHeader() {
        System.out.println("####################################################################");
        System.out.println("#                       Welcome to chat!                           #");
    }

    public static void menu() {
        System.out.println("####################################################################");
        System.out.println("#   MENU:                                                          #");
        System.out.println("# exit        - turn off all the clients and shut down the program #");
        System.out.println("# list        - print to console list of the clients               #");
        System.out.println("# kill <name> - break connection with the specified user's name    #");
        System.out.println("# help        - print all commands with their description          #");
        System.out.println("####################################################################");
    }

    @Override
    public void run() {
        Scanner scan = new Scanner(System.in);
        String line;
        printHeader();
        menu();
        while (true) {
            System.out.print(">");
            try {
                line = scan.nextLine();
            } catch (NoSuchElementException n) {
                System.out.println("server closed.");
                break;
            }

            StringTokenizer tokenizer = new StringTokenizer(line);
            int tokens = tokenizer.countTokens();
            if (tokens == 1) {
                switch (line) {
                    case "exit":
                        Log.write("server stopped");
                        Log.close();
                        System.exit(0);
                        break;
                    case "list":
                        server.list();
                        break;
                    case "help":
                        menu();
                        break;
                    default:
                        System.out.println("Incorrect input. Enter again.");
                }
            } else {
                String[] words = line.split(" ");
                if (words[0].equals("kill")) {
                    server.killSocket(words[1]);
                }
            }
            System.out.print(">");
        }
    }
}
