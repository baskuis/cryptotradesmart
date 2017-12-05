package com.ukora.tradestudent.tags

/**
 * Can be used to check if a class applies to a subset
 * or correlation tags
 *
 */
interface TagSubset {

    boolean applies(String toTags)

}