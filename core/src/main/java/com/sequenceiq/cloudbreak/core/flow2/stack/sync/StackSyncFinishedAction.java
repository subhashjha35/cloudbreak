package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.StackSyncService;

@Component("StackSyncFinishedAction")
public class StackSyncFinishedAction extends AbstractStackSyncAction<GetInstancesStateResult> {

    @Inject
    private StackSyncService stackSyncService;

    public StackSyncFinishedAction() {
        super(GetInstancesStateResult.class);
    }

    @Override
    protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
        Stack stack = context.getStack();
        // TODO !(actualContext instanceof StackScalingContext) requires for sync during upscale      here
        stackSyncService.updateInstances(stack, context.getInstanceMetaData(), payload.getStatuses(), true);
        sendEvent(context.getFlowId(), StackSyncEvent.SYNC_FINALIZED_EVENT.stringRepresentation(), null);
    }

    @Override
    protected Long getStackId(GetInstancesStateResult payload) {
        return payload.getCloudContext().getId();
    }
}
