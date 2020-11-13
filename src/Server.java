import model.Config;
import model.Packet;
import model.ServerParameters;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

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

                        // TO-DO Handle multiple packets by multiple clients

                        while (true) {
                            SocketAddress router = channel.receive(buf);

                            System.out.println("Length:" + buf.limit());

                            Serve serve = new Serve(router, buf);
                            serve.start();
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