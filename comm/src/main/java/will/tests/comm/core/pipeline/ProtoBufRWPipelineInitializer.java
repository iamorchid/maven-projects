package will.tests.comm.core.pipeline;

import com.google.protobuf.MessageLite;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class ProtoBufRWPipelineInitializer<T extends ChannelInboundHandler> implements PipelineInitializer {
    private final ProtoBufReadPipelineInitializer readPipeInitializer;
    private final ProtoBufWritePipelineInitializer writePipeInitializer;

    public ProtoBufRWPipelineInitializer(MessageLite messageLite, Supplier<T> bizHandler) {
        readPipeInitializer = new ProtoBufReadPipelineInitializer(messageLite, bizHandler);
        writePipeInitializer = new ProtoBufWritePipelineInitializer();
    }

    @Override
    public void init(ChannelPipeline pipeline) throws Exception {
        readPipeInitializer.init(pipeline);
        writePipeInitializer.init(pipeline);
    }

}
