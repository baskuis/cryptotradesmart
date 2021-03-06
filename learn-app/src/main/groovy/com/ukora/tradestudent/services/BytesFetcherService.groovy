package com.ukora.tradestudent.services

import com.ukora.domain.beans.bayes.numbers.NumberAssociation
import com.ukora.domain.beans.tags.TagGroup
import com.ukora.domain.entities.*
import com.ukora.domain.repositories.*
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class BytesFetcherService {

    @Autowired
    PropertyRepository propertyRepository
    @Autowired
    LessonRepository lessonRepository
    @Autowired
    SimulationResultRepository simulationResultRepository
    @Autowired
    SimulatedTradeEntryRepository simulatedTradeEntryRepository
    @Autowired
    BrainRepository brainRepository
    @Autowired
    BrainCountRepository brainCountRepository
    @Autowired
    NewsRepository newsRepository
    @Autowired
    TwitterRepository twitterRepository
    @Autowired
    MemoryRepository memoryRepository
    @Autowired
    CorrelationAssociationRepository correlationAssociationRepository
    @Autowired
    TextCorrelationAssociationRepository textCorrelationAssociationRepository

    @Autowired
    TagService tagService

    /**
     * Get property
     *
     * @param name
     * @return
     */
    @Cacheable("properties")
    Property getProp(String name) {
        List<Property> properties = propertyRepository.findByName(name)
        if (properties.size() > 0) {
            return properties.get(0)
        }
        null
    }

    /**
     * Save property
     *
     * @param property
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProp(Property property) {
        Property p = this.getProp(property.name)
        if (!p) {
            p = property
        }
        p.value = property.value
        propertyRepository.save(p)
    }

    /**
     * Save property
     *
     * @param name
     * @param value
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProp(String name, String value) {
        Property p = this.getProp(name)
        if (!p) {
            p = new Property(name: name)
        }
        p.value = value
        propertyRepository.save(p)
    }

    /**
     * Get latest simulated trade entries
     *
     * @return
     */
    List<SimulatedTradeEntry> getLatestSimulatedTradeEntries() {
        return simulatedTradeEntryRepository
                .findAll(new Sort(Sort.Direction.DESC, "date")).take(2000)
                .sort({ SimulatedTradeEntry a, SimulatedTradeEntry b ->
            a.getDate() <=> b.getDate()
        })
    }

    /**
     * Get latest simulated trade entry
     *
     * @return
     */
    SimulatedTradeEntry getLatestSimulatedTradeEntry() {
        List<SimulatedTradeEntry> tradeEntries = simulatedTradeEntryRepository.findAll(
                new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "date"))
        )?.getContent()
        return (tradeEntries && tradeEntries?.size() > 0 ? tradeEntries.first() : null)
    }

    /**
     * Get latest memory
     *
     * @return
     */
    Memory getLatestMemory() {
        List<Memory> memories = memoryRepository.findAll(
                new PageRequest(0, 1, new Sort(Sort.Direction.DESC, "metadata.datetime"))
        )?.getContent()
        return (memories && memories?.size() > 0 ? memories.first() : null)
    }

    /**
     * Insert simulation trade entry
     *
     * @param simulatedTradeEntry
     */
    void insertSimulatedTradeEntry(SimulatedTradeEntry simulatedTradeEntry) {
        simulatedTradeEntryRepository.insert(simulatedTradeEntry)
    }

    /**
     * Flush brain collection
     *
     */
    @CacheEvict(value = "brainNodes", allEntries = true)
    void whiskeyBender() {
        brainRepository.deleteAll()
    }

    /**
     * Set ceiling on brain nodes
     *
     * @param tagGroup
     * @param maxCount
     */
    @CacheEvict(value = "brainNodes", allEntries = true)
    void resetBrainNodesCount(TagGroup tagGroup, int maxCount) {
        brainRepository.findAll().each {
            if (it.tag && tagGroup.tags().collect({ it.tagName }).contains(it.tag)) {
                if (it.count > maxCount) {
                    it.count = maxCount
                    brainRepository.save(it)
                }
            }
        }
    }

    /**
     * Evict all entries from cache
     *
     */
    @CacheEvict(value = [
            "news",
            "twitter",
            "memory",
            "brainNodes",
            "simulations",
            "properties",
            "associations",
            "brainCount"
    ], allEntries = true)
    void flushCache() {}

    /**
     * Get all simulations
     *
     * @return
     */
    List<SimulationResult> getSimulations() {
        return simulationResultRepository.findAll()
    }

    /**
     * Persist a simulation
     *
     * @param simulation
     */
    @CacheEvict(value = "simulations", allEntries = true)
    void saveSimulation(SimulationResult simulation) {
        simulationResultRepository.save(simulation)
    }

    /**
     * Get brain count
     *
     * @param reference
     * @param tag
     * @param source
     * @return
     */
    @Cacheable("brainCount")
    BrainCount getBrainCount(String reference, String source) {
        List<BrainCount> brainCountList = brainCountRepository.findByReference(reference);
        if (brainCountList && brainCountList.size() > 0) {
            return brainCountList.first()
        }
        return new BrainCount(
                id: null,
                source: source,
                reference: reference,
                counters: [:]
        )
    }

    /**
     * Save brain count
     *
     * @param brainCount
     */
    @CacheEvict(value = "brainCount", allEntries = true)
    void saveBrainCount(BrainCount brainCount) {
        brainCountRepository.save(brainCount)
    }

    /**
     * Return existing or new number association object
     *
     * @param reference
     * @return
     */
    Brain getBrain(String reference, String tag) {
        List<Brain> brainList = brainRepository.findByReferenceAndTag(reference, tag)
        if (brainList && brainList.size() > 0) {
            return brainList.first()
        }
        return new Brain(
                id: null,
                tag: tag,
                reference: reference,
                mean: 0 as Double,
                count: 0 as Integer,
                standard_deviation: 0 as Double
        )
    }

    /**
     * Save a number association
     *
     * @param brain
     */
    @CacheEvict(value = [
            "brainNodes",
            "associations"
    ], allEntries = true)
    void saveBrain(Brain brain) {
        brainRepository.save(brain)
    }

    /**
     * Update lessons - mark them to be up for processing once more
     *
     */
    void resetLessons() {
        lessonRepository.findAll().each {
            it.processed = false
            it.textProcessed = false
            lessonRepository.save(it)
        }
    }

    /**
     * Return all brain nodes
     *
     * @return
     */
    @Cacheable("brainNodes")
    Map<String, BrainNode> getAllBrainNodes() {
        Map<String, BrainNode> nodes = [:]
        brainRepository.findAll().each { Brain b ->
            String tagGroupName = tagService?.tagGroupMap?.find {
                (it.value.tags().find { it.getTagName() == b.tag })
            }?.value?.getName()
            if (!tagGroupName) {
                Logger.log(String.format('Unable to find tagGroupName from tag[%s], tagGroupMap.keys[%s]', b.tag, this.tagService.tagGroupMap?.keySet()))
            }
            nodes.get(b.reference, new BrainNode(reference: b.reference)).tagReference.put(b.tag,
                    new NumberAssociation(
                            tagGroup: tagGroupName,
                            tag: b.tag,
                            mean: b.mean,
                            count: b.count,
                            standard_deviation: b.standard_deviation
                    )
            )
        }
        return nodes
    }

    /**
     * Get lesson count
     *
     * @param tag
     * @return
     */
    Long getLessonCount(String tag) {
        return lessonRepository.countByTag(tag)
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextTextLesson() {
        List<Lesson> unprocessedLessons = lessonRepository.findByTextProcessedNot(true)
        if (unprocessedLessons && unprocessedLessons.size() > 0) {
            return unprocessedLessons.get(0)
        }
        return null
    }

    /**
     * Get unprocessed lessons
     *
     * @return
     */
    List<Lesson> unproccessedLessons() {
        return lessonRepository.findByProcessedNot(true)
    }

    /**
     * Unprocessed text lessons
     *
     * @return
     */
    List<Lesson> unproccessedTextLessons() {
        return lessonRepository.findByTextProcessedNot(true)
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextLesson() {
        List<Lesson> unprocessedLessons = lessonRepository.findByProcessedNot(true)
        if (unprocessedLessons && unprocessedLessons.size() > 0) {
            return unprocessedLessons.get(0)
        }
        return null
    }

    /**
     * Save lesson
     *
     * @param lesson
     */
    void saveLesson(Lesson lesson) {
        lessonRepository.save(lesson)
    }

    /**
     * Get relevant news snapshot
     *
     * @param newsDate
     * @return
     */
    @Cacheable("news")
    List<News> getNews(Date newsDate) {
        Calendar calendar = Calendar.instance
        calendar.setTime(newsDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        return newsRepository.findByMetadataDatetimeBetween(fromDate, toDate)
    }

    /**
     * Get relevant twitter chatter
     *
     * @param twitterDate
     * @return
     */
    @Cacheable("twitter")
    Twitter getTwitter(Date twitterDate) {
        Calendar calendar = Calendar.instance
        calendar.setTime(twitterDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        def r = twitterRepository.findByMetadataDatetimeBetween(fromDate, toDate)
        if (r && r.size() > 0) return r.first()
        return null
    }

    /**
     * Get marketplace snapshot
     * from memory collection
     *
     * @param memoryDate
     * @return
     */
    @Cacheable("memory")
    Memory getMemory(Date memoryDate) {
        Calendar calendar = Calendar.instance
        calendar.setTime(memoryDate)
        calendar.add(Calendar.SECOND, -45)
        Date fromDate = calendar.time
        calendar.add(Calendar.SECOND, 90)
        Date toDate = calendar.time
        List<Memory> r = memoryRepository.findByMetadataDatetimeBetween(
                fromDate,
                toDate
        )
        if (r && r.size() > 0) return r.find({
            it?.exchange?.exchange == 'COINBASEPRO' &&
                    it?.exchange?.details?.tradecurrency == 'BTC' &&
                    it?.exchange?.details?.pricecurrency == 'USD'
        })
        return null
    }

    /**
     * Insert memory
     *
     *
     * @param the_memory
     */
    void insertMemory(Memory the_memory) {
        memoryRepository.insert(the_memory)
    }

    /**
     * Capture correlation association
     *
     * @param CorrelationAssociation correlationAssociation
     */
    void captureCorrelationAssociation(CorrelationAssociation correlationAssociation) {
        def r = correlationAssociationRepository.findByDate(correlationAssociation?.memory?.metadata?.datetime)
        CorrelationAssociation existing = r.size() > 0 ? r.first() : null
        if (existing) {
            existing.tagProbabilities = correlationAssociation.tagProbabilities
            existing.tagScores = correlationAssociation.tagScores
            existing.tagProbabilities = correlationAssociation.tagProbabilities
            existing.date = correlationAssociation?.memory?.metadata?.datetime
            correlationAssociationRepository.save(existing)
        } else {
            correlationAssociation.date = correlationAssociation?.memory?.metadata?.datetime
            correlationAssociationRepository.save(correlationAssociation)
        }
    }

    /**
     * Capture text correlation association
     *
     * @param TextCorrelationAssociation textCorrelationAssociation
     */
    void captureTextCorrelationAssociation(TextCorrelationAssociation textCorrelationAssociation) {
        def r = textCorrelationAssociationRepository.findByDate(textCorrelationAssociation?.date)
        TextCorrelationAssociation existing = r.size() > 0 ? r.first() : null
        if (existing) {
            existing.strategyProbabilities = textCorrelationAssociation.strategyProbabilities
            textCorrelationAssociationRepository.save(existing)
        } else {
            textCorrelationAssociationRepository.save(textCorrelationAssociation)
        }
    }

}
