package pl.mwa.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pl.mwa.role.RoleRepository;



public class UserServiceTest {

	
	private UserRepository repository;
	private RoleRepository roleRepository;
	private BCryptPasswordEncoder passwordEncoder;
	private UserService service;

	
	
	@Before
	public void setUp() throws Exception {
		repository = Mockito.mock(UserRepository.class);
		roleRepository = Mockito.mock(RoleRepository.class);
		service = new UserService(repository, roleRepository, passwordEncoder);
	}

	@Test
	public void test() {
	}

	@Test
	public void testGetUser() {
		//given
		User user = new User()
				.builder()
				.username("alfa")
				.firstname("beta")
				.lastname("gamma")
				.password("delta")
				.email("alfa@alfa.pl")
				.build();
		when(repository.findByUsername("alfa")).thenReturn(user);
		//when
		User user2 = service.findByUserName("alfa");
		//then
		assertEquals("alfa",user.getUsername());
		assertEquals("beta",user.getFirstname());
		assertEquals("gamma",user.getLastname());
		assertEquals("alfa@alfa.pl",user.getEmail());
	}
	
	@Test
	public void given_user_when_find_other_then_isNotEqual() {
		new User();
		//given
		User user = User
				.builder()
				.username("alfa")
				.firstname("beta")
				.lastname("gamma")
				.password("delta")
				.email("alfa@alfa.pl")
				.build();
		when(repository.findByUsername("alfa")).thenReturn(user);
		//when
		User user2 = service.findByUserName("alfa");
		//then
		assertNotEquals("alfa1",user2.getUsername());
		assertNotEquals("beta1",user2.getFirstname());
		assertNotEquals("gamma1",user2.getLastname());
		assertNotEquals("beta@alfa.pl",user2.getEmail());
	}
	
	
	   @Test
	    public void given_CreateUserDto_with_empty_name_when_creating_user_then_null_pointer_execption_is_thrown(){
	        //given
	        CreateUserDto createUserDto =
	                new CreateUserDto().builder()
	                .username(null)
	                .build();
	        //when
	        try{
	            Long userId = service.createUser(createUserDto);
	            Assert.fail();
	        }catch (NullPointerException e){
	            //then exception is thrown
	        }
	    }
	    
	    @Test
	    public void given_user_should_return_userDto(){
	        User user = new User().builder()
	        		.firstname("Alan")
	        		.lastname("Delone")
	        		.email("alan@delone.fr")
	        		.build();
	        Mockito.when(repository.findById(Mockito.any()))
	                .thenReturn(Optional.of(user));
	        UserDto userDto = service.getUser(1L);
	        Assert.assertEquals(user.getFirstname(), userDto.getFirstname());
	        Assert.assertEquals(user.getLastname(), userDto.getLastname());
	        Assert.assertEquals(user.getEmail(), userDto.getEmail());
	    }
	
	
	
}
