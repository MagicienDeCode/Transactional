# @Transactional Kotlin Example <br/> 疯狂虐杀面试者的事务
How to make transactional failed? How to use @Transactional
<br/> 怎么做会使事务失败？ 怎么用事务注解
## 1. @Transactional
事务注解会让一个方法在执行完之后，要么提交所有的改变，要么什么都不改变。ACID原则

### 1.1 Mysql engine: use InnoDB <br/> 数据库引擎必须支持事务
MyISAM: Transactions NO  Before MySQL 5.5.5, MyISAM is the default storage engine. 
(The default was changed to InnoDB in MySQL 5.5.5.) MyISAM is based on the older (and no longer available) 
ISAM storage engine but has many useful extensions.   
<br/>
如果MySQL 数据库的引擎是MyISAM，那它根本不支持事务

[More detail for MyISAM](https://dev.mysql.com/doc/refman/5.5/en/myisam-storage-engine.html)

### 1.2 Make sure the class will be instance as a Bean:  @Service <br/> 类必须实例化成Spring的Bean
 
### 1.3 Function must be public <br/> 方法必须是 *public* 的
Method visibility and @Transactional
<br/>
When using proxies, you should apply the @Transactional annotation only to methods with public visibility.
If you do annotate protected, private or package-visible methods with the @Transactional annotation,
no error is raised, but the annotated method does not exhibit the configured transactional settings. 
Consider the use of AspectJ (see below) if you need to annotate non-public methods.

### 1.4 Self-invocation <br/> 自我调用问题

在同一个Bean中 一个正常的方法叫一个带事务注解的方法，事物是不会奏效的
<br/>
`testSelfInvocation`叫`save`方法，虽然最后有Runtime的异常，但数据依然插入了
<br/>
我们可以通过延迟注入一个相同类型的Bean来解决这个问题
```$kotlin
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

### 1.5 catch custom exception but do not throw it, insert won't be rollback <br/> 只抓不抛异常是不会让事务回滚的

```$kotlin
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

### 1.6 Throws exception but do not rollback <br/> 抛出自定义异常是不会让事务回滚的
![](https://github.com/MagicienDeCode/images/blob/master/transactional/exception_relation.png)
By default, a transaction will be rolling back on RuntimeException and Error 
but not on checked exceptions (business exceptions). 
<br/>
事务默认只会回滚`RuntimeException`
[Spring @Transactional ](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html)

- Not Runtime Exception, so transaction won't be rollback 不是Runtime的异常不行
```$kotlin
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

- Declare rollbackFor 声明需要为哪些异常回滚
```$kotlin
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

- RuntimeException 默认的回滚异常
```$kotlin
@Transactional
fun runtimeException() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
// NO INSERT
```

## 2. Propagation 事务的传播模式
### 2.1 REQUIRED & REQUIRED_NEW
- REQUIRED
    * 如果没有就创建一个，如果有，就用当前的
- REQUIRED_NEW
    * 每次都会创建一个新的，如果当前就有一个，那么会把当前的挂起
```$java
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
REQUIRED_NEW(TransactionDefinition.PROPAGATION_REQUIRED_NEW),
```

- REQUIRED 如果两个方法共享一个实务，一个需要回滚，一个需要提交，就会出现`UnexpectedRollbackException`
```kotlin
// NO INSERT TO DB
@Transactional
fun runtimeException() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
 
// org.springframework.transaction.UnexpectedRollbackException: 
// Transaction silently rolled back because it has been marked as rollback-only
@Transactional
fun callAnotherTransactionalThrowRuntimeException(){
    val woman = Woman("1")
    womanRepository.save(woman)
    try {
        manService.runtimeException()
    }catch (e:RuntimeException){}
}
```

- REQUIRED_NEW 两个方法都有自己的事务，所以不会互相影响。
![](https://github.com/MagicienDeCode/images/blob/master/transactional/propagation_required_new.png)
These two functions are in the same transaction. When function `runtimeException` throws an exception, 
it will mark current transaction should be rollback. But `callAnotherTransactionalThrowRuntimeException` catch it, 
so current transaction think it should commit. That's why it throws UnexpectedRollbackException .
Another example with @Transactional(propagation = Propagation.REQUIRED_NEW)
```kotlin
// NO INSERT TO DB
@Transactional(propagation = Propagation.REQUIRED_NEW)
fun runtimeExceptionWithPropagationRequiredNew() {
    val man = Man("1", Woman("1"))
    manRepository.save(man)
    throw RuntimeException("custom exception")
}
 
    /*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as ...
Hibernate: select man0_.id as id1_0_1_, man0_.created_at as created_2_0_1_, ...
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, ...
Hibernate: insert into woman (reference, id) values (?, ?)
     */
    @Transactional
    fun callAnotherTransactional(){
        val woman = Woman("1")
        womanRepository.save(woman)
        try {
            manService.runtimeExceptionWithPropagationRequiredNew()
        }catch (e:RuntimeException){}
    }
```
`runtimeExceptionWithPropagationRequiredNew` will rollback, but `callAnotherTransactional` will commit.

### 2.2 SUPPORTS 如果当前有事务就用，如果没有就不用事务的
```$java
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
```$kotlin
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
### 2.3 NESTED 不建议使用，和JDBC版本有关
```$java
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
```
org.springframework.transaction.NestedTransactionNotSupportedException: 
JpaDialect does not support savepoints - check your JPA provider's capabilities
```
there is no test.  <br/>
I see somewhere says that DataSourceTransactionManager only available for JdbcTemplate and ibatis, need JDBC3.0 

## 3. Isolation 事务的隔离

- Dirty read 脏读: read the uncommitted change of a concurrent transaction. 
    * A事务在读取某一行数据的时候，能够读到B事务还未提交的、对同一行数据的修改。
- Non-repeatable read 不可重复: get different value on re-read of a row if a concurrent transaction updates 
the same row and commits. 
    * 在同一个事务里，在T1时间读取到的某一行的数据，在T2时间再次读取同一行数据时，发生了变化。后者变化可能是被更新了、消失了。
- Phantom read 幻读: get different rows after re-execution of a range query if another transaction adds or 
removes some rows in the range and commits. 
    * 在同一个事务里，用条件A，在T1时间查询到的数据是10行，但是在T2时间查询到的数据多于10行。需要注意的是，
    和 Nonrepeatable read 不同，Phantom read 在T1时读到的数据在T2时不会发生变化。注意，为何只说比10行多，
    那么比10行少就不是 Phantom read 了吗？因为 Nonrepeatable read 包含了数据消失的情况。

| Isolation level | Dirty read | Non-repeatable read | Phantom read |
| -------------   | ---------- | ------------------- | ------------ |
| READ_UNCOMMITTED| may occur  | may occur           | may occur    |
| READ_COMMITTED  |            | may occur           | may occur    | 
| REPEATABLE_READ |            |                     | may occur    | 
| SERIALIZABLE    |            |                     |              | 

### Bonus 

- @Transactional(readOnly = true) call another @Transactional, modification won't be saved
```$java
// this function just change the reference to anna
@Transactional
fun modifyAndSave(uuid: UUID) {
    val woman = womanRepository.findById(uuid).get()
    woman.reference = "anna"
    womanRepository.save(woman)
}
```
```$java
// readonly transactional call modifyAndSave
@Transactional(readOnly = true)
fun testReadOnly(uuid: UUID) {
    println("test begin")
    womanService.modifyAndSave(uuid)
    println("test end")
}
```
Here is the integration test
```$java
@Test
fun `readonly call another transactional won't change`() {
    val woman = womenRepository.save(Woman("111"))
    println("before is: ${woman.reference}")
    testService.testReadOnly(woman.id)
    val womanAfter = womenRepository.findById(woman.id).get()
    println("after is: ${womanAfter.reference}")
}
/*
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
Hibernate: insert into woman (reference, id) values (?, ?)
before is: 111
test begin
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
test end
Hibernate: select woman0_.id as id1_1_0_, woman0_.created_at as created_2_1_0_, woman0_.reference as referenc3_1_0_, woman0_.updated_at as updated_4_1_0_ from woman woman0_ where woman0_.id=?
after is: 111
*/
```
We see the reference doesn't chang.

- in order to fix it: 
    1. Solution 1 : remove **readonly** 
    2. Solution 2 : keep readonly, but add **Propagation.REQUIRES_NEW** to other functions.
