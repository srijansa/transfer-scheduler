package com.onedatashare.scheduler.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import com.onedatashare.scheduler.services.maps.CarbonMapService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController("/api/carbon")
public class CarbonEntryController {

    private final CarbonMapService carbonMapService;


    public CarbonEntryController(CarbonMapService carbonMapService) {
        this.carbonMapService = carbonMapService;
    }

    /***
     * Returns the most recent carbon information for the scheduled job.
     * @param uuid: the job UUID
     * @return: a list of the carbon intensity
     */
    @GetMapping("/latest/{uuid}")
    public ResponseEntity<Map<CarbonIntensityMapKey, List<CarbonIpEntry>>> getLatestCarbonIntensityForJob(@PathVariable String uuid) {
        try {
            return ResponseEntity.ok(this.carbonMapService.getLatestCarbonEntryForJob(UUID.fromString(uuid)));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /***
     * Returns every measurment of the scheduled job.
     * @param uuid: the job UUIID
     * @return
     */
    @GetMapping("/all/{uuid}")
    public ResponseEntity<Map<CarbonIntensityMapKey, List<CarbonIpEntry>>> getAllCarbonIntensityForJob(@PathVariable String uuid) {
        try {
            return ResponseEntity.ok(this.carbonMapService.getAllCarbonIntensityForJob(UUID.fromString(uuid)));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
