package com.sequenceiq.datalake.flow.create.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.exception.PollerException;
import com.dyngr.exception.PollerStoppedException;
import com.dyngr.exception.UserBreakException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.flow.create.event.EnvWaitRequest;
import com.sequenceiq.datalake.flow.create.event.EnvWaitSuccessEvent;
import com.sequenceiq.datalake.flow.create.event.SdxCreateFailedEvent;
import com.sequenceiq.datalake.service.sdx.EnvironmentService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class EnvWaitHandler extends ExceptionCatcherEventHandler<EnvWaitRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvWaitHandler.class);

    @Inject
    private EnvironmentService environmentService;

    @Override
    public String selector() {
        return "EnvWaitRequest";
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e) {
        return new SdxCreateFailedEvent(resourceId, e);
    }

    @Override
    protected void doAccept(HandlerEvent event) {
        EnvWaitRequest envWaitRequest = event.getData();
        Long sdxId = envWaitRequest.getResourceId();
        Selectable response;
        try {
            LOGGER.debug("start polling env for sdx: {}", sdxId);
            DetailedEnvironmentResponse detailedEnvironmentResponse = environmentService.waitAndGetEnvironment(sdxId);
            response = new EnvWaitSuccessEvent(sdxId, detailedEnvironmentResponse);
        } catch (UserBreakException userBreakException) {
            LOGGER.info("Env polling exited before timeout. Cause: ", userBreakException);
            response = new SdxCreateFailedEvent(sdxId, userBreakException);
        } catch (PollerStoppedException pollerStoppedException) {
            LOGGER.info("Env poller stopped for sdx: {}", sdxId, pollerStoppedException);
            response = new SdxCreateFailedEvent(sdxId, pollerStoppedException);
        } catch (PollerException exception) {
            LOGGER.info("Env polling failed for sdx: {}", sdxId, exception);
            response = new SdxCreateFailedEvent(sdxId, exception);
        } catch (Exception anotherException) {
            LOGGER.error("Something wrong happened in sdx creation wait phase", anotherException);
            response = new SdxCreateFailedEvent(sdxId, anotherException);
        }
        sendEvent(response, event);
    }
}
