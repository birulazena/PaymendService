package com.github.birulazena.PaymentService.changelog;

import jakarta.annotation.PostConstruct;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MongoLiquibaseRunner {

    private static final String CHANGELOG = "db/changelog-master.yml";

    @Value("${spring.mongodb.uri}")
    private String url;

    @PostConstruct
    public void runMigrations() {
        try {
            Database database = DatabaseFactory.getInstance()
                    .openDatabase(url, null, null, null, new ClassLoaderResourceAccessor());

            Liquibase liquibase = new Liquibase(
                    CHANGELOG,
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update((String)null);

            System.out.println("MongoDB migrations applied successfully!");

        } catch (Exception e) {
            throw new RuntimeException("Failed to run MongoDB Liquibase migrations", e);
        }
    }
}
