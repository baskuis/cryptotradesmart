package com.ukora.domain.beans.tags

abstract class AbstractCorrelationTag {

    abstract String getTagName()

    abstract boolean entry()

    abstract boolean exit()

}
