package will.tests.comm.core.pipeline;

import com.google.protobuf.MessageLite;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;

import java.util.function.Supplier;

public class ProtoBufReadPipelineInitializer<T extends ChannelInboundHandler>
        implements PipelineInitializer {
    private final MessageLite messageLite;
    private final Supplier<T> bizHandler;

    public ProtoBufReadPipelineInitializer(MessageLite messageLite, Supplier<T> bizHandler) {
        this.messageLite = messageLite;
        this.bizHandler = bizHandler;
    }

    @Override
    public void init(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast(new ProtobufVarint32FrameDecoder());
        pipeline.addLast(new ProtobufDecoder(messageLite));
        pipeline.addLast(bizHandler.get());
    }
}
