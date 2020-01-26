package xiang.fr.transactional.domain

import java.time.LocalDateTime
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "woman")
data class Woman(
    // normally, it should be val, in order to test some use case, here var
    var reference: String
) {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID()
    @Column(insertable = false, updatable = false)
    val createdAt: LocalDateTime? = null
    @Column(insertable = false, updatable = false)
    val updatedAt: LocalDateTime? = null
}