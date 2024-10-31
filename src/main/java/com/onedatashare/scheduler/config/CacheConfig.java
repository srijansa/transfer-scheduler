package com.onedatashare.scheduler.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.eureka.one.EurekaOneDiscoveryStrategyFactory;
import com.hazelcast.map.IMap;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import com.onedatashare.scheduler.services.VaultSSLService;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Properties;


@Configuration
public class CacheConfig {

    private final Environment env;
    private final VaultSSLService vaultSslService;
    private final Logger logger;

    public CacheConfig(Environment environment, VaultSSLService vaultSSLService) {
        this.env = environment;
        this.vaultSslService = vaultSSLService;
        this.logger = LoggerFactory.getLogger(CacheConfig.class);
    }

    @Value("${hazelcast.enterprise.license}")
    String hazelcastLicenseKey;

    @SneakyThrows
    @Bean(name = "hazelcastInstance")
    @Profile("prod")
    public HazelcastInstance prodHazelcastInstance(EurekaClient eurekaClient, SSLConfig sslConfig) {
        while (eurekaClient.getApplications().getRegisteredApplications().isEmpty()) {
            Thread.sleep(1000); // Wait until Eureka has registered nodes
        }
        Config config = new Config();
        config.setClusterName("prod-scheduler-cluster");
        config.setLicenseKey(this.hazelcastLicenseKey);
        config.getNetworkConfig().setPortAutoIncrement(true);
        config.getNetworkConfig().setSSLConfig(sslConfig);

        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        logger.info(this.env.getProperty("eureka.client.serviceUrl.defaultZone"));
        config.getNetworkConfig().getJoin().getEurekaConfig()
                .setEnabled(true)
                .setProperty("self-registration", "true")
                .setProperty("namespace", "hazelcast")
                .setProperty("name", "hazelcast-prod")
                .setProperty("shouldUseDns", "false")
                .setProperty("serviceUrl.default", this.env.getProperty("eureka.client.serviceUrl.defaultZone"))
                .setProperty("use-classpath-eureka-client-props", "false");
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean(name = "hazelcastInstance")
    @Profile("dev")
    public HazelcastInstance devHazelcastInstance(SSLConfig sslConfig) {
        Config config = new Config();
        config.setClusterName("dev-scheduler-cluster");
        config.setLicenseKey(this.hazelcastLicenseKey);
        config.getNetworkConfig().setSSLConfig(sslConfig);
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().setPortAutoIncrement(true);
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public SSLConfig sslConfig() {
        Properties properties = new Properties();
        properties.setProperty("protocol", "TLSv1.2");
        properties.setProperty("mutualAuthentication", "OPTIONAL");
        properties.setProperty("keyMaterialDuration", vaultSslService.getStoreDuration().toString());
        properties.setProperty("validateIdentity", "false");

        SSLConfig sslConfig = new SSLConfig();
        sslConfig.setEnabled(true);
        sslConfig.setProperties(properties);
        sslConfig.setFactoryImplementation(this.vaultSslService);

        return sslConfig;
    }

    @Bean
    public IMap<CarbonIntensityMapKey, List<CarbonIpEntry>> historicalTransferJobMeasurements(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("historical-transfer-job-measurements");
    }

    @Bean
    public TransportClientFactories transportClientFactories() {
        return Jersey3TransportClientFactories.getInstance();
    }

    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
