package com.sequenceiq.it.cloudbreak.v4;

import java.io.IOException;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.Entity;
import com.sequenceiq.it.cloudbreak.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class KubernetesAction {
    private KubernetesAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        KubernetesTestDto kubernetesTestDto = (KubernetesTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        kubernetesTestDto.setResponse(
                client.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .post(workspaceId, kubernetesTestDto.getRequest()));
        Log.whenJson("Kubernetes config post request: ", kubernetesTestDto.getRequest());
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        KubernetesTestDto kubernetesTestDto = (KubernetesTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        kubernetesTestDto.setResponse(
                client.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .get(workspaceId, kubernetesTestDto.getRequest().getName()));
        Log.whenJson(" get Kubernetes config response: ", kubernetesTestDto.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        KubernetesTestDto kubernetesTestDto = (KubernetesTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        kubernetesTestDto.setResponses(
                (Set<KubernetesV4Response>) client.getCloudbreakClient()
                        .kubernetesV4Endpoint()
                        .list(workspaceId)
                        .getResponses());
        Log.whenJson(" get all Kubernetes config response: ", kubernetesTestDto.getResponse());
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        KubernetesTestDto kubernetesTestDto = (KubernetesTestDto) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        client.getCloudbreakClient()
                .kubernetesV4Endpoint()
                .delete(workspaceId, kubernetesTestDto.getName());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
        } catch (Exception e) {
            post(integrationTestContext, entity);
        }
    }
}
