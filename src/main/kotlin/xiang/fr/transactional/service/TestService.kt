package xiang.fr.transactional.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TestService(
    private val womanService: WomanService,
    private val manService: ManService
) {
    @Transactional(readOnly = true)
    fun testReadOnly(uuid: UUID) {
        println("test begin")
        womanService.modifyAndSave(uuid)
        println("test end")
    }
}
