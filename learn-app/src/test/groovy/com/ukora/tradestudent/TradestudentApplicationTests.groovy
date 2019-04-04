package com.ukora.tradestudent

import com.ukora.tradestudent.services.AliasService
import com.ukora.domain.beans.tags.TagGroup
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
		assert applicationContext != null
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

	@Test
	void testThatWeAreAbleToFindAllTheTags() {
		tagGroupMap = applicationContext.getBeansOfType(TagGroup)
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'up' }) }?.value?.getName() == 'updown'
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'down' }) }?.value?.getName() == 'updown'
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'buy' }) }?.value?.getName() == 'buysell'
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'sell' }) }?.value?.getName() == 'buysell'
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'upreversal' }) }?.value?.getName() == 'updownreversal'
		assert tagGroupMap.find { (it.value.tags().find { it.getTagName() == 'downreversal' }) }?.value?.getName() == 'updownreversal'
		int tagCount
		tagGroupMap.each { it.value.tags().each { tagCount++ } }
		assert tagCount == 6
	}

}
