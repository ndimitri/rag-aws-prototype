package com.example.ragbedrock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagent.model.GetIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.GetIngestionJobResponse;
import software.amazon.awssdk.services.bedrockagent.model.StartIngestionJobRequest;
import software.amazon.awssdk.services.bedrockagent.model.StartIngestionJobResponse;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.RetrieveAndGenerateType;
import software.amazon.awssdk.services.bedrockagentruntime.model.VectorSearchRerankingConfigurationType;


@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {


  private final BedrockAgentClient agentClient;
  private final BedrockAgentRuntimeClient agentRuntimeClient;

  @Value("${bedrock.knowledge-base.id}")
  private String knowledgeBaseId;

  @Value("${bedrock.knowledge-base.data-source-id}")
  private String dataSourceId;

  @Value("${bedrock.knowledge-base.model-arn:arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0}")
  private String modelArn;

  // Déclenche l'ingestion Bedrock (chunking + embedding géré par AWS)
  public void startIngestion() throws InterruptedException {
    log.info("Démarrage de l'ingestion Bedrock Knowledge Base ...");

    StartIngestionJobResponse response = agentClient.startIngestionJob(
        StartIngestionJobRequest.builder()
            .knowledgeBaseId(knowledgeBaseId)
            .dataSourceId(dataSourceId)
            .build()
    );

    String jobId = response.ingestionJob().ingestionJobId();
    log.info("Ingestion démarrée, Job ID: {}", jobId);

    //Polling jussqu'à fin d'ingestion
    waitForIngestion(jobId);
  }

  private void waitForIngestion(String jobId) throws InterruptedException {
    String status = "STARTING";

    while (!status.equals("COMPLETE") && !status.equals("FAILED")) {
      Thread.sleep(15_000); //Attente 15s entre chaque check

      GetIngestionJobResponse job = agentClient.getIngestionJob(
          GetIngestionJobRequest.builder()
              .knowledgeBaseId(knowledgeBaseId)
              .dataSourceId(dataSourceId)
              .ingestionJobId(jobId)
              .build()
      );

      status = job.ingestionJob().statusAsString();
      log.info("Ingestion status: {}", status);

    }

    if (status.equals("FAILED"))
      throw new RuntimeException("Ingestion Bedrock échouée pour jobId=" + jobId);

    log.info("Ingestion terminée avec succès !");

  }

  public String query(String userQuestion) {
    // Appel à l'API Bedrock "RetrieveAndGenerate" qui combine recherche vectorielle + génération de texte
    RetrieveAndGenerateResponse response = agentRuntimeClient.retrieveAndGenerate(
        RetrieveAndGenerateRequest.builder()
            // Définit la question de l'utilisateur comme entrée
            .input(b -> b.text(userQuestion))
            .retrieveAndGenerateConfiguration(b -> b
                // Indique qu'on utilise une Knowledge Base comme source de données
                .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)
                .knowledgeBaseConfiguration(kb -> kb
                    // Identifiant de la Knowledge Base Bedrock à interroger
                    .knowledgeBaseId(knowledgeBaseId)
                    // ARN du modèle LLM utilisé pour générer la réponse finale (Claude 3.5 Sonnet)
                    .modelArn(modelArn)
                    .retrievalConfiguration(r -> r
                        .vectorSearchConfiguration(v -> v
                            // Récupère les 20 chunks les plus proches sémantiquement via la recherche vectorielle
                            .numberOfResults(20)
                            .rerankingConfiguration(rr -> rr
                                // Utilise le modèle de re-ranking natif Bedrock pour affiner les résultats
                                .type(
                                    VectorSearchRerankingConfigurationType.BEDROCK_RERANKING_MODEL)
                                .bedrockRerankingConfiguration(br -> br
                                    .modelConfiguration(m -> m
                                        // ARN du modèle de re-ranking Amazon utilisé pour scorer les chunks
                                        .modelArn(
                                            "arn:aws:bedrock:us-east-1::foundation-model/amazon.rerank-v1:0")
                                    )
                                    // Après re-ranking, ne conserve que les 5 chunks les plus pertinents
                                    .numberOfRerankedResults(5)
                                )
                            )
                        )
                    )
                )
            )
            .build()
    );

    return response.output().text();
  }

}