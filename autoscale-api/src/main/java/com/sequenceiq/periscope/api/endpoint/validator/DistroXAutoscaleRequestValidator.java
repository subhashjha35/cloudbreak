package com.sequenceiq.periscope.api.endpoint.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.util.ValidatorUtil;

public class DistroXAutoscaleRequestValidator
        implements ConstraintValidator<ValidDistroXAutoscaleRequest, DistroXAutoscaleClusterRequest> {
    @Inject
    private ValidatorUtil validatorUtil;

    @Override
    public boolean isValid(DistroXAutoscaleClusterRequest request, ConstraintValidatorContext context) {
        Set<String> duplicateAutoscaleHostGroups = validatorUtil.getDuplicateAutoscaleHostGroups(request);

        if (duplicateAutoscaleHostGroups.size() > 0) {
            String message = String.format("Hostgroup(s) %s configured with multiple autoscaling policies.",
                    duplicateAutoscaleHostGroups.toString());
            com.sequenceiq.cloudbreak.validation.ValidatorUtil.addConstraintViolation(context, message, "hostGroup")
                    .disableDefaultConstraintViolation();
            return false;
        }

        List<AdjustmentType> invalidLoadAlertHostGroups =
        request.getLoadAlertRequests().stream()
                .map(loadAlertRequest -> loadAlertRequest.getScalingPolicy().getAdjustmentType())
                .filter(adjustmentType -> !AdjustmentType.LOAD_BASED.equals(adjustmentType))
                .collect(Collectors.toList());

        if (invalidLoadAlertHostGroups.size() > 0) {
            String message = String.format("LoadAlert Autoscale policy does not support AdjustmentType of type %s.",
                    invalidLoadAlertHostGroups.toString());
            com.sequenceiq.cloudbreak.validation.ValidatorUtil.addConstraintViolation(context, message, "adjustmentType")
                    .disableDefaultConstraintViolation();
            return false;
        }

        Set<String> distinctHostGroups = new HashSet<>();
        Set<String> duplicateHostGroups =
        request.getLoadAlertRequests().stream()
                .map(loadAlertRequest -> loadAlertRequest.getScalingPolicy().getHostGroup())
                .filter(n -> !distinctHostGroups.add(n))
                .collect(Collectors.toSet());

        if (duplicateHostGroups.size() > 0) {
            String message = String.format("Hostgroup(s) %s configured with multiple Loadbased autoscaling policies.",
                    duplicateHostGroups.toString());
            com.sequenceiq.cloudbreak.validation.ValidatorUtil.addConstraintViolation(context, message, "hostGroup")
                    .disableDefaultConstraintViolation();
            return false;
        }

        return true;
    }
}
