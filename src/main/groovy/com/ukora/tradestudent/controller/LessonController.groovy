package com.ukora.tradestudent.controller

import com.ukora.tradestudent.entities.CorrelationAssociation
import com.ukora.tradestudent.services.ProbabilityFigurerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LessonController {

    @Autowired
    ProbabilityFigurerService probabilityFigurerService

    @RequestMapping(path = '/probablities')
    CorrelationAssociation getProbabilities(@RequestParam(value = 'date') Date date){
        return probabilityFigurerService.determineProbability(date)
    }

}
