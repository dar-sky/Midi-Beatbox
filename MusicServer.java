import java.io.*;
import java.net.*;
import java.util.*;

public class MusicServer {

    private final List<ObjectOutputStream> clientOutputStreams = new ArrayList<>();;
    private static final int SERVER_PORT = 4242;

    public static void main(String[] args) {
        new MusicServer().startServer();
    }

    public class ClientHandler implements Runnable {
        private ObjectInputStream in;
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            try {
                clientSocket = socket;
                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        public void run() {
            try {
                Object o1, o2;
                while ((o1 = in.readObject()) != null) {
                    o2 = in.readObject();

                    System.out.println("Read message and checkbox boolean state array objects.");
                    broadcast(o1, o2);
                }
            } catch (EOFException eofEx) {
                // This exception is expected when client disconnects
                System.out.println("Client disconnected.");
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                closeResources();
            }
        }

        private void closeResources() {
            try {
                in.close();
                clientSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void startServer() {

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                    clientOutputStreams.add(out);

                    Thread t = new Thread(new ClientHandler(clientSocket));
                    t.start();

                    System.out.println("Connection established with client: " + clientSocket.getInetAddress());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void broadcast(Object one, Object two) {
        Iterator<ObjectOutputStream> it = clientOutputStreams.iterator();
        while (it.hasNext()) {
            try {
                ObjectOutputStream out = it.next();
                out.writeObject(one);
                out.writeObject(two);
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
