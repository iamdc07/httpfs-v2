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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        SocketChannel socketChannel = null;
        try {
            while (!Config.isValid) {
                String command = scanner.nextLine();
                validate(command);

                if (Config.isValid) {
                    try (DatagramChannel channel = DatagramChannel.open()) {
                        channel.bind(new InetSocketAddress(Config.port));
                        System.out.println("Listening on Port " + Config.port);
                        ByteBuffer buf = ByteBuffer
                                .allocate(Packet.MAX_LEN)
                                .order(ByteOrder.BIG_ENDIAN);
                        buf.clear();

                        channel.configureBlocking(false);
                        Selector selector = Selector.open();
                        // TO-DO Handle multiple packets by multiple clients
                        while (true) {
                            SocketAddress router = null;
                            ArrayList<Packet> packetList = new ArrayList<>();

                            // Receive a packet within the timeout
                            channel.register(selector, OP_READ);

                            selector.select();
                            Set<SelectionKey> keys = selector.selectedKeys();
                            System.out.println("Selector Keys Size:" + keys.size());
                            Iterator<SelectionKey> iterator = keys.iterator();
                            SelectionKey key = iterator.next();

                            while (key.isReadable()) {
//                                if (selectionKey.isReadable()) {
                                if (router == null)
                                    router = channel.receive(buf);
                                else
                                    channel.receive(buf);

                                buf.flip();

                                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN)
                                    break;

                                Packet responsePacket = Packet.fromBuffer(buf);
                                buf.clear();
                                packetList.add(responsePacket);
                                System.out.println("Received Arraylist Size:" + packetList.size());
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

//                            String response = "";

                            ArrayList<Packet> copy = new ArrayList<>(packetList);
                            Serve serve = new Serve(router, copy);
                            serve.start();
//                            serve.join();
                            packetList.clear();
                            buf.clear();
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