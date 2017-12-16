package com.ukora.tradestudent.strategy.probability

import com.ukora.tradestudent.bayes.numbers.NumberAssociationProbability

interface ProbabilityCombinerStrategy {

    void setEnabled(boolean enabled)

    boolean isEnabled()

    Double combineProbabilities(String tag, Map<String, Map<String, NumberAssociationProbability>> numberAssociationProbabilities)

}