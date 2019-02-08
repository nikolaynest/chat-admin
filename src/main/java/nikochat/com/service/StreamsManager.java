package nikochat.com.service;

import java.io.*;
import java.net.Socket;

/**
 * Created by nikolay on 23.08.14.
 */
public class StreamsManager {
    private static BufferedReader input;
    private static PrintWriter output;

    public static BufferedReader createInput(Socket socket, Class cl) {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error creating output stream in " + cl.getName());
            e.printStackTrace();
        }
        return input;
    }

    public static PrintWriter createOutput(Socket socket, Class cl) {
        try {
            output = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error creating input stream in " + cl.getName());
            e.printStackTrace();
        }
        return output;
    }

    public static void closeInput(BufferedReader input, Class cl) {
        try {
            input.close();
        } catch (IOException e) {
            System.out.println("Error closing BufferedReader stream in " + cl.getName());
            e.printStackTrace();
        }
    }

    public static void closeOutput(PrintWriter output) {
        output.close();
    }

}
