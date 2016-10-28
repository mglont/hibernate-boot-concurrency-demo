package random.domain

import groovy.transform.CompileStatic

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import random.service.UserRepository

import java.util.concurrent.locks.ReentrantLock

import static org.junit.Assert.*

@CompileStatic
@RunWith(SpringRunner)
@SpringBootTest
class UserTests {
    @Autowired
    UserRepository userRepository

    @Autowired
    EntityManagerFactory emf

    private void userDiff(User orig, User persisted, User fetchedById) {
        if (orig != persisted) {
            printt "orig $orig and persisted $persisted not same"
        }
        if (orig != fetchedById) {
            printt "orig $orig and fetched by id $fetchedById not same"
        }
    }

    private void printt(def msg) {
        println "${Thread.currentThread().name} -- $msg"
    }

    @Test
    void testConcurrentInsertionAndRetrieval() {
        final int ORIG_SIZE = 32; // how many to create in the first session
        final String TEMPLATE = "testUsername"
        final AtomicLong idGenerator = new AtomicLong()

        createInitialUsers(ORIG_SIZE, TEMPLATE, idGenerator)

        // how many new ones to create in different persistence contexts/sessions
        final int SIZE = 4 * Runtime.getRuntime().availableProcessors()
        final CountDownLatch startLatch = new CountDownLatch(1)
        final CountDownLatch finishLatch = new CountDownLatch(SIZE)
        ExecutorService pool = Executors.newFixedThreadPool(SIZE)

        SIZE.times { i ->
            final int originalIdx = i + 1
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await()
                    } catch (InterruptedException e) {
                        println "Worker thread $originalIdx interrupted...: $e"
                        finishLatch.countDown()
                        Thread.currentThread().interrupt()
                    }
                    EntityManager thisEm = emf.createEntityManager()
                    User newUser = null
                    try {
                        long newIdx = idGenerator.incrementAndGet()
                        String newUsername = "$TEMPLATE$newIdx"
                        assertNull(userRepository.findByUsername(newUsername))

                        // creating new content in this session is all right
                        thisEm.getTransaction().begin()
                        newUser = new User()
                        newUser.setUsername(newUsername)
                        User persistedUser = userRepository.save(newUser)
                        User newUserFromDb = userRepository.findOne(newUser.id)
                        userDiff(newUser, persistedUser, newUserFromDb)
                        assertNotNull(persistedUser)
                        thisEm.flush()
                        thisEm.getTransaction().commit()
                    } catch(Exception e) {
                        println("Rolling back new user $newUser because of $e")
                        thisEm.getTransaction().rollback()
                        thisEm.close()
                        finishLatch.countDown()
                    }

                    // getting something inserted in another session is problematic
                    String knownUsername = "$TEMPLATE$originalIdx"
                    User knownUser = userRepository.findByUsername(knownUsername)
                    try {
                        assertNotNull knownUser
                    } catch (Throwable t) {
                        println "${Thread.currentThread().name}: $t"
                        t.printStackTrace(System.out)
                    } finally {
                        if (thisEm?.isOpen()) {
                            thisEm.close()
                        }
                        if (thisEm.isOpen()) {
                            println  "EM for ${Thread.currentThread().name} still open"
                        }
                        println """${Thread.currentThread().name}: count \
${userRepository.count()} -- inserted $newUser and found $knownUser"""
                        finishLatch.countDown()
                    }
                }
            })
        }
        // simulate throwing concurrent requests to another service
        startLatch.countDown()
        pool.shutdown()
        try {
            finishLatch.await()
        } catch(InterruptedException e) {
            println("Interrupted main thread while waiting for worker threads: $e")
            Thread.currentThread().interrupt()
        } finally {
            try {
                pool.awaitTermination(1, TimeUnit.SECONDS)
                assertEquals("some worker threads were still running", 0, finishLatch.count)
                List<Runnable> queuingJobs = pool.shutdownNow()
                assertEquals(0, queuingJobs.size())
                assertTrue(pool.isTerminated())
            } catch(InterruptedException ignored) {
                println "was interrupted while shutting down the pool"
            }
        }

        // see what we got
        EntityManager em2 = emf.createEntityManager()
        for (int i = 1; i <= ORIG_SIZE + SIZE; ++i) {
            User expected = userRepository.findByUsername("$TEMPLATE$i")
            assertNotNull expected
            println i
        }
        assertEquals("did not persist all instances", ORIG_SIZE + SIZE, userRepository.count())
        em2.close()
    }

    void createInitialUsers(int count, String template, AtomicLong idGenerator) {
        EntityManager em1 = emf.createEntityManager()
        em1.getTransaction().begin()
        try {
            count.times { int i ->
                final long id = idGenerator.incrementAndGet()
                User thisUser = new User()
                thisUser.setUsername("$template$id")
                thisUser.setId(id)
                userRepository.save(thisUser)
            }
            em1.flush()
            em1.getTransaction().commit()
        } catch (Exception e) {
            println("Cannot insert user from original session: $e")
            if (em1.getTransaction().active) {
                em1.getTransaction().rollback()
            }
        } finally {
            em1.close()
        }
        assertFalse("em1 is still open",  em1.isOpen())
    }
}
