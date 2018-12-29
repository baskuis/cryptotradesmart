package com.ukora.tradestudent.strategy.text.probablitity.impl

import com.ukora.domain.entities.KeywordAssociation
import com.ukora.tradestudent.strategy.text.probablitity.TextProbabilityCombinerStrategy
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.stereotype.Component

@Component
class AverageTextProbabilityCombinerStrategy implements TextProbabilityCombinerStrategy {

    @Override
    String getAlias() {
        return "pierre"
    }

    boolean enabled = true

    @Override
    boolean isEnabled() {
        return enabled
    }

    @Override
    Double combineProbabilities(String tag, Map<String, KeywordAssociation> keywordAssociationProbabilities) {
        Double total = 0
        keywordAssociationProbabilities.each {
            if(!it.value.tagProbabilities.get(tag) || !NerdUtils.assertRange(it.value.tagProbabilities.get(tag))) return
            total += (1 + it.value.tagProbabilities.get(tag)) / 2
        }
        return total / keywordAssociationProbabilities.size()
    }

}
