package com.ukora.tradestudent.services

import com.ukora.tradestudent.tags.buysell.BuySellTagGroup
import com.ukora.tradestudent.tags.reversal.UpDownReversalTagGroup
import com.ukora.tradestudent.tags.trend.UpDownTagGroup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MemoryLimitService {

    final static MAX_BUY_SELL_COUNT = 50000
    final static MAX_UP_DOWN_COUNT = 400000
    final static MAX_REVERSAL_COUNT = 100000

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    BuySellTagGroup buySellTagGroup

    @Autowired
    UpDownTagGroup upDownTagGroup

    @Autowired
    UpDownReversalTagGroup upDownReversalTagGroup

    @Scheduled(cron = "0 0 * * * *")
    def resetCounts(){
        bytesFetcherService.resetBrainNodesCount(buySellTagGroup, MAX_BUY_SELL_COUNT)
        bytesFetcherService.resetBrainNodesCount(upDownTagGroup, MAX_UP_DOWN_COUNT)
        bytesFetcherService.resetBrainNodesCount(upDownReversalTagGroup, MAX_REVERSAL_COUNT)
    }

}
