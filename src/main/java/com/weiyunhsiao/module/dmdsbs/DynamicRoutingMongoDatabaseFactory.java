package com.weiyunhsiao.module.dmdsbs;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.weiyunhsiao.module.dmdsbs.clients.GroupClient;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;
import com.weiyunhsiao.module.dmdsbs.provider.DynamicMongoDataSourceProvider;
import com.weiyunhsiao.module.dmdsbs.resolver.MongoDataSourceAnnotationValueResolver;
import com.weiyunhsiao.module.dmdsbs.strategy.DynamicDataSourceStrategy;
import com.weiyunhsiao.module.dmdsbs.strategy.LoadBalanceDynamicDataSourceStrategy;
import com.weiyunhsiao.module.dmdsbs.toolkit.DynamicDataSourceContextHolder;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.mongodb.core.MongoDatabaseFactorySupport;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DynamicRoutingMongoDatabaseFactory extends MongoDatabaseFactorySupport<MongoClient> implements InitializingBean, DisposableBean {
    private static final String UNDERLINE = "_";

    @Getter
    private final Map<String, MongoClientWrapper<MongoClient>> wrappedClients = new ConcurrentHashMap<>();

    @Getter
    private final Map<String, GroupClient> groups = new ConcurrentHashMap<>();

    @Setter
    private String primary = "master";

    @Setter
    //分群路由策略
    private Class<? extends DynamicDataSourceStrategy> strategy = LoadBalanceDynamicDataSourceStrategy.class;

    private final List<DynamicMongoDataSourceProvider> dynamicMongoDataSourceProviders;

    private final MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver;

    public DynamicRoutingMongoDatabaseFactory(List<DynamicMongoDataSourceProvider> dynamicMongoDataSourceProviders) {
        this(dynamicMongoDataSourceProviders, null);
    }

    //TODO 這邊有一個空的MongoClient, 是否可以放master ?
    public DynamicRoutingMongoDatabaseFactory(List<DynamicMongoDataSourceProvider> dynamicMongoDataSourceProviders,
                                              MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver) {
        super(MongoClients.create(), "noNeed", true, new MongoExceptionTranslator());
        this.dynamicMongoDataSourceProviders = dynamicMongoDataSourceProviders;
        this.mongoDataSourceAnnotationValueResolver = mongoDataSourceAnnotationValueResolver;
    }

    @Override
    public ClientSession getSession(ClientSessionOptions options) {
        log.trace("--- trying start a session, options: {}", options);
        return determinePrimaryClient().getClient().startSession(options);
    }

    @Override
    public void afterPropertiesSet(){
        log.trace("--- trying setting data source");
        for (DynamicMongoDataSourceProvider dynamicMongoDataSourceProvider : dynamicMongoDataSourceProviders) {
            Map<String, MongoClientWrapper<MongoClient>> clients = dynamicMongoDataSourceProvider.loadDataSources();
            clients.forEach(this::binding);
        }
    }

    public synchronized void binding(String name, MongoClientWrapper<MongoClient> wrappedClient) {
        if(Objects.isNull(name) || name.trim().isEmpty()) {
            throw new IllegalArgumentException("datasource name is empty");
        }

        log.debug("trying bind new datasource: {}", name);
        //解綁已存在連線: 從wrappedClients中移除、 從群組中移除、 關閉連線
        unbinding(name);

        //將新連線加入集合中
        wrappedClients.put(name, wrappedClient);
        //入群
        joinGroup(name, wrappedClient);

        //印出mongodb資訊
        printClientInfo(wrappedClient);
        log.debug("binding finish");
    }

    public synchronized void unbinding(String name) {
        if(Objects.isNull(name) || name.trim().isEmpty()) {
            throw new IllegalArgumentException("datasource name is empty");
        }

        MongoClientWrapper<MongoClient> removedClient = wrappedClients.remove(name);
        if (Objects.nonNull(removedClient)) {
            log.debug("detect datasource that have the same name: {}, try to unbind the old one", name);
            leaveGroup(name);
            closeClient(removedClient);
            log.debug("unbinding finish");
        }
    }

    @Override
    protected MongoDatabase doGetMongoDatabase(String dbName) {
        log.trace("--- doGetMongoDatabase: {}", dbName);
        return determineClient().getClient().getDatabase(dbName);
    }

    @Override
    protected void closeClient() {
        log.trace("--- close client");
        wrappedClients.values().forEach(this::closeClient);
    }

    @Override
    protected String getDefaultDatabaseName() {
        log.trace("--- get default database name()");
        return determinePrimaryClient().getDatabaseName();
    }

    @Override
    protected MongoClient getMongoClient() {
        log.trace("--- get mongo client");
        MongoClientWrapper<MongoClient> wrapper = determineClient();
        if(Objects.nonNull(wrapper)) {
            log.debug("determine client success: {}", wrapper.getClientName());
        }
        return wrapper.getClient();
    }


    private void closeClient(MongoClientWrapper<MongoClient> wrappedClient) {
        if (Objects.nonNull(wrappedClient)) {
            log.debug("starting close {} client", wrappedClient.getClientName());
            wrappedClient.getClient().close();
            log.debug("close {} client successfully", wrappedClient.getClientName());
        }
    }

    private void leaveGroup(String name) {
        if (name.contains(UNDERLINE)) {
            String group = name.split(UNDERLINE)[0];
            log.trace("--- the client {} try to leave group {}", name, group);

            GroupClient groupClient = groups.get(group);
            groupClient.removeClient(name);
            if (groupClient.isEmpty()) {
                log.trace("--- group {} have no member", group);
                groups.remove(group);
            }
        }
    }

    @SneakyThrows
    private void joinGroup(String name, MongoClientWrapper<MongoClient> wrappedClient) {
        if (name.contains(UNDERLINE)) {
            log.debug("{} starting join group", name);
            String group = name.split(UNDERLINE)[0];
            GroupClient groupClient = groups.get(group);
            if (Objects.isNull(groupClient)) {
                groupClient = new GroupClient(group, strategy.getDeclaredConstructor().newInstance());
                groups.put(group, groupClient);
            }
            groupClient.addClient(name, wrappedClient);
            log.debug("join group finish, group size: {}", groupClient.size());
        }
    }

    private void printClientInfo(MongoClientWrapper<MongoClient> wrappedClient) {

        if(log.isDebugEnabled()){
            String dbName = wrappedClient.getDatabaseName();
            MongoClient mongoClient = wrappedClient.getClient();

            MongoDatabase mongoDatabase = wrappedClient.getClient().getDatabase(dbName);
            mongoDatabase.getReadConcern();
            mongoDatabase.getReadPreference();
            mongoDatabase.getWriteConcern();

            String serverVersion = getServerVersion(mongoDatabase);

            log.debug("********** MongoClient Info **********\n");
            log.debug("clientName: {}, database name: {}, server version: {}", wrappedClient.getClient(), dbName, serverVersion);
            log.debug("cluster state: {}", mongoClient.getClusterDescription());
            log.debug("read concern level: {}", mongoDatabase.getReadConcern().getLevel());
            log.debug("read preference: {}, isSecondaryOk: {}", mongoDatabase.getReadPreference().getName(), mongoDatabase.getReadPreference().isSecondaryOk());
            log.debug("write concern: {}", mongoDatabase.getWriteConcern());
            log.debug("********** MongoClient Info **********\n");
        }
    }

    private String getServerVersion(MongoDatabase mongoDatabase) {
        return mongoDatabase.runCommand(BsonDocument.parse("{ buildInfo: 1 }")).get("version", String.class);
    }

    private MongoClientWrapper<MongoClient> determineClient() {
        log.trace("--- determineClient");
        String ds = DynamicDataSourceContextHolder.peek();

        log.trace("--- ds found from DynamicDataSourceContextHolder: {}", ds);
        if (isActualMongoTransactionActive()) {
            log.debug("MongoDb Transaction Active, Use Primary Client");
            return determinePrimaryClient();
        }

        if (!groups.isEmpty() && groups.containsKey(ds)) {
            log.trace("--- ds found in groups");
            return groups.get(ds).determineClient();
        } else if (wrappedClients.containsKey(ds)) {
            log.trace("--- ds found in wrappedClients");
            return wrappedClients.get(ds);
        }

        return determinePrimaryClient();
    }

    /**
     * Determines the primary MongoDB client.
     *
     * @return The primary MongoDB client
     * @throws IllegalArgumentException if the primary MongoDB datasource is not found
     */
    private MongoClientWrapper<MongoClient> determinePrimaryClient() {
        log.trace("--- determine primary client");

        String primaryName = resolvePrimaryName();
        log.debug("resolve primary client name: {}", primaryName);

        //primary 通常不會分群, 所以先從wrappedClients開始找
        MongoClientWrapper<MongoClient> wrappedClient = wrappedClients.get(primaryName);
        if (Objects.isNull(wrappedClient)) {
            GroupClient groupClient = groups.get(primaryName);
            ;
            if (Objects.nonNull(groupClient)) {
                wrappedClient = groupClient.determineClient();
            }
        }

        if (Objects.isNull(wrappedClient)) {
            throw new IllegalArgumentException("Primary MongoDb Datasource not found, please check your configuration");
        }

        return wrappedClient;
    }

    private String resolvePrimaryName() {
        log.trace("--- resolvePrimaryName");
        if (Objects.nonNull(mongoDataSourceAnnotationValueResolver)) {
            return Optional.ofNullable(
                            mongoDataSourceAnnotationValueResolver.determineAnnotationValue(null, primary))
                    .orElse("");
        }

        return primary;
    }

    /**
     * Checks if there is an active MongoDB transaction.
     *
     * @return {@code true} if there is an active MongoDB transaction, {@code false} otherwise.
     */
    private boolean isActualMongoTransactionActive() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            Object transactionResource = TransactionSynchronizationManager.getResource(this);
            return Objects.nonNull(transactionResource);
        }
        return false;
    }
}
