package com.ukora.domain.entities

import org.springframework.data.annotation.Id

class SimulationResult {

    @Id
    String id

    enum ExecutionType {
        BASIC, FLEX
    }

    ExecutionType executionType = ExecutionType.BASIC
    Exchange exchange
    Metadata metadata = new Metadata(
            datetime: new Date(),
            hostname: InetAddress.getLocalHost().getHostName()
    )
    Date startDate
    Date endDate
    String probabilityCombinerStrategy
    String tradeExecutionStrategy
    Double tradeIncrement
    Double buyThreshold
    Double sellThreshold
    Double differential
    Integer tradeCount
    Double totalValue

    Double numericalWeight
    Double textNewsWeight
    Double textTwitterWeight

    Map<String, Double> tagGroupWeights = [:]

}
