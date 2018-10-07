package com.ukora.tradestudent.services.associations.text


class TextUtils {

    static final WHITESPACE_SPLIT = /[^a-z^0-9^A-Z]+/

    static List<String> splitText(String text){
        text?.split(WHITESPACE_SPLIT)

    }

}
