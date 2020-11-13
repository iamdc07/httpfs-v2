import model.Config;
import model.Packet;
import model.ServerParameters;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Serve extends Thread {
    SocketAddress socketAddress;
    ByteBuffer buf;

    public Serve(SocketAddress socketAddress, ByteBuffer buf) {
        this.socketAddress = socketAddress;
        this.buf = buf;
    }

    @Override
    public void run() {
        try (DatagramChannel channel = DatagramChannel.open()) {
            if (Config.isVerbose)
                System.out.println("Thread forked!");

            HttpRequest httpRequest = new HttpRequest();
//            Charset utf8 = StandardCharsets.UTF_8;
//            StringBuilder sb = new StringBuilder();

            // Parse a packet from the received raw data.
            buf.flip();
            Packet packet = Packet.fromBuffer(buf);
            buf.flip();

            //Perform the operations on received request...
            if (Config.isVerbose)
                System.out.println("Incoming Connection:  " + packet.getPeerAddress());

            ServerParameters serverParameters = new ServerParameters();
            String payload = new String(packet.getPayload(), UTF_8);
            httpRequest.processRequest(new StringBuilder(payload), serverParameters);

            System.out.println("Peer port:" + packet.getPeerPort());

//                            logger.info("Packet: {}", packet);
//                            logger.info("Payload: {}", payload);
//                            logger.info("Router: {}", router);

            // Send the response to the router not the client.
            // The peer address of the packet is the address of the client already.
            // We can use toBuilder to copy properties of the current packet.
            // This demonstrate how to create a new packet from an existing packet.
            Packet responsePacket = packet.toBuilder()
                    .setPayload(serverParameters.response.getBytes())
                    .create();
            channel.send(responsePacket.toBuffer(), socketAddress);
            buf.clear();

//            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
//            ServerParameters serverParameters = new ServerParameters();
//            while (byteBuffer.hasRemaining()) {
//                int length = socketChannel.read(byteBuffer);
//                if (length == -1)
//                    break;
//
//                if (length > 0) {
//                    byteBuffer.flip();
//
//                    String lines = String.valueOf(utf8.decode(byteBuffer));
//
//                    utf8.encode(lines);
//                    sb.append(lines);
//                }
//            }
//            byteBuffer.clear();
//            if (Config.isVerbose)
//                System.out.println("Request Received");
//
//            httpRequest.processRequest(sb, serverParameters);
            // Send data back to the client
//            ByteBuffer buf = utf8.encode(serverParameters.response);
//            socketChannel.write(buf);
//            socketChannel.finishConnect();
//            socketChannel.close();
            if (Config.isVerbose)
                System.out.println("Response sent!\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }


    }
}
