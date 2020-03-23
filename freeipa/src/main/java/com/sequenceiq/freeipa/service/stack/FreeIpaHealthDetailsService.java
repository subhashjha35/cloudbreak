package com.sequenceiq.freeipa.service.stack;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.NodeHealthDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class FreeIpaHealthDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaHealthDetailsService.class);

    private static final String EXTERNAL_COMMAND_OUTPUT = "ExternalCommandOutput";

    private static final String STATUS_OK = "OK";

    private static final int STATUS_GROUP = 2;

    private static final int NODE_GROUP = 1;

    private static final String MESSAGE_UNAVAILABLE = "Message Unavailable";

    private static final Pattern RESULT_PATTERN = Pattern.compile("(ecure port|: TCP) \\([0-9]*\\): (.*)");

    private static final Pattern NEW_NODE_PATTERN = Pattern.compile("Check connection from master to remote replica '(.[^\']*)");

    private static final String WRONG_MASTER_MESSAGE = "invalid 'cn': must be";

    @Value("${freeipa.healthcheck.max-retries:3}")
    private int maxRetries;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    public HealthDetailsFreeIpaResponse getHealthDetails(String environmentCrn, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrn, accountId);
        List<InstanceMetaData> instances = stack.getAllInstanceMetaDataList();
        HealthDetailsFreeIpaResponse response = new HealthDetailsFreeIpaResponse();
        // The code here is weird because the server_conncheck API requires a specific node
        // and we don't know what determines what that node needs to be.  But if we use
        // the wrong master, we get an exception that says
        //   invalid 'cn': must be "ipaserver0.xx.wl.cloudera.site"
        // so we parse that, set that as master, and then retry the node.
        String masterCn = findMaster(stack).getDiscoveryFQDN();
        int retries = 0;
        for (int idx = 0; idx < instances.size(); idx++) {
            InstanceMetaData instance =  instances.get(idx);
            if (isInstanceAvailable(instance, response)) {
                try {
                    RPCResponse<Boolean> rpcResponse = checkFreeIpaHealth(stack, masterCn, instance);
                    parseMessages(rpcResponse, response);
                } catch (FreeIpaClientException e) {
                    String msg = e.getLocalizedMessage();
                    if (msg.contains(WRONG_MASTER_MESSAGE) && retries < maxRetries) {
                        masterCn = msg.substring(msg.indexOf('"') + 1);
                        masterCn = masterCn.substring(0, masterCn.indexOf('"'));
                        instances.add(instance);
                        retries++;
                    } else {
                        NodeHealthDetails nodeResponse = new NodeHealthDetails();
                        response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
                        nodeResponse.setName(instance.getDiscoveryFQDN());
                        nodeResponse.setStatus(InstanceStatus.UNREACHABLE);
                        nodeResponse.addIssue(e.getLocalizedMessage());
                        LOGGER.error(String.format("Unable to check the health of FreeIPA instance: %s", instance.getInstanceId()), e);
                    }
                }
            }
        }
        return updateResponse(stack, masterCn, response);
    }

    private boolean isInstanceAvailable(InstanceMetaData instance, HealthDetailsFreeIpaResponse response) {
        if (instance.isAvailable()) {
            return true;
        } else {
            NodeHealthDetails nodeResponse = new NodeHealthDetails();
            response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
            nodeResponse.setName(instance.getDiscoveryFQDN());
            nodeResponse.setStatus(instance.getInstanceStatus());
            nodeResponse.addIssue("Unable to check health as instance is " + instance.getInstanceStatus().name());
            return false;
        }
    }

    private HealthDetailsFreeIpaResponse updateResponse(Stack stack, String masterCn, HealthDetailsFreeIpaResponse response) {
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setCrn(stack.getResourceCrn());
        response.setName(masterCn);
        if (isOverallHealthy(response)) {
            response.setStatus(DetailedStackStatus.PROVISIONED.getStatus());
        } else {
            response.setStatus(DetailedStackStatus.UNHEALTHY.getStatus());
        }
        updateResponseWithInstanceIds(response, stack);
        return response;
    }

    private void updateResponseWithInstanceIds(HealthDetailsFreeIpaResponse response, Stack stack) {
        Map<String, String> nameIdMap = getNameIdMap(stack);
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            node.setInstanceId(nameIdMap.get(node.getName()));
        }
    }

    private Map<String, String> getNameIdMap(Stack stack) {
        return stack.getInstanceGroups().stream().flatMap(ig -> ig.getInstanceMetaData().stream())
                .collect(Collectors.toMap(InstanceMetaData::getDiscoveryFQDN, InstanceMetaData::getInstanceId));
    }

    private InstanceMetaData findMaster(Stack stack) {
        InstanceGroup masterGroup = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> InstanceGroupType.MASTER == instanceGroup.getInstanceGroupType()).findFirst().get();
        return masterGroup.getNotDeletedInstanceMetaDataSet().stream().findFirst().orElse(masterGroup.getInstanceMetaData().stream().findFirst().get());
    }

    private RPCResponse<Boolean> checkFreeIpaHealth(Stack stack, String masterCn, InstanceMetaData instance) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
        return freeIpaClient.serverConnCheck(masterCn, instance.getDiscoveryFQDN());
    }

    private boolean isOverallHealthy(HealthDetailsFreeIpaResponse response) {
        for (NodeHealthDetails node: response.getNodeHealthDetails()) {
            if (node.getStatus().equals(InstanceStatus.CREATED)) {
                return true;
            }
        }
        return false;
    }

    private void parseMessages(RPCResponse<Boolean> rpcResponse, HealthDetailsFreeIpaResponse response) {
        String precedingMessage = MESSAGE_UNAVAILABLE;
        NodeHealthDetails nodeResponse = null;
        for (RPCMessage message : rpcResponse.getMessages()) {
            Matcher nodeMatcher = NEW_NODE_PATTERN.matcher(message.getMessage());
            if (nodeMatcher.find()) {
                nodeResponse = new NodeHealthDetails();
                response.addNodeHealthDetailsFreeIpaResponses(nodeResponse);
                nodeResponse.setStatus(InstanceStatus.CREATED);
                nodeResponse.setName(nodeMatcher.group(NODE_GROUP));
            }
            if (nodeResponse == null) {
                LOGGER.info("No node for message: {}" + message.getMessage());
            } else {
                // When parsing the messages, if there's an error, the error
                // appears in the preceding message.
                if (EXTERNAL_COMMAND_OUTPUT.equals(message.getName())) {
                    Matcher matcher = RESULT_PATTERN.matcher(message.getMessage());
                    if (matcher.find()) {
                        if (!STATUS_OK.equals(matcher.group(STATUS_GROUP))) {
                            nodeResponse.setStatus(InstanceStatus.FAILED);
                            nodeResponse.addIssue(precedingMessage);
                        }
                    }
                    precedingMessage = message.getMessage();
                }
            }
        }
    }
}
