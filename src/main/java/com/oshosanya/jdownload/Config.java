package com.oshosanya.jdownload;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class Config {
    @Bean
    @Scope("prototype")
    public DownloadTask downloadTaskPrototype() {
        return new DownloadTask();
    }
}
