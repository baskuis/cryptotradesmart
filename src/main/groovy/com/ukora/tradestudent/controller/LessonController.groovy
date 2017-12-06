package com.ukora.tradestudent.controller

import com.ukora.tradestudent.TradestudentApplication
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.CaptureAssociationsService
import com.ukora.tradestudent.services.ProbabilityFigurerService
import com.ukora.tradestudent.services.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping('/learn')
class LessonController {

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    @Autowired
    BuySellTradingHistoricalSimulatorService tradingHistoricalSimulatorService

    @Autowired
    BytesFetcherService bytesFetcherService

    @RequestMapping(path = '/correlations', produces = 'application/json')
    CorrelationAssociation getCorrelationAssociations(@RequestParam(value = 'date') Date date){
        return probabilityFigurerService.getCorrelationAssociations(date)
    }

    @RequestMapping(path = '/braindump', produces = 'application/json')
    Map<String, BrainNode> getBrainNodes(){
        return probabilityFigurerService.getBrainNodes()
    }

    @RequestMapping(path = '/brainon')
    String brainOn(@RequestParam(value = 'speed') Integer speed){
        if(!speed || speed > 100 || speed < 1) speed = 1
        CaptureAssociationsService.leaningEnabled = true
        CaptureAssociationsService.learningSpeed = speed
        "ON"
    }

    @RequestMapping(path = '/brainoff')
    String brainOn(){
        CaptureAssociationsService.leaningEnabled = false
        "OFF"
    }

    @RequestMapping(path = '/wiskeybender')
    String dropBrain(){
        bytesFetcherService.wiskyBender()
        "OK"
    }

    @RequestMapping(path = '/relearn')
    String resetLessons(){
        bytesFetcherService.resetLessons()
        "OK"
    }

    @RequestMapping(path = '/flushcache')
    String flushCache(){
        bytesFetcherService.flushCache()
        "OK"
    }

    @RequestMapping(path = '/debugon')
    String debugOn(){
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        "OK"
    }

    @RequestMapping(path = '/debugoff')
    String debugOff(){
        TradestudentApplication.DEBUG_LOGGING_ENABLED = false
        "OK"
    }

    @RequestMapping(path = '/simulation')
    String simulation(@RequestParam(value = 'date') Date date){
        tradingHistoricalSimulatorService.runSimulation(date)
        "STARTED"
    }

    @InitBinder
    void binder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(NerdUtils.getGMTDateFormat(), true))
    }

}