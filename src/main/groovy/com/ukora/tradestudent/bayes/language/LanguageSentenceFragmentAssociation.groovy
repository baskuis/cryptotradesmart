package com.ukora.tradestudent.bayes.language

class LanguageSentenceFragmentAssociation implements LanguageAssociation {

    @Override
    LanguageAssociation.Type getType() {
        return LanguageAssociation.Type.Fragment
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
