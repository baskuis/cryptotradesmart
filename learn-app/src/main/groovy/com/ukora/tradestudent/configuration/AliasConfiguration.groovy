package com.ukora.tradestudent.configuration

import com.ukora.tradestudent.filter.UseAliasesFilter
import com.ukora.tradestudent.services.AliasService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AliasConfiguration {

    @Autowired
    AliasService aliasService

    @Bean
    UseAliasesFilter useAliasesFilter() {
        return new UseAliasesFilter(aliasService)
    }

}
