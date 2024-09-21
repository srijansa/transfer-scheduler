package com.onedatashare.scheduler.services.maps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.query.PredicateBuilder;
import com.hazelcast.query.Predicates;
import com.onedatashare.scheduler.model.TransferJobRequest;
import com.onedatashare.scheduler.services.MessageSender;
import com.onedatashare.scheduler.services.listeners.EntryExpiredHazelcast;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TransferSchedulerMapService {

    private final IMap<UUID, HazelcastJsonValue> fileTransferNodeMetaDataIMap;
    private final ObjectMapper objectMapper;
    private final PredicateBuilder.EntryObject e;

    public TransferSchedulerMapService(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance, ObjectMapper objectMapper, MessageSender messageSender) {
        this.fileTransferNodeMetaDataIMap = hazelcastInstance.getMap("file-transfer-schedule-map");
        this.objectMapper = objectMapper;
        this.e = Predicates.newPredicateBuilder().getEntryObject();
        this.fileTransferNodeMetaDataIMap.addEntryListener(new EntryExpiredHazelcast(messageSender), true);
    }

    public Collection<UUID> getKeys() {
        return this.fileTransferNodeMetaDataIMap.keySet();
    }

    public TransferJobRequest getValue(UUID uuid) throws JsonProcessingException {
        HazelcastJsonValue hazelcastJsonValue = this.fileTransferNodeMetaDataIMap.get(uuid);
        return this.objectMapper.readValue(hazelcastJsonValue.getValue(), TransferJobRequest.class);
    }

    public List<TransferJobRequest> queryUserScheduledJobs(String userEmail) throws JsonProcessingException {
        Collection<HazelcastJsonValue> values = this.fileTransferNodeMetaDataIMap.values(Predicates.equal("ownerId", userEmail));
        List<TransferJobRequest> scheduledJobsForUser = new ArrayList<>();
        for (HazelcastJsonValue value : values) {
            TransferJobRequest transferJobRequest = this.objectMapper.readValue(value.getValue(), TransferJobRequest.class);
            scheduledJobsForUser.add(transferJobRequest);
        }
        return scheduledJobsForUser;
    }

    public TransferJobRequest queryJob(UUID jobUuid) throws JsonProcessingException {
        HazelcastJsonValue hazelcastJsonValue = this.fileTransferNodeMetaDataIMap.get(jobUuid);
        return this.objectMapper.readValue(hazelcastJsonValue.getValue(), TransferJobRequest.class);
    }

    public void deleteJob(UUID jobUuid) {
        this.fileTransferNodeMetaDataIMap.delete(jobUuid);
    }

    public void putJob(TransferJobRequest transferJobRequest, LocalDateTime jobStartTime) throws JsonProcessingException {
        long delay = Duration.between(LocalDateTime.now(), jobStartTime).getSeconds();
        String jsonRequestValue = this.objectMapper.writeValueAsString(transferJobRequest);
        this.fileTransferNodeMetaDataIMap.put(transferJobRequest.getJobUuid(), new HazelcastJsonValue(jsonRequestValue), delay, TimeUnit.SECONDS);
    }
}
