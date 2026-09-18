package com.lchari.learning.graph.rag.chapters;

import static com.lchari.learning.graph.rag.util.CustomLogger.printResults;
import static com.lchari.learning.graph.rag.util.Utils.answerQuestion;
import static com.lchari.learning.graph.rag.util.Utils.ensureDimensionMatches;
import static com.lchari.learning.graph.rag.util.Utils.requireCompatibleStoredEmbeddings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.chat.ChatProvider;
import com.lchari.learning.graph.rag.provider.embedding.EmbeddingProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Chapter2 {
  private static final Logger logger = LoggerFactory.getLogger(Chapter2.class);

  private final AppConfig appConfig;
  private final EmbeddingProvider embeddingProvider;
  private final ChatProvider chatProvider;
  private final Neo4jRagRepository neo4jRagRepository;

  @Inject
  public Chapter2(AppConfig appConfig,
                  EmbeddingProvider embeddingProvider,
                  ChatProvider chatProvider,
                  Neo4jRagRepository neo4jRagRepository) {
    this.appConfig = appConfig;
    this.embeddingProvider = embeddingProvider;
    this.chatProvider = chatProvider;
    this.neo4jRagRepository = neo4jRagRepository;
  }

  public void doExercise() {
    String question = appConfig.chapter2Config().question();

    logger.info("Doing Chapter2 exercises.");
    logger.info("Question: {}", question);

    EmbeddingIndexMetadata metadata = requireCompatibleStoredEmbeddings(appConfig, neo4jRagRepository);
    List<Double> questionEmbedding = embeddingProvider.getEmbeddings(List.of(question)).getFirst();

    ensureDimensionMatches(metadata, questionEmbedding.size());
    logger.info("Question embedding dimensions: {}", questionEmbedding.size());

    List<RetrievedChunk> vectorEmbeddingResults = neo4jRagRepository.vectorSearch(
        appConfig.chapter2Config().vectorIndex(),
        questionEmbedding,
        appConfig.chapter2Config().topK()
    );
    printResults(vectorEmbeddingResults);

    String vectorAnswer = answerQuestion(chatProvider, question, vectorEmbeddingResults);
    logger.info("Answer to question: {} \nMethod: Using vector results. \nResponse:{}\n", question, vectorAnswer);

    List<RetrievedChunk> hybridResults = neo4jRagRepository.hybridSeach(
        appConfig.chapter2Config().vectorIndex(),
        appConfig.chapter2Config().fulltextIndex(),
        questionEmbedding,
        question,
        appConfig.chapter2Config().topK()
    );
    printResults(hybridResults);

    String hybridAnswer = answerQuestion(chatProvider, question, hybridResults);
    logger.info("Answer to question: {} \nMethod: Using hybrid results. \nResponse:{}\n", question, hybridAnswer);
  }
}
