package com.ukora.tradestudent.utils

import com.ukora.tradestudent.TradestudentApplication
import org.apache.commons.io.input.ReversedLinesFileReader

class Logger {

    final static LOG_FILE_LOCATION = 'application.out'

    static void debug(String message){
        if(TradestudentApplication.DEBUG_LOGGING_ENABLED){
            println message
        }
    }

    static void log(String message){
        println message
    }

    static String tailLog(){
        File file = new File(LOG_FILE_LOCATION)
        int n_lines = 10
        int counter = 0
        String out = String.format('Cannot read %s', LOG_FILE_LOCATION)
        if(file.exists()) {
            out = ''
            try {
                ReversedLinesFileReader object = new ReversedLinesFileReader(file)
                while (counter < n_lines) {
                    out = object.readLine() + "\n" + out
                    counter++
                }
            } catch(NullPointerException e){
                /** Ignore */
            }
        }
        return out
    }

}
