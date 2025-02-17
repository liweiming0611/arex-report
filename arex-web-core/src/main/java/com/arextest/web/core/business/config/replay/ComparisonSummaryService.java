package com.arextest.web.core.business.config.replay;

import com.arextest.web.core.business.config.ConfigurableHandler;
import com.arextest.web.model.contract.contracts.config.application.ApplicationOperationConfiguration;
import com.arextest.web.model.contract.contracts.config.application.ApplicationServiceConfiguration;
import com.arextest.web.model.contract.contracts.config.replay.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by rchen9 on 2023/2/7.
 */
@Component
public class ComparisonSummaryService {

    @Resource
    ComparisonExclusionsConfigurableHandler exclusionsConfigurableHandler;
    @Resource
    ComparisonInclusionsConfigurableHandler inclusionsConfigurableHandler;
    @Resource
    ComparisonReferenceConfigurableHandler referenceConfigurableHandler;
    @Resource
    ComparisonListSortConfigurableHandler listSortConfigurableHandler;
    @Resource
    ConfigurableHandler<ApplicationServiceConfiguration> applicationServiceConfigurationConfigurableHandler;

    private static final int COMPARE_CONFIG_TYPE = 0;

    public ComparisonSummaryConfiguration getComparisonDetailsSummary(String interfaceId) {
        ComparisonSummaryConfiguration comparisonSummaryConfiguration =
                new ComparisonSummaryConfiguration();
        getComparisonExclusionsConfiguration(interfaceId, comparisonSummaryConfiguration);
        getComparisonInclusionsConfiguration(interfaceId, comparisonSummaryConfiguration);
        getComparisonListSortConfiguration(interfaceId, comparisonSummaryConfiguration);
        getComparisonReferenceConfiguration(interfaceId, comparisonSummaryConfiguration);
        return comparisonSummaryConfiguration;
    }

    public ReplayCompareConfig getReplayComparisonConfig(String appId) {
        ReplayCompareConfig replayCompareConfig = new ReplayCompareConfig();
        replayCompareConfig.setReplayComparisonItems(getReplayComparisonItems(appId));
        return replayCompareConfig;
    }

    private void getComparisonExclusionsConfiguration(String interfaceId,
                                                      ComparisonSummaryConfiguration comparisonSummaryConfiguration) {
        Set<List<String>> exclusionSet = new HashSet<>();
        List<ComparisonExclusionsConfiguration> comparisonExclusionsConfigurationList =
                exclusionsConfigurableHandler.queryByInterfaceId(interfaceId);
        Optional.ofNullable(comparisonExclusionsConfigurationList).orElse(Collections.emptyList()).forEach(item -> {
            exclusionSet.add(item.getExclusions());
        });
        comparisonSummaryConfiguration.setExclusionList(exclusionSet);
    }

    private void getComparisonInclusionsConfiguration(String interfaceId,
                                                      ComparisonSummaryConfiguration comparisonSummaryConfiguration) {
        Set<List<String>> inclusionSet = new HashSet<>();
        List<ComparisonInclusionsConfiguration> comparisonInclusionsConfigurationList =
                inclusionsConfigurableHandler.queryByInterfaceId(interfaceId);
        Optional.ofNullable(comparisonInclusionsConfigurationList).orElse(Collections.emptyList()).forEach(item -> {
            inclusionSet.add(item.getInclusions());
        });
        comparisonSummaryConfiguration.setInclusionList(inclusionSet);
    }

    private void getComparisonListSortConfiguration(String interfaceId,
                                                    ComparisonSummaryConfiguration comparisonSummaryConfiguration) {
        Map<List<String>, List<List<String>>> listSortMap = new HashMap<>();
        List<ComparisonListSortConfiguration> comparisonListSortConfigurationList =
                listSortConfigurableHandler.queryByInterfaceId(interfaceId);
        Optional.ofNullable(comparisonListSortConfigurationList).orElse(Collections.emptyList()).forEach(item -> {
            if (CollectionUtils.isNotEmpty(item.getListPath())) {
                listSortMap.put(item.getListPath(), item.getKeys());
            }
        });
        comparisonSummaryConfiguration.setListSortMap(listSortMap);
    }

    private void getComparisonReferenceConfiguration(String interfaceId,
                                                     ComparisonSummaryConfiguration comparisonSummaryConfiguration) {
        Map<List<String>, List<String>> referenceMap = new HashMap<>();
        List<ComparisonReferenceConfiguration> comparisonReferenceConfigurationList =
                referenceConfigurableHandler.queryByInterfaceId(interfaceId);
        Optional.ofNullable(comparisonReferenceConfigurationList).orElse(Collections.emptyList()).forEach(item -> {
            if (CollectionUtils.isNotEmpty(item.getFkPath())) {
                referenceMap.put(item.getFkPath(), item.getPkPath());
            }
        });
        comparisonSummaryConfiguration.setReferenceMap(referenceMap);
    }

    private List<ReplayCompareConfig.ReplayComparisonItem> getReplayComparisonItems(String appId) {
        Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap = new HashMap<>();
        buildExclusions(replayConfigurationMap, exclusionsConfigurableHandler.useResultAsList(appId, COMPARE_CONFIG_TYPE));
        buildInclusions(replayConfigurationMap, inclusionsConfigurableHandler.useResultAsList(appId, COMPARE_CONFIG_TYPE));
        buildListSort(replayConfigurationMap, listSortConfigurableHandler.useResultAsList(appId, COMPARE_CONFIG_TYPE));
        buildReference(replayConfigurationMap, referenceConfigurableHandler.useResultAsList(appId, COMPARE_CONFIG_TYPE));
        mergeGlobalComparisonConfig(replayConfigurationMap, appId);
        return new ArrayList<>(replayConfigurationMap.values());
    }

    private void mergeGlobalComparisonConfig(Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap,
                                             String appId) {
        if (replayConfigurationMap.containsKey(null)) {

            ReplayCompareConfig.ReplayComparisonItem globalConfig = replayConfigurationMap.get(null);
            Set<List<String>> globalExclusionList = globalConfig.getExclusionList();
            Set<List<String>> globalInclusionList = globalConfig.getInclusionList();

            List<String> operationIdList = getOperationIdList(appId);
            for (String operationId : operationIdList) {
                ReplayCompareConfig.ReplayComparisonItem tempReplayConfig = replayConfigurationMap.getOrDefault(
                        operationId, new ReplayCompareConfig.ReplayComparisonItem());
                tempReplayConfig.setOperationId(operationId);

                if (globalExclusionList != null) {
                    Set<List<String>> exclusionList = tempReplayConfig.getExclusionList() == null ?
                            new HashSet<>() : tempReplayConfig.getExclusionList();
                    exclusionList.addAll(globalExclusionList);
                    tempReplayConfig.setExclusionList(exclusionList);
                }

                if (globalInclusionList != null) {
                    Set<List<String>> inclusionList = tempReplayConfig.getInclusionList() == null ?
                            new HashSet<>() : tempReplayConfig.getInclusionList();
                    inclusionList.addAll(globalInclusionList);
                    tempReplayConfig.setInclusionList(inclusionList);
                }
                replayConfigurationMap.put(operationId, tempReplayConfig);
            }
            replayConfigurationMap.remove(null);
        }
    }

    private List<String> getOperationIdList(String appId) {
        List<String> operationIdList = new ArrayList<>();

        List<ApplicationServiceConfiguration> applicationServiceConfigurations =
                applicationServiceConfigurationConfigurableHandler.useResultAsList(appId);

        Optional.ofNullable(applicationServiceConfigurations)
                .orElse(Collections.emptyList())
                .forEach(item -> {
                    List<ApplicationOperationConfiguration> operationList = item.getOperationList();
                    if (CollectionUtils.isNotEmpty(operationList)) {
                        for (ApplicationOperationConfiguration applicationOperationConfiguration : operationList) {
                            operationIdList.add(applicationOperationConfiguration.getId());
                        }
                    }
                });
        return operationIdList;
    }

    private void buildExclusions(Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap,
                                 List<ComparisonExclusionsConfiguration> comparisonExclusionsConfigurations) {
        if (CollectionUtils.isNotEmpty(comparisonExclusionsConfigurations)) {
            for (ComparisonExclusionsConfiguration compareExclusionsConfig : comparisonExclusionsConfigurations) {

                String operationId = compareExclusionsConfig.getOperationId();
                List<String> exclusions = compareExclusionsConfig.getExclusions();

                if (CollectionUtils.isNotEmpty(exclusions)) {
                    ReplayCompareConfig.ReplayComparisonItem tempReplayComparisonItem =
                            replayConfigurationMap.getOrDefault(operationId, new ReplayCompareConfig.ReplayComparisonItem());
                    tempReplayComparisonItem.setOperationId(operationId);

                    Set<List<String>> tempExclusionList = tempReplayComparisonItem.getExclusionList() ==
                            null ? new HashSet<>() : tempReplayComparisonItem.getExclusionList();
                    tempExclusionList.add(exclusions);
                    tempReplayComparisonItem.setExclusionList(tempExclusionList);
                    replayConfigurationMap.put(operationId, tempReplayComparisonItem);
                }
            }
        }
    }

    private void buildInclusions(Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap,
                                 List<ComparisonInclusionsConfiguration> comparisonInclusionsConfigurations) {
        if (CollectionUtils.isNotEmpty(comparisonInclusionsConfigurations)) {
            for (ComparisonInclusionsConfiguration comparisonInclusionsConfiguration : comparisonInclusionsConfigurations) {

                String operationId = comparisonInclusionsConfiguration.getOperationId();
                List<String> inclusions = comparisonInclusionsConfiguration.getInclusions();

                if (CollectionUtils.isNotEmpty(inclusions)) {
                    ReplayCompareConfig.ReplayComparisonItem tempReplayComparisonItem =
                            replayConfigurationMap.getOrDefault(operationId, new ReplayCompareConfig.ReplayComparisonItem());
                    tempReplayComparisonItem.setOperationId(operationId);

                    Set<List<String>> tempInclusionList = tempReplayComparisonItem.getInclusionList() ==
                            null ? new HashSet<>() : tempReplayComparisonItem.getInclusionList();
                    tempInclusionList.add(inclusions);
                    tempReplayComparisonItem.setInclusionList(tempInclusionList);
                    replayConfigurationMap.put(operationId, tempReplayComparisonItem);
                }
            }
        }
    }

    private void buildListSort(Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap,
                               List<ComparisonListSortConfiguration> comparisonListSortConfigurations) {
        if (CollectionUtils.isNotEmpty(comparisonListSortConfigurations)) {
            for (ComparisonListSortConfiguration compareListSortConfig : comparisonListSortConfigurations) {

                String operationId = compareListSortConfig.getOperationId();
                List<String> listPath = compareListSortConfig.getListPath();
                List<List<String>> keys = compareListSortConfig.getKeys();

                if (CollectionUtils.isNotEmpty(listPath) && CollectionUtils.isNotEmpty(keys)) {
                    ReplayCompareConfig.ReplayComparisonItem tempReplayComparisonItem =
                            replayConfigurationMap.getOrDefault(operationId, new ReplayCompareConfig.ReplayComparisonItem());
                    tempReplayComparisonItem.setOperationId(operationId);

                    Map<List<String>, List<List<String>>> tempListSortMap = tempReplayComparisonItem.getListSortMap() ==
                            null ? new HashMap<>() : tempReplayComparisonItem.getListSortMap();
                    tempListSortMap.put(listPath, keys);
                    tempReplayComparisonItem.setListSortMap(tempListSortMap);
                    replayConfigurationMap.put(operationId, tempReplayComparisonItem);
                }
            }
        }
    }

    private void buildReference(Map<String, ReplayCompareConfig.ReplayComparisonItem> replayConfigurationMap,
                                List<ComparisonReferenceConfiguration> comparisonReferenceConfigurations) {
        if (CollectionUtils.isNotEmpty(comparisonReferenceConfigurations)) {
            for (ComparisonReferenceConfiguration comparisonReferenceConfiguration : comparisonReferenceConfigurations) {

                String operationId = comparisonReferenceConfiguration.getOperationId();
                List<String> fkPath = comparisonReferenceConfiguration.getFkPath();
                List<String> pkPath = comparisonReferenceConfiguration.getPkPath();

                if (CollectionUtils.isNotEmpty(fkPath) && CollectionUtils.isNotEmpty(pkPath)) {
                    ReplayCompareConfig.ReplayComparisonItem tempReplayComparisonItem =
                            replayConfigurationMap.getOrDefault(operationId, new ReplayCompareConfig.ReplayComparisonItem());
                    tempReplayComparisonItem.setOperationId(operationId);

                    Map<List<String>, List<String>> tempReferenceMap = tempReplayComparisonItem.getReferenceMap() ==
                            null ? new HashMap<>() : tempReplayComparisonItem.getReferenceMap();
                    tempReferenceMap.put(fkPath, pkPath);
                    tempReplayComparisonItem.setReferenceMap(tempReferenceMap);
                    replayConfigurationMap.put(operationId, tempReplayComparisonItem);
                }
            }
        }
    }

}
