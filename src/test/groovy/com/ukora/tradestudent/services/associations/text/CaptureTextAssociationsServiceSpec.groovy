package com.ukora.tradestudent.services.associations.text

import com.ukora.tradestudent.entities.BrainCount
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
        CaptureTextAssociationsService.bumpCount(brainCount, tagName)
        CaptureTextAssociationsService.bumpCount(brainCount, tagName)
        CaptureTextAssociationsService.bumpCount(brainCount, tagName)

        then:
        brainCount.counters.get(tagName) == 3

    }


}
