package xiang.fr.transactional.repository

import org.springframework.data.repository.CrudRepository
import xiang.fr.transactional.domain.Man
import java.util.*

interface ManRepository : CrudRepository<Man, UUID>