import model.Config;
import model.Packet;
import model.ServerParameters;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Serve extends Thread {
    SocketAddress socketAddress;
    ArrayList<Packet> packetsReceived;

    public Serve(SocketAddress socketAddress, ArrayList<Packet> packetsReceived) {
        this.socketAddress = socketAddress;
        this.packetsReceived = packetsReceived;
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
//            System.out.println("Limit:" + buf.limit());

            String response = "";

            for (Packet each : packetsReceived) {
                System.out.println("Received Packet " + each.getSequenceNumber());
                String responsePayload = new String(each.getPayload(), StandardCharsets.UTF_8);
                response = response.concat(responsePayload.trim());
            }

            //Perform the operations on received request...
            if (Config.isVerbose)
                System.out.println("Incoming Connection:  " + packetsReceived.get(0).getPeerAddress());

            ServerParameters serverParameters = new ServerParameters();

            ArrayList<Packet> packetList = generatePackets(httpRequest, serverParameters, packetsReceived.get(0), response);

            System.out.println("Peer port:" + packetsReceived.get(0).getPeerPort());

//                            logger.info("Packet: {}", packet);
//                            logger.info("Payload: {}", payload);
//                            logger.info("Router: {}", router);

            // Send the response to the router not the client.
            // The peer address of the packet is the address of the client already.
            // We can use toBuilder to copy properties of the current packet.
            // This demonstrate how to create a new packet from an existing packet.
//            Packet responsePacket = packet.toBuilder()
//                    .setPayload(serverParameters.response.getBytes())
//                    .create();

            for (Packet each : packetList) {
                channel.send(each.toBuffer(), socketAddress);
                System.out.println("Packet Sent as Response");
            }

//            buf.clear();
            packetList.clear();
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

    private static ArrayList<Packet> generatePackets(HttpRequest httpRequest, ServerParameters serverParameters, Packet packet, String payload) {
        ArrayList<Packet> packetList = new ArrayList<>();
//        String payload = httpRequest.processRequest(serverParameters);
        httpRequest.processRequest(new StringBuilder(payload), serverParameters);
        byte[] buffer = serverParameters.response.getBytes();
        long seq = 1L;
        int i = 0;

//        for (int i = 0; i < buffer.length; i = i + 1013)
        while (i < buffer.length){
//            byte[] bytes = payload.getBytes(i, i + 1012);
            System.out.println("I:" + i + " I + 1012:" + (i + 1012) + " buffer Length:" + buffer.length);
            byte[] slice;
            if (i + 1012 > buffer.length) {
                slice = Arrays.copyOfRange(buffer, i, buffer.length - 1);
            } else {
                slice = Arrays.copyOfRange(buffer, i, i + 1012);
            }

            System.out.println("SLICE length:" + slice.length);

            Packet responsePacket = packet.toBuilder()
                    .setSequenceNumber(seq)
                    .setPayload(slice)
                    .create();

            packetList.add(responsePacket);
            seq++;
            i += 1013;
        }

//        i -= 1012;
//
//        if ((buffer.length - i) > 0) {
//            System.out.println("i:" + i + " buffer Length:" + (buffer.length - 1));
//            byte[] slice = Arrays.copyOfRange(buffer, i, i + (buffer.length - 1));
//
//            Packet responsePacket = packet.toBuilder()
//                    .setSequenceNumber(seq)
//                    .setPayload(slice)
//                    .create();
//
//            packetList.add(responsePacket);
//        }

        return packetList;
    }
}
