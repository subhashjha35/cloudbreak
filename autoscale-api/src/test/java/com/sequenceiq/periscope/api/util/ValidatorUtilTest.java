package com.sequenceiq.periscope.api.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;

public class ValidatorUtilTest {

    ValidatorUtil underTest = new ValidatorUtil();

    @Test
    public void testGetDuplicateAutoscaleHostGroupsWithDuplicates() {
        List<String> timeHostGroups = Arrays.asList("hdfs1", "compute1", "hdfs3");
        List<String> loadHostGroups = Arrays.asList("compute2", "hdfs1", "hdfs3");

        Set<String> duplicates = underTest
                .getDuplicateAutoscaleHostGroups(getTestRequest(timeHostGroups, loadHostGroups));
        assertTrue("Duplicate HostGroups size in request should match", duplicates.size() == 2);
        assertEquals("Duplicate HostGroups should match", "[hdfs1, hdfs3]", duplicates.toString());
    }

    @Test
    public void testGetDuplicateAutoscaleHostGroupsWithMultipleTimeRequests() {
        List<String> timeHostGroups = Arrays.asList("compute1", "compute1", "compute1");

        Set<String> duplicates = underTest
                .getDuplicateAutoscaleHostGroups(getTestRequest(timeHostGroups, null));
        assertTrue("No Duplicate HostGroups in request", duplicates.size() == 0);
    }

    @Test
    public void testGetDuplicateAutoscaleHostGroupsWithoutDuplicates() {
        List<String> timeHostGroups = Arrays.asList("compute1", "hdfs1", "hdfs3");
        List<String> loadHostGroups = Arrays.asList("test2", "test3", "test5");
        Set<String> duplicates = underTest
                .getDuplicateAutoscaleHostGroups(getTestRequest(timeHostGroups, loadHostGroups));
        assertTrue("No Duplicate HostGroups in request", duplicates.size() == 0);
    }

    @Test
    public void testGetDuplicateAutoscaleHostGroupsWithoutAutoscaleConfigs() {
        Set<String> duplicates = underTest
                .getDuplicateAutoscaleHostGroups(getTestRequest(null, null));
        assertTrue("No Duplicate HostGroups in request", duplicates.size() == 0);
    }

    @Test
    public void testGetDuplicateAutoscaleHostGroupsWithOnlyLoadRequest() {
        List<String> loadHostGroups = Arrays.asList("test2", "test3, test4", "test5");
        Set<String> duplicates = underTest
                .getDuplicateAutoscaleHostGroups(getTestRequest(null, loadHostGroups));
        assertTrue("No Duplicate HostGroups in request", duplicates.size() == 0);
    }

    private DistroXAutoscaleClusterRequest getTestRequest(List<String> timeHostGroups,
            List<String> loadHostGroups) {
        DistroXAutoscaleClusterRequest testRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = new ArrayList<>();
        List<LoadAlertRequest> loadAlertRequests = new ArrayList<>();

        if (timeHostGroups != null) {
            for (String hostGroup : timeHostGroups) {
                TimeAlertRequest request = new TimeAlertRequest();
                ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
                scalingPolicyRequest.setHostGroup(hostGroup);
                request.setScalingPolicy(scalingPolicyRequest);
                timeAlertRequests.add(request);
            }
            testRequest.setTimeAlertRequests(timeAlertRequests);
        }

        if (loadHostGroups != null) {
            for (String hostGroup : loadHostGroups) {
                LoadAlertRequest request = new LoadAlertRequest();
                ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
                scalingPolicyRequest.setHostGroup(hostGroup);
                request.setScalingPolicy(scalingPolicyRequest);
                loadAlertRequests.add(request);
            }
            testRequest.setLoadAlertRequests(loadAlertRequests);
        }

        return testRequest;
    }
}
