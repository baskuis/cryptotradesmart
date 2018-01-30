package com.ukora.tradestudent.services

import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap

class AliasServiceTest extends Specification {

    AliasService aliasService = new AliasService()

    def setup() {
        aliasService.beanToAlias = [
                "bean1": "alias1",
                "bean2": "alias2",
                "bean3": "alias3"
        ]
    }

    /**
     * Test that aliases are correctly replaced
     */
    def "test replaceAllWithAlias"() {

        when:
        Map r = aliasService.replaceAllWithAlias([
                bean1:[
                        bean2: [
                                'foo': 'bar',
                                'fizz': 'bean3',
                                'list': ['hi']
                        ],
                        bean3: ['foo':'bar']
                ]])

        then:
        r.alias1.alias2.fizz == 'alias3'
        r.alias1.alias2.list.contains('hi')

    }

}
