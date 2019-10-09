package will.tests.comm.core.pipeline;

import io.netty.channel.ChannelPipeline;

@FunctionalInterface
public interface PipelineInitializer {
    public void init(ChannelPipeline pipeline) throws Exception;
}
