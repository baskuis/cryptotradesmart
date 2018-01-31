package com.ukora.tradestudent.services

import spock.lang.Specification

class AliasServiceTest extends Specification {

    AliasService aliasService = new AliasService()

    def setup() {
        aliasService.beanToAlias = [
                "bean1": "alias1",
                "bean2": "alias2",
                "bean3": "alias3"
        ]
    }

}
