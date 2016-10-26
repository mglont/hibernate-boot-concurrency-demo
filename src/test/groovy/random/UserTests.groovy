package random

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.ContextConfiguration
import random.domain.User
import random.service.UserRepository

import static org.junit.Assert.*

@RunWith(SpringRunner)
@SpringBootTest(classes = [AppConfig.class])
@ContextConfiguration
class UserTests {
    @Autowired
    UserRepository userRepository

    @Test
    void testSomething() {
        assertNotNull userRepository

        final String PWD = 'pass'

        def u = new User(id: null, username: "foo0", password: PWD)
        assertNotNull userRepository.save(u)
    }
}
