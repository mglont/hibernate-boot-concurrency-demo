package random.service

import org.springframework.data.jpa.repository.JpaRepository
import random.domain.User

interface UserRepository extends JpaRepository<User, Long> {
    User findOne(Long id)
    User findByUsername(String username)
    List<User> findAll()
    User save(User u)
    void flush()
}
