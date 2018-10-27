package com.ukora.tradestudent.converters

import com.ukora.tradestudent.utils.NerdUtils

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class StringDateConverter implements AttributeConverter<String, Date> {

    @Override
    Date convertToDatabaseColumn(String s) {
        return NerdUtils.parseString(s)
    }

    @Override
    String convertToEntityAttribute(Date date) {
        return date.toString()
    }

}
