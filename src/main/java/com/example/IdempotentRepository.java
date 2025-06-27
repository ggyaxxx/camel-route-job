package com.example;

import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;




@Configuration
public class IdempotentRepository {
    @Bean(name = "myFileRepository")
    public org.apache.camel.spi.IdempotentRepository fileIdempotentRepository() {
        return FileIdempotentRepository.fileIdempotentRepository(new File("/tmp/idempotent-repo.dat"));
    }

}