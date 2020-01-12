import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HttpServer {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(2080);
        while(true) {
            Socket socket = serverSocket.accept();

            socket.getInputStream().read();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = new GZIPOutputStream(baos);
            os.write(("<div>Hello, World! I am Will and now is " + new Date() + ", random is " + new Random().nextInt() + ".<div>.")
                    .getBytes("UTF-8"));
            os.close();

            byte[] data = baos.toByteArray();
            System.out.println("data length: " + data.length);

            socket.getOutputStream().write(("HTTP/1.1 200 OK\r\n" +
                    "Server: Apache-Coyote/1.1\r\n" +
                    "Content-Type: text/html;charset=UTF-8\r\n" +
//                    "Transfer-Encoding: chunked\r\n" +
                    "Content-Encoding: gzip\r\n" +
                    "Content-Length: " + data.length + "\r\n" +
                    "\r\n").getBytes("UTF-8"));
            socket.getOutputStream().write(data);
            socket.close();
        }
    }
}
