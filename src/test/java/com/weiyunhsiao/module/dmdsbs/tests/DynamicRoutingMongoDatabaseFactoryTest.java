package com.weiyunhsiao.module.dmdsbs.tests;

import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.DynamicRoutingMongoDatabaseFactory;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.DynamicRoutingMongoDatasourceAutoConfigure;
import com.weiyunhsiao.module.dmdsbs.clients.GroupClient;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;
import com.weiyunhsiao.module.dmdsbs.configuration.CustomizeMongoTemplateConfiguration;
import com.weiyunhsiao.module.dmdsbs.doc.PlayerDocument;
import com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension;
import com.weiyunhsiao.module.dmdsbs.toolkit.DynamicDataSourceContextHolder;
import com.weiyunhsiao.module.dmdsbs.toolkit.MongoToolkit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS1;
import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS2;
import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS2_1;
import static com.weiyunhsiao.module.dmdsbs.extensions.StandaloneMongoDbExtension.DS2_2;

@Disabled
@DisplayName("MongoDatabaseFactory測試")
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {"logging.level.com.weiyunhsiao.module.dmdsbs=DEBUG", "logging.level.com.mongodb=DEBUG"})
@ContextConfiguration(classes = {DynamicRoutingMongoDatasourceAutoConfigure.class, CustomizeMongoTemplateConfiguration.class})
@ExtendWith(StandaloneMongoDbExtension.class)
public class DynamicRoutingMongoDatabaseFactoryTest {

    @Autowired
    private DynamicRoutingMongoDatabaseFactory dynamicRoutingMongoDatabaseFactory;

    @Autowired
    private MongoToolkit mongoToolkit;

    @Test
    @Order(0)
    @DisplayName("ping")
    public void ping() {
        Assertions.assertTrue(mongoToolkit.ping());
    }

    @Test
    @Order(1)
    @DisplayName("沒有指定ds時應使用primary")
    public void Should_ChoosePrimary_When_NoSpecifyDS() {
        final String name = "primary";
        PlayerDocument playerWhoInPrimary = new PlayerDocument(name);
        mongoToolkit.save(playerWhoInPrimary);

        DynamicDataSourceContextHolder.push(DS2_1);
        Assertions.assertTrue(Objects.isNull(mongoToolkit.getOneByName(name)));

        DynamicDataSourceContextHolder.push(DS2_2);
        Assertions.assertTrue(Objects.isNull(mongoToolkit.getOneByName(name)));

        DynamicDataSourceContextHolder.push(DS1);
        Assertions.assertTrue(Objects.nonNull(mongoToolkit.getOneByName(name)));
        Assertions.assertEquals(name, mongoToolkit.getOneByName(name).getName());

        clean();
    }

    @Test
    @Order(2)
    @DisplayName("指定ds時應該要正確切換")
    public void When_SpecifyDS_Then_SwitchDS() {

        PlayerDocument playerWhoInDS1 = new PlayerDocument(DS1);
        PlayerDocument playerWhoInDS2 = new PlayerDocument(DS2_1);

        mongoToolkit.save(playerWhoInDS1);

        DynamicDataSourceContextHolder.push(DS2_1);
        mongoToolkit.save(playerWhoInDS2);
        DynamicDataSourceContextHolder.clear();

        //先進後出, 順序是: ds1 ds2_1 ds1
        DynamicDataSourceContextHolder.push(DS1);
        DynamicDataSourceContextHolder.push(DS2_1);
        DynamicDataSourceContextHolder.push(DS1);

        //檢查是否切換至ds1
        Assertions.assertEquals(DS1, mongoToolkit.getOneByName(DS1).getName());
        DynamicDataSourceContextHolder.poll();

        //檢查是否切換至ds2_1
        Assertions.assertEquals(DS2_1, mongoToolkit.getOneByName(DS2_1).getName());
        DynamicDataSourceContextHolder.poll();

        //檢查是否切換至ds1
        Assertions.assertEquals(DS1, mongoToolkit.getOneByName(DS1).getName());
        DynamicDataSourceContextHolder.poll();

        DynamicDataSourceContextHolder.push(DS2_1);

        clean();
    }

    @Test
    @Order(3)
    @DisplayName("指定DS Group時應該要附載均衡")
    public void Should_LoadBalance_When_SpecifyDSGroup() {
        PlayerDocument playerWhoInDS2_1 = new PlayerDocument(DS2_1);
        PlayerDocument playerWhoInDS2_2 = new PlayerDocument(DS2_2);

        //先向ds2_1儲存資料
        DynamicDataSourceContextHolder.push(DS2_1);
        mongoToolkit.save(playerWhoInDS2_1);

        //再向ds2_2儲存資料
        DynamicDataSourceContextHolder.push(DS2_2);
        mongoToolkit.save(playerWhoInDS2_2);
        DynamicDataSourceContextHolder.clear();

        //準備答案, 不論先後但期望依序訪問
        List<String> answers = new ArrayList<>(
                List.of(DS2_1, DS2_2)
        );

        //指定訪問group
        DynamicDataSourceContextHolder.push(DS2);
        List<PlayerDocument> result1 = mongoToolkit.getAllPlayers();
        List<PlayerDocument> result2 = mongoToolkit.getAllPlayers();

        Assertions.assertEquals(1, result1.size());
        Assertions.assertEquals(1, result2.size());

        Assertions.assertTrue(answers.remove(result1.getFirst().getName()));
        Assertions.assertTrue(answers.remove(result2.getFirst().getName()));

        Assertions.assertTrue(answers.isEmpty());

        clean();
    }



    @Test
    @Order(Integer.MAX_VALUE - 1)
    @DisplayName("MongoDatabaseFactory 解綁測試")
    public void When_UnbindingClient_Then_RemoveFromDatasourcesAndGroups() {
        dynamicRoutingMongoDatabaseFactory.unbinding(DS2_1);

        Map<String, MongoClientWrapper<MongoClient>> datasources = dynamicRoutingMongoDatabaseFactory.getWrappedClients();
        Map<String, GroupClient> groups = dynamicRoutingMongoDatabaseFactory.getGroups();

        Assertions.assertEquals(2, datasources.size());
        Assertions.assertEquals(1, groups.size());
        Assertions.assertEquals(1, groups.get(DS2).size());

        Assertions.assertNull(datasources.get(DS2_1));
        Assertions.assertNull(groups.get(DS2).getClientWrapperMap().get(DS2_1));

        dynamicRoutingMongoDatabaseFactory.unbinding(DS2_2);

        Assertions.assertEquals(1, datasources.size());
        Assertions.assertTrue(groups.isEmpty());
    }

    @Test
    @Order(Integer.MAX_VALUE)
    @DisplayName("MongoDatabaseFactory 連線關閉測試")
    public void When_UnbindingClient_Then_CloseConnection() {
        Map<String, MongoClientWrapper<MongoClient>> a = dynamicRoutingMongoDatabaseFactory.getWrappedClients();
        MongoClient mongoClient = dynamicRoutingMongoDatabaseFactory.getWrappedClients().get(DS1).getClient();
        dynamicRoutingMongoDatabaseFactory.unbinding(DS1);

        Assertions.assertThrowsExactly(IllegalStateException.class, () -> mongoClient.listDatabases().first());
    }


    @SneakyThrows
    private void clean() {
        mongoToolkit.dropPlayerCollection();
        DynamicDataSourceContextHolder.clear();
        Thread.sleep(Duration.ofMillis(50));
    }

}
