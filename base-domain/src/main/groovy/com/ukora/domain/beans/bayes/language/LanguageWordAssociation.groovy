package com.ukora.domain.beans.bayes.language

class LanguageWordAssociation implements LanguageAssociation {

    @Override
    LanguageAssociation.Type getType() {
        return LanguageAssociation.Type.Word
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
