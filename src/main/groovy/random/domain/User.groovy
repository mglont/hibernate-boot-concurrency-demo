package random.domain

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class User implements Serializable {
    private static final long serialVersionUID = 1L

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id
    String username
    String password

    String toString() {
        "User $username -- $password"
    }

    boolean equals(def other) {
        if (!(other instanceof User) || !other) {
            return false
        }
        User otherUser = other //Groovy type coercion
        this.username == otherUser.username &&
                this.password == otherUser.password &&
                this.id == otherUser.id
    }
}
