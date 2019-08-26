package will.tests.comm.core.pipeline;

import com.google.protobuf.MessageLite;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

import java.util.function.Supplier;

public class ProtoBufRWPipelineInitializer<T extends ChannelInboundHandler>
        extends ProtoBufWriteOnlyPipelineInitializer<T> {
    private final MessageLite messageLite;
    private final Supplier<T> bizHandler;

    public ProtoBufRWPipelineInitializer(MessageLite messageLite, Supplier<T> bizHandler) {
        this.messageLite = messageLite;
        this.bizHandler = bizHandler;
    }

    @Override
    public void init(ChannelPipeline pipeline) throws Exception {
        super.init(pipeline);

        pipeline.addLast(new ProtobufVarint32FrameDecoder());
        pipeline.addLast(new ProtobufDecoder(messageLite));
        pipeline.addLast(bizHandler.get());
    }
}
