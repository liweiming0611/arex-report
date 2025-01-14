package com.arextest.web.core.repository.mongo;


import com.arextest.web.core.repository.ConfigRepositoryField;
import com.arextest.web.core.repository.ConfigRepositoryProvider;
import com.arextest.web.core.repository.mongo.util.MongoHelper;
import com.arextest.web.model.contract.contracts.config.replay.ComparisonReferenceConfiguration;
import com.arextest.web.model.dao.mongodb.ConfigComparisonReferenceCollection;
import com.arextest.web.model.mapper.ConfigComparisonReferenceMapper;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rchen9 on 2022/9/16.
 */
@Repository
public class ComparisonReferenceConfigurationRepositoryImpl implements
        ConfigRepositoryProvider<ComparisonReferenceConfiguration>,
        ConfigRepositoryField {

    private static final String APP_ID = "appId";
    private static final String OPERATION_ID = "operationId";
    private static final String PK_PATH = "pkPath";
    private static final String FK_PATH = "fkPath";
    private static final String EXPIRATION_TYPE = "expirationType";
    private static final String EXPIRATION_DATE = "expirationDate";
    private static final String FS_INTERFACE_ID = "fsInterfaceId";
    private static final String COMPARE_CONFIG_TYPE = "compareConfigType";


    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<ComparisonReferenceConfiguration> list() {
        throw new UnsupportedOperationException("this method is not implemented");
    }

    @Override
    public List<ComparisonReferenceConfiguration> listBy(String appId) {
        Query query = Query.query(Criteria.where(APP_ID).is(appId));
        List<ConfigComparisonReferenceCollection> configComparisonReferenceCollections = mongoTemplate.find(query, ConfigComparisonReferenceCollection.class);
        return configComparisonReferenceCollections.stream().map(ConfigComparisonReferenceMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    public List<ComparisonReferenceConfiguration> listBy(String appId, String operationId) {
        Query query = Query.query(Criteria.where(APP_ID).is(appId).and(OPERATION_ID).is(operationId));
        List<ConfigComparisonReferenceCollection> configComparisonReferenceCollections = mongoTemplate.find(query, ConfigComparisonReferenceCollection.class);
        return configComparisonReferenceCollections.stream().map(ConfigComparisonReferenceMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    @Override
    public List<ComparisonReferenceConfiguration> queryByInterfaceIdAndOperationId(String interfaceId,
                                                                                   String operationId) {
        Query query = new Query();
        if (StringUtils.isNotBlank(operationId)) {
            query.addCriteria(new Criteria().orOperator(Criteria.where(FS_INTERFACE_ID).is(interfaceId),
                    Criteria.where(OPERATION_ID).is(operationId)));
        } else {
            query.addCriteria(Criteria.where(FS_INTERFACE_ID).is(interfaceId));
        }
        List<ConfigComparisonReferenceCollection> configComparisonReferenceCollections =
                mongoTemplate.find(query, ConfigComparisonReferenceCollection.class);
        return configComparisonReferenceCollections.stream()
                .map(ConfigComparisonReferenceMapper.INSTANCE::dtoFromDao).collect(Collectors.toList());
    }

    @Override
    public boolean update(ComparisonReferenceConfiguration configuration) {
        Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        Update update = MongoHelper.getConfigUpdate();
        MongoHelper.appendSpecifiedProperties(update, configuration, PK_PATH, FK_PATH, EXPIRATION_TYPE, EXPIRATION_DATE);
        UpdateResult updateResult = mongoTemplate.updateMulti(query, update, ConfigComparisonReferenceCollection.class);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean remove(ComparisonReferenceConfiguration configuration) {
        Query query = Query.query(Criteria.where(DASH_ID).is(configuration.getId()));
        DeleteResult remove = mongoTemplate.remove(query, ConfigComparisonReferenceCollection.class);
        return remove.getDeletedCount() > 0;
    }

    @Override
    public boolean insert(ComparisonReferenceConfiguration configuration) {
        ConfigComparisonReferenceCollection configComparisonReferenceCollection =
                ConfigComparisonReferenceMapper.INSTANCE.daoFromDto(configuration);

        Update update = new Update();
        MongoHelper.appendFullProperties(update, configComparisonReferenceCollection);

        Query query = Query.query(
                Criteria.where(APP_ID).is(configComparisonReferenceCollection.getAppId())
                        .and(OPERATION_ID).is(configComparisonReferenceCollection.getOperationId())
                        .and(COMPARE_CONFIG_TYPE).is(configComparisonReferenceCollection.getCompareConfigType())
                        .and(FS_INTERFACE_ID).is(configComparisonReferenceCollection.getFsInterfaceId())
                        .and(PK_PATH).is(configComparisonReferenceCollection.getPkPath())
                        .and(FK_PATH).is(configComparisonReferenceCollection.getFkPath())
        );

        ConfigComparisonReferenceCollection dao = mongoTemplate.findAndModify(query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                ConfigComparisonReferenceCollection.class);
        return dao != null;
    }

    @Override
    public boolean insertList(List<ComparisonReferenceConfiguration> configurationList) {
        if (CollectionUtils.isEmpty(configurationList)) {
            return false;
        }
        List<ConfigComparisonReferenceCollection> configComparisonReferenceCollections = configurationList.stream()
                .map(ConfigComparisonReferenceMapper.INSTANCE::daoFromDto)
                .collect(Collectors.toList());
        Collection<ConfigComparisonReferenceCollection> insertAll = mongoTemplate.insertAll(configComparisonReferenceCollections);
        return CollectionUtils.isNotEmpty(insertAll);
    }

    @Override
    public boolean removeByAppId(String appId) {
        Query query = Query.query(Criteria.where(APP_ID).is(appId));
        DeleteResult remove = mongoTemplate.remove(query, ConfigComparisonReferenceCollection.class);
        return remove.getDeletedCount() > 0;
    }
}