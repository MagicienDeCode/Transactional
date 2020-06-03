package xiang.fr.transactional.service

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import xiang.fr.transactional.MyException
import xiang.fr.transactional.domain.Man
import xiang.fr.transactional.domain.Woman
import xiang.fr.transactional.repository.ManRepository
import java.util.*

@Service
class ManService(
    private val manRepository: ManRepository,
    @Lazy
    private val self: ManService
) {
    // NO INSERT
    fun notSelfInvocation() {
        val man = Man("1", Woman("1"))
        self.save(man)
    }

    // NO INSERT
    @Transactional
    fun save(man: Man): Man {
        val result = manRepository.save(man)
        throw RuntimeException("test")
        return result
    }

    // NO INSERT
    @Transactional
    fun selfWithTransactional() {
        val man = Man("1", Woman("1"))
        this.save(man)
    }

    /*
    In proxy mode (which is the default), only external method calls coming in through the proxy
    are intercepted. This means that self-invocation, in effect, a method within the target object
    calling another method of the target object, will not lead to an actual transaction at runtime
    even if the invoked method is marked with @Transactional. Also, the proxy must be fully
    initialized to provide the expected behaviour so you should not rely on this feature in your
    initialization code, i.e. @PostConstruct.
     */
    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    fun testSelfInvocation() {
        val man = Man("1", Woman("1"))
        this.save(man)
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
    */
    @Transactional
    fun catchException() {
        try {
            val man = Man("1", Woman("1"))
            manRepository.save(man)
            throw MyException("custom exception")
        } catch (e: Exception) {
        }
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    @Transactional
    @Throws(MyException::class)
    fun catchThrowsException() {
        try {
            val man = Man("1", Woman("1"))
            manRepository.save(man)
            throw MyException("custom exception")
        } catch (e: MyException) {
            throw e
        }
    }

    // NO INSERT TO DB
    @Transactional(rollbackFor = [MyException::class])
    @Throws(MyException::class)
    fun catchThrowsExceptionWithTransactional() {
        try {
            val man = Man("1", Woman("1"))
            manRepository.save(man)
            throw MyException("custom exception")
        } catch (e: MyException) {
            throw e
        }
    }

    // NO INSERT TO DB
    @Transactional
    fun runtimeException() {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
        throw RuntimeException("custom exception")
    }

    // NO INSERT TO DB
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun runtimeExceptionWithPropagationRequiresNew() {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
        throw RuntimeException("custom exception")
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    fun propagationSupport() {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
        throw RuntimeException("custom exception")
    }

    @Transactional(propagation = Propagation.NESTED)
    fun propagationNestedRunTimeException() {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
        throw RuntimeException("custom exception")
    }

    @Transactional(propagation = Propagation.NESTED)
    fun propagationNested() {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
    }
}
