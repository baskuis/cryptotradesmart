package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class SimulationResultService {

    public final Integer MAX_AGE_IN_HOURS = 48

    @Autowired
    BytesFetcherService bytesFetcherService

    static final Integer DISTANCE_FROM_BINARY_SOFTENING_FACTOR = 5
    static final Integer THRESHOLD_BALANCE_SOFTENING_FACTOR = 3
    static final Double MINIMUM_DIFFERENTIAL = 1

    /**
     * Get top performing simulation
     *
     * @return
     */
    SimulationResult getTopPerformingSimulation() {
        List<SimulationResult> r = getTopPerformingSimulations()
        r ? r.first() : null
    }

    /**
     * Get top performing simulations
     *
     * @return
     */
    List<SimulationResult> getTopPerformingSimulations() {
        return bytesFetcherService.getSimulations()?.findAll({
            it.endDate > Date.from(Instant.now().minusSeconds(MAX_AGE_IN_HOURS * 3600)) && it.differential > MINIMUM_DIFFERENTIAL
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(20)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
    }

    /**
     * Sorting function
     *
     * @param simulationResult
     * @return
     */
    static getScore(SimulationResult simulationResult) {
        Double thresholdBalance = Math.abs(simulationResult.buyThreshold - simulationResult.sellThreshold)
        Double distanceFromBinary
        if (simulationResult.buyThreshold < simulationResult.sellThreshold) {
            distanceFromBinary = 1 - simulationResult.sellThreshold
        } else {
            distanceFromBinary = 1 - simulationResult.buyThreshold
        }
        (1 - (distanceFromBinary / DISTANCE_FROM_BINARY_SOFTENING_FACTOR)) *
                (1 - (thresholdBalance / THRESHOLD_BALANCE_SOFTENING_FACTOR)) *
                Math.pow(
                        simulationResult.differential /
                                timeDeltaIn(simulationResult.startDate, simulationResult.endDate, ChronoUnit.DAYS)
                        , 2)
    }

    /**
     * Get time delta
     *
     * @param from
     * @param to
     * @param unit
     * @return
     */
    static Double timeDeltaIn(Date from, Date to, ChronoUnit unit) {
        Double diff = (from && to) ? Math.abs(to.time - from.time) : 0
        switch (unit) {
            case ChronoUnit.DAYS:
                return diff / 86400000
            case ChronoUnit.HOURS:
                return diff / 3600000
            case ChronoUnit.SECONDS:
                return diff / 1000
            case ChronoUnit.MILLIS:
                return diff
        }
        return null
    }

}
