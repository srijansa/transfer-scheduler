package com.onedatashare.scheduler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.onedatashare.scheduler.enums.MessageType;
import com.onedatashare.scheduler.model.TransferJobRequest;
import com.onedatashare.scheduler.model.TransferParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class MessageSender {
    private final HazelcastInstance hazelcastInstance;
    private final ObjectMapper objectMapper;

    public MessageSender(@Qualifier("hazelcastInstance") HazelcastInstance hazelcastInstance, ObjectMapper objectMapper) {
        this.hazelcastInstance = hazelcastInstance;
        this.objectMapper = objectMapper;
    }

    public void sendMessage(Object obj, MessageType messageType) throws JsonProcessingException, InterruptedException {
        String transferNodeName = "";
        if (messageType.equals(MessageType.TRANSFER_JOB_REQUEST)) {
            TransferJobRequest transferJobRequest = (TransferJobRequest) obj;
            transferNodeName = transferJobRequest.getTransferNodeName();
        } else if (messageType.equals(MessageType.APPLICATION_PARAM_CHANGE)) {
            TransferParams transferParams = (TransferParams) obj;
            transferNodeName = transferParams.getTransferNodeName();
        }

        String jsonMsg = this.objectMapper.writeValueAsString(obj);
        JsonNode jsonNode = this.objectMapper.readTree(jsonMsg);
        ((ObjectNode) jsonNode).put("type", messageType.toString());
        IQueue<HazelcastJsonValue> iqueue = this.hazelcastInstance.getQueue(transferNodeName);
        iqueue.put(new HazelcastJsonValue(jsonNode.toString()));
    }

}