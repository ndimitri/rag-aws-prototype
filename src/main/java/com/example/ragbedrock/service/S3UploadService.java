package com.example.ragbedrock.service;

import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3UploadService {

  private final S3Client s3Client;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Value("aws.s3.prefix:oracle-exports/")
  private String s3Prefix;

  public void uploadFiles(List<Path> files){

    //Nettoyage du prefix S3 avant upload (full replace mensuel)
    clearPrefix();

  }

  private void clearPrefix() {
    //List et supprime tous les fichier du prefix
    var objects = s3Client.listObjectsV2(b -> b.bucket(bucketName).prefix(s3Prefix));
    objects.contents().forEach(obj ->
        s3Client.deleteObject(b -> b.bucket(bucketName).key(obj.key()))
    );

    log.info("Ancien export S3 nettoyé ({})", s3Prefix);
  }
}
