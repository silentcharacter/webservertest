package webserver;

import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Ilya.Igolnikov on 09.10.2014.
 */
public class Server extends Object implements Cloneable {
    /**
     * Common mime type for dynamic content: plain text
     */
    public static final String MIME_PLAINTEXT = "text/plain";

    public static final String MIME_JSON = "application/json";

    public static final int BUFSIZE = 8192;

    private static final long SLEEP_TIME = 5000;

    public static void main(String[] args) throws Exception {
        NetworkService networkService = new NetworkService(8080, 20);
        networkService.run();

        System.out.println("Server started, Hit Enter to stop.\n");

        try {
            System.in.read();
        } catch (Throwable ignored) {
        }

        networkService.stop();
        System.out.println("Server stopped.\n");
    }

    static class NetworkService implements Runnable {
        private final ServerSocket serverSocket;
        private final ExecutorService pool;
        private Thread mainThread;

        public NetworkService(int port, int poolSize) throws IOException {
            serverSocket = new ServerSocket(port);
            pool = Executors.newFixedThreadPool(poolSize);
        }

        public void run() { // run the service
            Thread mainThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (;!serverSocket.isClosed();) {
                            Socket socket = serverSocket.accept();
                            pool.execute(new Handler(socket));
                        }
                    } catch (IOException ex) {
                        pool.shutdown();
                    }
                }
            });
            mainThread.setDaemon(true);
            mainThread.setName("Main Listener");
            mainThread.start();
        }

        /**
         * Stop the server.
         */
        public void stop() {
            try {
                safeClose(serverSocket);
//                closeAllConnections();
                if (mainThread != null) {
                    mainThread.join();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Handler implements Runnable {

        private final Socket socket;

        Handler(Socket socket) { this.socket = socket; }

        public void run() {
            // read and service request on socket
            PushbackInputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                outputStream = socket.getOutputStream();
                inputStream = new PushbackInputStream(socket.getInputStream(), BUFSIZE);

                Response r = serve(inputStream);
                if (r == null) {
                    throw new ResponseException(Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
                } else {
                    r.setRequestMethod(Method.POST);
                    r.send(outputStream);
                }

            } catch (SocketException e) {
                if (!("Shutdown".equals(e.getMessage()))) {
                    e.printStackTrace();
                }
            } catch (SocketTimeoutException | InterruptedException ste) {
                ste.printStackTrace();
            } catch (IOException ioe) {
                Response r = new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
                if (outputStream != null)
                    r.send(outputStream);
            } catch (ResponseException re) {
                Response r = new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                r.send(outputStream);
            } finally {
                safeClose(inputStream);
                safeClose(outputStream);
                safeClose(socket);
            }
        }

        public Response serve(PushbackInputStream inputStream) throws IOException, ResponseException, InterruptedException {
            JSONObject jsonObj = parseBody(inputStream);
            Double a = jsonObj.getDouble("a");
            Double b = jsonObj.getDouble("b");
            JSONObject c = new JSONObject(Collections.singletonMap("sum", a  + b));
            Thread.sleep(SLEEP_TIME);
            return new Response(Response.Status.OK, MIME_JSON, c.toString());
        }

        public JSONObject parseBody(PushbackInputStream inputStream) throws IOException, ResponseException {

            // Read the first 8192 bytes.
            // The full header should fit in here.
            // Apache's default header limit is 8KB.
            // Do NOT assume that a single read will get the entire header at once!
            byte[] buf = new byte[BUFSIZE];
            int splitbyte = 0;
            int rlen = 0;
            {
                int read = -1;
                try {
                    read = inputStream.read(buf, 0, BUFSIZE);
                } catch (Exception e) {
                    safeClose(inputStream);
                    throw new SocketException("Shutdown");
                }
                if (read == -1) {
                    // socket was been closed
                    safeClose(inputStream);
                    throw new SocketException("Shutdown");
                }
                while (read > 0) {
                    rlen += read;
                    splitbyte = findHeaderEnd(buf, rlen);
                    if (splitbyte > 0)
                        break;
                    read = inputStream.read(buf, rlen, BUFSIZE - rlen);
                }
            }

            if (splitbyte < rlen) {
                inputStream.unread(buf, splitbyte, rlen - splitbyte);
            }

            StringBuilder postLineBuffer = new StringBuilder();
            long size = rlen - splitbyte;

            byte[] buf1 = new byte[512];
            while (rlen >= 0 && size > 0) {
                rlen = inputStream.read(buf1, 0, (int)Math.min(size, 512));
                size -= rlen;
                if (rlen > 0) {
                    String str = new String(buf1, "UTF-8");
                    postLineBuffer.append(str);
                }
            }

            String postLine = postLineBuffer.toString().trim();

            return new JSONObject(postLine);
        }

        /**
         * Find byte index separating header from body. It must be the last byte of the first two sequential new lines.
         */
        private int findHeaderEnd(final byte[] buf, int rlen) {
            int splitbyte = 0;
            while (splitbyte + 3 < rlen) {
                if (buf[splitbyte] == '\r' && buf[splitbyte + 1] == '\n' && buf[splitbyte + 2] == '\r' && buf[splitbyte + 3] == '\n') {
                    return splitbyte + 4;
                }
                splitbyte++;
            }
            return 0;
        }
    }


    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    private static void safeClose(Socket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    private static void safeClose(ServerSocket closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }


    public static final class ResponseException extends Exception {

        private final Response.Status status;

        public ResponseException(Response.Status status, String message) {
            super(message);
            this.status = status;
        }

        public ResponseException(Response.Status status, String message, Exception e) {
            super(message, e);
            this.status = status;
        }

        public Response.Status getStatus() {
            return status;
        }
    }
}



