package com.wealthcopilot.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-api")
public class ExternalApiProperties {

    private String bootstrapKey = "";
    private String bootstrapLabel = "Instructor demo key";

    public String getBootstrapKey() {
        return bootstrapKey;
    }

    public void setBootstrapKey(String bootstrapKey) {
        this.bootstrapKey = bootstrapKey;
    }

    public String getBootstrapLabel() {
        return bootstrapLabel;
    }

    public void setBootstrapLabel(String bootstrapLabel) {
        this.bootstrapLabel = bootstrapLabel;
    }
}
