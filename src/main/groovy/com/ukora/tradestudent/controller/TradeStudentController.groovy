package com.ukora.tradestudent.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ukora.tradestudent.TradestudentApplication
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.entities.SimulationResult
import com.ukora.tradestudent.services.AliasService
import com.ukora.tradestudent.services.BytesFetcherService
import com.ukora.tradestudent.services.associations.CaptureAssociationsService
import com.ukora.tradestudent.services.MemoryLimitService
import com.ukora.tradestudent.services.ProbabilityFigurerService
import com.ukora.tradestudent.services.SimulationResultService
import com.ukora.tradestudent.services.associations.TechnicalAnalysisService
import com.ukora.tradestudent.services.learner.TraverseLessonsService
import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping('/learn')
class TradeStudentController {

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    @Autowired
    BuySellTradingHistoricalSimulatorService tradingHistoricalSimulatorService

    @Autowired
    BytesFetcherService bytesFetcherService

    @Autowired
    TraverseLessonsService traverseLessonsService

    @Autowired
    SimulationResultService simulationResultService

    @Autowired
    MemoryLimitService memoryLimitService

    @Autowired
    TechnicalAnalysisService technicalAnalysisService

    @Autowired
    AliasService aliasService

    ObjectMapper objectMapper = new ObjectMapper()

    @RequestMapping(path = '/correlations', produces = 'application/json', method = RequestMethod.GET)
    Object getCorrelationAssociations(@RequestParam(value = 'date') Date date){
        aliasService.replaceAllWithAlias(
                objectMapper.convertValue(probabilityFigurerService.getCorrelationAssociations(date), Object)
        )
    }

    @RequestMapping(path = '/braindump', produces = 'application/json', method = RequestMethod.GET)
    Object getBrainNodes(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(probabilityFigurerService.getBrainNodes(), Object)
        )
    }

    @RequestMapping(path = '/brainon', method = RequestMethod.GET)
    String brainOn(@RequestParam(value = 'speed') Integer speed){
        if(!speed || speed > 50 || speed < 1) speed = 1
        CaptureAssociationsService.leaningEnabled = true
        CaptureAssociationsService.learningSpeed = speed
        "ON"
    }

    @RequestMapping(path = '/brainoff', method = RequestMethod.GET)
    String brainOn(){
        CaptureAssociationsService.leaningEnabled = false
        "OFF"
    }

    @RequestMapping(path = '/whiskeybender', method = RequestMethod.GET)
    String dropBrain(){
        //return "DISABLED"
        bytesFetcherService.whiskeyBender()
        "OK"
    }

    @RequestMapping(path = '/relearn', method = RequestMethod.GET)
    String resetLessons(){
        //return "DISABLED"
        bytesFetcherService.resetLessons()
        "OK"
    }

    @RequestMapping(path = '/flushcache', method = RequestMethod.GET)
    String flushCache(){
        bytesFetcherService.flushCache()
        "OK"
    }

    @RequestMapping(path = '/debugon', method = RequestMethod.GET)
    String debugOn(){
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        "OK"
    }

    @RequestMapping(path = '/debugoff', method = RequestMethod.GET)
    String debugOff(){
        TradestudentApplication.DEBUG_LOGGING_ENABLED = false
        "OK"
    }

    @RequestMapping(path = '/simulation', method = RequestMethod.GET)
    String simulation(@RequestParam(value = 'date') Date date){
        tradingHistoricalSimulatorService.runSimulation(date)
        "STARTED"
    }

    @RequestMapping(path = '/forcecomplete', method = RequestMethod.GET)
    String forceCompleteSimulation(){
        tradingHistoricalSimulatorService.forceCompleteSimulation = true
        "COMPLETED"
    }

    @RequestMapping(path = '/simulations', method = RequestMethod.GET)
    Object simulations(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(bytesFetcherService.getSimulations(), Object)
        )
    }

    @RequestMapping(path = '/multion', method = RequestMethod.GET)
    String multiOn(){
        BuySellTradingHistoricalSimulatorService.multiThreadingEnabled = true
        "ON"
    }

    @RequestMapping(path = '/multioff', method = RequestMethod.GET)
    String multiOff(){
        BuySellTradingHistoricalSimulatorService.multiThreadingEnabled = false
        "OFF"
    }

    @RequestMapping(path = '/learnbuysell', method = RequestMethod.GET)
    String learnBuySell(){
        traverseLessonsService.learnFromBuySellBehavior()
        "STARTED"
    }

    @RequestMapping(path = '/learntradehistory', method = RequestMethod.GET)
    String learnTrendHistory(){
        traverseLessonsService.learnFromHistoryTrendData()
        "STARTED"
    }

    @RequestMapping(path = '/bestsimulation', method = RequestMethod.GET)
    Object getBestSimulation(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(simulationResultService.getTopPerformingSimulation(), Object)
        )
    }

    @RequestMapping(path = '/bestsimulations', method = RequestMethod.GET)
    Object getBestSimulations(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(simulationResultService.getTopPerformingSimulations(), Object)
        )
    }

    @RequestMapping(path = '/latestentry', method = RequestMethod.GET)
    Object getLatestEntry(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(bytesFetcherService.getLatestSimulatedTradeEntry(), Object)
        )
    }

    @RequestMapping(path = '/latestentries', method = RequestMethod.GET)
    Object getLatestEntries(){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(bytesFetcherService.getLatestSimulatedTradeEntries(), Object)
        )
    }

    @RequestMapping(path = '/resetcounts', method = RequestMethod.GET)
    String resetCounts(){
        memoryLimitService.resetCounts()
        "OK"
    }

    @RequestMapping(path = '/technicalanalysis', method = RequestMethod.GET)
    Object extractTechnicalAnalysis(@RequestParam(value = 'date') Date date){
        aliasService.replaceAllWithAlias(
            objectMapper.convertValue(technicalAnalysisService.extractTechnicalAnalysis(date), Object)
        )
    }

    @RequestMapping(path = '/aliases', method = RequestMethod.GET)
    Map getAliases(){
        [
            aliases: aliasService.getBeanToAlias(),
            beans: aliasService.getAliasToBean()
        ]
    }

    @RequestMapping(path = '/log', method = RequestMethod.GET)
    String log(){
        Logger.tailLog()
    }

    @InitBinder
    void binder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(NerdUtils.getGMTDateFormat(), true))
    }

}