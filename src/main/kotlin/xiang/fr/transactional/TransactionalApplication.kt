package xiang.fr.transactional

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TransactionalApplication

fun main(args: Array<String>) {
    runApplication<TransactionalApplication>(*args)
}