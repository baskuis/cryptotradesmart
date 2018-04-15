package com.ukora.tradestudent.strategy.text.probablitity

import com.ukora.tradestudent.entities.KeywordAssociation

interface TextProbabilityCombinerStrategy {

    /**
     * Indicates alias for this algorithm
     *
     * @return
     */
    String getAlias()

    void setEnabled(boolean enabled)

    boolean isEnabled()

    Double combineProbabilities(String tag, Map<String, KeywordAssociation> keywordAssociationProbabilities)

}