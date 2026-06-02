package com.afetch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "afetch")
public class AfetchProperties {

    private Jwt jwt = new Jwt();
    private String refreshCookieName = "afetch_refresh";
    private OpenRouter openrouter = new OpenRouter();
    private Gcs gcs = new Gcs();
    private String localUploadDir = "uploads";

    public static class Jwt {
        private String secret;
        private int accessExpirationMinutes = 15;
        private int refreshExpirationDays = 30;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getAccessExpirationMinutes() { return accessExpirationMinutes; }
        public void setAccessExpirationMinutes(int accessExpirationMinutes) { this.accessExpirationMinutes = accessExpirationMinutes; }
        public int getRefreshExpirationDays() { return refreshExpirationDays; }
        public void setRefreshExpirationDays(int refreshExpirationDays) { this.refreshExpirationDays = refreshExpirationDays; }
    }

    public static class OpenRouter {
        private String apiKey;
        private String model;
        private String baseUrl = "https://openrouter.ai/api/v1";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    public static class Gcs {
        private String bucket;
        private boolean enabled;

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public String getRefreshCookieName() { return refreshCookieName; }
    public void setRefreshCookieName(String refreshCookieName) { this.refreshCookieName = refreshCookieName; }
    public OpenRouter getOpenrouter() { return openrouter; }
    public void setOpenrouter(OpenRouter openrouter) { this.openrouter = openrouter; }
    public Gcs getGcs() { return gcs; }
    public void setGcs(Gcs gcs) { this.gcs = gcs; }
    public String getLocalUploadDir() { return localUploadDir; }
    public void setLocalUploadDir(String localUploadDir) { this.localUploadDir = localUploadDir; }
}
