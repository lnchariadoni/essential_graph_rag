package com.lchari.learning.graph.rag.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void ollamaConfigFromParseBaseUrlAndTimeout() {
        Config config = ConfigFactory.parseString("""
            baseUrl = "http://localhost:11434"
            requestedTimeoutSeconds = 120
            """);
        
        var ollamaConfig = AppConfig.OllamaConfig.from(config);

        assertEquals("http://localhost:11434", ollamaConfig.baseUrl());
        assertEquals(120L, ollamaConfig.requestedTimeoutSeconds());
    }

    @Test
    void openAIConfigFromParsesBaseUrlAndApiKey() {
        Config config = ConfigFactory.parseString("""
            baseUrl = "https://api.openai.com/v1"
            apiKey = "test-key-123"
            """);
        
        var openAIConfig = AppConfig.OpenAIConfig.from(config);

        assertEquals("https://api.openai.com/v1", openAIConfig.baseUrl());
        assertEquals("test-key-123", openAIConfig.apiKey());
    }

    @Test
    void neto4jConfigFromParsesAllFields() {
        Config config = ConfigFactory.parseString("""
            url = "neo4j://localhost:7687"
            username = "test-user"
            password = "test-password"
            database = "test-db"
            """);
        
        var neo4jConfig = AppConfig.Neo4jConfig.from(config);

        assertEquals("neo4j://localhost:7687", neo4jConfig.url());
        assertEquals("test-user", neo4jConfig.username());
        assertEquals("test-password", neo4jConfig.password());
        assertEquals("test-db", neo4jConfig.database());
    }

    @Test
    void chapter2ConfigFromParsesAllFields() {
        Config config = ConfigFactory.parseString("""
            pdfUrl = "https://example.com/test.pdf"
            pdfPath = "data/test-downloadedpdf"
            chunkSize = 1000
            chunkOverlap = 100
            topK = 10
            vectorIndex = "test-vector-index"
            fulltextIndex = "test-fulltext-index"
            ingest = false
            question = "Test question for coverage"
            """);
        
        var chapter2Config = AppConfig.Chapter2Config.from(config);

        assertEquals("https://example.com/test.pdf", chapter2Config.pdfUrl());
        assertEquals(Path.of("data/test-downloadedpdf"), chapter2Config.pdfPath());
        assertEquals(1000, chapter2Config.chunkSize());
        assertEquals(100, chapter2Config.chunkOverlap());
        assertEquals(10, chapter2Config.topK());
        assertEquals("test-vector-index", chapter2Config.vectorIndex());
        assertEquals("test-fulltext-index", chapter2Config.fulltextIndex());
        assertFalse(chapter2Config.ingest());
        assertEquals("Test question for coverage", chapter2Config.question());
    }

    @Test
    void loadBuildsAppConfigFromExplicitConfigWihtoutGlobalState() {
        Config config = ConfigFactory.load("application-test");

        AppConfig appConfig = AppConfig.load(config);

        assertNotNull(appConfig);
        assertEquals("http://localhost:11434", appConfig.ollalamaConfig().baseUrl());
        assertEquals("test-api-key", appConfig.openAIConfig().apiKey());
        assertEquals("test-user", appConfig.neo4jConfig().username());
        assertEquals("test-password", appConfig.neo4jConfig().password());
        assertEquals("test-db", appConfig.neo4jConfig().database());

        assertEquals("openai-default", appConfig.embeddingModelProfile().name());
        assertEquals("openai", appConfig.embeddingModelProfile().provider());
        assertEquals("text-embedding-3-small", appConfig.embeddingModelProfile().model());

        assertEquals("openai-default", appConfig.llmModelProfile().name());
        assertEquals("openai", appConfig.llmModelProfile().provider());
        assertEquals("gpt-4", appConfig.llmModelProfile().model());

        assertEquals("test-vector-index", appConfig.chapter2Config().vectorIndex());
        assertFalse(appConfig.chapter2Config().ingest());
    }

    @Test
    void loadResolvesActiveEmbeddingAndLlmProfilesByName() {
        Config config = ConfigFactory.parseString("""
            providers.ollama { baseUrl = "http://localhost:11434", requestedTimeoutSeconds = 60 }
            providers.openai { baseUrl = "https://api.openai.com/v1", apiKey = "key" }
            
            embeddings.active = "local-default"
            embeddings.profiles.local-default { provider = "ollama", model = "nomic-embed-text" }
            embeddings.profiles.open-default = { provider = "openai", model = "text-embedding-3-small" }
            
            llms.active = "openai-default"
            llms.profiles.local-default { provider = "ollama", model = "gemma" }
            llms.profiles.openai-default { provider = "openai", model = "gpt-4o" }
            
            neo4j { url = "neo4j://localhost:7687", username = "u", password = "p", database = "db" }
            
            chapter2 {
                pdfUrl = "https://example.com/test.pdf"
                pdfPath = "data/test-downloadedpdf"
                chunkSize = 500
                chunkOverlap = 40
                topK = 4
                vectorIndex = "v"
                fulltextIndex = "test-fulltext-index"
                ingest = false
                question = "q"
            }
            """);

        AppConfig appConfig = AppConfig.load(config);

        assertEquals("local-default", appConfig.embeddingModelProfile().name());
        assertEquals("ollama", appConfig.embeddingModelProfile().provider());
        assertEquals("nomic-embed-text", appConfig.embeddingModelProfile().model());

        assertEquals("openai-default", appConfig.llmModelProfile().name());
        assertEquals("openai", appConfig.llmModelProfile().provider());
        assertEquals("gpt-4o", appConfig.llmModelProfile().model());
    }

    @Test
    void loadThrowsWhenActiveProfileNameDoesNotExists() {
        Config config = ConfigFactory.parseString("""
            providers.ollama { baseUrl = "http://localhost:11434", requestedTimeoutSeconds = 60 }
            providers.openai { baseUrl = "https://api.openai.com/v1", apiKey = "key" }
            
            embeddings.active = "missing-profile"
            embeddings.profiles.local-default { provider = "ollama", model = "nomic-embed-text" }
            embeddings.profiles.open-default = { provider = "openai", model = "text-embedding-3-small" }
            
            llms.active = "openai-default"
            llms.profiles.local-default { provider = "ollama", model = "gemma" }
            llms.profiles.openai-default { provider = "openai", model = "gpt-4o" }
            
            neo4j { url = "neo4j://localhost:7687", username = "u", password = "p", database = "db" }
            
            chapter2 {
                pdfUrl = "https://example.com/test.pdf"
                pdfPath = "data/test-downloadedpdf"
                chunkSize = 500
                chunkOverlap = 40
                topK = 4
                vectorIndex = "v"
                fulltextIndex = "test-fulltext-index"
                ingest = false
                question = "q"
            }
            """);

        assertThrows(ConfigException.Missing.class, () -> AppConfig.load(config));
    }

    @Test
    void appConfigRecordsCanBeConstructedDirectly() {
        // Test instantiation of all records to ensure proper construction
        var ollama = new AppConfig.OllamaConfig("http://localhost:11434", 120L);
        var openai = new AppConfig.OpenAIConfig("https://api.openai.com/v1", "test-key");
        var embeddingProfile = new AppConfig.EmbeddingModelProfile("test", "ollama", "model");
        var llmProfile = new AppConfig.LLMModelProfile("test", "ollama", "model");
        var neo4j = new AppConfig.Neo4jConfig("neo4j://localhost:7687", "user", "password", "db");

        // Verify all records can be constructed and accessed
        assertEquals("http://localhost:11434", ollama.baseUrl());
        assertEquals("https://api.openai.com/v1", openai.baseUrl());
        assertEquals("test", embeddingProfile.name());
        assertEquals("test", llmProfile.name());
        assertEquals("neo4j://localhost:7687", neo4j.url());
    }
}