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
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Serve extends Thread {
    SocketAddress socketAddress;
    ArrayList<Packet> packetsReceived;
    ChannelHelper channelHelper;
    int i = 0;

    public Serve(SocketAddress socketAddress, ArrayList<Packet> packetsReceived, ChannelHelper channelHelper) {
        this.socketAddress = socketAddress;
        this.packetsReceived = packetsReceived;
        this.channelHelper = channelHelper;
    }

    @Override
    public void run() {
        HashMap<Long, TimerTask> tasksMap = new HashMap<>();
        ArrayBlockingQueue<Packet> ackBuffer = new ArrayBlockingQueue<>(99);

        try (DatagramChannel channel = DatagramChannel.open()) {
            if (Config.isVerbose)
                System.out.println("Thread forked!");

            HttpRequest httpRequest = new HttpRequest();
            String response = "";

            for (Packet each : packetsReceived) {
                System.out.println("Received Packet SEQ: " + each.getSequenceNumber());
                String responsePayload = new String(each.getPayload(), StandardCharsets.UTF_8);
                response = response.concat(responsePayload.trim());
            }

            //Perform the operations on received request...
            if (Config.isVerbose)
                System.out.println("Incoming Connection:  " + packetsReceived.get(0).getPeerAddress());

            ServerParameters serverParameters = new ServerParameters();

            HashMap<Long, Packet> packetList = generatePackets(httpRequest, serverParameters, packetsReceived.get(0), response);

            System.out.println("Peer port:" + packetsReceived.get(0).getPeerPort());

            boolean flag = true;

            Timer timer = new Timer(true);

            channelHelper.join();

            // Send Packets
            while (flag) {
                flag = sendPackets(packetList, channel, socketAddress, timer, tasksMap);

                int k = 0;

                // Create Buffer for Response
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                // Receive each Ack one by one
                while (tasksMap.size() != 0) {
                    channel.receive(buf);
                    buf.flip();

                    Packet ackPacket = Packet.fromBuffer(buf);
                    buf.clear();

                    if (!ackBuffer.contains(ackPacket) && ackPacket.getType() == 2) {
                        ackBuffer.add(ackPacket);

                        System.out.println("ACK SEQ:" + ((int) ackPacket.getSequenceNumber()));

                        if (tasksMap.containsKey(ackPacket.getSequenceNumber())) {
                            TimerTask timerTask = tasksMap.get(ackPacket.getSequenceNumber());
                            timerTask.cancel();
                            tasksMap.remove(ackPacket.getSequenceNumber());
                            System.out.println("CANCELLED TASK:" + (ackPacket.getSequenceNumber()));
                        }
                    }

                    System.out.println("Received Ack List Size:" + ackBuffer.size());
                }
            }

            timer.cancel();
            packetList.clear();
            if (Config.isVerbose)
                System.out.println("Response sent!\n");
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static HashMap<Long, Packet> generatePackets(HttpRequest httpRequest, ServerParameters serverParameters, Packet packet, String payload) {
        HashMap<Long, Packet> packetList = new HashMap<>();
        httpRequest.processRequest(new StringBuilder(payload), serverParameters);
        byte[] buffer = serverParameters.response.getBytes();
        int i = 0;
        byte[] bytes;
        bytes = "\r\n".getBytes();
        while (i < buffer.length) {
            byte[] slice;
            if (i + 1012 > buffer.length) {
                slice = Arrays.copyOfRange(buffer, i, buffer.length - 1);
            } else {
                slice = Arrays.copyOfRange(buffer, i, i + 1012);
            }

            Packet responsePacket = packet.toBuilder()
                    .setSequenceNumber(Server.modifyCurrentSequence(true))
                    .setPayload(slice)
                    .create();

            packetList.put(Server.modifyCurrentSequence(false), responsePacket);
            Server.sequenceNumbers.add(Server.currentSequence);
            i += 1013;
        }

        Packet lastPacket = packet.toBuilder()
                .setSequenceNumber(Server.modifyCurrentSequence(true))
                .setType(0)
                .setPayload(bytes)
                .create();

        packetList.put(Server.modifyCurrentSequence(false), lastPacket);
        Server.sequenceNumbers.add(Server.currentSequence);

        return packetList;
    }

    private static boolean sendPackets(HashMap<Long, Packet> packetList, DatagramChannel channel, SocketAddress routerAddress, Timer timer, HashMap<Long, TimerTask> tasksMap) throws IOException {
        ++Server.start;
        System.out.println("Current Sequence:" + Server.currentSequence);
        for (int i = Server.start; i < Server.start + 5; i++) {
            System.out.println("Packets Sent as Response: " + (Server.modifyCurrentSequence(false)));

            if (packetList.size() == 1) {
                channel.send(packetList.get((long) i).toBuffer(), routerAddress);
                TimerTask task = new PacketTimeout(packetList.get((long) i), channel, routerAddress);
                tasksMap.put((long) i, task);
                timer.schedule(task, 5000, 5000);
                packetList.remove((long) i);
                Server.start++;
                return false;
            }

            channel.send(packetList.get((long) i).toBuffer(), routerAddress);
            TimerTask task = new PacketTimeout(packetList.get((long) i), channel, routerAddress);
            tasksMap.put((long) i, task);
            timer.schedule(task, 5000, 5000);
            packetList.remove((long) i);
            Server.start++;
        }


        return true;
    }
}
