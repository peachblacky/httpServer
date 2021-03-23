import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;

public class Server {
    public Server() { }

    public void start() {
        try {
            ServerSocket serverConnect = new ServerSocket(8080);
            System.out.println("Server awaken.");
            System.out.println("Waiting for queries on port " + 8080 + "...");

            while(true) {
                QueryHandler myQueryHandler = new QueryHandler(serverConnect.accept());
                System.out.println("Connection awaken. " + new Date());
//                new Thread(new Server(serverConnect.accept())).start();
                myQueryHandler.run();
            }
        } catch (IOException e) {
            System.err.println("Server connection error : " + e.getMessage());
        }
    }
}
