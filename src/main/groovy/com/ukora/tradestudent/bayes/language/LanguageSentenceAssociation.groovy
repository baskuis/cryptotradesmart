package com.ukora.tradestudent.bayes.language

class LanguageSentenceAssociation implements LanguageAssociation {

    @Override
    LanguageAssociation.Type getType() {
        return LanguageAssociation.Type.Sentence
    }

    @Override
    String getTag() {
        return null
    }

    @Override
    int getCount() {
        return null
    }

}
