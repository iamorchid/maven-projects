package will.test.cluster.internal.master;

import com.envision.eos.commons.transport.ServerManager;
import com.envision.eos.commons.transport.pipeline.PipelineInitializer;
import io.netty.channel.*;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import will.test.Config;
import will.test.cluster.*;
import will.test.common.Utils;
import will.test.event.master.AbdicateEvent;
import will.test.event.master.BecomeMasterEvent;
import will.test.event.master.NodeJoinEvent;
import will.test.event.master.NodeLeaveEvent;
import will.test.lock.LockServerNaException;
import will.test.message.IBizMessage;
import will.test.message.internal.IMessage;
import will.test.message.internal.master.ClusterStateUpdateMessage;
import will.test.message.internal.master.HeartbeatResponseMessage;
import will.test.message.internal.master.JoinResponseMessage;
import will.test.message.internal.node.HeartbeatRequestMessage;
import will.test.message.internal.node.JoinRequestMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Implement the core responsibilities assumed by the master.
 *
 * @author jian.zhang4
 */
@Slf4j
public class Master implements IMaster, PipelineInitializer {
    private final static AttributeKey<InternalNodeInfo> NODE_INFO_KEY = AttributeKey.newInstance("node.info.key");

    private final Config config;
    private final MasterInfo selfInfo;
    private final ServerManager serverManager;

    private final Map<NodeInfo, Channel> nodes = new ConcurrentHashMap<>();
    private final AtomicBoolean destroyed = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture> refreshTask = new AtomicReference<>();

    public Master(final Config config, final MasterInfo selfInfo) {
        this.config = config;
        this.selfInfo = selfInfo;
        this.serverManager = new ServerManager(
                selfInfo.getEndpoint().getPort(),
                Utils.getServerCtx(config), this);
    }

    @Override
    public void init(ChannelPipeline channelPipeline) throws Exception {
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Master.class.getClassLoader())));
        channelPipeline.addLast(new SimpleChannelInboundHandler<IMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, IMessage message) throws Exception {
                if (message != null) {
                    handleMessage(ctx.channel(), message);
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                ctx.fireChannelInactive();
                clean(ctx.channel());
            }
        });
    }

    public CompletableFuture<MasterInfo> start() {
        ChannelFuture channelFuture = serverManager.start();
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("master [{}][{}][{}] has started successfully",
                        config.getClusterId(), selfInfo.getEndpoint(), selfInfo.getMasterId());
                config.getMasterEventHandler().onBecomeMaster(new BecomeMasterEvent(this));
                scheduleRefreshTask(future.channel().eventLoop());
            }
        });
        return Utils.conv(channelFuture, selfInfo);
    }

    private void scheduleRefreshTask(EventLoop loop) {
        final int periodMillis = Math.min(config.getLockExpireAfter() * 1000 / 3, 1000);
        final AtomicInteger checkPastCycles = new AtomicInteger(0);

        refreshTask.set(loop.scheduleAtFixedRate(() -> {
                    try {
                        // We check periodically if we are still the valid master of the cluster
                        boolean performAbdicationCheck = checkPastCycles.getAndIncrement() >= 10;

                        if (!config.getLockImpl().refreshMaster(config.getClusterId(), config.getLockExpireAfter())) {
                            log.warn("master {} failed to refresh lock expire time", getFullId());
                            performAbdicationCheck = true;
                        }

                        if (performAbdicationCheck) {
                            abdicateIfNoLongerMaster();
                            checkPastCycles.set(0);
                        }
                    } catch (LockServerNaException error) {
                        log.error("redis not available", error);
                    } catch (Throwable e) {
                        log.error("[BUG] hit un-expected error", e);
                    }
                }, periodMillis / 2, periodMillis, TimeUnit.MILLISECONDS)
        );
    }

    private String getFullId() {
        return selfInfo.getGlobalId(config.getClusterId());
    }

    private void abdicateIfNoLongerMaster() throws LockServerNaException {
        MasterInfo persistedMaster = config.getLockImpl().getMaster(config.getClusterId());
        if (persistedMaster == null) {
            log.error("[BUG] live local master {} has no persisted info", getFullId());
            if (config.getLockImpl().setMasterIfAbsent(config.getClusterId(), selfInfo, config.getLockExpireAfter())) {
                log.info("master {} re-set persistent info successfully", getFullId());
            } else {
                // someone else has taken over the mastership and we need to re-check
                abdicateIfNoLongerMaster();
            }
        } else if (ObjectUtils.notEqual(selfInfo, persistedMaster)) {
            log.warn("local master [{}] has been replaced by new master [{}]",
                    selfInfo.getEndpoint(), persistedMaster.getEndpoint());
            destroy();
        }
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            ScheduledFuture refreshKeyTask = refreshTask.getAndSet(null);
            if (refreshKeyTask != null) {
                refreshKeyTask.cancel(false);
            }

            serverManager.shutdown();
            config.getMasterEventHandler().onAbdicateEvent(new AbdicateEvent(this));

            log.info("master {} has been destroyed", getFullId());
        }
    }

    private void writeMessage(Channel channel, NodeInfo node, IMessage message) {
        if (message != null) {
            channel.writeAndFlush(message).addListener(future -> {
                if (future.cause() != null) {
                    log.error("failed to send {} to node {}", Utils.getType(message), node, future.cause());
                }
            });
        }
    }

    private void handleMessage(final Channel channel, IMessage message) {
        if (message instanceof JoinRequestMessage) {
            handleJoinMessage(channel, (JoinRequestMessage) message);
            return;
        }

        InternalNodeInfo internalNode = channel.attr(NODE_INFO_KEY).get();
        if (internalNode == null) {
            log.error("[BUG] reject message [{}] from channel [{}] as no join is seen",
                    Utils.getType(message), Utils.getPeerAddress(channel));
            channel.close();
            return;
        }

        // Always to update the heartbeat if we receive a message from the node
        internalNode.refreshHeartbeat();

        final NodeInfo node = internalNode.getNode();
        if (message instanceof HeartbeatRequestMessage) {
            handleHeartbeatMessage(channel, node, (HeartbeatRequestMessage) message);
        } else if (message instanceof IBizMessage){
            handleBizMessage(channel, node, (IBizMessage) message);
        } else {
            log.error("received un-supported message [{}] from {}", Utils.getType(message), node);
        }
    }

    private void handleBizMessage(Channel channel, NodeInfo node, IBizMessage message) {
        IMessage response = config.getMasterMsgHandler().handle(node, message);
        writeMessage(channel, node, response);
    }

    private void handleJoinMessage(final Channel channel, final JoinRequestMessage message) {
        final NodeInfo node = message.getNode();

        int responseCode = JoinResponseMessage.CODE_SUCCESS;
        String error = null;

        InternalNodeInfo internalNode = channel.attr(NODE_INFO_KEY).get();
        if (internalNode != null) {
            log.warn("node {} has already joined previously", node);
            // Here we just refresh the heartbeat
            internalNode.refreshHeartbeat();
        } else if (ObjectUtils.notEqual(config.getClusterId(), message.getClusterId())) {
            log.warn("reject node {} from a different cluster [{}], current cluster [{}]",
                    node, message.getClusterId(), config.getClusterId());
            error = String.format("cluster id [%s] in req doesn't match [%s]", message.getClusterId(), config.getClusterId());
            responseCode = JoinResponseMessage.CODE_CLUSTER_UNMATCHED;
        } else {
            final ScheduledFuture future = channel.eventLoop().scheduleAtFixedRate(() -> {
                InternalNodeInfo interNode = channel.attr(NODE_INFO_KEY).get();
                if (interNode != null && interNode.getHeartbeatLast() + config.getHeartbeatTimeoutNs() < System.nanoTime()) {
                    log.warn("node {} didn't update heartbeat in last {}s", interNode.getNode(), config.getHeartbeatTimeout());
                    channel.close();
                }
            }, 1000, config.getHeartbeatTimeout() * 1000, TimeUnit.MILLISECONDS);

            internalNode = new InternalNodeInfo(node, future, System.nanoTime());
            channel.attr(NODE_INFO_KEY).set(internalNode);

            if (nodes.putIfAbsent(node, channel) != null) {
                log.error("[BUG] found duplicated join node {}", node);
            } else {
                log.info("new node {} has joined", node);
            }

            // Notify business logic of the newly joined node
            config.getMasterEventHandler().onNodeJoin(new NodeJoinEvent(this, node));
        }

        // Anyway, we need to answer the node
        IMessage response = new JoinResponseMessage(responseCode, selfInfo.getMasterId(), message.getRequestId(), error);
        writeMessage(channel, node, response);

        if (responseCode == JoinResponseMessage.CODE_SUCCESS) {
            // notify all nodes (including the newly joined one) about the latest cluster state
            broadcastSafe(nodes, new ClusterStateUpdateMessage(getClusterSafe()), null);
        }
    }

    private void handleHeartbeatMessage(Channel channel, NodeInfo node, HeartbeatRequestMessage message) {
        if (message.getPayload() != null) {
            IMessage bizResponse = config.getMasterMsgHandler().handle(node, message.getPayload());
            writeMessage(channel, node, bizResponse);
        }

        IMessage response = new HeartbeatResponseMessage(selfInfo.getMasterId(), config.getHeartbeatRspPayload(node));
        writeMessage(channel, node, response);
    }

    private void clean(Channel channel) {
        InternalNodeInfo internalNode = channel.attr(NODE_INFO_KEY).getAndSet(null);
        if (internalNode != null) {
            final NodeInfo node = internalNode.getNode();
            log.warn("clean the connection associated with inactive node {}", node);

            internalNode.getTimeoutTask().cancel(false);
            nodes.remove(node);

            // Notify business logic of the left node
            config.getMasterEventHandler().onNodeLeave(new NodeLeaveEvent(this, node));
        }
    }

    private ClusterState getClusterSafe() {
        return new ClusterState(config.getClusterId(), selfInfo, new ArrayList<>(nodes.keySet()));
    }

    @Override
    public ClusterState getCluster() throws NoLongerMasterException {
        return doUnderMastership(this::getClusterSafe);
    }

    @Override
    public MasterInfo getSelfInfo() throws NoLongerMasterException {
        return doUnderMastership(() -> selfInfo);
    }

    private void broadcastSafe(Map<NodeInfo, Channel> currentNodes, IMessage message,
                               BiConsumer<NodeInfo, io.netty.util.concurrent.Future> handler) {
        currentNodes.forEach((nodeInfo, channel) -> {
            ChannelFuture promise = channel.writeAndFlush(message);
            if (handler != null) {
                promise.addListener(future -> handler.accept(nodeInfo, future));
            }
        });
    }

    @Override
    public CompletableFuture<BroadcastResult> broadcast(IBizMessage message) throws NoLongerMasterException {
        return doUnderMastership(() -> {
            final Map<NodeInfo, Channel> snapshot = new HashMap<>(nodes);

            final CompletableFuture<BroadcastResult> result = new CompletableFuture<>();
            final AtomicInteger counter = new AtomicInteger(0);
            final Map<NodeInfo, Throwable> errors = new ConcurrentHashMap<>(snapshot.size());

            broadcastSafe(snapshot, message, (node, future) -> {
                counter.incrementAndGet();
                if (future.cause() != null) {
                    errors.putIfAbsent(node, future.cause());
                }
                if (counter.get() == snapshot.size()) {
                    result.complete(new BroadcastResult(snapshot.size(), errors));
                }
            });

            return result;
        });
    }

    @Override
    public CompletableFuture<Boolean> send(NodeInfo node, IBizMessage message) throws NoLongerMasterException {
        return doUnderMastership(() -> {
            Channel channel = nodes.get(node);
            if (channel != null) {
                return Utils.conv(channel.writeAndFlush(message), Boolean.TRUE);
            }
            return Utils.doneWithError(new RuntimeException("found no node: " + node));
        });
    }

    private <T> T doUnderMastership(Supplier<T> resultSupplier) throws NoLongerMasterException {
        if (destroyed.get()) {
            throw new NoLongerMasterException(selfInfo + " is no longer master any more");
        }
        return resultSupplier.get();
    }

}
