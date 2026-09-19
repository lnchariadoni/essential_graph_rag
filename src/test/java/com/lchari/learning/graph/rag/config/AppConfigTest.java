package com.lchari.learning.graph.rag.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.Config;
import java.nio.file.Path;

public class AppConfigTest {

    @Test
    public void testOllamaConfigFrom() {
        // Test OllamaConfig.from() method
        Config config = ConfigFactory.parseString("""
            baseUrl = "http://localhost:11434"
            requestedTimeoutSeconds = 120
            """);
        
        var ollamaConfig = AppConfig.OllamaConfig.from(config);
        assertEquals("http://localhost:11434", ollamaConfig.baseUrl());
        assertEquals(120L, ollamaConfig.requestedTimeoutSeconds());
    }

    @Test
    public void testOpenAIConfigFrom() {
        // Test OpenAIConfig.from() method
        Config config = ConfigFactory.parseString("""
            baseUrl = "https://api.openai.com/v1"
            apiKey = "test-key-123"
            """);
        
        var openAIConfig = AppConfig.OpenAIConfig.from(config);
        assertEquals("https://api.openai.com/v1", openAIConfig.baseUrl());
        assertEquals("test-key-123", openAIConfig.apiKey());
    }

    @Test
    public void testNeo4jConfigFrom() {
        // Test Neo4jConfig.from() method
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
    public void testChapter2ConfigFrom() {
        // Test Chapter2Config.from() method
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
    public void testAppConfigLoad() {
        // Test the main AppConfig.load() method with mocked environment variables 
        // This requires setting environment variables to avoid config loading failures
        try {
            // Set required environment variables for testing
            System.setProperty("OPENAI_API_KEY", "test-key");
            System.setProperty("NEO4J_USERNAME", "test-user");
            System.setProperty("NEO4J_PASSWORD", "test-pass");
            
            AppConfig config = AppConfig.load();
            assertNotNull(config);
            assertNotNull(config.ollalamaConfig());
            assertNotNull(config.openAIConfig());
            assertNotNull(config.embeddingModelProfile());
            assertNotNull(config.llmModelProfile());
            assertNotNull(config.neo4jConfig());
            assertNotNull(config.chapter2Config());
            
            // Verify some key values from config
            assertEquals("test-user", config.neo4jConfig().username());
            assertEquals("test-key", config.openAIConfig().apiKey());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        finally {
            // Clean up system properties
            System.clearProperty("OPENAI_API_KEY");
            System.clearProperty("NEO4J_USERNAME");
            System.clearProperty("NEO4J_PASSWORD");
        }
    }

    @Test
    public void testAppConfigRecords() {
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