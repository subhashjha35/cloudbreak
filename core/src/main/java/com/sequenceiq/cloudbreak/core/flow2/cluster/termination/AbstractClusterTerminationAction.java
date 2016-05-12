package com.sequenceiq.cloudbreak.core.flow2.cluster.termination;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.cloud.event.ClusterPayload;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.AbstractAction;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

abstract class AbstractClusterTerminationAction<P extends ClusterPayload>
        extends AbstractAction<ClusterTerminationState, ClusterTerminationEvent, ClusterContext, P> {

    @Inject
    private ClusterService clusterService;

    protected AbstractClusterTerminationAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    @Override
    protected ClusterContext createFlowContext(String flowId, StateContext<ClusterTerminationState, ClusterTerminationEvent> stateContext, P payload) {
        Cluster cluster = clusterService.getById(payload.getClusterId());
        return new ClusterContext(flowId, cluster);
    }

    @Override
    protected Object getFailurePayload(P payload, Optional<ClusterContext> flowContext, Exception ex) {
        return null;
    }

    @Override
    protected Selectable createRequest(ClusterContext context) {
        return null;
    }
}
