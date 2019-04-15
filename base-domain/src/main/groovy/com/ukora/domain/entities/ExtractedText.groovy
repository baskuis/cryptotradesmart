package com.ukora.domain.entities

class ExtractedText {

    enum TextSource {NEWS,TWITTER}

    Map<TextSource, Set<String>> extract = [:]

    Set<String> extract(TextSource textSource){
        return extract.get(textSource)
    }

}
