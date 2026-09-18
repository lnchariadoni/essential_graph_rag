package com.lchari.learning.graph.rag.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.lchari.learning.graph.rag.config.AppConfig;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

public class GraphRagConfigModule extends AbstractModule {
  @Provides
  @Singleton
  AppConfig provideAppConfig() {
    return AppConfig.load();
  }
}
