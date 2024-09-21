package com.onedatashare.scheduler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.onedatashare.scheduler.enums.MessageType;
import com.onedatashare.scheduler.model.CarbonIntensityMapKey;
import com.onedatashare.scheduler.model.TransferJobRequest;
import com.onedatashare.scheduler.model.TransferSla;
import com.onedatashare.scheduler.model.carbon.CarbonIpEntry;
import com.onedatashare.scheduler.services.maps.CarbonMapService;
import com.onedatashare.scheduler.services.maps.InitialJobCarbonMapService;
import com.onedatashare.scheduler.services.maps.TransferSchedulerMapService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JobSchedulerAlgo {

    private final CarbonMapService carbonIntensityMap;
    private final TransferSchedulerMapService transferSchedulerMapService;
    private final InitialJobCarbonMapService initialJobCarbonMapService;
    private final MessageSender messageSender;

    public JobSchedulerAlgo(InitialJobCarbonMapService initialJobCarbonMapService, TransferSchedulerMapService transferSchedulerMapService, CarbonMapService carbonMapService, MessageSender messageSender) {
        this.transferSchedulerMapService = transferSchedulerMapService;
        this.carbonIntensityMap = carbonMapService;
        this.initialJobCarbonMapService = initialJobCarbonMapService;
        this.messageSender = messageSender;
    }

    @Async
    @Scheduled(cron = "* 0/10 * * * *")
    public void jobScheduler() throws JsonProcessingException, InterruptedException {
        //iterate over potential jobs and see which meet the carbon sla
        for (UUID uuid : this.transferSchedulerMapService.getKeys()) {
            TransferJobRequest scheduledFileTransfer = this.transferSchedulerMapService.getValue(uuid);
            //get all carbon measurements pertaining to job done by all free nodes.
            Set<CarbonIntensityMapKey> carbonKeysOfAllPossibleNodes = this.carbonIntensityMap.getCarbonMeasurementsForUserAndJob(scheduledFileTransfer.getOwnerId(), scheduledFileTransfer.getJobUuid());
            double minCi = Double.MAX_VALUE;
            CarbonIntensityMapKey minCarbonKey = null;
            List<CarbonIpEntry> minValues = null;

            //iterate over all possible options for 1 job and find the node with the lowest carbon intensity
            if (carbonKeysOfAllPossibleNodes.isEmpty()) {
                continue;
            }

            //find node with lowest ci to run the job
            for (CarbonIntensityMapKey carbonKey : carbonKeysOfAllPossibleNodes) {
                List<CarbonIpEntry> carbonIpForNode = this.carbonIntensityMap.getValueForKey(carbonKey);
                double currentCi = this.averageCarbonIntensity(carbonIpForNode);
                if (minCi > currentCi) {
                    minCi = currentCi;
                    minCarbonKey = carbonKey;
                    minValues = carbonIpForNode;
                }
            }

            //if this is the first min measurement, store it in map and try to find lower.
            if (!this.initialJobCarbonMapService.containsKey(minCarbonKey)) {
                this.initialJobCarbonMapService.putCarbonKeyValue(minCarbonKey, minValues);
            }

            TransferSla sla = scheduledFileTransfer.getTransferSla();
            double initialScheduledJobCi = this.initialJobCarbonMapService.carbonIntensityForKey(minCarbonKey);
            double targetCi = initialScheduledJobCi * (1 - sla.getPercentCarbon());

            if (minCi <= targetCi) {
                //we launch the job
                scheduledFileTransfer.setTransferNodeName(minCarbonKey.getTransferNodeName());
                this.messageSender.sendMessage(scheduledFileTransfer, MessageType.TRANSFER_JOB_REQUEST);
                this.transferSchedulerMapService.deleteJob(scheduledFileTransfer.getJobUuid());
            }

        }
    }

    public double averageCarbonIntensity(List<CarbonIpEntry> carbonIpEntryList) {
        return carbonIpEntryList.stream().mapToDouble(CarbonIpEntry::getCarbonIntensity).average().getAsDouble();
    }
}
