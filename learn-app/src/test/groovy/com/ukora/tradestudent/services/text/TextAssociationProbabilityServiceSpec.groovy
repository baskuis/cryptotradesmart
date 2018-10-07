package com.ukora.tradestudent.services.text

import spock.lang.Specification

class TextAssociationProbabilityServiceSpec extends Specification {

    def "simple unique test"() {
        expect:
        ['hi', 'hello', 'hi'].unique() == ['hi', 'hello']
    }

}
