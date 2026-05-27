package com.lab.scheduled;

import com.lab.service.PredictionTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 故障预测定时任务
 */
@Component
@Slf4j
public class FaultPredictionScheduler {
    
    private final PredictionTaskService predictionTaskService;
    
    public FaultPredictionScheduler(PredictionTaskService predictionTaskService) {
        this.predictionTaskService = predictionTaskService;
    }
    
    /**
     * 每天凌晨 0 点执行故障预测
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyFaultPrediction() {
        log.info("⏰ 触发每日故障预测定时任务");
        predictionTaskService.dailyFaultPrediction();
    }
}
