package com.ukora.tradestudent.services

import com.ukora.tradestudent.TradestudentApplication
import com.ukora.domain.entities.AbstractAssociation
import com.ukora.domain.entities.Memory
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AssociationService {

    @Autowired
    BytesFetcherService bytesFetcherService

    /**
     * Hydrate lesson - with all the relevant info
     *
     * @param lesson
     * @return
     */
    def <T extends AbstractAssociation> T hydrateAssociation(T someAssociation) {
        if (!someAssociation) return null
        try {
            //TODO: Get memory .. other class (to caching works)
            someAssociation.memory = bytesFetcherService.getMemory(someAssociation.date)
            if (someAssociation.memory == null) {
                Logger.log("empty memory object")
                return null
            }
            someAssociation.exchange = someAssociation.memory.exchange
            someAssociation.price = someAssociation.memory.graph.price
            someAssociation.date = someAssociation.memory.metadata.datetime
        } catch (Exception e) {
            e.printStackTrace()
        }
        if (TradestudentApplication.CONSIDER_TWITTER) {
            try {
                someAssociation.twitter = bytesFetcherService.getTwitter(someAssociation.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        if (TradestudentApplication.CONSIDER_NEWS) {
            try {
                someAssociation.news = bytesFetcherService.getNews(someAssociation.date)
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        someAssociation.intervals.each { String key ->
            Calendar calendar = Calendar.instance
            calendar.setTime(someAssociation.date)
            switch (key) {
                case '1minute':
                    calendar.add(Calendar.MINUTE, -1)
                    break
                case '2minute':
                    calendar.add(Calendar.MINUTE, -2)
                    break
                case '3minute':
                    calendar.add(Calendar.MINUTE, -3)
                    break
                case '4minute':
                    calendar.add(Calendar.MINUTE, -4)
                    break
                case '5minute':
                    calendar.add(Calendar.MINUTE, -5)
                    break
                case '6minute':
                    calendar.add(Calendar.MINUTE, -6)
                    break
                case '7minute':
                    calendar.add(Calendar.MINUTE, -7)
                    break
                case '8minute':
                    calendar.add(Calendar.MINUTE, -8)
                    break
                case '9minute':
                    calendar.add(Calendar.MINUTE, -9)
                    break
                case '10minute':
                    calendar.add(Calendar.MINUTE, -10)
                    break
                case '15minute':
                    calendar.add(Calendar.MINUTE, -15)
                    break
                case '30minute':
                    calendar.add(Calendar.MINUTE, -30)
                    break
                case '45minute':
                    calendar.add(Calendar.MINUTE, -45)
                    break
                case '1hour':
                    calendar.add(Calendar.HOUR, -1)
                    break
                case '2hour':
                    calendar.add(Calendar.HOUR, -2)
                    break
                case '4hour':
                    calendar.add(Calendar.HOUR, -4)
                    break
                case '8hour':
                    calendar.add(Calendar.HOUR, -8)
                    break
                case '16hour':
                    calendar.add(Calendar.HOUR, -16)
                    break
                case '24hour':
                    calendar.add(Calendar.HOUR, -24)
                    break
                default:
                    throw new RuntimeException(String.format("Unknown interval %s", key))
            }
            try {
                Memory thisMemory = bytesFetcherService.getMemory(calendar.time)
                someAssociation.previousMemory.put(key, thisMemory)
                try {
                    if (thisMemory?.graph?.price && someAssociation?.price && someAssociation?.price > 0) {
                        Double previousPriceProportion = thisMemory.graph.price / someAssociation.price
                        if (!previousPriceProportion.naN) {
                            someAssociation.previousPrices.put(key, previousPriceProportion)
                        }
                    } else {
                        Logger.debug("missing price cannot set previous price")
                        Logger.debug("thisMemory?.graph?.price: " + thisMemory?.graph?.price)
                        Logger.debug("someAssociation?.price: " + someAssociation?.price)
                    }
                } catch (e) {
                    e.printStackTrace()
                }
            } catch (e) {
                e.printStackTrace()
            }
            try {
                someAssociation.previousNews.put(key, bytesFetcherService.getNews(calendar.time))
            } catch (e) {
                e.printStackTrace()
            }
        }
        return someAssociation
    }

}
