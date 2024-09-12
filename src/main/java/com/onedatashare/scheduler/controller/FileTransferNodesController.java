package com.onedatashare.scheduler.controller;

import com.onedatashare.scheduler.model.FileTransferNodeMetaData;
import com.onedatashare.scheduler.services.FileTransferNodeDiscovery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/api/nodes")
public class FileTransferNodesController {

    private final FileTransferNodeDiscovery fileTransferNodeDiscovery;

    public FileTransferNodesController(FileTransferNodeDiscovery fileTransferNodeDiscovery) {
        this.fileTransferNodeDiscovery = fileTransferNodeDiscovery;
    }

    @GetMapping()
    public List<FileTransferNodeMetaData> getFileTransferNodes(@RequestParam(required = false, defaultValue = "") String odsUserName) {
        return this.fileTransferNodeDiscovery.getUsersFileTransferNodes(odsUserName);
    }


    @GetMapping("/count")
    public ResponseEntity<Integer> totalConnectedFileTransferNodes(){
        return ResponseEntity.ok(this.fileTransferNodeDiscovery.totalConnectedFileTransferNodes());
    }

}
