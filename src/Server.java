import model.Config;
import model.ServerParameters;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        SocketChannel socketChannel = null;
        try {
            while (!Config.isValid) {
                String command = scanner.nextLine();
                validate(command);

                if (Config.isValid) {
                    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

                    serverSocketChannel.socket().bind(new InetSocketAddress(Config.port));
                    System.out.println("Listening on Port " + Config.port);

                    while (true) {
                        socketChannel = serverSocketChannel.accept();

                        Serve serve = new Serve(socketChannel);
                        serve.start();
                    }
                } else {
                    System.out.println("Please enter a valid command.\n");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (socketChannel != null)
                socketChannel.close();
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

            if (!Config.hasPath)
            {
                Config.path = "./";
                Config.hasPath = true;
            }
        }
    }
}

// httpfs -v -d ./
// httpfs -v -d ../../../files