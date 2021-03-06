package com.ukora.domain.beans.bayes.numbers

class NumberAssociationProbability extends NumberAssociation {

    Double probability

    NumberAssociationProbability(NumberAssociation numberAssociation){
        this.tag = numberAssociation.tag
        this.mean = numberAssociation.mean
        this.standard_deviation = numberAssociation.standard_deviation
        this.count = numberAssociation.count
        this.relevance = numberAssociation.relevance
    }

    NumberAssociationProbability(String tag, Double mean, Double standard_deviation, Integer count){
        this.tag = tag
        this.mean = mean
        this.standard_deviation = standard_deviation
        this.count = count
    }

}
