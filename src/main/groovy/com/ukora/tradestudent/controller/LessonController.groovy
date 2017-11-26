package com.ukora.tradestudent.controller

import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.ProbabilityFigurerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping('/learn')
class LessonController {

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    @RequestMapping(path = '/correlations', produces = 'application/json')
    CorrelationAssociation getCorrelationAssociations(@RequestParam(value = 'date') Date date){
        return probabilityFigurerService.getCorrelationAssociations(date)
    }

    @RequestMapping(path = '/braindump', produces = 'application/json')
    Map<String, BrainNode> getBrainNodes(){
        return probabilityFigurerService.getBrainNodes()
    }

}