package random.service

import random.domain.User

import org.springframework.data.repository.CrudRepository

interface UserRepository extends CrudRepository<User, Long> {
    User save(User u)
    User findByUsername(String username)
    Set<User> findAll()
}
