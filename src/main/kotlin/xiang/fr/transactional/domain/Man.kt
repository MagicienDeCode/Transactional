package xiang.fr.transactional.domain

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "man")
data class Man(
    val reference: String,
    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val woman: Woman
) {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID = UUID.randomUUID()
    @Column(insertable = false, updatable = false)
    val createdAt: LocalDateTime? = null
    @Column(insertable = false, updatable = false)
    val updatedAt: LocalDateTime? = null
}