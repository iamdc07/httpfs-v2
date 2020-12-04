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
    static int start = 1;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Packet> receivedPackets = Collections.synchronizedList(new ArrayList<>());
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

                        while (true) {
                            SocketAddress router = null;

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

                                // Add Client request packets
                                if (!sequenceNumbers.contains(responsePacket.getSequenceNumber()) && responsePacket.getType() == 0) {
                                    System.out.println("Adding this packet");

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
                                    System.out.println("Resending ACK for Packet: " + responsePacket.getSequenceNumber());
                                    continue;
                                }

                                Server.start = (int) currentSequence;

                                if (finalPacket != null && sequenceNumbers.size() == (finalPacket.getSequenceNumber())) {
                                    flag = false;
                                    break;
                                }

                                System.out.println("Received Arraylist Size:" + receivedPackets.size());
                            }

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

                            if (receivedPackets.size() == 0)
                                continue;

                            ArrayList<Packet> copy = new ArrayList<>(receivedPackets);
                            Serve serve = new Serve(router, copy, channelHelper);
                            serve.start();
                            serve.join();
                            receivedPackets.clear();
                            flag = true;
                            finalPacket = null;
                            buf.clear();
                            channel.configureBlocking(true);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
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

            boolean flag = true;

            while (flag) {
                // Create Buffer for Response
                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                selector.select(3000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

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