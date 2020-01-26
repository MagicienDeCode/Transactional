# @Transactional Kotlin Example
How to make transactional failed? How to use @Transactional

## @Transactional

### Mysql engine: use InnoDB
MyISAM: Transactions NO  Before MySQL 5.5.5, MyISAM is the default storage engine. (The default was changed to InnoDB in MySQL 5.5.5.) MyISAM is based on the older (and no longer available) ISAM storage engine but has many useful extensions.   

https://dev.mysql.com/doc/refman/5.5/en/myisam-storage-engine.html

### Make sure the class will be instance as a Bean:  @Service

### Function must be public 
Method visibility and @Transactional

When using proxies, you should apply the @Transactional annotation only to methods with public visibility. If you do annotate protected, private or package-visible methods with the @Transactional annotation, no error is raised, but the annotated method does not exhibit the configured transactional settings. Consider the use of AspectJ (see below) if you need to annotate non-public methods.

### Self-invocation
```$xslt
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
}
```

### Catch custom exception but do not throw it, insert won't be rollback
```$xslt
class MyException(m: String) : Exception(m)
 
 
@Transactional
fun catchException() {
    try {
        val man = Man("1", Woman("1"))
        manRepository.save(man)
        throw MyException("custom exception")
    } catch (e: Exception) {
 
    }
}
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
```

### Throws exception but do not rollback
By default, a transaction will be rolling back on RuntimeException and Error but not on checked exceptions (business exceptions). 

https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html

* Not Runtime Exception, so transaction won't be rollback
```$xslt
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
Hibernate: insert into woman (reference, id) values (?, ?)
Hibernate: insert into man (reference, woman_id, id) values (?, ?, ?)
```

* Declare rollbackFor 
```$xslt
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
// NO INSERT
```

* RuntimeException
```$xslt
@Transactional
fun runtimeException() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
// NO INSERT
```

## Propagation

```$xslt
/**
 * Support a current transaction, create a new one if none exists.
 * Analogous to EJB transaction attribute of the same name.
 * <p>This is the default setting of a transaction annotation.
 */
REQUIRED(TransactionDefinition.PROPAGATION_REQUIRED),
/**
 * Create a new transaction, and suspend the current transaction if one exists.
 * Analogous to the EJB transaction attribute of the same name.
 * <p><b>NOTE:</b> Actual transaction suspension will not work out-of-the-box
 * on all transaction managers. This in particular applies to
 * {@link org.springframework.transaction.jta.JtaTransactionManager},
 * which requires the {@code javax.transaction.TransactionManager} to be
 * made available to it (which is server-specific in standard Java EE).
 * @see org.springframework.transaction.jta.JtaTransactionManager#setTransactionManager
 */
REQUIRES_NEW(TransactionDefinition.PROPAGATION_REQUIRES_NEW),
```
org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
```aidl
// NO INSERT TO DB
@Transactional
fun runtimeException() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
 
 
// org.springframework.transaction.UnexpectedRollbackException: Transaction silently rolled back because it has been marked as rollback-only
@Transactional
fun callAnotherTransactionalThrowRuntimeException(){
    val woman = Woman("1")
    womanRepository.save(woman)
    try {
        manService.runtimeException()
    }catch (e:RuntimeException){}
}
```

These two functions are in the same transaction.

When function `runtimeException` throws an exception, it will mark current transaction should be rollback. But `callAnotherTransactionalThrowRuntimeException` catch it, so current transaction think it should commit. That's why it throws UnexpectedRollbackException .

Another example with @Transactional(propagation = Propagation.REQUIRES_NEW)
```aidl
// NO INSERT TO DB
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun runtimeExceptionWithPropagationRequiresNew() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
 
 
    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: select man0_.id as id1_0_1_, man0_.created_at as created_2_0_1_, man0_.reference as referenc3_0_1_, man0_.updated_at as updated_4_0_1_, man0_.woman_id as woman_id5_0_1_, woman1_.id as id1_1_0_, woman1_.created_at as created_2_1_0_, woman1_.reference as referenc3_1_0_, woman1_.updated_at as updated_4_1_0_ from man man0_ left outer join woman woman1_ on man0_.woman_id=woman1_.id where man0_.id=?
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
     */
    @Transactional
    fun callAnotherTransactional(){
        val woman = Woman("1")
        womanRepository.save(woman)
        try {
            manService.runtimeExceptionWithPropagationRequiresNew()
        }catch (e:RuntimeException){}
    }
```
`runtimeExceptionWithPropagationRequiresNew` will rollback, but `callAnotherTransactional` will commit.