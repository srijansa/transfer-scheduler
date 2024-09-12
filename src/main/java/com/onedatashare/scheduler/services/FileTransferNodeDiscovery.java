package com.onedatashare.scheduler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import com.onedatashare.scheduler.model.FileTransferNodeMetaData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileTransferNodeDiscovery {

    private final IMap<String, HazelcastJsonValue> nodeMap;
    private final ObjectMapper objectMapper;


    public FileTransferNodeDiscovery(IMap<String, HazelcastJsonValue> fileTransferNodeMetaDataIMap, ObjectMapper objectMapper) {
        this.nodeMap = fileTransferNodeMetaDataIMap;
        this.objectMapper = objectMapper;
    }

    public List<FileTransferNodeMetaData> getUsersFileTransferNodes(String odsUserName) {
        Predicate<String, HazelcastJsonValue> query = Predicates.equal("odsOwner", odsUserName);
        return this.nodeMap.values(query).stream().map(hazelcastJsonValue -> {
            try {
                return this.objectMapper.readValue(hazelcastJsonValue.getValue(), FileTransferNodeMetaData.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public int totalConnectedFileTransferNodes() {
        return this.nodeMap.size();
    }

}
