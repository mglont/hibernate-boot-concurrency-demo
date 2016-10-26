package random.domain

import javax.persistence.Entity
import javax.persistence.Id
import org.springframework.stereotype.Repository

@Entity
//@Repository
class User implements Serializable {
    private static final long serialVersionUID = 1L

    @Id
    Long id
    String username
    String password

    String toString() {
        "User $username -- $password"
    }
}
