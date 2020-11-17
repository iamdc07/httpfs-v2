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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

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
        ArrayList<TimerTask> tasks = new ArrayList<>();
        ArrayBlockingQueue<Packet> ackBuffer = new ArrayBlockingQueue<>(99);

        try (DatagramChannel channel = DatagramChannel.open()) {
            if (Config.isVerbose)
                System.out.println("Thread forked!");

            HttpRequest httpRequest = new HttpRequest();
//            Charset utf8 = StandardCharsets.UTF_8;
//            StringBuilder sb = new StringBuilder();

            // Parse a packet from the received raw data.
//            System.out.println("Limit:" + buf.limit());

            String response = "";
            int start = 0;

            for (Packet each : packetsReceived) {
                System.out.println("Received Packet SEQ: " + each.getSequenceNumber());
                String responsePayload = new String(each.getPayload(), StandardCharsets.UTF_8);
                response = response.concat(responsePayload.trim());
            }

            //Perform the operations on received request...
            if (Config.isVerbose)
                System.out.println("Incoming Connection:  " + packetsReceived.get(0).getPeerAddress());

            ServerParameters serverParameters = new ServerParameters();

            ArrayList<Packet> packetList = generatePackets(httpRequest, serverParameters, packetsReceived.get(0), response);

            System.out.println("Peer port:" + packetsReceived.get(0).getPeerPort());

            boolean flag = true;

            Timer timer = new Timer(true);

            // Send Packets
            while (flag) {
                flag = sendPackets(packetList, channel, socketAddress, start, timer, tasks);

                int k = 0;

                // Create Buffer for Response
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                // Receive each Ack one by one
                while (k < 5) {
                    channel.receive(buf);
                    buf.flip();

                    Packet ackPacket = Packet.fromBuffer(buf);
                    buf.clear();

                    if (!ackBuffer.contains(ackPacket)) {
                        ackBuffer.add(ackPacket);

                        System.out.println("ACK SEQ:" + ((int) ackPacket.getSequenceNumber()));

                        if (tasks.size() >= ackBuffer.size()) {
                            TimerTask timerTask = tasks.get((int) ackPacket.getSequenceNumber() - 1);
                            timerTask.cancel();
                            System.out.println("CANCELLED TASK:" + (ackPacket.getSequenceNumber() - 1));
                        }
                    }

                    if (ackBuffer.size() == tasks.size()) {
                        break;
                    }
                    System.out.println("Received Ack List Size:" + ackBuffer.size());
                    k++;
                }

                start += 5;
            }

            timer.cancel();

//            for (Packet each : packetList) {
//                channel.send(each.toBuffer(), socketAddress);
//                System.out.println("Packet Sent as Response");
//            }

//            Packet packet = packetList.get(packetList.size() - 1)
//                    .toBuilder()
//                    .setPayload(new byte[0])
//                    .setType(0).create();

//            channel.send(packet.toBuffer(), socketAddress);
//            System.out.println("FINAL Packet Sent as Response");

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
        int i = 0;
        long seq = 1L;
        byte[] bytes;
        bytes = "\r\n".getBytes();

        while (i < buffer.length){
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

        Packet lastPacket = packet.toBuilder()
                .setSequenceNumber(seq)
                .setType(0)
                .setPayload(bytes)
                .create();

        packetList.add(lastPacket);

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

    private static boolean sendPackets(ArrayList<Packet> packetList, DatagramChannel channel, SocketAddress routerAddress, int start, Timer timer, ArrayList<TimerTask> tasks) throws IOException {
        for (int i = start; i < start + 5; i++) {
            System.out.println("Packets Sent as Response: " + (i + 1));

            if (i == packetList.size() - 1) {
                channel.send(packetList.get(i).toBuffer(), routerAddress);
                TimerTask task = new PacketTimeout(packetList.get(i), channel, routerAddress);
                tasks.add(task);
                timer.schedule(task, 5000, 5000);
                return false;
            }

            channel.send(packetList.get(i).toBuffer(), routerAddress);
            TimerTask task = new PacketTimeout(packetList.get(i), channel, routerAddress);
            tasks.add(task);
            timer.schedule(task, 5000, 5000);
        }


        return true;
    }
}
