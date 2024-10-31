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

import java.util.*;
import java.util.stream.Collectors;

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

    public Map<CarbonIntensityMapKey, List<CarbonIpEntry>> getLatestCarbonEntryForJob(UUID uuid) throws JsonProcessingException {
        Predicate<HazelcastJsonValue, HazelcastJsonValue> predicate = e.get("jobUuid").equal(uuid);
        Set<HazelcastJsonValue> keys = this.carbonIntensityMap.keySet(predicate);
        CarbonIntensityMapKey latestKeyForJob = null;

        for (HazelcastJsonValue key : keys) {
            CarbonIntensityMapKey carbonIntensityMapKey = this.objectMapper.readValue(key.toString(), CarbonIntensityMapKey.class);
            if (latestKeyForJob == null) {
                latestKeyForJob = carbonIntensityMapKey;
            }
            if (carbonIntensityMapKey.getTimeMeasuredAt().isAfter(latestKeyForJob.getTimeMeasuredAt())) {
                latestKeyForJob = carbonIntensityMapKey;
            }
        }

        List<CarbonIpEntry> value = this.getValueForKey(latestKeyForJob);
        Map<CarbonIntensityMapKey, List<CarbonIpEntry>> retMap = new HashMap<>();
        retMap.put(latestKeyForJob, value);
        return retMap;
    }

    public Map<CarbonIntensityMapKey, List<CarbonIpEntry>> getAllCarbonIntensityForJob(UUID uuid) throws JsonProcessingException {
        Predicate<HazelcastJsonValue, HazelcastJsonValue> predicate = e.get("jobUuid").equal(uuid);
        Set<HazelcastJsonValue> keySet = this.carbonIntensityMap.keySet(predicate);
        Map<CarbonIntensityMapKey, List<CarbonIpEntry>> retMap = new HashMap<>();

        for (HazelcastJsonValue key : keySet) {
            CarbonIntensityMapKey localKey = this.objectMapper.readValue(key.getValue(), CarbonIntensityMapKey.class);
            List<CarbonIpEntry> carbonIpEntryList = this.getValueForKey(localKey);
            retMap.put(localKey, carbonIpEntryList);
        }

        return retMap.entrySet().stream()
                .sorted((entry1, entry2) -> entry2.getKey().getTimeMeasuredAt().compareTo(entry1.getKey().getTimeMeasuredAt()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
