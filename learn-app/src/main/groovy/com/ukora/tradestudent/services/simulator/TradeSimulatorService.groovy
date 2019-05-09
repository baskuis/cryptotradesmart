package com.ukora.tradestudent.services.simulator

import com.ukora.domain.entities.ExtractedText
import com.ukora.tradestudent.services.simulator.combined.CombinedTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.flex.FlexTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.simulator.origin.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.strategy.trading.flex.FlexTradeExecutionStrategy
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

    @Autowired
    CombinedTradingHistoricalSimulatorService combinedTradingHistoricalSimulatorService

    @Deprecated
    void runSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running simulation starting from %s", Date.from(from)))
        buySellTradingHistoricalSimulatorService.runSimulation(Date.from(from))
    }

    @Scheduled(cron = "0 0 3 * * *")
    void runFlexSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running flex simulation starting from %s", Date.from(from)))
        flexTradingHistoricalSimulatorService.runSimulation(
                Date.from(from),
                null,
                null
        )
    }

    @Scheduled(cron = "0 0 9 * * *")
    void runTwitterFlexSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running twitter flex simulation starting from %s", Date.from(from)))
        flexTradingHistoricalSimulatorService.runSimulation(
                Date.from(from),
                FlexTradeExecutionStrategy.Type.TEXT,
                ExtractedText.TextSource.TWITTER
        )
    }

    @Scheduled(cron = "0 0 15 * * *")
    void runNewsFlexSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running news flex simulation starting from %s", Date.from(from)))
        flexTradingHistoricalSimulatorService.runSimulation(
                Date.from(from),
                FlexTradeExecutionStrategy.Type.TEXT,
                ExtractedText.TextSource.NEWS
        )
    }

    @Scheduled(cron = "0 0 21 * * *")
    void runNumericalFlexSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running numerical flex simulation starting from %s", Date.from(from)))
        flexTradingHistoricalSimulatorService.runSimulation(
                Date.from(from),
                FlexTradeExecutionStrategy.Type.NUMERIC,
                null
        )
    }

    void runCombinedSimulation() {
        Instant from = Instant.now().minus(20, ChronoUnit.DAYS)
        Logger.log(String.format("Running combined simulation starting from %s", Date.from(from)))
        combinedTradingHistoricalSimulatorService.runSimulation(Date.from(from))
    }

}
