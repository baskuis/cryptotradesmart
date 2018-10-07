package com.ukora.tradestudent.entities

class ExtractedText {

    enum TextSource {NEWS,TWITTER}

    Map<TextSource, List<String>> extract = [:]

    void hydrate(TextSource textSource, List<String> keywords){
        extract.put(textSource, keywords)
    }

    List<String> extract(TextSource textSource){
        return extract.get(textSource)
    }

}
