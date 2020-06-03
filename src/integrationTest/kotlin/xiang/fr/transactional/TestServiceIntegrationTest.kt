package xiang.fr.transactional

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import xiang.fr.transactional.domain.Woman
import xiang.fr.transactional.repository.WomanRepository
import xiang.fr.transactional.service.TestService

@SpringBootTest
class TestServiceIntegrationTest {
    @Autowired
    lateinit var testService: TestService

    @Autowired
    lateinit var womenRepository: WomanRepository

    @Test
    fun `readonly call another transactional won't change`() {
        val woman = womenRepository.save(Woman("111"))
        println("before is: ${woman.reference}")
        testService.testReadOnly(woman.id)
        val womanAfter = womenRepository.findById(woman.id).get()
        println("after is: ${womanAfter.reference}")
    }
}
