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
### REQUIRED & REQUIRED_NEW
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
```xslt
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
```xslt
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
### SUPPORTS
```$xslt
	/**
	 * Support a current transaction, execute non-transactionally if none exists.
	 * Analogous to EJB transaction attribute of the same name.
	 * <p>Note: For transaction managers with transaction synchronization,
	 * {@code SUPPORTS} is slightly different from no transaction at all,
	 * as it defines a transaction scope that synchronization will apply for.
	 * As a consequence, the same resources (JDBC Connection, Hibernate Session, etc)
	 * will be shared for the entire specified scope. Note that this depends on
	 * the actual synchronization configuration of the transaction manager.
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
	 */
	SUPPORTS(TransactionDefinition.PROPAGATION_SUPPORTS)
```
See example below
```$xslt
@Transactional(propagation = Propagation.SUPPORTS)
fun propagationSupport(){
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
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
```
### NESTED
```$xslt
	/**
	 * Execute within a nested transaction if a current transaction exists,
	 * behave like {@code REQUIRED} otherwise. There is no analogous feature in EJB.
	 * <p>Note: Actual creation of a nested transaction will only work on specific
	 * transaction managers. Out of the box, this only applies to the JDBC
	 * DataSourceTransactionManager. Some JTA providers might support nested
	 * transactions as well.
	 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
	 */
	NESTED(TransactionDefinition.PROPAGATION_NESTED)
```
As I have 
`org.springframework.transaction.NestedTransactionNotSupportedException: JpaDialect does not support savepoints - check your JPA provider's capabilities`
there is no test.  <br/>
I see somewhere says that DataSourceTransactionManager only available for JdbcTemplate and ibatis, need JDBC3.0 

## Isolation

* Dirty read : read the uncommitted change of a concurrent transaction. A事务在读取某一行数据的时候，能够读到B事务还未提交的、对同一行数据的修改。
* Non-repeatable read : get different value on re-read of a row if a concurrent transaction updates the same row and commits. 在同一个事务里，在T1时间读取到的某一行的数据，在T2时间再次读取同一行数据时，发生了变化。后者变化可能是被更新了、消失了。
* Phantom read : get different rows after re-execution of a range query if another transaction adds or removes some rows in the range and commits. 在同一个事务里，用条件A，在T1时间查询到的数据是10行，但是在T2时间查询到的数据多于10行。需要注意的是，和 Nonrepeatable read 不同，Phantom read 在T1时读到的数据在T2时不会发生变化。注意，为何只说比10行多，那么比10行少就不是 Phantom read 了吗？因为 Nonrepeatable read 包含了数据消失的情况。

| Isolation level | Dirty read | Non-repeatable read | Phantom read |
| -------------   | ---------- | ------------------- | ------------ |
| READ_UNCOMMITTED| may occur  | may occur           | may occur    |
| READ_COMMITTED  |            | may occur           | may occur    | 
| REPEATABLE_READ |            |                     | may occur    | 
| SERIALIZABLE    |            |                     |              | 


