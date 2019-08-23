package will.tests.comm.protocol.echo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import will.tests.comm.core.pipeline.PipelineInitializer;

public class ServerPipelineInitializer implements PipelineInitializer {
    private static final Logger LOG = LoggerFactory.getLogger(ServerPipelineInitializer.class);

    @Override
    public void init(ChannelPipeline pipeline) throws Exception {
        pipeline.addFirst("FrameDecoder", new LineBasedFrameDecoder(1024));
        pipeline.addAfter("FrameDecoder","StringDecoder", new StringDecoder(CharsetUtil.UTF_8));
        pipeline.addAfter("StringDecoder", "BizHandler", new SimpleChannelInboundHandler<String>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                LOG.info("received message: {} from {}", msg, ctx.channel());

                // echo back the message
                ctx.channel().writeAndFlush(msg);
            }
        });
        pipeline.addLast("stringEncoder", new LineEncoder(CharsetUtil.UTF_8));
    }
}
