import model.Config;
import model.Packet;
import model.ServerParameters;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    static long currentSequence = 0L;
    static Set<Long> sequenceNumbers = new HashSet<>();
    static Set<Long> closingPackets = new HashSet<>();
    static int start = 1;

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        List<Packet> receivedPackets = Collections.synchronizedList(new ArrayList<>());
//        Set<Long> sequenceNumbers = new HashSet<>();
        Packet finalPacket = null;
        boolean flag = true;


        SocketChannel socketChannel = null;
        try {
            while (!Config.isValid) {
//                String command = scanner.nextLine();
                String command  = "httpfs -v -d ../../../files";
                validate(command);

                Packet packet = connect();
                receivedPackets.add(packet);

                if (Config.isValid) {
                    try (DatagramChannel channel = DatagramChannel.open()) {
                        InetSocketAddress socketAddress = new InetSocketAddress(Config.port);
                        channel.bind(socketAddress);
                        System.out.println("Listening on Port " + Config.port);
                        ByteBuffer buf = ByteBuffer
                                .allocate(Packet.MAX_LEN)
                                .order(ByteOrder.BIG_ENDIAN);
                        buf.clear();

//                        channel.configureBlocking(false);
//                        Selector selector = Selector.open();

//                        DatagramChannel channel1 = DatagramChannel.open();
//                        channel1.socket().setReuseAddress(true);
//                        channel1.bind(new InetSocketAddress(Config.port));


                        // TO-DO Handle multiple packets by multiple clients
                        while (true) {
                            SocketAddress router = null;
//                            ArrayList<Packet> responsePackets = new ArrayList<>();

                            // Receive a packet within the timeout
//                            channel.register(selector, OP_READ);
//
//                            selector.select();
//                            Set<SelectionKey> keys = selector.selectedKeys();
//                            System.out.println("Selector Keys Size:" + keys.size());
//                            Iterator<SelectionKey> iterator = keys.iterator();
//                            SelectionKey key = iterator.next();

                            while (flag) {
                                System.out.println("Listening on Port " + Config.port);
                                if (router == null)
                                    router = channel.receive(buf);
                                else
                                    channel.receive(buf);

                                buf.flip();

                                Packet responsePacket;

                                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN) {
                                    buf.clear();
                                    continue;
                                }

                                responsePacket = Packet.fromBuffer(buf);
                                buf.clear();

                                System.out.println("PACKET SEQ:" + responsePacket.getSequenceNumber());

//                                if (responsePacket.getType() == 1) {
//                                    if (!closingPackets.contains(responsePacket.getSequenceNumber())) {
////                                        receivedPackets.clear();
////                                        finalPacket = null;
////                                        buf.clear();
//                                        closingPackets.add(responsePacket.getSequenceNumber());
//                                    } else
//                                        continue;
//                                }

//                                if (!(responsePacket.getSequenceNumber() >= start - 1 && responsePacket.getSequenceNumber() <= start + 4)) {
//                                    continue;
//                                }

                                // Add Client request packets
                                if (!sequenceNumbers.contains(responsePacket.getSequenceNumber()) && responsePacket.getType() == 0) {
                                    System.out.println("Adding this packet");


                                    // TO-DO Add ack resend - Drop and delay
                                    // Send ACK for the received packet
//                                    Packet packet = responsePacket.toBuilder()
//                                            .setType(2)
//                                            .setPayload(new byte[0])
//                                            .create();
//
//                                    channel.send(packet.toBuffer(),router);
                                    modifyCurrentSequence(true);

                                    sendAck(responsePacket, channel, router);
                                    System.out.println("Current Sequence Pointer:" + currentSequence);

                                    String responsePayload = new String(responsePacket.getPayload(), StandardCharsets.UTF_8);

                                    if (responsePayload.equalsIgnoreCase("\r\n"))
                                        finalPacket = responsePacket;
                                    else {
                                        // Add packet to buffer
                                        receivedPackets.add(responsePacket);
                                    }
                                    sequenceNumbers.add(responsePacket.getSequenceNumber());
                                } else if (responsePacket.getType() == 0) {
                                    sendAck(responsePacket, channel, router);
                                    System.out.println("Resensding ACK for Packet: " + responsePacket.getSequenceNumber());
                                    continue;
                                }

                                Server.start = (int) currentSequence;

                                if (finalPacket != null && sequenceNumbers.size() == (finalPacket.getSequenceNumber())) {
                                    flag = false;
                                    break;
                                }

//                                responsePackets.add(responsePacket);
                                System.out.println("Received Arraylist Size:" + receivedPackets.size());
//                                System.out.println("Length:" + buf.limit());
//                                }
//                                // Wait for response
//                                selector.select(5000);
//
//                                keys = selector.selectedKeys();
//                                if (keys.isEmpty()) {
//                                    System.out.println("No response after timeout");
//                                    keys.clear();
//                                    break;
//                                }
                            }

//                            channel.socket().setReuseAddress(true);
                            System.out.println("Starting channelHelper");
                            ChannelHelper channelHelper = new ChannelHelper(socketAddress, router, channel);
                            channelHelper.start();

                            // Reorder packets
                            synchronized (receivedPackets) {
                                Collections.sort(receivedPackets, new Comparator<>() {
                                    @Override
                                    public int compare(Packet o1, Packet o2) {
                                        return Long.compare(o1.getSequenceNumber(), o2.getSequenceNumber());
                                    }
                                });
                            }

//                            String response = "";

                            if (receivedPackets.size() == 0)
                                continue;

                            ArrayList<Packet> copy = new ArrayList<>(receivedPackets);
                            Serve serve = new Serve(router, copy, channelHelper);
                            serve.start();
                            serve.join();
//                            responsePackets.clear();
                            receivedPackets.clear();
//                            sequenceNumbers.clear();
                            flag = true;
                            finalPacket = null;
                            buf.clear();
                            channel.configureBlocking(true);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
//                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
//
//                    serverSocketChannel.socket().bind(new InetSocketAddress(Config.port));
//                    System.out.println("Listening on Port " + Config.port);
//
//                    while (true) {
//                        socketChannel = serverSocketChannel.accept();
//
//                        Serve serve = new Serve(socketChannel);
//                        serve.start();
//                    }
                } else {
                    System.out.println("Please enter a valid command.\n");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected static long modifyCurrentSequence(boolean modify) {
        if (modify)
            currentSequence++;

        return currentSequence;
    }

    private static void sendAck(Packet responsePacket, DatagramChannel channel, SocketAddress routerAddress) throws IOException {
        Packet packet = responsePacket.toBuilder()
                .setType(2)
                .setPayload(new byte[0])
                .create();

        channel.send(packet.toBuffer(), routerAddress);
        System.out.println("Sending ACK for packet: " + packet.getSequenceNumber());
    }

    private static Packet connect() {
        try (DatagramChannel channel = DatagramChannel.open()) {
            InetSocketAddress socketAddress = new InetSocketAddress(Config.port);
            channel.bind(socketAddress);
            SocketAddress routerAddress = null;

            channel.configureBlocking(false);
            Selector selector = Selector.open();
            channel.register(selector, OP_READ);

            Set<SelectionKey> keys = selector.selectedKeys();
            System.out.println("Selector Keys Size:" + keys.size());

//            Packet packet = new Packet.Builder()
//                    .setType(1)
//                    .setSequenceNumber(currentSequence)
//                    .create();

            boolean flag = true;

            while (flag) {
                // Create Buffer for Response
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                selector.select(3000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
//                SelectionKey key = iter.next();

                // Receive Response
                routerAddress = channel.receive(buf);
                buf.flip();

                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
                    continue;

                Packet receivedPacket = Packet.fromBuffer(buf);
                buf.clear();

                if (receivedPacket.getType() == 0) {
                    sequenceNumbers.add(receivedPacket.getSequenceNumber());
                    modifyCurrentSequence(true);
                    sendAck(receivedPacket, channel, routerAddress);
                    selector.close();
                    channel.socket().close();
//                    key.cancel();
                    channel.close();
                    return receivedPacket;
                } else if (receivedPacket.getType() == 1 && !sequenceNumbers.contains(receivedPacket.getSequenceNumber())) {
                    sequenceNumbers.add(receivedPacket.getSequenceNumber());
                    modifyCurrentSequence(true);
                }

                Packet responsePacket = receivedPacket.toBuilder()
                        .setSequenceNumber(currentSequence)
                        .create();

                channel.send(responsePacket.toBuffer(), routerAddress);
                System.out.println("Handshake completed");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static void validate(String input) {
        String[] words = input.split(" ");

        if (words[0].equalsIgnoreCase("httpfs")) {

            for (int i = 0; i < words.length; i++) {
                switch (words[i]) {
                    case "-v":
                        Config.isVerbose = true;
                        break;
                    case "-p":
                        Config.hasPort = true;
                        Config.port = Integer.parseInt(words[i + 1]);
                        i++;
                        break;
                    case "-d":
                        Config.hasPath = true;
                        Config.path = words[i + 1];
                        i++;
                        break;
                    default:
                        Config.isValid = true;
                }
            }

            if (!Config.hasPort) {
                Config.port = 8080;
                Config.hasPort = true;
            }

            if (!Config.hasPath) {
                Config.path = "./";
                Config.hasPath = true;
            }
        }
    }
}

// httpfs -v -d ./
// httpfs -v -d ../../../files