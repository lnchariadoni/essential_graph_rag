package com.lchari.learning.graph.rag.module;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lchari.learning.graph.rag.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

class GraphRagNeo4JModuleTest {

  private final GraphRagNeo4JModule module = new GraphRagNeo4JModule();

  @Test
  void provideNeo4jConfigDelegatesToAppConfig() {
    AppConfig.Neo4jConfig neo4jConfig = new AppConfig.Neo4jConfig("neo4j://localhost:7687",
        "user",
        "pass",
        "neo4j");

    AppConfig appConfig = mock(AppConfig.class);
    when(appConfig.neo4jConfig()).thenReturn(neo4jConfig);

    AppConfig.Neo4jConfig result = module.provideNeo4jConfig(appConfig);

    assertSame(neo4jConfig, result);
  }

  @Test
  void provideNeo4JDriverReturnsNonNullDriverWithoutEagerlyConnecting() {
    AppConfig.Neo4jConfig neo4jConfig = new AppConfig.Neo4jConfig("bolt://localhost:7687",
        "user",
        "pass",
        "neo4j");

    Driver driver = module.provideNeo4JDriver(neo4jConfig);

    assertNotNull(driver);
    driver.close();

  }
}
