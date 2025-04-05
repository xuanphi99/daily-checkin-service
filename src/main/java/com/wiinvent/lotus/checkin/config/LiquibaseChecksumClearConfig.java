package com.wiinvent.lotus.checkin.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class LiquibaseChecksumClearConfig {
    private final Logger logger = LoggerFactory.getLogger(LiquibaseChecksumClearConfig.class);

    @Value("${liquibase.clear-checksum.enabled:true}")
    private boolean clearChecksumEnabled;

    @Bean
    public CommandLineRunner clearLiquibaseChecksums(DataSource dataSource) {
        return args -> {
            if (clearChecksumEnabled) {
                try (Connection connection = dataSource.getConnection()) {
                    Database database = DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

                    Liquibase liquibase = new Liquibase(
                            "db/changelog/db.changelog-master.xml",
                            new ClassLoaderResourceAccessor(),
                            database
                    );

                    liquibase.clearCheckSums();
                    logger.info(" Liquibase checksums cleared.");
                } catch (Exception e) {
                    logger.error(" ERROR Liquibase checksums cleared. {}", e.getMessage());
                }
            }
        };
    }
}
