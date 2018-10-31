package com.ukora.tradestudent.services.simulator

import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TradeSimulatorService {

    @Autowired
    BuySellTradingHistoricalSimulatorService buySellTradingHistoricalSimulatorService

    @Scheduled(cron = "0 0 * * * *")
    void runMedSimulation(){
        Instant current = Instant.now().minus(30, ChronoUnit.DAYS)
        Logger.log(String.format("Running simulation starting from %s", Date.from(current)))
        buySellTradingHistoricalSimulatorService.runSimulation(Date.from(current))
    }

}
