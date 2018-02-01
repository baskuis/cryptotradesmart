package com.ukora.tradestudent

import com.ukora.tradestudent.services.AliasService
import com.ukora.tradestudent.tags.TagGroup
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner)
@SpringBootTest
class TradestudentApplicationTests {

	Map<String, TagGroup> tagGroupMap

	@Autowired
	ApplicationContext applicationContext

	@Autowired
	AliasService aliasService

	@Test
	void contextLoads() {
	}

	@Test
	void ableToFindTagGroupByTagName(){
		tagGroupMap = applicationContext.getBeansOfType(TagGroup)
		String group = tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'buy' }) }?.key
		assert group != null
		group = tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'invalid' }) }?.key
		assert group == null
	}

	@Test
	void aliasesAreAllUnique(){
		assert aliasService.beanToAlias.collect { it.value }.size() == aliasService.beanToAlias.collect { it.value }.unique().size()
	}

}
