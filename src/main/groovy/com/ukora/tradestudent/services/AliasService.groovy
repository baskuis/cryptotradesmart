package com.ukora.tradestudent.services

import com.ukora.tradestudent.strategy.probability.ProbabilityCombinerStrategy
import com.ukora.tradestudent.strategy.trading.TradeExecutionStrategy
import jdk.nashorn.internal.runtime.ECMAException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

/**
 * This is a convenience service to translate beans to aliases
 *
 */
@Service
class AliasService {

    static final String NO_ALIAS = 'no alias'
    static final String NO_BEAN_NAME = 'no bean'

    Map<String, String> beanToAlias = new ConcurrentHashMap()
    Map<String, String> aliasToBean = new ConcurrentHashMap()

    @Autowired
    ApplicationContext applicationContext

    @PostConstruct
    void init() {

        /** Load aliases for p combiners */
        applicationContext.getBeansOfType(ProbabilityCombinerStrategy).each {
            beanToAlias.put(it.key, it.value.alias)
            aliasToBean.put(it.value.alias, it.key)
        }

        /** Load aliases for trade executor strategies */
        applicationContext.getBeansOfType(TradeExecutionStrategy).each {
            beanToAlias.put(it.key, it.value.alias)
            aliasToBean.put(it.value.alias, it.key)
        }

    }

    Map<String, String> getBeanToAlias() {
        return beanToAlias
    }

    Map<String, String> getAliasToBean() {
        return aliasToBean
    }

    String getAliasByBeanName(String beanName){
        return beanToAlias.getOrDefault(beanName, NO_ALIAS)
    }

    String getBeanNameByAlias(String alias){
        return aliasToBean.getOrDefault(alias, NO_BEAN_NAME)
    }

}
