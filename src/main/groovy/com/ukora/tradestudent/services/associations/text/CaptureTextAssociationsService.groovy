package com.ukora.tradestudent.services.associations.text

import com.ukora.tradestudent.entities.Lesson
import com.ukora.tradestudent.entities.News
import com.ukora.tradestudent.entities.Twitter
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CaptureTextAssociationsService {

    public static final String SEP = "/"
    public static final String SPACE = " "
    public static final String GENERAL = "general"
    public static final String INSTANT = "instant"

    public static boolean leaningEnabled = true
    public static Integer learningSpeed = 15

    public static Double PRACTICAL_ZERO = 0.000000001

    @Autowired
    BytesFetcherService bytesFetcherService

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

                    /** get news */
                    List<News> articles = bytesFetcherService.getNews(lesson.date)
                    if(!articles) Logger.log(String.format('No articles found for date %s', lesson.date))

                    /** get twitter */
                    Twitter tweets = bytesFetcherService.getTwitter(lesson.date)
                    if(!tweets) Logger.log(String.format('No tweets found for date %s', lesson.date))

                    List<String> articles_text = TextUtils.splitText(
                            articles.collect({ it.article.title + SPACE + it.article.summary}).join(SPACE)
                    )
                    List<String> twitter_text = TextUtils.splitText(
                            tweets.statuses.text
                    )
                    println articles_text
                    println twitter_text

                    



                } else {
                    Logger.debug("no lesson")
                }
            }
        }else{
            Logger.log("leaning disabled")
        }
    }

}
