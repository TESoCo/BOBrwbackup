package com.example.config;

import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.dao")
public class MongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:protobob}")
    private String databaseName;

    @Value("${spring.data.mongodb.gridfs.database:protobob}")
    private String gridFsDatabaseName;



    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory() {

        // Para MongoDB Atlas, la URI ya incluye la base de datos
        // Para Standalone, podemos necesitar ajustarla
        String connectionString = mongoUri;

        if (!mongoUri.endsWith("/" + databaseName) && !mongoUri.contains("/?")) {
            if (mongoUri.endsWith("/")) {
                connectionString = mongoUri + databaseName;
            } else {
                connectionString = mongoUri + "/" + databaseName;
            }
        }

        System.out.println("Using MongoDB connection: " + connectionString);
        System.out.println("Database: " + databaseName);
        System.out.println("GridFS Database: " + gridFsDatabaseName);

        return new SimpleMongoClientDatabaseFactory(connectionString);
    }



    @Bean
    public MongoTemplate mongoTemplate() {
        MongoTemplate template = new MongoTemplate(mongoDatabaseFactory());
        System.out.println("MongoTemplate initialized successfully");
        return template;
    }


    @Bean
    public MappingMongoConverter mappingMongoConverter() {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory());
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext());
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }

    @Bean
    public MongoMappingContext mongoMappingContext() {
        return new MongoMappingContext();
    }

    @Bean
    public GridFsTemplate gridFsTemplate() {
        GridFsTemplate gridFsTemplate = new GridFsTemplate(mongoDatabaseFactory(), mappingMongoConverter());
        System.out.println("GridFsTemplate initialized successfully");
        return gridFsTemplate;
    }
}