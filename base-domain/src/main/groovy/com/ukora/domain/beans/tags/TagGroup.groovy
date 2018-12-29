package com.ukora.domain.beans.tags

/**
 * A tag group defines a list
 * of mutually exclusive tags.
 * For example buy/sell - both
 * cannot apply at the same time
 *
 * A tag group is also a tag sub set
 *
 */
interface TagGroup extends TagSubset {

    List<? extends AbstractCorrelationTag> tags()

    String getName()

}