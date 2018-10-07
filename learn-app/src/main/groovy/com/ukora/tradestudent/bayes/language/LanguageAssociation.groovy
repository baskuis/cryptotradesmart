package com.ukora.tradestudent.bayes.language

interface LanguageAssociation {

    enum Type {Sentence,Fragment,Word}

    Type getType()
    String getTag()
    int getCount()

}
