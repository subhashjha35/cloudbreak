package com.sequenceiq.environment.parameters.v1.converter;

import static com.sequenceiq.environment.CloudPlatform.OPENSTACK;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.CloudPlatform;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;
import com.sequenceiq.environment.parameters.dao.domain.YarnParameters;
import com.sequenceiq.environment.parameters.dto.OpenstackParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto.Builder;

@Component
public class OpenstackEnvironmentParametersConverter extends BaseEnvironmentParametersConverter {

    @Override
    protected BaseParameters createInstance() {
        return new YarnParameters();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return OPENSTACK;
    }

    @Override
    protected void postConvert(BaseParameters baseParameters, Environment environment, ParametersDto parametersDto) {
        super.postConvert(baseParameters, environment, parametersDto);
    }

    @Override
    protected void postConvertToDto(Builder builder, BaseParameters source) {
        super.postConvertToDto(builder, source);
        builder.withOpenstackParameters(OpenstackParametersDto.builder().build());
    }
}