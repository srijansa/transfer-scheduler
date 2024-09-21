package com.onedatashare.scheduler.services.maps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CarbonMapService {
    private final PredicateBuilder.EntryObject e;
    private final ObjectMapper objectMapper;
    IMap<HazelcastJsonValue, HazelcastJsonValue> carbonIntensityMap;

    public CarbonMapService(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance, ObjectMapper objectMapper) {
        this.carbonIntensityMap = hazelcastInstance.getMap("carbon-intensity-map");
        this.e = Predicates.newPredicateBuilder().getEntryObject();
        this.objectMapper = objectMapper;
    }


    public Set<CarbonIntensityMapKey> getCarbonMeasurementsForUserAndJob(String ownerId, UUID jobUuid) throws JsonProcessingException {
        Predicate<HazelcastJsonValue, HazelcastJsonValue> predicate = e.get("ownerId").equal(ownerId).and(e.get("jobUuid").equal(jobUuid)).and(e.get("runningJob").equal("false"));
        Set<HazelcastJsonValue> keySet = this.carbonIntensityMap.keySet(predicate);
        Set<CarbonIntensityMapKey> carbonIntensityMapKeys = new HashSet<>();
        for (HazelcastJsonValue value : keySet) {
            CarbonIntensityMapKey key = this.objectMapper.readValue(value.getValue(), CarbonIntensityMapKey.class);
            carbonIntensityMapKeys.add(key);
        }
        return carbonIntensityMapKeys;
    }

    public List<CarbonIpEntry> getValueForKey(CarbonIntensityMapKey key) throws JsonProcessingException {
        String json = this.objectMapper.writeValueAsString(key);
        HazelcastJsonValue jsonValue = this.carbonIntensityMap.get(new HazelcastJsonValue(json));
        return this.objectMapper.readValue(jsonValue.getValue(), new TypeReference<List<CarbonIpEntry>>() {
        });
    }

}
