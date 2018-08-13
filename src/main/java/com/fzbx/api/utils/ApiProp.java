package com.fzbx.api.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ApiProp {


    public static class Verify {

        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public ApiProp() {
    }
}

