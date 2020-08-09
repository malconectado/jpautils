# jpautils
Jpa query processor and model generate

QueryProcessor.java es un clase que transforma un Map<String,String> en un objeto JPASpecification.

La ventaja de esto yace en que se puede generar una api en la que se consulta a la base de datos en funcion de casi cualquier campo de una tabla sin tener que crear consultas en los repositorios. 

Por ejemplo: Tenemos la entidad usuario

@Entity
@Table(name="g_user")
@Getter
@Setter
@NoArgsConstructor
public class User {
	
	@Id
	private Long id;
	
	private String username;

	private String password;
	
}

Para usar JPASpecification el repositorio debe tener esta forma:


import java.util.Optional;

import User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>{

	
}