import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

class Server {
    static final Charset ASCII = Charset.forName("US-ASCII");
    static final String STRESS_REQUEST = "GET /stress";
    static final String EMPTY_OBJECT = "{}";
    static final byte[] RESPONSE_404 = "HTTP/1.0 404 Not Found\r\n\r\n".getBytes(ASCII);
    static final byte[] RESPONSE_OK_JSON = "HTTP/1.0 200 OK\r\nContent-Type: application/json\r\n".getBytes(ASCII);
    static final byte[] CONTENT_LENGTH = "Content-Length: ".getBytes(ASCII);
    static final byte[] BODY_SEP = "\r\n\r\n".getBytes(ASCII);
    static final char KEYVAL_SEPARATOR = '&';
    static final char VAL_SEPARATOR = '=';
    static final char QUERY_END = ' ';

    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(10000, 100)) {
            System.out.println("Listening to localhost:10000/");
            while(true){
                awaitConnection(server);
            }
        }
    }

    static void awaitConnection(ServerSocket server) {
        try (Socket s = server.accept()) {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), ASCII));
            String requestLine = in.readLine();
            if(requestLine.startsWith(STRESS_REQUEST)){
                sendStressResponse(requestLine, s);
            } else {
                s.getOutputStream().write(RESPONSE_404);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void sendStressResponse(String requestLine, Socket s) throws IOException {
        String response = buildJsonFromQuery(requestLine);
        OutputStream os = s.getOutputStream();
        os.write(RESPONSE_OK_JSON);
        os.write(CONTENT_LENGTH);
        os.write(String.valueOf(response.length()).getBytes(ASCII));
        os.write(BODY_SEP);
        os.write(response.getBytes(ASCII));
    }

    static String buildJsonFromQuery(String requestLine) {
        int keyStart = STRESS_REQUEST.length() + 1;
        int valStart = -1;
        int end = requestLine.lastIndexOf(QUERY_END);
        if(keyStart >= end) return EMPTY_OBJECT;

        HashMap<String,String> result = new HashMap<>();
        for(int i = keyStart; i <= end; i++){
            char c = requestLine.charAt(i);
            if(c == VAL_SEPARATOR){
                valStart = i;
            } else if(c == KEYVAL_SEPARATOR || c == QUERY_END){
                if(valStart == -1){
                    result.put(requestLine.substring(keyStart, i), null);
                } else {
                    result.put(requestLine.substring(keyStart, valStart), requestLine.substring(valStart+1, i));
                }
                keyStart = i+1;
                valStart = -1;
            }
        }

        //Very unsafe. Does not escape JSON properly. Does not decode URL.
        StringBuilder sb = new StringBuilder(requestLine.length() + result.size() * 4);
        sb.append('{');
        boolean isSubsequent = false;
        for (Map.Entry<String, String> e : result.entrySet()) {
            if(isSubsequent) sb.append(',');
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
            isSubsequent = true;
        }
        sb.append('}');
        return sb.toString();
    }
}
