package com.ukora.tradestudent.entities

import com.mongodb.DBObject

class Lesson {
    Exchange exchange
    String id
    Date date
    Double price
    DBObject dbObject

    Memory memory
    Twitter twitter
    List<News> news
}
