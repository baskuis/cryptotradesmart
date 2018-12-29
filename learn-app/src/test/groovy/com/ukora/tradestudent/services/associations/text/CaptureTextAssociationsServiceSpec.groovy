package com.ukora.tradestudent.services.associations.text

import com.ukora.domain.entities.BrainCount
import com.ukora.domain.entities.ExtractedText
import spock.lang.Specification

class CaptureTextAssociationsServiceSpec extends Specification {

    CaptureTextAssociationsService captureTextAssociationsService = new CaptureTextAssociationsService()

    def "bump count"() {

        setup:
        String tagName = "testTag"
        BrainCount brainCount = new BrainCount(
                counters: [:]
        )

        when:
        CaptureTextAssociationsService.bumpCount(brainCount, tagName, 1)
        CaptureTextAssociationsService.bumpCount(brainCount, tagName, 2)
        CaptureTextAssociationsService.bumpCount(brainCount, tagName, 3)

        then:
        brainCount.counters.get(tagName) == 6

    }

    def "test getWordCount"() {

        setup:
        ExtractedText extractedText = new ExtractedText(extract: [
                (ExtractedText.TextSource.TWITTER): ['hi','hello','hi']
        ])
        ExtractedText.TextSource source = ExtractedText.TextSource.TWITTER

        when:
        Map r = CaptureTextAssociationsService.getWordCount(extractedText, source)

        then:
        r == ['hi': 2, 'hello': 1]

    }

}
