package codeurjc_students.atra.unit;

import codeurjc_students.atra.dto.NewUserDTO;
import codeurjc_students.atra.dto.UserDTO;
import codeurjc_students.atra.exception.EntityNotFoundException;
import codeurjc_students.atra.exception.IncorrectParametersException;
import codeurjc_students.atra.exception.PermissionException;
import codeurjc_students.atra.model.Mural;
import codeurjc_students.atra.model.Route;
import codeurjc_students.atra.model.User;
import codeurjc_students.atra.model.auxiliary.Visibility;
import codeurjc_students.atra.model.auxiliary.VisibilityType;
import codeurjc_students.atra.repository.ActivityRepository;
import codeurjc_students.atra.repository.MuralRepository;
import codeurjc_students.atra.repository.RouteRepository;
import codeurjc_students.atra.repository.UserRepository;
import codeurjc_students.atra.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private MuralRepository muralRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    //<editor-fold desc="createUser">
    @Test
    void whenCreateUserOkThenDBIsUpdated() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername("pepe");
        user.setPassword("pass");
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when
        when(userRepository.existsByUsername("pepe")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.createUser(user);

        assertEquals("pepe", returnedUser.getUsername());
        assertEquals("encoded", returnedUser.getPassword());
        assertEquals("juan", returnedUser.getName());
        assertEquals("francisco@gmail.com", returnedUser.getEmail());

        verify(passwordEncoder).encode("pass");
        verify(userRepository).existsByUsername("pepe");
        verify(userRepository).save(any(User.class));

    }

    @Test
    void createUserWithExistingUsernameThrowsIncorrectParametersException() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername("pepe");
        user.setPassword("pass");
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when
        when(userRepository.existsByUsername("pepe")).thenReturn(true);

        //then
        assertThrows(IncorrectParametersException.class, () -> userService.createUser(user));

        verify(userRepository).existsByUsername("pepe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserNullUsernameThrowsIncorrectParametersException() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername(null);
        user.setPassword("pass");
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when

        //then
        assertThrows(IncorrectParametersException.class, () -> userService.createUser(user));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserEmptyUsernameThrowsIncorrectParametersException() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername("");
        user.setPassword("pass");
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when

        //then
        assertThrows(IncorrectParametersException.class, () -> userService.createUser(user));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserNullPasswordThrowsIncorrectParametersException() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername("pepe");
        user.setPassword(null);
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when

        //then
        assertThrows(IncorrectParametersException.class, () -> userService.createUser(user));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserEmptyPasswordThrowsIncorrectParametersException() {
        //given
        NewUserDTO user = new NewUserDTO();
        user.setUsername("pepe");
        user.setPassword("");
        user.setName("juan");
        user.setEmail("francisco@gmail.com");

        //when

        //then
        assertThrows(IncorrectParametersException.class, () -> userService.createUser(user));

        verify(userRepository, never()).existsByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
    //</editor-fold>

    //<editor-fold desc="patchUser">
    @Test
    void whenPatchUserAllFieldsOkThenDbIsUpdated() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setEmail("juan@francisco.com");
        existingUser.setName("Don Iñigo Montoya");
        UserDTO newUserDTO = new UserDTO();
        newUserDTO.setUsername("pepe");
        newUserDTO.setName("juan");
        newUserDTO.setEmail("francisco@gmail.com");

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.patchUser(existingUser, 1L, newUserDTO);

        assertEquals("pepe", returnedUser.getUsername());
        assertEquals("complexPassword", returnedUser.getPassword());
        assertEquals("juan", returnedUser.getName());
        assertEquals("francisco@gmail.com", returnedUser.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).save(returnedUser);
    }

    @Test
    void whenPatchUserSomeFieldsOkThenDbIsUpdated() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setEmail("juan@francisco.com");
        existingUser.setName("Don Iñigo Montoya");
        UserDTO newUserDTO = new UserDTO();
        newUserDTO.setUsername("pepe");

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.patchUser(existingUser, 1L, newUserDTO);

        assertEquals("pepe", returnedUser.getUsername());
        assertEquals("complexPassword", returnedUser.getPassword());
        assertEquals("Don Iñigo Montoya", returnedUser.getName());
        assertEquals("juan@francisco.com", returnedUser.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).save(returnedUser);
    }

    @Test
    void patchDifferentUserThrowsPermissionException() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setId(2L);
        User otherUser = new User();
        otherUser.setId(1L);
        UserDTO newUserDTO = new UserDTO();
        newUserDTO.setUsername("pepe");

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(otherUser));

        //then
        assertThrows(PermissionException.class, ()->userService.patchUser(existingUser, 1L, newUserDTO));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void patchInexistentUserThrowsEntityNotFoundException() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        UserDTO newUserDTO = new UserDTO();
        newUserDTO.setUsername("pepe");

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->userService.patchUser(existingUser, 1L, newUserDTO));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void patchUserNullFieldsDoesNotEdit() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setEmail("juan@francisco.com");
        existingUser.setName("Don Iñigo Montoya");
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(null);
        userDTO.setName(null);
        userDTO.setEmail(null);

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.patchUser(existingUser, 1L, userDTO);

        assertEquals("Juan Francisco", returnedUser.getUsername());
        assertEquals("Don Iñigo Montoya", returnedUser.getName());
        assertEquals("juan@francisco.com", returnedUser.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).save(returnedUser);
    }

    @Test
    void patchUserEmptyFieldsEditsPartially() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setEmail("juan@francisco.com");
        existingUser.setName("Don Iñigo Montoya");
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("");
        userDTO.setName("");
        userDTO.setEmail("");

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.patchUser(existingUser, 1L, userDTO);

        assertEquals("Juan Francisco", returnedUser.getUsername());
        assertEquals("Juan Francisco", returnedUser.getName());
        assertEquals("", returnedUser.getEmail());

        verify(userRepository).findById(1L);
        verify(userRepository).save(returnedUser);
    }

    @Test
    void patchRolesDoesNothing() {
        //given
        User existingUser = new User("Juan Francisco", "complexPassword");
        existingUser.setEmail("juan@francisco.com");
        existingUser.setName("Don Iñigo Montoya");
        existingUser.setRoles(List.of("USER"));
        UserDTO userDTO = new UserDTO();
        userDTO.setRoles(List.of("ADMIN","USER"));


        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(u->u.getArgument(0)); //return the first argument save was called with

        //then
        User returnedUser = userService.patchUser(existingUser, 1L, userDTO);

        assertNotEquals(List.of("ADMIN","USER"), returnedUser.getRoles());

        verify(userRepository).findById(1L);
        verify(userRepository).save(returnedUser);
    }
    //</editor-fold>

    //<editor-fold desc="deleteUser">

    @Test
    void deleteUserOk() {
        //given
        Mural muralOwned = mock(Mural.class);
        Mural mural2 = mock(Mural.class);
        Mural mural3 = mock(Mural.class);

        Route routePR = mock(Route.class);
        Route routeMS = mock(Route.class);
        Route routeMP = mock(Route.class);
        Route routePU = mock(Route.class);

        User user = new User();
        user.setMemberMurals(List.of(muralOwned, mural2, mural3));

        //when
        when(routePR.getVisibility()).thenReturn(new Visibility(VisibilityType.PRIVATE));
        when(routeMS.getVisibility()).thenReturn(new Visibility(VisibilityType.MURAL_SPECIFIC));
        when(routeMP.getVisibility()).thenReturn(new Visibility(VisibilityType.MURAL_PUBLIC));
        when(routePU.getVisibility()).thenReturn(new Visibility(VisibilityType.PUBLIC));

        when(routeRepository.findAllByCreatedBy(user)).thenReturn(List.of(routePR, routeMS, routeMP, routePU));
        when(muralRepository.findByOwner(user)).thenReturn(List.of(muralOwned));

        //then
        userService.deleteUser(user);

        verify(routePR, never()).setVisibility(any());
        verify(routeMS).setVisibility(new Visibility(VisibilityType.PUBLIC));
        verify(routeMP).setVisibility(new Visibility(VisibilityType.PUBLIC));
        verify(routePU, never()).setVisibility(any());

        verify(muralOwned).removeOwner();
        verify(mural2).removeMember(user);
        verify(mural3).removeMember(user);

        verify(activityRepository).deleteAll(anyCollection()); //does this just test that it was called at least once with any params?
        verify(routeRepository).findAllByCreatedBy(user);
        verify(routeRepository).save(routeMS);
        verify(routeRepository).save(routeMP);
        verify(muralRepository, times(2)).save(muralOwned);
        verify(muralRepository).save(mural2);
        verify(muralRepository).save(mural3);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteNullUserDoesNothing() {
        User user = new User();

        verify(activityRepository, never()).deleteAll(); //does this just test that it was called at least once with any params?
        verify(routeRepository, never()).save(any());
        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).delete(user);

    }

    //</editor-fold>

}

