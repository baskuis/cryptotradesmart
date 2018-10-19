package com.ukora.tradestudent.converters

import com.ukora.tradestudent.services.TagService
import com.ukora.tradestudent.tags.AbstractCorrelationTag
import org.springframework.beans.factory.annotation.Autowired

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class AbstractCorrelationTagConverter implements AttributeConverter<AbstractCorrelationTag, String> {

    @Autowired
    TagService tagService

    @Override
    String convertToDatabaseColumn(AbstractCorrelationTag tag) {
        return tag?.tagName
    }

    @Override
    AbstractCorrelationTag convertToEntityAttribute(String tagName) {
        return tagService.getTagByName(tagName)
    }

}