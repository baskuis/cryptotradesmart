package com.ukora.tradestudent.services

import com.ukora.domain.entities.*
import com.ukora.domain.repositories.*
import com.ukora.domain.beans.tags.TagGroup
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
    BrainNodeRepository brainNodeRepository
    @Autowired
    NewsRepository newsRepository
    @Autowired
    TwitterRepository twitterRepository
    @Autowired
    MemoryRepository memoryRepository

    /**
     * Get property
     *
     * @param name
     * @return
     */
    @Cacheable("properties")
    Property getProperty(String name) {
        return propertyRepository.findByName(name)
    }

    /**
     * Save property
     *
     * @param property
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProperty(Property property) {
        propertyRepository.save(property)
    }

    /**
     * Save property
     *
     * @param name
     * @param value
     */
    @CacheEvict(value = "properties", allEntries = true)
    void saveProperty(String name, String value) {
        propertyRepository.save(new Property(
                name: name,
                value: value
        ))
    }

    /**
     * Get latest simulated trade entries
     *
     * @return
     */
    List<SimulatedTradeEntry> getLatestSimulatedTradeEntries() {
        return simulatedTradeEntryRepository
                .findAll(new Sort(Sort.Direction.DESC, "date"))[0..1999]
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
        return (tradeEntries?.size() > 0 ? tradeEntries.first() : null)
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
            if (it.tag && tagGroup.tags().collect({ it.tagName }).contains(it.tag.tagName)) {
                it.count = maxCount
                brainRepository.save(it)
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
    @Cacheable("simulations")
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
        return brainCountRepository.findByReference(reference) ?: new BrainCount(
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
        return brainRepository.findByReferenceAndTag(reference, tag)?.first() ?: new Brain(
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
        brainNodeRepository.findAll().each {
            nodes.put(it.reference, it)
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
        return lessonRepository.findByTextProcessedNot(true)
    }

    /**
     * Get the next un-processed lesson
     *
     * @return
     */
    Lesson getNextLesson() {
        return lessonRepository.findByProcessedNot(true)
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
        if(r && r.size() > 0) return r.first()
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
        List<Memory> r = memoryRepository.findByMetadataDatetimeBetween(fromDate, toDate)
        if(r && r.size() > 0) return r.first()
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

}
