import model.Packet;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

import static java.nio.channels.SelectionKey.OP_READ;

public class ChannelHelper extends Thread {
    SocketAddress routerAddress;
    InetSocketAddress socketAddress;
    DatagramChannel channel;
    int i = 0;

    public ChannelHelper(InetSocketAddress socketAddress, SocketAddress routerAddress, DatagramChannel channel) {
        this.routerAddress = routerAddress;
        this.socketAddress = socketAddress;
        this.channel = channel;
    }

    @Override
    public void run() {
        try {
//            channel.socket().setReuseAddress(true);
//            channel.bind(socketAddress);
            channel.configureBlocking(false);
            Selector selector = Selector.open();
            SelectionKey key1 = channel.register(selector, OP_READ);

            Set<SelectionKey> keys = selector.selectedKeys();
            System.out.println("Selector Keys Size:" + keys.size());

//            SelectionKey key = iterator.next();

            while (i < 2) {
                selector.select(5000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                if (!iter.hasNext())
                    i++;

                while (iter.hasNext()) {

                    SelectionKey key = iter.next();

                    if (key.isReadable()) {
                        ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);

                        channel.receive(buf);
                        buf.flip();

                        Packet packet = Packet.fromBuffer(buf);
                        System.out.println("BUF LIMIT AND SEQ: " + buf.limit() + " ");

                        Packet ackPacket = packet.toBuilder()
                                .setType(2)
                                .setPayload(new byte[0])
                                .create();

                        System.out.println("ACKED PACKET:" + ackPacket.getSequenceNumber());

                        channel.send(ackPacket.toBuffer(), routerAddress);
                    }
                    iter.remove();
                }
            }

            keys.clear();
            key1.cancel();

//            while (true) {
//                // Create Buffer for Response
//                ByteBuffer buf = ByteBuffer.allocate(Packet.MAX_LEN);
//
//                keys = selector.selectedKeys();
//                if (keys.isEmpty()) {
//                    System.out.println("No response after timeout");
//                    break;
//                }
//
//                System.out.println("IN CHANNEL HELPER ");
//                channel.receive(buf);
//                buf.flip();
//
//                if (buf.limit() < Packet.MIN_LEN || buf.limit() > Packet.MAX_LEN) {
//                    System.out.println("BUF LIMIT AND SEQ: " + buf.limit() + " ");
//                    buf.clear();
//                    continue;
//                }
//
//                Packet packet = Packet.fromBuffer(buf);
//                System.out.println("BUF LIMIT AND SEQ: " + buf.limit() + " ");
//
//                Packet ackPacket = packet.toBuilder()
//                        .setType(2)
//                        .setPayload(new byte[0])
//                        .create();
//
//                System.out.println("ACKED PACKET:" + ackPacket.getSequenceNumber());
//
//                channel.send(ackPacket.toBuffer(), routerAddress);
//
//                // Wait for response
//                selector.select(5000);
////
////                Set<SelectionKey> keys = selector.selectedKeys();
////                System.out.println("Selector Keys Size:" + keys.size());
//
////                this.wait(2000);
//
//
//
////                i++;
//            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
