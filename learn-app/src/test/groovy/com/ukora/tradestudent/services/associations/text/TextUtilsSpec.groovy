package com.ukora.tradestudent.services.associations.text

import spock.lang.Specification
import spock.lang.Unroll


class TextUtilsSpec extends Specification {

    @Unroll
    def "splitText #scenario"() {

        when:
        def r = TextUtils.splitText(text)

        then:
        r == expected

        where:
        scenario              | text      | expected
        'simple whitespace'   | 'a word'  | ['a', 'word']
        'special character'   | 'a,word'  | ['a', 'word']
        'combined characters' | 'a, word' | ['a', 'word']

    }

}
