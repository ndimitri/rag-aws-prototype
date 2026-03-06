package com.example.ragbedrock.service;

import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncOrchestratorService {

    private final OracleExportService exportService;
    private final S3UploadService s3UploadService;
    private final KnowledgeBaseService knowledgeBaseService;

    public void runFullSync() throws Exception {
      log.info("=== Sync mensuel démarré ===");
      long start = System.currentTimeMillis();

      //1. Export Oracle -> JSON
      List<Path> files = exportService.exportToJsonFiles();

      //2. Upload sur S3 (remplace l'ancien export
      s3UploadService.uploadFiles(files);

      //3. Démarrer l'ingestion Bedrock (chunking + embedding géré par AWS)
      knowledgeBaseService.startIngestion();

      // 4. Nettoyage des fichiers temporaires locaux
      files.forEach(f -> f.toFile().delete());

      long duration = (System.currentTimeMillis() - start) / 1000;
      log.info("=== Sync terminé en {}s ===", duration);

    }
}
