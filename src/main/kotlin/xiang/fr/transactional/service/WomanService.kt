package xiang.fr.transactional.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xiang.fr.transactional.domain.Woman
import xiang.fr.transactional.repository.WomanRepository
import java.util.*

@Service
class WomanService(
    private val womanRepository: WomanRepository,
    private val manService: ManService
) {

    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: update woman set reference=? where id=?
 */
    @Transactional
    fun changeReferenceWithoutSave() {
        val woman = Woman("1")
        val result = womanRepository.save(woman)
        val search = womanRepository.findByIdOrNull(result.id)

        search?.apply { search.reference = "222" }
    }

    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: update woman set reference=? where id=?
     */
    fun changeReferenceWithoutTransactional() {
        val woman = Woman("1")
        val result = womanRepository.save(woman)
        val search = womanRepository.findByIdOrNull(result.id)

        search?.let {
            it.reference = "222"
            womanRepository.save(it)
        }
    }

    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: update woman set reference=? where id=?
     */
    @Transactional
    fun changeReference() {
        val woman = Woman("1")
        val result = womanRepository.save(woman)
        val search = womanRepository.findByIdOrNull(result.id)

        search?.let {
            it.reference = "222"
            womanRepository.save(it)
        }
    }

    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: update woman set reference=? where id=?
     */
    @Transactional
    fun incorrectExample(ref: String) {
        val woman = Woman("1")
        val result = womanRepository.save(woman)
        val search = womanRepository.findByIdOrNull(result.id)

        search?.apply {
            reference = "222"
        }

        // if some conditions are true, save, if not, do nothing
        if ("Some logic check".equals("111")) {
            womanRepository.save(search!!)
        }
    }

    // org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
    @Transactional
    fun callAnotherTransactionalThrowRuntimeException() {
        val woman = Woman("1")
        womanRepository.save(woman)
        try {
            manService.runtimeException()
        } catch (e: RuntimeException) {
        }
    }

    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: select man0_.id as id1_0_1_, man0_.created_at as created_2_0_1_, man0_.reference as referenc3_0_1_, man0_.updated_at as updated_4_0_1_, man0_.woman_id as woman_id5_0_1_, woman1_.id as id1_1_0_, woman1_.created_at as created_2_1_0_, woman1_.reference as referenc3_1_0_, woman1_.updated_at as updated_4_1_0_ from man man0_ left outer join woman woman1_ on man0_.woman_id=woman1_.id where man0_.id=?
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
     */
    @Transactional
    fun callAnotherTransactional() {
        val woman = Woman("1")
        womanRepository.save(woman)
        try {
            manService.runtimeExceptionWithPropagationRequiresNew()
        } catch (e: RuntimeException) {
        }
    }

    /*
    Hibernate: insert into woman (reference, id) values (?, ?)
    Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
     */
    fun nonTransactionalCallSupport() {
        manService.propagationSupport()
    }

    // NO INSERT
    @Transactional
    fun transactionalCallSupport() {
        manService.propagationSupport()
    }

    @Transactional
    fun exceptionAfterCommit() {
        val woman = Woman("1")
        womanRepository.save(woman)
        manService.propagationNested()
        throw RuntimeException()
    }

    @Transactional
    fun commitAfterException() {
        manService.propagationNestedRunTimeException()
        val woman = Woman("1")
        womanRepository.save(woman)
    }

    @Transactional
    fun modifyAndSave(uuid: UUID) {
        val woman = womanRepository.findById(uuid).get()
        woman.reference = "anna"
        womanRepository.save(woman)
    }
}
