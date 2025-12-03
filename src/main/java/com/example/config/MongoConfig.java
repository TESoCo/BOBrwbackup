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


        System.out.println("Using MongoDB connection: " + mongoUri.replaceFirst("//.*@", "//***:***@"));


        try {
            MongoDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoUri);
            System.out.println("MongoDatabaseFactory created successfully");
            return factory;
        } catch (Exception e) {
            System.err.println("Failed to create MongoDatabaseFactory: " + e.getMessage());
            throw e;
        }
    }



    @Bean
    public MongoTemplate mongoTemplate() {
        try {
            MongoTemplate template = new MongoTemplate(mongoDatabaseFactory(), mappingMongoConverter());
            System.out.println("MongoTemplate initialized successfully");

            // Test connection
            template.executeCommand("{ ping: 1 }");
            System.out.println("MongoDB connection test successful");

            return template;
        } catch (Exception e) {
            System.err.println("Failed to initialize MongoTemplate: " + e.getMessage());
            throw new RuntimeException("MongoDB initialization failed", e);
        }
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
        try {
            GridFsTemplate gridFsTemplate = new GridFsTemplate(mongoDatabaseFactory(), mappingMongoConverter());
            System.out.println("GridFsTemplate initialized successfully");
            return gridFsTemplate;
        } catch (Exception e) {
            System.err.println("Failed to initialize GridFsTemplate: " + e.getMessage());
            throw new RuntimeException("GridFsTemplate initialization failed", e);
        }
    }
}