package random.domain

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicLong
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import random.service.UserRepository

import static org.junit.Assert.*

@RunWith(SpringRunner)
@SpringBootTest
class UserTests {
    @Autowired
    UserRepository userRepository

    @Autowired
    EntityManagerFactory emf

    @Test
    void testSomething() {
        final int ORIG_SIZE = 10; // how many to create in the first session
        final String BAR = "testUsername"
        final AtomicLong idGenerator = new AtomicLong()

        EntityManager em1 = emf.createEntityManager()

        em1.getTransaction().begin()

        try {
            for (int i = 0; i < ORIG_SIZE; i++) {
                User thisUser = new User()
                thisUser.setUsername(BAR + i)
                thisUser.setId(idGenerator.incrementAndGet())
                thisUser = userRepository.save(thisUser)
                em1.flush()
            }
            em1.getTransaction().commit()
        } catch (Exception e) {
            System.err.println("Cannot insert user from original session")
            em1.getTransaction().rollback()
            throw e
        } finally {
            if (em1?.isOpen()) {
                em1.close() //end of first session
            }
        }

        // how many new ones to create in different persistence contexts/sessions
        final int SIZE = 2 * Runtime.getRuntime().availableProcessors()
        final CountDownLatch startLatch = new CountDownLatch(1)
        final CountDownLatch finishLatch = new CountDownLatch(SIZE)
        final AtomicReferenceArray<User> userRefs = new AtomicReferenceArray<>(SIZE)
        ExecutorService pool = Executors.newFixedThreadPool(SIZE)

        SIZE.times { i ->
            final int originalIdx = i
            final int newIdx = ORIG_SIZE + 1 + i
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        startLatch.await()
                    } catch (InterruptedException e) {
                        finishLatch.countDown()
                        Thread.currentThread().interrupt()
                    }
                    EntityManager thisEm = emf.createEntityManager()
                    User newUser
                    try {
                        thisEm.getTransaction().begin()
                        // creating new content in this session is all right
                        String newUsername = BAR + newIdx
                        assertNull(userRepository.findByUsername(newUsername))
                        newUser = new User()
                        newUser.setUsername(newUsername)
                        newUser.setId(idGenerator.incrementAndGet())
                        newUser = userRepository.save(newUser)
                        thisEm.flush()
                        assertNotNull(newUser)
                        thisEm.getTransaction().commit()
                    } catch(Exception e) {
                        System.out.println("Rolling back new user $newUser because of $e")
                        thisEm.getTransaction().rollback()
                        throw e
                    }
                   userRefs.compareAndSet(originalIdx, null, newUser)

                    // getting something inserted in another session is problematic
                    String knownUsername = BAR + originalIdx
                    User knownUser = userRepository.findByUsername(knownUsername)
                    try {
                        assertNotNull(knownUser)
                        assertEquals(newIdx, userRepository.count())
                    } finally {
                        if (thisEm?.isOpen()) {
                            thisEm.close()
                        }
                        finishLatch.countDown()
                    }
                }
            })
        }
        // simulate throwing concurrent requests to another service
        startLatch.countDown()
        try {
            finishLatch.await()
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt()
        } finally {
            try {
                pool.shutdown()
                pool.awaitTermination(1, TimeUnit.SECONDS)
                List<Runnable> queuingJobs = pool.shutdownNow()
                assertEquals(0, queuingJobs.size())
                assertTrue(pool.isTerminated())
            } catch(InterruptedException ignored) {}
        }

        // see what we got
        EntityManager em2 = emf.createEntityManager()
        ORIG_SIZE.times { int i ->
            User expected = userRepository.findByUsername(BAR + i)
            assertNotNull(expected)
        }

        SIZE.times { int i ->
            User actual = userRefs.get(i)
            User expected = userRepository.findByUsername(BAR + (ORIG_SIZE + 1 + i))
            assertNotNull(actual)
            assertNotNull(expected)
            assertEquals(expected, actual)
        }
        em2.close()
    }
}
