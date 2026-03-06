package com.example.ragbedrock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagent.BedrockAgentClient;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;


@Configuration
public class BedrockConfig {

  @Bean
  public BedrockRuntimeClient bedrockRuntimeClient(){
    return BedrockRuntimeClient.builder()
        .region(Region.EU_WEST_3)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }


  @Bean
  public BedrockAgentClient bedrockAgentClient() {
    return BedrockAgentClient.builder()
        .region(Region.EU_WEST_3)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }


  @Bean
  public BedrockAgentRuntimeClient bedrockAgentRuntimeClient() {
    return BedrockAgentRuntimeClient.builder()
        .region(Region.EU_WEST_3)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }
}
