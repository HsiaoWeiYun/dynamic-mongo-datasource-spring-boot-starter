package com.weiyunhsiao.module.dmdsbs.toolkit;

import com.weiyunhsiao.module.dmdsbs.doc.PlayerDocument;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Objects;

@Slf4j
public class MongoToolkit {

    private final MongoTemplate mongoTemplate;

    public MongoToolkit(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public boolean ping() {
        try {
            Document document = mongoTemplate.executeCommand(Document.parse("{ hello: 1 }"));
            return Objects.nonNull(document.getInteger("maxWireVersion"));
        } catch (Exception e) {
            log.error("", e);
        }
        return false;
    }

    public PlayerDocument save(PlayerDocument player) {
        return mongoTemplate.save(player);
    }

    public PlayerDocument getOneByName(String name) {
        return mongoTemplate.findOne(Query.query(Criteria.where("name").is(name)), PlayerDocument.class);
    }

    public List<PlayerDocument> getAllPlayers() {
        return mongoTemplate.findAll(PlayerDocument.class);
    }

    public void dropPlayerCollection() {
        dropCollection(PlayerDocument.class);
    }

    public void dropCollection(Class<?> clazz) {
        mongoTemplate.dropCollection(clazz);
    }

}
