package random.service

import random.domain.User

import org.springframework.data.repository.CrudRepository

interface UserRepository extends CrudRepository<User, Long> {
    User findOne(Long id)
    User findByUsername(String username)
    List<User> findAll()
    User save(User u)
    void flush()
}
