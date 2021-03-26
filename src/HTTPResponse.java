import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;

public class HTTPResponse {
    private String statusLine;
    private final Deque<String> headers = new ArrayDeque<>();
    private byte[] data;

    public static class Builder {
        private final HTTPResponse newResponse;

        public Builder() {
            newResponse = new HTTPResponse();
        }

        public Builder statusLine(String statusLine) {
            newResponse.statusLine = statusLine;
            return this;
        }

        public Builder header(String header) {
            newResponse.headers.addLast(header);
            return this;
        }

        public Builder data(byte[] newData) {
            newResponse.data = newData;
            return this;
        }

        public void printResponse(PrintWriter out, BufferedOutputStream dataOut) throws IOException {
            out.println(newResponse.statusLine);
            newResponse.headers.forEach(out::println);
            out.println();
            out.flush();
            dataOut.write(newResponse.data, 0, newResponse.data.length);
            dataOut.flush();
        }

    }
}
