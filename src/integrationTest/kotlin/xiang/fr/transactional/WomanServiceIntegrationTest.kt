package xiang.fr.transactional

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import xiang.fr.transactional.service.WomanService

@SpringBootTest
class WomanServiceIntegrationTest {
    @Autowired
    lateinit var womanService: WomanService

    @Test
    fun `change reference without save`() {
        womanService.changeReferenceWithoutSave()
    }

    @Test
    fun `change reference without transactional`() {
        womanService.changeReferenceWithoutTransactional()
    }

    @Test
    fun `change reference`() {
        womanService.changeReference()
    }

    @Test
    fun `incorrect Example`() {
        womanService.incorrectExample("999")
    }

    // org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
    // @Test
    fun `two transactionals with default propagation`() {
        womanService.callAnotherTransactionalThrowRuntimeException()
    }

    @Test
    fun `two transactionals with one default propagation and another new`() {
        womanService.callAnotherTransactional()
    }

    /*
    @Test
    fun `non transactional call propagation support`(){
        womanService.nonTransactionalCallSupport()
    }

    @Test
    fun `transactional call propagation support`(){
        womanService.transactionalCallSupport()
    }
    */

    /*
    @Test
    fun `nested exception after commit`(){
        womanService.exceptionAfterCommit()
    }
     */
}