package com.ukora.tradestudent.controller

import com.ukora.tradestudent.TradestudentApplication
import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.entities.ExtractedText
import com.ukora.tradestudent.entities.SimulatedTradeEntry
import com.ukora.tradestudent.entities.SimulationResult
import com.ukora.tradestudent.services.*
import com.ukora.tradestudent.services.associations.CaptureAssociationsService
import com.ukora.tradestudent.services.associations.TechnicalAnalysisService
import com.ukora.tradestudent.services.associations.text.CaptureTextAssociationsService
import com.ukora.tradestudent.services.learner.TraverseLessonsService
import com.ukora.tradestudent.services.simulator.BuySellTradingHistoricalSimulatorService
import com.ukora.tradestudent.services.text.TextAssociationProbabilityService
import com.ukora.tradestudent.utils.Logger
import com.ukora.tradestudent.utils.NerdUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.propertyeditors.CustomDateEditor
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping('/learn')
class TradeStudentController {

    @Autowired
    ProbabilityCombinerService probabilityCombinerService

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

    @Autowired
    TextAssociationProbabilityService textAssociationProbabilityService

    @Autowired
    LessonTotalsService lessonTotalsService

    @RequestMapping(path = '/correlations', produces = 'application/json', method = RequestMethod.GET)
    CorrelationAssociation getCorrelationAssociations(@RequestParam(value = 'date') Date date) {
        probabilityCombinerService.getCorrelationAssociations(date)
    }

    @RequestMapping(path = '/braindump', produces = 'application/json', method = RequestMethod.GET)
    Map<String, BrainNode> getBrainNodes() {
        probabilityCombinerService.getBrainNodes()
    }

    @RequestMapping(path = '/indicators', produces = 'application/json', method = RequestMethod.GET)
    Set<String> getIndicators() {
        probabilityCombinerService.getRelevantBrainNodes().keySet()
    }

    @RequestMapping(path = '/brainon', method = RequestMethod.GET)
    String brainOn(@RequestParam(value = 'speed') Integer speed) {
        if (!speed || speed > 80 || speed < 1) speed = 1
        CaptureAssociationsService.leaningEnabled = true
        CaptureAssociationsService.learningSpeed = speed
        CaptureTextAssociationsService.leaningEnabled = true
        CaptureTextAssociationsService.learningSpeed = speed
        "ON"
    }

    @RequestMapping(path = '/brainoff', method = RequestMethod.GET)
    String brainOn() {
        CaptureAssociationsService.leaningEnabled = false
        "OFF"
    }

    @RequestMapping(path = '/whiskeybender', method = RequestMethod.GET)
    String dropBrain() {
        return "DISABLED"
        bytesFetcherService.whiskeyBender()
        "OK"
    }

    @RequestMapping(path = '/relearn', method = RequestMethod.GET)
    String resetLessons() {
        return "DISABLED"
        bytesFetcherService.resetLessons()
        "OK"
    }

    @RequestMapping(path = '/flushcache', method = RequestMethod.GET)
    String flushCache() {
        bytesFetcherService.flushCache()
        "OK"
    }

    @RequestMapping(path = '/debugon', method = RequestMethod.GET)
    String debugOn() {
        TradestudentApplication.DEBUG_LOGGING_ENABLED = true
        "OK"
    }

    @RequestMapping(path = '/debugoff', method = RequestMethod.GET)
    String debugOff() {
        TradestudentApplication.DEBUG_LOGGING_ENABLED = false
        "OK"
    }

    @RequestMapping(path = '/simulation', method = RequestMethod.GET)
    String simulation(@RequestParam(value = 'date') Date date) {
        tradingHistoricalSimulatorService.runSimulation(date)
        "STARTED"
    }

    @RequestMapping(path = '/forcecomplete', method = RequestMethod.GET)
    String forceCompleteSimulation() {
        tradingHistoricalSimulatorService.forceCompleteSimulation = true
        "COMPLETED"
    }

    @RequestMapping(path = '/simulations', method = RequestMethod.GET)
    Object simulations() {
        aliasService.replaceAllWithAlias(
                objectMapper.convertValue(bytesFetcherService.getSimulations(), Object)
        )
    }

    @RequestMapping(path = '/multion', method = RequestMethod.GET)
    String multiOn() {
        BuySellTradingHistoricalSimulatorService.multiThreadingEnabled = true
        "ON"
    }

    @RequestMapping(path = '/multioff', method = RequestMethod.GET)
    String multiOff() {
        BuySellTradingHistoricalSimulatorService.multiThreadingEnabled = false
        "OFF"
    }

    @RequestMapping(path = '/learnbuysell', method = RequestMethod.GET)
    String learnBuySell() {
        traverseLessonsService.learnFromBuySellBehavior()
        "STARTED"
    }

    @RequestMapping(path = '/learntradehistory', method = RequestMethod.GET)
    String learnTrendHistory() {
        traverseLessonsService.learnFromHistoryTrendData()
        "STARTED"
    }

    @RequestMapping(path = '/bestsimulation', method = RequestMethod.GET)
    SimulationResult getBestSimulation() {
        simulationResultService.getTopPerformingSimulation()
    }

    @RequestMapping(path = '/bestsimulations', method = RequestMethod.GET)
    List<SimulationResult> getBestSimulations() {
        simulationResultService.getTopPerformingSimulations()
    }

    @RequestMapping(path = '/latestentry', method = RequestMethod.GET)
    SimulatedTradeEntry getLatestEntry() {
        bytesFetcherService.getLatestSimulatedTradeEntry()
    }

    @RequestMapping(path = '/latestentries', method = RequestMethod.GET)
    List<SimulatedTradeEntry> getLatestEntries() {
        bytesFetcherService.getLatestSimulatedTradeEntries()
    }

    @RequestMapping(path = '/resetcounts', method = RequestMethod.GET)
    String resetCounts() {
        memoryLimitService.resetCounts()
        "OK"
    }

    @RequestMapping(path = '/technicalanalysis', method = RequestMethod.GET)
    Map<String, Double> extractTechnicalAnalysis(@RequestParam(value = 'date') Date date) {
        technicalAnalysisService.extractTechnicalAnalysis(date)
    }

    @RequestMapping(path = '/aliases', method = RequestMethod.GET)
    Map getAliases() {
        [
                aliases: aliasService.getBeanToAlias(),
                beans  : aliasService.getAliasToBean()
        ]
    }

    @RequestMapping(path = '/probabilitycombiners', method = RequestMethod.GET)
    List getTopPerformingProbabilityCombiners(){
        simulationResultService.getTopPerformingProbabilityCombiners()
    }

    @RequestMapping(path = '/tradeexecutionstrategies', method = RequestMethod.GET)
    List getTopPerformingTradeExecutionStrategies(){
        simulationResultService.getTopPerformingTradeExecutionStrategies()
    }

    @RequestMapping(path = '/keywords', method = RequestMethod.GET)
    def getTextAssociationProbabilityService(@RequestParam(value = 'date') Date date){
        textAssociationProbabilityService.getTextKeywordsByDate(date)
    }

    @RequestMapping(path = '/lessontotals', method = RequestMethod.GET)
    def getLessonTotals(){
        lessonTotalsService.getTagCountSummary()
    }

    @RequestMapping(path = '/keyword', method = RequestMethod.GET)
    def getKeywordAssociation(@RequestParam(value = 'keyword') String keyword){
        [
                (ExtractedText.TextSource.TWITTER): textAssociationProbabilityService.getKeywordAssociation(keyword, ExtractedText.TextSource.TWITTER),
                (ExtractedText.TextSource.NEWS): textAssociationProbabilityService.getKeywordAssociation(keyword, ExtractedText.TextSource.NEWS)
        ]
    }

    @RequestMapping(path = '/textassociations', method = RequestMethod.GET)
    def getTextAssociations(@RequestParam(value = 'date') Date date){
        textAssociationProbabilityService.tagCorrelationByText(date)
    }

    @RequestMapping(path = '/log', method = RequestMethod.GET)
    String log() {
        Logger.tailLog()
    }

    @InitBinder
    void binder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new CustomDateEditor(NerdUtils.getGMTDateFormat(), true))
    }

}