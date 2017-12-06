package com.ukora.tradestudent.utils

import com.ukora.tradestudent.TradestudentApplication

class Logger {

    static void debug(String message){
        if(TradestudentApplication.DEBUG_LOGGING_ENABLED){
            println message
        }
    }

    static void log(String message){
        println message
    }

}
