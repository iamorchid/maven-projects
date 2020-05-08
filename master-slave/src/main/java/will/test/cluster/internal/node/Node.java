package will.test.cluster.internal.node;

import com.envision.eos.commons.transport.ClientManager;
import com.envision.eos.commons.transport.EndPoint;
import com.envision.eos.commons.transport.ServerManager;
import com.envision.eos.commons.transport.context.NettyContextResolver;
import com.envision.eos.commons.transport.context.ServerNettyContext;
import com.envision.eos.commons.transport.pipeline.PipelineInitializer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import will.test.Config;
import will.test.cluster.ClusterState;
import will.test.cluster.INode;
import will.test.cluster.MasterInfo;
import will.test.cluster.NodeInfo;
import will.test.cluster.internal.MasterSelector;
import will.test.cluster.internal.master.Master;
import will.test.common.Utils;
import will.test.event.node.ClusterStateUpdateEvent;
import will.test.event.node.JoinMasterEvent;
import will.test.event.node.LeaveMasterEvent;
import will.test.event.node.StartupEvent;
import will.test.lock.LockServerNaException;
import will.test.message.IBizMessage;
import will.test.message.internal.IMessage;
import will.test.message.internal.master.ClusterStateUpdateMessage;
import will.test.message.internal.master.HeartbeatResponseMessage;
import will.test.message.internal.master.IMasterMessage;
import will.test.message.internal.master.JoinResponseMessage;
import will.test.message.internal.node.HeartbeatRequestMessage;
import will.test.message.internal.node.JoinRequestMessage;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Node implements INode, PipelineInitializer {
    private final static int MAX_JOIN_RESPONSE_DELAY = 3;

    private final Config config;
    private final EndPoint address;
    private final MasterSelector masterSelector;
    private final ServerManager serverManager;
    private final ClientManager clientManager;
    private final ScheduledExecutorService scheduledService;

    private final AtomicBoolean detectingCluster = new AtomicBoolean(false);
    private final AtomicReference<PendingJoin> pendingJoinRef = new AtomicReference<>();
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final AtomicReference<InternalMasterInfo> masterRef = new AtomicReference<>();
    private final AtomicReference<ClusterState> clusterStateRef = new AtomicReference<>();

    public Node(Config config) {
        // validate the config firstly
        config.validate();

        this.config = config;
        this.address = new EndPoint(config.getPublicIp(), config.getNodePort());
        this.masterSelector = new MasterSelector(config);

        ServerNettyContext ctx = Utils.getServerCtx(config);
        this.serverManager = new ServerManager(config.getNodePort(), ctx, this);
        this.clientManager = new ClientManager(NettyContextResolver.createClientContext(ctx), 5000, this);
        this.scheduledService = clientManager.getContext().getLoopGroup();
    }

    @Override
    public void init(ChannelPipeline channelPipeline) throws Exception {
        channelPipeline.addLast(new ObjectEncoder());
        channelPipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(Master.class.getClassLoader())));
        channelPipeline.addLast(new SimpleChannelInboundHandler<IMessage>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, IMessage message) throws Exception {
                if (message != null) {
                    if (message instanceof IMasterMessage) {
                        handleMasterMessage(ctx.channel(), (IMasterMessage) message);
                    } else if (message instanceof IBizMessage) {
                        handleBizMessage(ctx.channel(), (IBizMessage) message);
                    } else {
                        log.error("received un-supported message [{}] from {}",
                                Utils.getType(message), Utils.getPeerAddress(ctx.channel()));
                    }
                }
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                super.channelInactive(ctx);

                final InternalMasterInfo master = masterRef.get();
                if (master != null && Objects.equals(master.getEndpoint(), Utils.getPeerAddress(ctx.channel()))) {
                    log.warn("the connection to master {} has lost", getGlobalId(master.getMaster()));
                    startClusterDetection(null, false);
                }
            }
        });
    }

    private void rescheduleClusterDetection(CompletableFuture<Boolean> future, boolean isAppStartup) {
        long delayMillis = 1000 + (System.currentTimeMillis() % 2000);
        log.warn("would re-schedule cluster detection after {}ms", delayMillis);
        scheduledService.schedule(() -> doClusterDetection(future, isAppStartup), delayMillis, TimeUnit.MILLISECONDS);
    }

    private synchronized void doClusterDetection(CompletableFuture<Boolean> future, boolean isAppStartup) {
        if (!detectingCluster.get()) {
            log.error("[BUG] call doClusterDetection without holding detectingCluster CAS");
        }

        // clean any state from previous detection
        beforeClusterDetection();

        masterSelector.select(isAppStartup).whenComplete((masterInfo, error) -> {
            if (error != null) {
                if (error instanceof LockServerNaException) {
                    rescheduleClusterDetection(future, isAppStartup);
                } else {
                    log.error("hit un-expected error during master selection", error);
                    if (isAppStartup) {
                        // We need to fail this if we hit un-expected error during app start up
                        future.completeExceptionally(error);
                    } else {
                        rescheduleClusterDetection(future, false);
                    }
                }
            } else {
                startJoinMaster(masterInfo, future, isAppStartup);
            }
        });
    }

    private void startClusterDetection(@Nullable CompletableFuture<Boolean> future, boolean isAppStartup) {
        if (detectingCluster.compareAndSet(false, true)) {
            log.info("start cluster detection for cluster [{}]", config.getClusterId());

            CompletableFuture<Boolean> wrapper = new CompletableFuture<>();
            wrapper.whenComplete((result, cause) -> {
                detectingCluster.set(false);

                if (future != null) {
                    if (cause != null) {
                        future.completeExceptionally(cause);
                    } else {
                        future.complete(result);
                    }
                }
            });

            doClusterDetection(wrapper, isAppStartup);
        } else {
            log.warn("cluster detection for cluster [{}] is already in-progress", config.getClusterId());
        }
    }

    private void beforeClusterDetection() {
        resetMaster();
        resetPendingJoin(null);
    }

    private PendingJoin resetPendingJoin(PendingJoin newPendingJoin) {
        return pendingJoinRef.getAndSet(newPendingJoin);
    }

    private InternalMasterInfo resetMaster() {
        InternalMasterInfo oldMaster = masterRef.getAndSet(null);
        if (oldMaster != null) {
            log.info("cancel heartbeat schedule for last master {}", getGlobalId(oldMaster.getMaster()));
            oldMaster.getHeartbeatSchedule().cancel(false);
            clientManager.closeEndpoint(oldMaster.getEndpoint());

            config.getNodeEventHandler().onLeaveMaster(new LeaveMasterEvent(this, oldMaster.getMaster()));
        }
        return oldMaster;
    }

    public CompletableFuture<Boolean> start() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        serverManager.start().addListener(listener -> {
            if (listener.isSuccess()) {
                config.getNodeEventHandler().onStartupEvent(new StartupEvent(this));
                startClusterDetection(future, true);
            } else {
                log.error("failed to listen to local address [{}]", address);
                future.completeExceptionally(listener.cause());
            }
        });
        return future;
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            serverManager.shutdown();
            clientManager.shutdown();

            masterSelector.stopMaster();
        }
    }

    private void handleBizMessage(Channel channel, IBizMessage message) {
        IBizMessage response = config.getNodeMsgHandler().handle(message);
        if (response != null) {
            channel.writeAndFlush(response).addListener(future -> {
                if (future.cause() != null) {
                    log.error("failed to send {} to target [{}]", Utils.getType(message), Utils.getPeerAddress(channel));
                }
            });
        }
    }

    private void handleMasterMessage(Channel channel, IMasterMessage message) {
        if (message instanceof JoinResponseMessage) {
            handleJoinResponse(channel, (JoinResponseMessage) message);
            return;
        }

        InternalMasterInfo currentMaster = masterRef.get();
        if (!validateMasterMessage(currentMaster, channel, message)) {
            // This is an invalid master message
            return;
        }
        ;

        // Always perform the following operation when we receive message from master
        currentMaster.refreshHeartbeatResponse();

        if (message instanceof HeartbeatResponseMessage) {
            handleHeartbeatResponse(currentMaster.getMaster(), (HeartbeatResponseMessage) message);
        } else if (message instanceof ClusterStateUpdateMessage) {
            handleClusterUpdateMessage((ClusterStateUpdateMessage) message);
        }
    }

    private void handleJoinResponse(Channel channel, JoinResponseMessage message) {
        /**
         * Before we're sure that it's the accepted join response we're expecting, we can't reset the pending join.
         * See {@link Node#scheduleJoinResponseWaiting(PendingJoin, CompletableFuture)} for more details.
         */
        final PendingJoin pendingJoin = pendingJoinRef.get();

        if (Objects.isNull(pendingJoin)) {
            log.error("[BUG] received un-expected join response from [{}]", Utils.getPeerAddress(channel));
        } else if (ObjectUtils.notEqual(pendingJoin.master.getMasterId(), message.getMasterId())) {
            log.error("[BUG] master ids [{}, {}] doesn't match between join request and response",
                    pendingJoin.master.getMasterId(), message.getMasterId());
        } else if (ObjectUtils.notEqual(pendingJoin.requestId, message.getRequestId())) {
            log.error("[BUG] request ids [{}, {}] doesn't match between join request and response",
                    pendingJoin.requestId, message.getRequestId());
        } else if (message.getCode() != JoinResponseMessage.CODE_SUCCESS) {
            log.error("failed to join master {} with error: {}", getGlobalId(pendingJoin.master), message.getError());
            if (message.getCode() == JoinResponseMessage.CODE_CLUSTER_UNMATCHED
                    // The following check order matters here. If the error happens in startup and
                    // we re-set the pending join before another round of join is kicked off, we can
                    // finish the join with error.
                    && pendingJoin.isAppStartup
                    && pendingJoinRef.compareAndSet(pendingJoin, null)) {
                abortJoinWithError(pendingJoin, message.getError());
            }
        } else if (pendingJoinRef.compareAndSet(pendingJoin, null)) {
            /**
             * Here we are sure that we have received expected join response before the join response
             * waiting task kicks off a new round of cluster detection.
             */
            log.info("received in-time join response from master {}", getGlobalId(pendingJoin.master));
            completePendingJoin(pendingJoin);
        }
    }

    private void abortJoinWithError(final PendingJoin currentJoin, String error) {
        currentJoin.joinWaiting.completeExceptionally(new RuntimeException(error));
        if (detectingCluster.get()) {
            log.error("[BUG] detectingCluster is not reset to false after completing joining waiting future");
        }
    }

    private void completePendingJoin(final PendingJoin currentJoin) {
        if (resetMaster() != null) {
            log.error("[BUG] old master MUST be cleared before master joining");
        }

        final ScheduledFuture heartbeatSchedule = scheduledService.scheduleAtFixedRate(() -> {
            InternalMasterInfo master = masterRef.get();
            if (master == null) {
                log.warn("[BUG] found no master in heartbeat schedule");
            } else {
                sendHeartbeat(master);
            }
        }, 100, config.getHeatbeatPeriod() * 1000, TimeUnit.MILLISECONDS);

        masterRef.set(new InternalMasterInfo(currentJoin.master, heartbeatSchedule));
        config.getNodeEventHandler().onJoinMaster(new JoinMasterEvent(this, currentJoin.master));

        log.info("successfully joined master {}", getGlobalId(currentJoin.master));

        currentJoin.joinWaiting.complete(Boolean.TRUE);
        if (detectingCluster.get()) {
            log.error("[BUG] detectingCluster is not reset to false after completing joining waiting future");
        }
    }

    private void sendHeartbeat(InternalMasterInfo master) {
        try {
            if (master.getHeartbeatRspLast() + config.getHeartbeatTimeoutNs() < System.nanoTime()) {
                log.warn("received no heart response from {} in last {}s",
                        getGlobalId(master.getMaster()), config.getHeartbeatTimeout());
                startClusterDetection(null, false);
            } else {
                IMessage message = new HeartbeatRequestMessage(config.getHeartbeatReqPayload());
                writeMasterMessage(master.getMaster(), message);
            }
        } catch (Exception e) {
            log.error("hit un-expected error", e);
        }
    }

    private void writeMasterMessage(MasterInfo master, IMessage message) {
        if (message != null) {
            clientManager.sendMessage(master.getEndpoint(), message).addListener(future -> {
                if (future.cause() != null) {
                    log.error("failed to send {} to master {}", Utils.getType(message), getGlobalId(master));
                }
            });
        }
    }

    private void handleHeartbeatResponse(MasterInfo master, HeartbeatResponseMessage message) {
        if (message.getPayload() != null) {
            IMessage response = config.getNodeMsgHandler().handle(message.getPayload());
            writeMasterMessage(master, response);
        }
    }

    private void handleClusterUpdateMessage(ClusterStateUpdateMessage message) {
        clusterStateRef.set(message.getCurrent());
        config.getNodeEventHandler().onClusterStateChange(new ClusterStateUpdateEvent(this, message.getCurrent()));
    }

    private boolean validateMasterMessage(InternalMasterInfo currentMaster, Channel channel, IMasterMessage message) {
        if (currentMaster == null) {
            log.error("don't expect message [{}] from [{}] as master is not set",
                    Utils.getType(message), Utils.getPeerAddress(channel));
            return false;
        }

        if (!Objects.equals(currentMaster.getId(), message.getMasterId())) {
            log.error("[{}] is not the expected master [{}]", message.getMasterId(), currentMaster.getId());
            return false;
        }

        return true;
    }

    private void startJoinMaster(final MasterInfo masterInfo, CompletableFuture<Boolean> joinWaiting, boolean isAppStartup) {
        final String requestId = UUID.randomUUID().toString();
        final JoinRequestMessage message = new JoinRequestMessage(requestId, config.getClusterId(), getSelfInfo());

        // prepare the pending join information before sending out the join message
        final PendingJoin newPendingJoin = new PendingJoin(requestId, masterInfo, joinWaiting, isAppStartup);
        if (resetPendingJoin(newPendingJoin) != null) {
            log.error("[BUG] last pending join is not cleaned during master joining");
        }

        clientManager.sendMessage(masterInfo.getEndpoint(), message).addListener(result -> {
            if (result.isSuccess()) {
                // We need to schedule a future task to ensure that we receive the expected join
                // response. If not, a new round of detection would be kicked off.
                scheduleJoinResponseWaiting();
            } else {
                // We need to re-schedule the cluster detection as previous master may have died.
                log.info("master {} not available, re-schedule cluster detection", getGlobalId(masterInfo), result.cause());
                rescheduleClusterDetection(joinWaiting, isAppStartup);
            }
        });
    }

    private void scheduleJoinResponseWaiting() {
        final PendingJoin currentJoin = pendingJoinRef.get();
        if (currentJoin == null) {
            log.info("pending join has already completed");
            return;
        }

        scheduledService.schedule(() -> {
            /**
             * The join response handling logic should only reset the pending join when it's sure that
             * the pending join progress can be finished and no more retry is needed. Otherwise, we would
             * hang without joining any master. <p/>
             *
             * Refer to {@link Node#handleJoinResponse(Channel, JoinResponseMessage)} about more details.
             */
            if (pendingJoinRef.compareAndSet(currentJoin, null)) {
                // Note that we need to retry the join for time-out case
                log.error("received no expected join response from master {}", getGlobalId(currentJoin.master));
                doClusterDetection(currentJoin.joinWaiting, currentJoin.isAppStartup);
            }
        }, MAX_JOIN_RESPONSE_DELAY, TimeUnit.SECONDS);
    }

    private String getGlobalId(MasterInfo masterInfo) {
        return masterInfo.getGlobalId(config.getClusterId());
    }

    @Override
    public ClusterState getCluster() {
        return clusterStateRef.get();
    }

    @Override
    public NodeInfo getSelfInfo() {
        return new NodeInfo(config.getHostName(), address);
    }

    @Override
    public CompletableFuture<Boolean> send(IBizMessage message) {
        InternalMasterInfo master = masterRef.get();
        if (master == null) {
            return Utils.doneWithError(new IllegalStateException("master not available"));
        }
        return Utils.conv(clientManager.sendMessage(master.getEndpoint(), message), Boolean.TRUE);
    }

    @Override
    public CompletableFuture<Boolean> send(NodeInfo peerNode, IBizMessage message) {
        return Utils.conv(clientManager.sendMessage(peerNode.getEndpoint(), message), Boolean.TRUE);
    }

    private static class PendingJoin {
        final String requestId;
        final MasterInfo master;
        final CompletableFuture<Boolean> joinWaiting;
        final boolean isAppStartup;
        final long startTimeNs;

        PendingJoin(String requestId, MasterInfo master, CompletableFuture<Boolean> joinWaiting, boolean isAppStartup) {
            this.requestId = requestId;
            this.master = master;
            this.joinWaiting = joinWaiting;
            this.isAppStartup = isAppStartup;
            this.startTimeNs = System.nanoTime();
        }
    }
}
