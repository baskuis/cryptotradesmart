package com.ukora.tradestudent.strategy.probability

import com.ukora.domain.beans.bayes.numbers.NumberAssociationProbability

interface ProbabilityCombinerStrategy {

    /**
     * Indicates alias for this algorithm
     *
     * @return
     */
    String getAlias()

    void setEnabled(boolean enabled)

    boolean isEnabled()

    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities)

}