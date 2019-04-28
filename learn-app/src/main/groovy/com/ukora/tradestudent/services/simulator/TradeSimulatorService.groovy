package com.ukora.tradestudent.services.simulator

import com.ukora.tradestudent.services.simulator.flex.FlexTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.origin.BuySellTradingHistoricalSimulatorService
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

    @Autowired
    FlexTradingHistoricalSimulatorService flexTradingHistoricalSimulatorService

    @Scheduled(cron = "0 0 0,12 * * *")
    void runSimulation(){
        Instant current = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running simulation starting from %s", Date.from(current)))
        buySellTradingHistoricalSimulatorService.runSimulation(Date.from(current))
    }

    @Scheduled(cron = "0 0 3,15 * * *")
    void runFlexSimulation() {
        Instant current = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running simulation starting from %s", Date.from(current)))
        flexTradingHistoricalSimulatorService.runSimulation(Date.from(current))
    }

}
