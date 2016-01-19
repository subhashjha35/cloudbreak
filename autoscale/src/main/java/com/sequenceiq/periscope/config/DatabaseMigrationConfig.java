package com.sequenceiq.periscope.config;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.commons.io.Charsets;
import org.apache.ibatis.migration.DataSourceConnectionProvider;
import org.apache.ibatis.migration.FileMigrationLoader;
import org.apache.ibatis.migration.operations.PendingOperation;
import org.apache.ibatis.migration.operations.UpOperation;
import org.apache.ibatis.migration.options.DatabaseOperationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DatabaseMigrationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private static final String DEFAULT_SCHEMA_LOCATION_IN_CONTAINER = "/schema";
    private static final String SCHEMA_IN_CONTAINER = "container";
    private static final String DEFAULT_SCHEMA_LOCATION_IN_SOURCE = "autoscale/src/main/resources/schema";
    private static final String PENDING_OPERATION_WARNING_MSG = "WARNING: Running pending migrations out of order can create unexpected results.";

    @Value("${periscope.schema.scripts.location:" + DEFAULT_SCHEMA_LOCATION_IN_SOURCE + "}")
    private String schemaLocation;

    @Value("${periscope.schema.migration.auto:true}")
    private boolean schemaMigrationEnabled;

    @Inject
    private DataSource dataSource;

    @Bean
    @DependsOn("dataSource")
    public UpOperation databaseUpMigration() {
        UpOperation upOperation = new UpOperation();
        PendingOperation pendingOperation = new PendingOperation();
        if (schemaMigrationEnabled) {
            DataSourceConnectionProvider dataSourceConnectionProvider = new DataSourceConnectionProvider(dataSource);
            DatabaseOperationOption operationOption = new DatabaseOperationOption();
            operationOption.setRemoveCRs(true);
            ByteArrayOutputStream upOutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream pendingOutStream = new ByteArrayOutputStream();
            FileMigrationLoader migrationsLoader = fileMigrationLoader();
            LOGGER.info("Applying the necessary database migration scripts from location: '{}'....", schemaLocation);
            upOperation = upOperation.operate(dataSourceConnectionProvider, migrationsLoader, operationOption, new PrintStream(upOutStream));
            pendingOperation.operate(dataSourceConnectionProvider, migrationsLoader, operationOption, new PrintStream(pendingOutStream));
            String upMigrationResult = upOutStream.toString().trim();
            String pendingMigrationResult = pendingOutStream.toString().trim();
            if (upMigrationResult.isEmpty() && pendingMigrationResult.equals(PENDING_OPERATION_WARNING_MSG)) {
                LOGGER.info("Schema is up to date. No migration necessary.");
            } else {
                logMigrationResult(upMigrationResult, "up");
                logMigrationResult(pendingMigrationResult, "pending");
            }
        }
        return upOperation;
    }


    @Bean
    public FileMigrationLoader fileMigrationLoader() {
        if (SCHEMA_IN_CONTAINER.equals(schemaLocation)) {
            schemaLocation = DEFAULT_SCHEMA_LOCATION_IN_CONTAINER;
        }
        File scriptsDir = new File(schemaLocation);
        Properties emptyProperties = new Properties();
        String charset = Charsets.UTF_8.displayName();
        return new FileMigrationLoader(scriptsDir, charset, emptyProperties);
    }

    private void logMigrationResult(String migrationResult, String operation) {
        if (!migrationResult.isEmpty()) {
            LOGGER.warn("Migration result of '{}' operation:\n{}", operation, migrationResult);
        }
    }
}
