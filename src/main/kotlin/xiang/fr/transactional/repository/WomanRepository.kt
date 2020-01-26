package xiang.fr.transactional.repository

import org.springframework.data.repository.CrudRepository
import xiang.fr.transactional.domain.Woman
import java.util.*

interface WomanRepository : CrudRepository<Woman, UUID>