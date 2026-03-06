package com.example.ragbedrock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OracleExportService {

  private final DataSource dataSource;
  private final ObjectMapper objectMapper;

  @Value("${export.source-table:source_docs}")
  private String sourceTable;

  @Value("${export.text-column:contenu}")
  private String textColumn;

  @Value("${export.output-dir:/tmp/oracle-export}")
  private String outputDir;

  public List<Path> exportToJsonFiles() throws Exception {
    Files.createDirectories(Path.of(outputDir));
    List<Path> exportedFiles = new ArrayList<>();

    String sql = "SELECT id, " + textColumn + ", updated_at FROM " + sourceTable;

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {

      while (rs.next()) {
        long id = rs.getLong("id");
        String text = rs.getString(textColumn);
        if (text == null || text.isBlank()) continue;

        // Un fichier JSON par ligne Oracle
        // Bedrock Knowledge Base va chunker lui-même le contenu
        Map<String, Object> doc = Map.of(
            "id", id,
            "content", text,
            "metadata", Map.of(
                "source", sourceTable,
                "updated_at", rs.getString("updated_at")
            )
        );

        Path filePath = Path.of(outputDir, "doc_" + id + ".json");
        objectMapper.writeValue(filePath.toFile(), doc);
        exportedFiles.add(filePath);
      }
    }

    log.info("{} fichiers exportés dans {}", exportedFiles.size(), outputDir);
    return exportedFiles;
  }
}