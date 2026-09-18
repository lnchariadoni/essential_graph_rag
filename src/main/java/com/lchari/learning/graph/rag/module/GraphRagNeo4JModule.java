package com.lchari.learning.graph.rag.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lchari.learning.graph.rag.config.AppConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphRagNeo4JModule extends AbstractModule {
  private static final Logger logger = LoggerFactory.getLogger(GraphRagNeo4JModule.class);

  @Provides
  @Singleton
  AppConfig.Neo4jConfig provideNeo4jConfig(AppConfig appConfig) {
    return appConfig.neo4jConfig();
  }

  @Provides
  @Singleton
  Driver provideNeo4JDriver(AppConfig.Neo4jConfig neo4jConfig) {
    Driver driver = GraphDatabase
        .driver(
            neo4jConfig.url(),
            AuthTokens.basic(neo4jConfig.username(), neo4jConfig.password())
        );

    Runtime
        .getRuntime()
        .addShutdownHook(new Thread(() -> {
          logger.info("Closing Neo4j connection.");
          driver.close();
        }));

    return driver;
  }
}
