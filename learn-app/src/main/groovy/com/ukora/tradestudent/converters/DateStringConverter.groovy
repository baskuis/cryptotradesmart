package com.ukora.tradestudent.converters

import com.ukora.tradestudent.utils.NerdUtils

import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class DateStringConverter implements AttributeConverter<Date, String> {

    @Override
    String convertToDatabaseColumn(Date d) {
        return d.toString()
    }

    @Override
    Date convertToEntityAttribute(String s) {
        return NerdUtils.parseString(s)
    }

}
