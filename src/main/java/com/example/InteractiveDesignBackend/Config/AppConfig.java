package com.example.InteractiveDesignBackend.Config;

//package com.example.InteractiveDesignBackend.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

   @Bean(name = "pdfRestTemplate")
   public RestTemplate pdfRestTemplate() {

       PoolingHttpClientConnectionManager cm =
               new PoolingHttpClientConnectionManager();
       cm.setMaxTotal(50);           // max 50 connections total
       cm.setDefaultMaxPerRoute(20); // max 20 to Node server

       CloseableHttpClient httpClient = HttpClients.custom()
               .setConnectionManager(cm)
               .build();

       HttpComponentsClientHttpRequestFactory factory =
               new HttpComponentsClientHttpRequestFactory(httpClient);
       factory.setConnectTimeout(5_000);    // 5s to connect
       factory.setReadTimeout(180_000);     // 3 min to read PDF response

       return new RestTemplate(factory);
   }
}