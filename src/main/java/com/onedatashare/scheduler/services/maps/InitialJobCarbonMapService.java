package com.onedatashare.scheduler.services.maps;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InitialJobCarbonMapService {

    private final IMap<CarbonIntensityMapKey, List<CarbonIpEntry>> initialMeasurementMap;

    public InitialJobCarbonMapService(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance) {
        this.initialMeasurementMap = hazelcastInstance.getMap("initial-transfer-measurements");
    }

    public void putCarbonKeyValue(CarbonIntensityMapKey key, List<CarbonIpEntry> carbonMeasurement) {
        this.initialMeasurementMap.put(key, carbonMeasurement);
    }

    public boolean containsKey(CarbonIntensityMapKey key) {
        return this.initialMeasurementMap.containsKey(key);
    }

    public double carbonIntensityForKey(CarbonIntensityMapKey key) {
        List<CarbonIpEntry> measurement = this.initialMeasurementMap.get(key);
        if (measurement != null && !measurement.isEmpty()) {
            return measurement.stream().mapToDouble(CarbonIpEntry::getCarbonIntensity).average().getAsDouble();
        } else {
            return 0.0;
        }
    }

}
