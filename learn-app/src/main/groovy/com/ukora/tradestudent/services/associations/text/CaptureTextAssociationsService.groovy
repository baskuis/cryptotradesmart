package com.ukora.tradestudent.services.associations.text

import com.ukora.domain.entities.BrainCount
import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.Lesson
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.LessonContainer
import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CaptureTextAssociationsService {

    public static final String SEP = "/"
    public static final String GENERAL = "general"

    public static boolean leaningEnabled = true
    public static Integer learningSpeed = 10

    public final static Integer numCores = Runtime.getRuntime().availableProcessors()

    static boolean busy = false

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    TextExtractorService textExtractorService

    @Autowired
    LessonContainer lessonContainer

    @Autowired
    TagService tagService

    @Scheduled(initialDelay = 40000l, fixedRate = 600000l)
    def reset() {
        busy = false
    }

    /**
     * Main schedule to digest lessons
     * and create associations
     *
     */
    @Scheduled(initialDelay = 40000l, fixedRate = 1000l)
    @Async
    void learn(){
        if(leaningEnabled) {
            if(busy) {
                Logger.debug('Still learning skipping')
                return
            }
            busy = true
            learningSpeed.times {
                int n = it
                List<Thread> threads = []
                numCores.times {
                    int c = it
                    threads.add Thread.start {
                        Logger.debug(String.format("capturing text lesson %s, core %s", n, c))
                        Lesson lesson = lessonContainer.getNextTextLesson()
                        if (lesson) {

                            /** Mark as processed */
                            lesson.textProcessed = true
                            bytesFetcherService.saveLesson(lesson)

                            /** Extract text for date */
                            ExtractedText extractedText = textExtractorService.extractTextForDate(lesson.date)

                            /** Capture news */
                            captureText(
                                    ExtractedText.TextSource.NEWS,
                                    getWordCount(extractedText, ExtractedText.TextSource.NEWS),
                                    lesson
                            )

                            /** Capture twitter */
                            captureText(
                                    ExtractedText.TextSource.TWITTER,
                                    getWordCount(extractedText, ExtractedText.TextSource.TWITTER),
                                    lesson
                            )

                        } else {
                            Logger.debug("no text lesson")
                        }
                    }
                }
                threads*.join()
            }
            busy = false
        }else{
            Logger.log("text leaning disabled")
        }
    }

    /**
     * Capture text
     *
     * @param source
     * @param keywords
     * @param lesson
     */
    void captureText(
            ExtractedText.TextSource source,
            Map<String, Integer> keywords,
            Lesson lesson
    ){

        /** capture articles keywords general/tag associations */
        keywords.each {
            BrainCount brainCount = bytesFetcherService.getBrainCount(
                    generateReference(
                            it.key,
                            source as String),
                    source as String
            )
            bumpCount(brainCount, lesson.tag, it.value)
            bumpCount(brainCount, tagService.getTagGroupByTagName(lesson.tag)?.name, it.value)
            bumpCount(brainCount, GENERAL, it.value)
            bytesFetcherService.saveBrainCount(brainCount)
        }

    }

    /**
     * Bump count
     *
     * @param brainCount
     * @param name
     */
    static void bumpCount(BrainCount brainCount, String name, Integer increment){
        brainCount.counters.put(name, brainCount.counters.getOrDefault(name, 0) + increment)
    }

    /**
     * Get word count
     *
     * @param extractedText
     * @param source
     * @return
     */
    static Map<String, Integer> getWordCount(ExtractedText extractedText, ExtractedText.TextSource source){
        Map<String, Integer> words = [:]
        extractedText.extract(source).each { words.put(it, words.getOrDefault(it, 0) + 1) }
        return words
    }

    /**
     * Generate reference
     *
     * @param word
     * @param tag
     * @param source
     * @return
     */
    static generateReference(String word, String source){
        return source + SEP + word
    }

}
