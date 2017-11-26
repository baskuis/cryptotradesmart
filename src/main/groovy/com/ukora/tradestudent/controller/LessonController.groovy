package com.ukora.tradestudent.controller

import com.ukora.tradestudent.entities.BrainNode
import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.CaptureAssociationsService
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

    @RequestMapping(path = '/brainon')
    String brainOn(@RequestParam(value = 'speed') Integer speed){
        if(!speed || speed > 100) speed = 1
        CaptureAssociationsService.leaningEnabled = true
        CaptureAssociationsService.learningSpeed = speed
        "ON"
    }

    @RequestMapping(path = '/brainoff')
    String brainOn(){
        CaptureAssociationsService.leaningEnabled = false
        "OFF"
    }

}