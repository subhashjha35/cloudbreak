package com.sequenceiq.periscope.api.util;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;

@Component
public class ValidatorUtil {

    public Set<String> getDuplicateAutoscaleHostGroups(DistroXAutoscaleClusterRequest request) {
        Set<String> loadAlertHostGroups = request.getLoadAlertRequests().stream()
                .map(loadAlert -> loadAlert.getScalingPolicy().getHostGroup())
                .collect(Collectors.toSet());

        Set<String> timeAlertHostGroups = request.getTimeAlertRequests().stream()
                .map(timeAlert -> timeAlert.getScalingPolicy().getHostGroup())
                .collect(Collectors.toSet());

        loadAlertHostGroups.retainAll(timeAlertHostGroups);
        return loadAlertHostGroups;
    }
}
