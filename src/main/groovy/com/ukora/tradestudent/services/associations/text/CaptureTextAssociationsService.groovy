package com.ukora.tradestudent.services.associations.text

import com.ukora.tradestudent.entities.BrainCount
import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.services.BytesFetcherService
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
    public static Integer learningSpeed = 15

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    TextExtractorService textExtractorService

    /**
     * Main schedule to digest lessons
     * and create associations
     *
     */
    @Scheduled(cron = "*/10 * * * * *")
    @Async
    void learn(){
        if(leaningEnabled) {
            learningSpeed.times {
                Logger.debug(String.format("capturing text lesson %s", it))
                Lesson lesson = bytesFetcherService.getNextTextLesson()
                if (lesson) {

                    /** Extract text for date */
                    ExtractedText extractedText = textExtractorService.extractTextForDate(lesson.date)

                    /** Capture news */
                    captureText(
                        ExtractedText.TextSource.NEWS,
                        extractedText.extract(ExtractedText.TextSource.NEWS),
                        lesson
                    )

                    /** Capture twitter */
                    captureText(
                            ExtractedText.TextSource.TWITTER,
                            extractedText.extract(ExtractedText.TextSource.TWITTER),
                            lesson
                    )

                } else {
                    Logger.debug("no text lesson")
                }
            }
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
            List<String> keywords,
            Lesson lesson
    ){

        /** capture articles keywords general/tag associations */
        keywords.each {

            /*******************************************************/
            /********************** TAG ****************************/
            /*******************************************************/
            BrainCount brainCount = bytesFetcherService.getBrainCount(
                    generateReference(
                            it,
                            lesson.tag.getTagName(),
                            source as String),
                    lesson.tag.getTagName(),
                    source as String
            )
            brainCount.count++
            bytesFetcherService.saveBrainCount(brainCount)

            /*******************************************************/
            /********************** GENERAL ************************/
            /*******************************************************/
            BrainCount generalBrainCount = bytesFetcherService.getBrainCount(
                    generateReference(
                            it,
                            GENERAL,
                            source as String),
                    GENERAL,
                    source as String
            )
            generalBrainCount.count++
            bytesFetcherService.saveBrainCount(generalBrainCount)

        }

    }

    /**
     * Generate reference
     *
     * @param word
     * @param tag
     * @param source
     * @return
     */
    static generateReference(String word, String tag, String source){
        return source + SEP + tag + SEP + word
    }

}
