import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import will.tests.comm.core.ClientManager;
import will.tests.comm.core.EndPoint;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RawMqttClientApp {

    public static void main(String[] args) throws Exception {
        final ClientManager mgr = new ClientManager(0, pipeline -> {
            pipeline.addLast("reader", new SimpleChannelInboundHandler<ByteBuf>() {
                public void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
                    byte pktType = buf.readByte();

                    if ((pktType >> 4) == 2) { // CONNACK
                        Preconditions.checkArgument(2 == buf.readByte(), "invalid remaining length"); // remaining length
                        buf.readByte(); // reserved byte

                        int ret = buf.readByte();
                        if (ret != 0) {
                            System.out.println("failed to connect: " + ret);
                            ctx.close();
                        } else {
                            System.out.println("connect successfully");
                        }
                    }
                }

                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    super.channelInactive(ctx);

                    System.out.println("channel become inactive");
                }
            });
        });

  /*
        // 1.0 beta
        EndPoint server = new EndPoint("10.27.20.52", 11883);
        ByteBuf data = getConnMsg(
                "will-dev03",
                "dev03",
                "DWXD/9yhn2Mv4nsBcsUL8h3Rvq4xK/zud1uuxE9On5iRuIZvKvQdV/5nv2M=");
*/
        // 2.0 beta
        EndPoint server = new EndPoint("localhost", 11883);
        ByteBuf data = getConnMsg(
                "mqtt-sample-subdev02|securemode=2,signmethod=sha256,timestamp=1574143977891|",
                "mqtt-sample-subdev02&gvQGUcaT",
                "2c7b8002bcd7e675494f3a6fcbf94a4801dc18566e2da9bfe994e6f05a5f80fb");
        mgr.sendMessage(server, data).addListener(new GenericFutureListener<Future<? super Boolean>>() {
            @Override
            public void operationComplete(Future<? super Boolean> future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("sent connect message");
                } else {
                    System.out.println("failed to send connect message");
                }
            }
        });

        System.in.read();


        mgr.sendMessage(server, getDisconnMsg()).addListener(new GenericFutureListener<Future<? super Boolean>>() {
            @Override
            public void operationComplete(Future<? super Boolean> future) throws Exception {
                if (future.isSuccess()) {
                    System.out.println("sent disconnect message");
                } else {
                    System.out.println("failed to send disconnect message");
                }
            }
        });
        TimeUnit.SECONDS.sleep(3);

        mgr.shutdown();
    }

    private static ByteBuf getConnMsg(String clientId, String userName, String passwd) {
        ByteBuf variableBuf = Unpooled.buffer(1024);

        // protocol name
        variableBuf.writeByte(0x00);
        variableBuf.writeByte(0x04);
        variableBuf.writeBytes(new byte[] {'M', 'Q', 'T', 'T'});

        // version
        variableBuf.writeByte(0x03);

        // connect flags (User Name Flag | Password Flag)
        variableBuf.writeByte(0xC0);

        // Keep Alive (60s)
        variableBuf.writeByte(0x00);
        variableBuf.writeByte(0x0a);

        // Client ID
        populateString(variableBuf, clientId);

        // User Name
        populateString(variableBuf, userName);

        // Password
        populateString(variableBuf, passwd);

        ByteBuf fixedHeader = Unpooled.buffer(16);
        // header flags
        fixedHeader.writeByte(0x10);
        encodeRemainingLength(variableBuf.readableBytes(), fixedHeader);

        CompositeByteBuf buf = Unpooled.compositeBuffer();
        buf.addComponent(true, fixedHeader);
        buf.addComponent(true, variableBuf);

        return buf;
    }

    private static ByteBuf getPublishMsg(String topic, String message, int qos) {
        Preconditions.checkArgument(qos <= 2, "invalid qos value: " + qos);

        ByteBuf variableBuf = Unpooled.buffer(1024);

        populateString(variableBuf, topic);
        populateString(variableBuf, message);

        ByteBuf fixedHeader = Unpooled.buffer(16);
        // header flags
        fixedHeader.writeByte((3 << 4) | (qos << 1)); // PUB | Qos
        encodeRemainingLength(variableBuf.readableBytes(), fixedHeader);

        CompositeByteBuf buf = Unpooled.compositeBuffer();
        buf.addComponent(true, fixedHeader);
        buf.addComponent(true, variableBuf);

        return buf;
    }

    private static ByteBuf getDisconnMsg() {
        ByteBuf fixedHeader = Unpooled.buffer(16);
        // header flags
        fixedHeader.writeByte(14 << 4);
        fixedHeader.writeByte(0);
        return fixedHeader;
    }

    private static void populateString(ByteBuf buf, String data) {
        byte[] dataBin = data.getBytes(StandardCharsets.UTF_8);
        buf.writeByte((dataBin.length >> 8));
        buf.writeByte((dataBin.length & 0xFF));
        buf.writeBytes(dataBin);
    }

    private static void encodeRemainingLength(int length, ByteBuf buf) {
        do {
            int encodedByte = length % 128;

            length = length / 128;
            if (length > 0) {
                encodedByte = encodedByte | 128;
            }

            buf.writeByte(encodedByte);
        } while (length > 0);
    }

}
