# jpautils
Jpa query processor and model generate

## QueryProcessor
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

Al implementar JpaSpecificationExecutor, nos ofrece ciertos metodos que tienen como parametro Specification<T> spec, ese parametro spec
se puede crear con la clase QueryProcessor.

Si crearamos un servicio para manejar los usuario:

	@Service
	public class UserService {		
		public Page<User> findAll(Map<String,String> query) {   
			QueryProcessor qProcessor = new QueryProcessor();
	      return userRepository.findAll(qProcessor.processQuery(query));
		}
	}
	
y creamos un controlador que use ese servicio:

	
	@RequestMapping("/user")
	public final class UserController {
		@Autowired
    	private UserService userService;
    	
		@GetMapping
		public Page<User> findAll(@RequestParam Map<String,String> query) {        
		    return userService.findAll(query);
		} 
	}
	
Ya con todos estos elementos creados podemos realizar las siguientes consultas con nuestra api
1. /user?username=test: nombre de usuario igual a test.
2. /user?username_in=test,test2: nombre de usuario igual a test o test2.
3. /user?id_lt=3:  id mayor que 3.
