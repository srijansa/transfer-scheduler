package com.onedatashare.scheduler.controller;

import com.onedatashare.scheduler.model.FileTransferNodeMetaData;
import com.onedatashare.scheduler.services.FileTransferNodeDiscovery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/api/nodes")
public class FileTransferNodeController {


    private final FileTransferNodeDiscovery fileTransferNodeDiscovery;

    public FileTransferNodeController(FileTransferNodeDiscovery fileTransferNodeDiscovery) {
        this.fileTransferNodeDiscovery = fileTransferNodeDiscovery;
    }

    @GetMapping("/{user}")
    public List<FileTransferNodeMetaData> getUserOdsConnectors(@PathVariable("user") String user) {
        return this.fileTransferNodeDiscovery.getUsersFileTransferNodes(user);
    }

    @GetMapping("/ods")
    public List<FileTransferNodeMetaData> getOdsNodes() {
        return this.fileTransferNodeDiscovery.getOdsNodes();
    }
}
