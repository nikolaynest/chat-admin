package nikochat.com.server;

import nikochat.com.app.AppConfig;
import nikochat.com.app.AppConstants;
import nikochat.com.service.Log;
import nikochat.com.service.StreamsManager;
import nikochat.com.ui.ServerMenu;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by nikolay on 23.08.14.
 */
public class Server {


    private ServerSocket server;
    private final Map<String, ServerThread> clients = Collections.synchronizedMap(new TreeMap<>());
    private final Queue<String> history = new ConcurrentLinkedQueue<>();

    public Server() {
        System.out.println("Server is running...");
        Log.write("Server is running...");

        new Thread(new ServerMenu(this)).start();

        try {
            server = new ServerSocket(AppConfig.PORT);
        } catch (IOException e) {
            System.out.println("Error creating server");
            Log.write("Error creating server");
            Log.write(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            try {
                Socket accept = server.accept();
                Log.write("server accept socket");
                ServerThread serverThread = new ServerThread(accept);
                new Thread(serverThread).start();
                Log.write("server start new ServerThread");
            } catch (IOException e) {
                System.out.println("Error accepting client on server");
                Log.write("Error accepting client on server");
                Log.write(e.getMessage());
                e.printStackTrace();
            }
        }
    }


    /**
     * Add to history message from system. Store last NUM_HISTORY_MESSAGE
     * @param mess message from system (from users or the server).
     */
    private void addToHistory(String mess) {
        if (history.size() >= AppConfig.NUM_HISTORY_MESSAGES) {
            history.remove();
        }
        history.add(mess);
    }

    /**
     * Close connection with specified user name
     * @param name user name
     */
    public void killSocket(String name) {
        ServerThread st = clients.get(name);
        if (st == null) {
            System.out.println("Нет пользователя с таким именем");
        } else {
            st.out.println(AppConstants.DENIED);
            try {
                st.closeConnection();
                Log.write("kill socket with the name " + name);
            } catch (IOException e) {
                System.out.println("Error close connection killing user " + name);
                Log.write("Error close connection killing user " + name);
                Log.write(e.getMessage());
                e.printStackTrace();
            }
            clients.remove(name);
            notifyAllClients();
            synchronized (clients) {
                for (ServerThread thread : clients.values()) {
                    thread.out.println("Пользователь " + name + " отсоединен.");
                }
            }

        }
    }

    /**
     * this method prints the list of all users available in real-time chatting
     */
    public void list() {
        System.out.println("Список всех подключенных клиентов:");
        if (clients.size() == 0) {
            System.out.println("0 клиентов");
        } else {
            synchronized (clients) {
                clients.keySet().forEach(System.out::println);
            }
        }
    }

    /**
     * Method update list of the online users.
     * Uses specific protocol, which is starts and ends with service command AppConstants.LIST
     * for the reason that client part of the application will
     * can distinguish usual message from user names list
     */
    private synchronized void notifyAllClients() {
        String[] list = clients.keySet().toArray(new String[clients.size()]);
        StringBuffer buffer = new StringBuffer(list.length);
        for (String s : list) {
            buffer.append(s).append("\n");
        }
        String resultList = buffer.toString();
        buffer = null;
        resendMessage(AppConstants.LIST);
        synchronized (clients) {
            for (ServerThread st : clients.values()) {
                st.out.print(resultList);
                st.out.println(AppConstants.LIST);
            }
        }
    }

    /**
     * Broadcast sending message to all clients
     * @param message
     */
    private void resendMessage(String message) {
        synchronized (clients) {
            for (ServerThread st : clients.values()) {
                st.out.println(message);
            }
        }
    }

    /**
     * Separate thread for handling registered client
     */
    class ServerThread implements Runnable {

        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String name;

        public ServerThread(Socket socket) {
            this.socket = socket;
            out = StreamsManager.createOutput(socket, this.getClass());
            in = StreamsManager.createInput(socket, this.getClass());
        }

        @Override
        public void run() {
            try {
                boolean goFurther = true; /*аварийный выход*/
                /** первым делом получаю имя нового "клиента" */
                try {
                    goFurther = readClientName();
                } catch (IOException e) {
                    System.out.println("Error reading name from client...");
                    Log.write("Error reading name from client...");
                    Log.write(e.getMessage());
                    e.printStackTrace();
                }
                if (goFurther) {
                    String time = getTimeWithoutMillis(LocalTime.now());
                    String invitation = time + " " + name + " has joined";
                    printHistory();
                    addToHistory(invitation);
                    resendMessage(invitation);

                    /** читаю из входящего потока сообщения */
                    while (true) {
                        String received = null;
                        try {
                            received = in.readLine();
                            time = getTimeWithoutMillis(LocalTime.now());
                        } catch (IOException e) {
                            System.out.println("Error reading message from client...");
                            Log.write("Error reading message from client...");
                            Log.write(e.getMessage());
                            e.printStackTrace();
                        }
                        if (received == null) {
                            Log.write("received message from client is null");
                            break;
                        }

                        if (!received.trim().equals(AppConstants.EXIT)) {
                            String str = time + " " + name + ": " + received;
                            resendMessage(str);
                            addToHistory(str);
                        } else {
                            received = time + " " + name + " exit from chat";
                            addToHistory(received);
                            resendMessage(received);
                            out.println(AppConstants.EXIT);
                            Log.write(received);
                            break;
                        }
                    }
                }
            } finally {
                try {
                    closeConnection();
                    if (name != null) {
                        if (clients.containsKey(name)) {
                            clients.remove(name);
                            notifyAllClients();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error closing socket on server side");
                    Log.write("Error closing socket on server side");
                    Log.write(e.getMessage());
                    e.printStackTrace();
                }
            }
        }


        /**
         * prints messages from history collection
         * to output stream of this client thread
         */
        private void printHistory() {
            synchronized (history) {
                history.forEach(out::println);
            }
        }

        /**
         * reads and handles name received from client
         * @return true if registering finished with success,
         *         false - if received name is null or server stores maximum numbers of clients
         * @throws IOException some errors while reading from input stream
         */
        private boolean readClientName() throws IOException {
            boolean continueProgram = true;
            while (true) {
                name = in.readLine();
                if (name == null) {
                    continueProgram = false;
                    Log.write("read name is null");
                    break;
                }
                if (!(clients.size() < AppConfig.MAX_USERS)) {
                    out.println(AppConstants.MAX_USERS_ERROR);
                    continueProgram = false;
                    Log.write("reduce addObserver new connection");
                    break;
                }
                if (clients.get(name) == null) {
                    clients.put(name, this);
                    Log.write("addObserver new user with the name: " + name);
                    out.println(AppConstants.OK_REGISTERED);

                    notifyAllClients();
                    break;
                } else {
                    out.println(AppConstants.REPEATED_NAME_ERROR);
                    out.print("> ");
                }
            }
            return continueProgram;
        }

        /**
         * Gives String representation of time without millis
         * @param time
         * @return String with formatted time without millis
         */
        private String getTimeWithoutMillis(LocalTime time) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
            return formatter.format(time);
        }

        private void closeConnection() throws IOException {
            in.close();
            out.close();
            socket.close();
            Log.write("close 'input', 'output' and 'socket' for user with the name: " + name);
        }
    }
}
