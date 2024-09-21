package com.onedatashare.scheduler.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.eureka.one.EurekaOneDiscoveryStrategyFactory;
import com.hazelcast.map.IMap;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.transport.jersey.TransportClientFactories;
import com.netflix.discovery.shared.transport.jersey3.Jersey3TransportClientFactories;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;


@Configuration
public class CacheConfig {


    @Bean(name = "hazelcastInstance")
    @Profile("prod")
    public HazelcastInstance prodHazelcastInstance(EurekaClient eurekaClient) {
        Config config = new Config();
        config.setClusterName("prod-scheduler-cluster");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        EurekaOneDiscoveryStrategyFactory.setEurekaClient(eurekaClient);
        config.getNetworkConfig().getJoin().getEurekaConfig().setEnabled(true)
                .setProperty("namespace", "hazelcast")
                .setProperty("use-classpath-eureka-client-props", "false")
                .setProperty("shouldUseDns", "false")
                .setProperty("self-registration", "true")
                .setProperty("use-metadata-for-host-and-port", "true");
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean(name = "hazelcastInstance")
    @Profile("dev")
    public HazelcastInstance devHazelcastInstance() {
        Config config = new Config();
        config.setClusterName("dev-scheduler-cluster");
        config.getNetworkConfig().setPortAutoIncrement(true);
        return Hazelcast.newHazelcastInstance(config);
    }

    @Bean
    public IMap<CarbonIntensityMapKey, List<CarbonIpEntry>> historicalTransferJobMeasurements(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        return hazelcastInstance.getMap("historical-transfer-job-measurements");
    }

    @Bean
    public TransportClientFactories transportClientFactories() {
        return Jersey3TransportClientFactories.getInstance();
    }
}
