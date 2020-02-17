package xiang.fr.transactional

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import xiang.fr.transactional.domain.Man
import xiang.fr.transactional.domain.Woman
import xiang.fr.transactional.service.ManService

@SpringBootTest
class ManServiceIntegrationTest {
    @Autowired
    lateinit var manService: ManService

    // NO INSERT
    // @Test
    fun `not self invocation`() {
        val man = Man("1", Woman("1"))
        manService.save(man)
    }

    // NO INSERT
    // @Test
    fun `trasactional`() {
        val man = Man("1", Woman("1"))
        manService.save(man)
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    // @Test
    fun `self invocation`() {
        manService.testSelfInvocation()
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    @Test
    fun `catch exception`() {
        manService.catchException()
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    // @Test
    fun `catch throws exception`() {
        manService.catchThrowsException()
    }

    // NO INSERT TO DB
    // @Test
    fun `catch throws exception with transactional`() {
        manService.catchThrowsExceptionWithTransactional()
    }

    // NO INSERT TO DB
    // @Test
    fun `runtime exception`() {
        manService.runtimeException()
    }

    /*
    @Test
    fun `self with transactional`(){
        manService.selfWithTransactional()
    }
     */
}