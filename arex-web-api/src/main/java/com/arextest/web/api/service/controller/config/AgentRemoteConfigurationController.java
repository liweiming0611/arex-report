package com.arextest.web.api.service.controller.config;

import com.arextest.common.model.response.Response;
import com.arextest.common.utils.ResponseUtils;
import com.arextest.web.common.LogUtils;
import com.arextest.web.core.business.config.ConfigurableHandler;
import com.arextest.web.core.business.config.application.ApplicationInstancesConfigurableHandler;
import com.arextest.web.core.business.config.application.ApplicationServiceConfigurableHandler;
import com.arextest.web.model.contract.contracts.common.enums.StatusType;
import com.arextest.web.model.contract.contracts.config.application.ApplicationConfiguration;
import com.arextest.web.model.contract.contracts.config.application.InstancesConfiguration;
import com.arextest.web.model.contract.contracts.config.instance.AgentRemoteConfigurationRequest;
import com.arextest.web.model.contract.contracts.config.instance.AgentRemoteConfigurationResponse;
import com.arextest.web.model.contract.contracts.config.record.DynamicClassConfiguration;
import com.arextest.web.model.contract.contracts.config.record.ServiceCollectConfiguration;
import com.arextest.web.model.mapper.InstancesMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * @author jmo
 * @since 2022/1/22
 */
@Slf4j
@Controller
@RequestMapping("/api/config/agent")
public final class AgentRemoteConfigurationController {

    private static final String NOT_RECORDING = "(not recording)";
    @Resource
    private ConfigurableHandler<DynamicClassConfiguration> dynamicClassHandler;
    @Resource
    private ConfigurableHandler<ApplicationConfiguration> applicationHandler;
    @Resource
    private ConfigurableHandler<ServiceCollectConfiguration> serviceCollectHandler;
    @Resource
    private ApplicationInstancesConfigurableHandler applicationInstancesConfigurableHandler;
    @Resource
    private ApplicationServiceConfigurableHandler applicationServiceHandler;
    private ScheduledExecutorService executorService;
    @Value("${arex.config.application.service.update.delaySeconds:30}")
    private long delayUpdateServiceSeconds;

    @PostConstruct
    private void init() {
        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @PostMapping("/load")
    @ResponseBody
    public Response load(@RequestBody AgentRemoteConfigurationRequest request) {
        final String appId = request.getAppId();
        if (StringUtils.isEmpty(appId)) {
            return InvalidResponse.REQUESTED_APP_ID_IS_EMPTY;
        }
        LogUtils.info(LOGGER, "from appId: {} , load config", appId);
        ApplicationConfiguration applicationConfiguration = this.loadApplicationResult(request);
        if (applicationConfiguration == null) {
            LogUtils.info(LOGGER, "from appId: {} load config resource not found", appId);
            return ResponseUtils.resourceNotFoundResponse();
        }
        ServiceCollectConfiguration serviceCollectConfiguration = serviceCollectHandler.useResult(appId);
        applicationServiceHandler.createOrUpdate(request.getAppId());
        AgentRemoteConfigurationResponse body = new AgentRemoteConfigurationResponse();
        body.setDynamicClassConfigurationList(dynamicClassHandler.useResultAsList(appId));
        body.setServiceCollectConfiguration(serviceCollectConfiguration);
        body.setStatus(applicationConfiguration.getStatus());
        InstancesConfiguration instancesConfiguration = InstancesMapper.INSTANCE.dtoFromContract(request);
        applicationInstancesConfigurableHandler.createOrUpdate(instancesConfiguration);
        List<InstancesConfiguration> instances = applicationInstancesConfigurableHandler.useResultAsList(appId,
                serviceCollectConfiguration.getRecordMachineCountLimit());
        Set<String> recordingHosts =
                instances.stream().map(InstancesConfiguration::getHost).collect(Collectors.toSet());
        if (recordingHosts.contains(request.getHost())) {
            body.setTargetAddress(request.getHost());
        } else {
            body.setTargetAddress(request.getHost() + NOT_RECORDING);
        }
        return ResponseUtils.successResponse(body);
    }

    private void delayUpdateApplicationService(AgentRemoteConfigurationRequest request) {
        executorService.schedule(new UpdateApplicationRunnable(request), delayUpdateServiceSeconds, TimeUnit.SECONDS);
    }

    private final class UpdateApplicationRunnable implements Runnable {
        private final AgentRemoteConfigurationRequest request;

        private UpdateApplicationRunnable(AgentRemoteConfigurationRequest request) {
            this.request = request;
        }

        @Override
        public void run() {
            try {
                applicationServiceHandler.createOrUpdate(request.getAppId());
            } catch (Throwable e) {
                LogUtils.error(LOGGER, "update application service error:{}", e.getMessage(), e);
            }
        }
    }

    private ApplicationConfiguration loadApplicationResult(AgentRemoteConfigurationRequest request) {
        ApplicationConfiguration applicationConfiguration = applicationHandler.useResult(request.getAppId());
        return applicationConfiguration;
    }
}
