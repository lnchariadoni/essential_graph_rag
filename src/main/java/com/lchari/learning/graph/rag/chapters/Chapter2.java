package com.lchari.learning.graph.rag.chapters;

import static com.lchari.learning.graph.rag.util.CustomLogger.printResults;
import static com.lchari.learning.graph.rag.util.Utils.answerQuestion;
import static com.lchari.learning.graph.rag.util.Utils.ensureDimensionMatches;
import static com.lchari.learning.graph.rag.util.Utils.ingest;
import static com.lchari.learning.graph.rag.util.Utils.requireCompatibleStoredEmbeddings;

import com.lchari.learning.graph.rag.config.AppConfig;
import com.lchari.learning.graph.rag.model.EmbeddingIndexMetadata;
import com.lchari.learning.graph.rag.model.RetrievedChunk;
import com.lchari.learning.graph.rag.neo4j.Neo4jRagRepository;
import com.lchari.learning.graph.rag.provider.ChatProvider;
import com.lchari.learning.graph.rag.provider.EmbeddingProvider;
import java.util.List;

public class Chapter2 {

  public static void doExercise(
      AppConfig config,
      EmbeddingProvider embeddingProvider,
      ChatProvider chatProvider,
      Neo4jRagRepository repository) {
    String question = config.chapter2Config().question();

    System.out.println("Doing Chapter2 exercises.");
    System.out.println("Question:" + question);

    EmbeddingIndexMetadata metadata = requireCompatibleStoredEmbeddings(config, repository);
    List<Double> questionEmbedding = embeddingProvider.getEmbeddings(List.of(question)).getFirst();

    ensureDimensionMatches(metadata, questionEmbedding.size());
    System.out.println("Question embedding dimensions:" + questionEmbedding.size());

    List<RetrievedChunk> vectorEmbeddingResults = repository.vectorSearch(
        config.chapter2Config().vectorIndex(),
        questionEmbedding,
        config.chapter2Config().topK()
    );
    printResults(vectorEmbeddingResults);

    String vectorAnswer = answerQuestion(chatProvider, question, vectorEmbeddingResults);
    System.out.printf("Answer to question: %s %nMethod: Using vector results. %nResponse:%s%n", question, vectorAnswer);

    List<RetrievedChunk> hybridResults = repository.hybridSeach(
        config.chapter2Config().vectorIndex(),
        config.chapter2Config().fulltextIndex(),
        questionEmbedding,
        question,
        config.chapter2Config().topK()
    );
    printResults(hybridResults);

    String hybridAnswer = answerQuestion(chatProvider, question, hybridResults);
    System.out.printf("Answer to question: %s %nMethod: Using hybrid results. %nResponse:%s%n", question, hybridAnswer);
  }
}
