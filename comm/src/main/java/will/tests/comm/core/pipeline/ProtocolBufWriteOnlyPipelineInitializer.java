package will.tests.comm.core.pipeline;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class ProtocolBufWriteOnlyPipelineInitializer<T extends ChannelInboundHandler>
        implements PipelineInitializer {

    @Override
    public void init(ChannelPipeline pipeline) throws Exception {
        pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
        pipeline.addLast(new ProtobufEncoder());
    }

}
