package com.onedatashare.scheduler.services.listeners;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryExpiredListener;
import com.onedatashare.scheduler.enums.MessageType;
import com.onedatashare.scheduler.model.TransferJobRequest;
import com.onedatashare.scheduler.services.MessageSender;
import lombok.SneakyThrows;

import java.util.UUID;

public class EntryExpiredHazelcast implements EntryExpiredListener<UUID, TransferJobRequest> {

    private final MessageSender messageSender;

    public EntryExpiredHazelcast(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @SneakyThrows
    @Override
    public void entryExpired(EntryEvent<UUID, TransferJobRequest> event) {
        messageSender.sendMessage(event.getValue(), MessageType.TRANSFER_JOB_REQUEST);
    }
}
