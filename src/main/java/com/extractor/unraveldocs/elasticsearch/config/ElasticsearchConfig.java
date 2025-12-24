package com.extractor.unraveldocs.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.URI;

/**
 * Elasticsearch configuration for the UnravelDocs application.
 * Provides client beans and repository configuration.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.elasticsearch.uris")
@EnableElasticsearchRepositories(basePackages = "com.extractor.unraveldocs.elasticsearch.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    /**
     * Creates a low-level REST client for Elasticsearch.
     */
    @Bean
    @PreDestroy
    public RestClient restClient() {
        String host = extractHost(elasticsearchUri);
        int port = extractPort(elasticsearchUri);
        String scheme = extractScheme(elasticsearchUri);

        log.info("Configuring Elasticsearch connection to {}://{}:{}", scheme, host, port);

        return RestClient.builder(new HttpHost(host, port, scheme)).build();
    }

    /**
     * Creates the Elasticsearch transport layer.
     */
    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    /**
     * Creates the Elasticsearch client for operations.
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    /**
     * Creates the ElasticsearchOperations bean for Spring Data Elasticsearch.
     */
    @Bean
    public ElasticsearchOperations elasticsearchTemplate(ElasticsearchClient client) {
        return new ElasticsearchTemplate(client);
    }

    private String extractHost(String uri) {
        return URI.create(uri).getHost();
    }

    private int extractPort(String uri) {
        int port = URI.create(uri).getPort();
        return port > 0 ? port : 9200;
    }

    private String extractScheme(String uri) {
        String scheme = URI.create(uri).getScheme();
        return scheme != null ? scheme : "http";
    }
}
