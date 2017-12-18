package com.ukora.tradestudent.services

import com.ukora.tradestudent.entities.SimulationResult
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.time.Instant

@Service
class SimulationResultService {

    public final Integer MAX_AGE_IN_HOURS = 48

    @Autowired
    BytesFetcherService bytesFetcherService

    /**
     * Get top performing simulation
     *
     * @return
     */
    SimulationResult getTopPerformingSimulation(){
        getTopPerformingSimulations()?.first()
    }

    /**
     * Get top performing simulations
     *
     * @return
     */
    List<SimulationResult> getTopPerformingSimulations(){
        return bytesFetcherService.getSimulations()?.findAll({
            it.endDate > Date.from(Instant.now().minusSeconds(MAX_AGE_IN_HOURS * 3600)) && it.differential > 1
        })?.sort({ SimulationResult a, SimulationResult b ->
            b.endDate <=> a.endDate
        })?.take(20)?.sort({ SimulationResult a, SimulationResult b ->
            getScore(b) <=> getScore(a)
        })
    }

    private static getScore(SimulationResult simulationResult){
        Double thresholdBalance = Math.abs(simulationResult.buyThreshold - simulationResult.sellThreshold)
        Double distanceFromBinary
        if(simulationResult.buyThreshold < simulationResult.sellThreshold){
            distanceFromBinary = 1 - simulationResult.sellThreshold
        } else {
            distanceFromBinary = 1 - simulationResult.buyThreshold
        }
        Math.pow(simulationResult.differential, distanceFromBinary / (distanceFromBinary + thresholdBalance)) *
                Date.from(Instant.now().minusNanos(simulationResult.endDate.time)).time
    }

}
