package com.example.ragbedrock.scheduler;

import com.example.ragbedrock.service.SyncOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlySyncScheduler {


  private final SyncOrchestratorService syncOrchestratorService;

  // Premier jour du mois à 2h du matin
  @Scheduled(cron = "0 0 2 1 * *")
  public void monthlySync() throws Exception {
    log.info("Déclenchement automatique du sync mensuel");
    syncOrchestratorService.runFullSync();
  }

  // Endpoint manuel si besoin de forcer un sync
  public void triggerManualSync() throws Exception {
    log.info("Sync manuel déclenché");
    syncOrchestratorService.runFullSync();
  }
}
