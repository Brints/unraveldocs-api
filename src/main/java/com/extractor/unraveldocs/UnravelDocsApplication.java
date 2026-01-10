package com.extractor.unraveldocs;

import com.google.cloud.spring.autoconfigure.vision.CloudVisionAutoConfiguration;
import io.awspring.cloud.autoconfigure.core.AwsAutoConfiguration;
import io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        S3AutoConfiguration.class,
        AwsAutoConfiguration.class,
        CloudVisionAutoConfiguration.class
})
@EnableScheduling
@EnableAsync
@EnableCaching
public class UnravelDocsApplication {

    static void main(String[] args) {
        SpringApplication.run(UnravelDocsApplication.class, args);
    }

}
