package com.ukora.tradestudent.services.associations.text

import com.ukora.domain.entities.ExtractedText
import com.ukora.domain.entities.News
import com.ukora.domain.entities.Twitter
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.utils.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TextExtractorService {

    public static final String SPACE = " "

    @Autowired
    BytesFetcherService bytesFetcherService

    @Cacheable("extractedText")
    ExtractedText extractTextForDate(Date date){

        /** get news */
        List<News> articles = bytesFetcherService.getNews(date)
        if(!articles) Logger.log(String.format('No articles found for date %s', date))

        /** get twitter */
        Twitter tweets = bytesFetcherService.getTwitter(date)
        if(!tweets) Logger.log(String.format('No tweets found for date %s', date))

        return new ExtractedText(
            extract: [
                (ExtractedText.TextSource.TWITTER): TextUtils.splitText(
                    tweets?.statuses?.text
                ),
                (ExtractedText.TextSource.NEWS): TextUtils.splitText(
                    articles.collect({
                        it.article?.title + SPACE + it.article?.summary
                    }).join(SPACE)
                )
            ]
        )

    }

}
