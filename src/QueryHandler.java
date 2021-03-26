import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Objects;
import java.util.StringTokenizer;

public class QueryHandler implements Runnable{
    static final File WEB_ROOT = new File(".");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_ALLOWED = "405.html";



    public Socket getConnect() {
        return connect;
    }

    //Socket
    private final Socket connect;

    public QueryHandler(Socket c) {
        connect = c;
    }

    @Override
    public void run() {
        BufferedReader in;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequest = null;
        while (!this.getConnect().isClosed()) {
            try {
                this.getConnect().setSoTimeout(0);
                in = new BufferedReader((new InputStreamReader((connect.getInputStream()))));
                out = new PrintWriter(connect.getOutputStream());
                dataOut = new BufferedOutputStream(connect.getOutputStream());
//                out = new BufferedWriter(new OutputStreamWriter(connect.getOutputStream()));

                String input = in.readLine();
                if (input == null) {
                    System.out.println("Lost connection with client");
                    break;
                }
                StringTokenizer parse = new StringTokenizer(input);
                String method = parse.nextToken().toUpperCase();
                fileRequest = parse.nextToken().toLowerCase();
                HashMap<String, String> HeadersTable = getHeadersTable(in);

                if (!method.equals("GET")) {
                    System.out.println("Method " + method + " not allowed");
                    File file = new File(WEB_ROOT, METHOD_NOT_ALLOWED);
                    int length = (int) file.length();
                    String contMimeType = "text/html";
                    byte[] fileData = readFileData(file, length);
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                    ZonedDateTime date = ZonedDateTime.now(ZoneOffset.UTC);
                    new HTTPResponse.Builder()
                            .statusLine("HTTP/1.1 405 Not allowed")
                            .header("Server : Rostiks server : 1.0")
                            .header("Date" + date.format(dateTimeFormatter))
                            .header("Last Modified: " + file.lastModified())
                            .header("Content-type: " + contMimeType)
                            .header("Content-length: " + length)
                            .data(fileData)
                            .printResponse(out, dataOut);
                } else {
                    if (fileRequest.endsWith("/")) {
                        fileRequest += DEFAULT_FILE;
                    }

                    File file = new File(WEB_ROOT, fileRequest);
                    int length = (int) file.length();
                    String content = getContentType(fileRequest);

                    byte[] fileData = readFileData(file, length);

                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
                    ZonedDateTime date = ZonedDateTime.now(ZoneOffset.UTC);
                    new HTTPResponse.Builder()
                            .statusLine("HTTP/1.1 200 OK")
                            .header("Server : Rostiks server : 1.0")
                            .header("Date" + date.format(dateTimeFormatter))
                            .header("Last Modified: " + file.lastModified())
                            .header("Connection: Keep-Alive")
                            .header("Content-type: " + content)
                            .header("Content-length: " + length)
                            .data(fileData)
                            .printResponse(out, dataOut);

                    System.out.println("File " + fileRequest + ", type " + content + "returned");
                }
            } catch (FileNotFoundException fnfe) {
                try {
                    fileNotFound(Objects.requireNonNull(out), Objects.requireNonNull(dataOut), fileRequest);
                } catch (IOException e) {
                    System.err.println("Error with file not found exception : " + e.getMessage());
                }
            } catch (SocketTimeoutException e) {
                System.err.println("Socket timed out");
                try {
                    this.getConnect().close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            } catch (IOException e) {
                System.err.println("Server error : " + e);
                try {
                    this.getConnect().close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    private HashMap<String, String> getHeadersTable(BufferedReader in) throws IOException {
        HashMap<String, String> HeadersMap = new HashMap<>();
        String curLine;
        String[] splitLine;
        while((curLine = in.readLine()) != null) {
            if(curLine.equals("")) {
                break;
            }
            splitLine = curLine.split(":");
            if(splitLine.length < 2) {
                throw new IOException("HTTP Request is incorrect!");
            }
            HeadersMap.put(splitLine[0].toLowerCase(), splitLine[1].toLowerCase());
        }
        return HeadersMap;
    }

    private void fileNotFound(PrintWriter out, BufferedOutputStream dataOut, String fileRequest) throws IOException {
        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
        int length = (int) file.length();
        String contMimeType = "text/html";
        byte[] fileData = readFileData(file, length);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server : Rostiks server : 1.0");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
        ZonedDateTime date = ZonedDateTime.now(ZoneOffset.UTC);
        out.println("Date" + date.format(dateTimeFormatter));
        out.println("Last Modified: " + file.lastModified());
        out.println("Content-type: " + contMimeType);
        out.println("Content-length: " + length);
        out.println();
        out.flush();

        dataOut.write(fileData, 0, length);
        dataOut.flush();

        System.out.println("File " + fileRequest + " not found");
    }

    private String getContentType(String fileRequest) {
        if (fileRequest.endsWith(".html")) {
            return "text/html";
        } else if (fileRequest.endsWith(".jpeg")) {
            return "text/jpeg";
        } else if (fileRequest.endsWith(".png")) {
            return "text/png";
        }else if(fileRequest.endsWith(".css")) {
            return "text/css";
        } else {
            return "text/plain";
        }
    }

    private byte[] readFileData(File file, int length) throws IOException {
        byte[] fileData = new byte[(int) length];
        try (FileInputStream fileIn = new FileInputStream(file)) {
            int ret = fileIn.read(fileData);
            if(ret == -1) {
                throw new IOException("reading data error");
            }
        }

        return fileData;
    }
}
