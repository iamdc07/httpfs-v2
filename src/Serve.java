import model.Config;
import model.ServerParameters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Serve extends Thread {
    SocketChannel socketChannel;

    public Serve(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    @Override
    public void run() {
        try {
            if (Config.isVerbose)
                System.out.println("Thread forked!");

            HttpRequest httpRequest = new HttpRequest();
            Charset utf8 = StandardCharsets.UTF_8;
            StringBuilder sb = new StringBuilder();

            //Perform the operations on received request...
            if (Config.isVerbose)
                System.out.println("Incoming Connection:  " + socketChannel.getRemoteAddress());

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            ServerParameters serverParameters = new ServerParameters();
            while (byteBuffer.hasRemaining()) {
                int length = socketChannel.read(byteBuffer);
                if (length == -1)
                    break;

                if (length > 0) {
                    byteBuffer.flip();

                    String lines = String.valueOf(utf8.decode(byteBuffer));

                    utf8.encode(lines);
                    sb.append(lines);
                }
            }
            byteBuffer.clear();
            if (Config.isVerbose)
                System.out.println("Request Received");

            httpRequest.processRequest(sb, serverParameters);
            // Send data back to the client
            ByteBuffer buf = utf8.encode(serverParameters.response);
            socketChannel.write(buf);
            socketChannel.finishConnect();
            socketChannel.close();

            if (Config.isVerbose)
                System.out.println("Response sent!\n");
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }


    }
}
