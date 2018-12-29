package com.ukora.domain.beans.bayes.language

interface LanguageAssociation {

    enum Type {Sentence,Fragment,Word}

    Type getType()
    String getTag()
    int getCount()

}
