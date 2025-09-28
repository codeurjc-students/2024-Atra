package codeurjc_students.ATRA.unit;

import codeurjc_students.ATRA.dto.MuralEditDTO;
import codeurjc_students.ATRA.exception.EntityNotFoundException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.exception.PermissionException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import codeurjc_students.ATRA.service.MuralService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class MuralServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private MuralRepository muralRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private MuralService muralService;

    //<editor-fold desc="createMural">
    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "MURAL_SPECIFIC", "MURAL_PUBLIC" })
    void createMural_WithNonPrivateNonPublicVis_ThrowsIncorrectParametersException(VisibilityType visibilityType) {
        //given
        Mural mural = new Mural(
                "","",null,visibilityType,null,null
        );
        //when
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.createMural(mural));
        verify(muralRepository, never()).save(any(Mural.class));
        verify(muralRepository, never()).findByCode(anyString());
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "PUBLIC", "PRIVATE" })
    void createMural_OK(VisibilityType visibilityType) {
        //given
        User user = new User();
        Mural mural = new Mural(
                "My Mural",
                "Mural created by me",
                user,
                visibilityType,
                "thumb".getBytes(),
                "banner".getBytes()
        );
        //when
        when(muralRepository.findByCode(any())).thenReturn(Optional.empty());
        when(muralRepository.save(any())).thenAnswer(iom->iom.getArgument(0));

        //then
        Mural actual = muralService.createMural(mural);
        verify(muralRepository).save(mural); //fails if we save a copy
        verify(muralRepository).findByCode(anyString());

        assertEquals(user.getMemberMurals(), List.of(mural));

        assertEquals(mural.getName(), actual.getName());
        assertEquals(mural.getDescription(), actual.getDescription());
        assertEquals(mural.getOwner(), actual.getOwner());
        assertEquals(mural.getMembers(), actual.getMembers());
        assertEquals(mural.getVisibility(), actual.getVisibility());
        assertEquals(mural.getBanner(), actual.getBanner());
        assertEquals(mural.getThumbnail(), actual.getThumbnail());
    }
    //</editor-fold>

    //<editor-fold desc="joinMural">
    @Test
    void joinMural_InvalidCodeIdCombination_ThrowsIncorrectParametersException() {
        //given
        User user = new User();
        //when
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.joinMural(user, null, null));
        assertThrows(IncorrectParametersException.class, ()->muralService.joinMural(user, "null", 1L));
        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void joinMural_UserInMural_Returns1() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.getMembers().add(user);
        user.getMemberMurals().add(mural);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        Integer actual = muralService.joinMural(user, null, 1L);
        assertEquals(1, actual);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void joinMural_UserBanned_Returns2() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.getBannedUsers().add(user);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        Integer actual = muralService.joinMural(user, null, 1L);
        assertEquals(2, actual);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void joinMural_NotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.getBannedUsers().add(user);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());
        when(muralRepository.findByCode("code")).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, ()->muralService.joinMural(user, null, 1L));
        assertThrows(EntityNotFoundException.class, ()->muralService.joinMural(user, "code", null));

        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void joinMural_WithId_OK() {
        //given
        User user = new User();
        Mural mural = new Mural();
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        Integer actual = muralService.joinMural(user, null, 1L);

        assertEquals(0, actual);
        assertEquals(user.getMemberMurals(), List.of(mural));
        assertEquals(mural.getMembers(), List.of(user));

        verify(muralRepository).findById(1L);
        verify(muralRepository).save(any());
        verify(userRepository).save(any());
    }

    @Test
    void joinMural_WithCode_OK() {
        //given
        User user = new User();
        Mural mural = new Mural();
        //when
        when(muralRepository.findByCode("code")).thenReturn(Optional.of(mural));
        //then
        Integer actual = muralService.joinMural(user, "code", null);

        assertEquals(0, actual);
        assertEquals(user.getMemberMurals(), List.of(mural));
        assertEquals(mural.getMembers(), List.of(user));

        verify(muralRepository).findByCode("code");
        verify(muralRepository).save(any());
        verify(userRepository).save(any());
    }
    //</editor-fold>

    //<editor-fold desc="banUser">
    @Test
    void banUser_UserNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        //when
        lenient().when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, ()-> muralService.banUser(user, 1L, 1L));

        assertFalse(mural.getBannedUsers().contains(user));

        verify(userRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void banUser_MuralNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(EntityNotFoundException.class, ()-> muralService.banUser(user, 1L, 1L));

        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void banUser_NullIds_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        //when
        //then
        assertThrows(IncorrectParametersException.class, ()-> muralService.banUser(user, null, 1L));
        assertThrows(IncorrectParametersException.class, ()-> muralService.banUser(user, 1L, null));

        verify(userRepository, never()).findById(any());
        verify(muralRepository, never()).findById(any());
        verify(muralRepository, never()).save(any());
    }

    @Test
    void banUser_NotMember_ThrowsIncorrectParametersException() {
        //given
        User user = new User();
        User userOwner = new User();
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.banUser(userOwner, 1L, 1L));

        verify(userRepository).findById(1L);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(mural);
    }

    @Test
    void banUser_BanOwner_ThrowsIncorrectParametersException() {
        //given
        User user = new User();
        User userOwner = new User();
        Mural mural = new Mural();
        mural.setOwner(user);
        mural.addMember(user);

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.banUser(userOwner, 1L, 1L));

        verify(userRepository).findById(1L);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(mural);
    }
    @Test
    void banUser_Ok() {
        //given
        Mural mural = new Mural();
        User user = new User();
        User userOwner = new User();
        User otherUser = new User();
        Activity activity = new Activity();
        Activity otherAct = new Activity();
        Route route = new Route();

        activity.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));
        route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));

        user.setName("pepe");
        userOwner.setName("juan");
        otherUser.setName("francisco");

        activity.setUser(user);
        route.setCreatedBy(user);

        otherAct.setUser(otherUser);
        otherAct.setRoute(route);

        mural.setOwner(userOwner);
        mural.getMembers().add(userOwner);
        mural.getMembers().add(user);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(routeRepository.findAllByCreatedBy(user)).thenReturn(List.of(route));
        when(activityRepository.findByUser(user)).thenReturn(List.of(activity));
        when(activityRepository.getByRoute(route)).thenReturn(List.of(otherAct));
        //then
        List<User> actual = muralService.banUser(userOwner, 1L, 1L);

        //ban conditions
        assertEquals(List.of(userOwner), actual);
        assertFalse(mural.getMembers().contains(user));
        assertTrue(mural.getBannedUsers().contains(user));

        verify(muralRepository).findById(1L);
        verify(muralRepository, atLeast(1)).save(mural);
        verify(activityRepository, atLeast(1)).save(any(Activity.class));
        verify(routeRepository, atLeast(1)).save(any(Route.class));

        //remove user conditions
        //Mural can see 1 route and 2 activities.
        //when the user is banned, the route and one activity are no longer visible
        //the other activity should have its route as null
        assertTrue(route.getVisibility().getAllowedMurals()==null || !route.getVisibility().getAllowedMurals().contains(1L));
        assertTrue(activity.getVisibility().getAllowedMurals()==null || !activity.getVisibility().getAllowedMurals().contains(1L));
        assertNull(otherAct.getRoute());
        assertNotNull(mural.getOwner());
    }
    //</editor-fold>

    //<editor-fold desc="removeUserFromMural">
    @Test
    void removeUserFromMural_OK() {
        //Mural can see 1 route and 2 activities.
        //when the user is banned, the route and one activity are no longer visible
        //the other activity should have its route as null
        //given
        Mural mural = new Mural();
        User user = new User();
        User userOwner = new User();
        User otherUser = new User();
        Activity activity = new Activity();
        Activity otherAct = new Activity();
        Route route = new Route();

        activity.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));
        route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));

        user.setName("pepe");
        userOwner.setName("juan");
        otherUser.setName("francisco");

        activity.setUser(user);
        route.setCreatedBy(user);

        otherAct.setUser(otherUser);
        otherAct.setRoute(route);

        mural.setOwner(userOwner);
        mural.getMembers().add(userOwner);
        mural.getMembers().add(user);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(routeRepository.findAllByCreatedBy(user)).thenReturn(List.of(route));
        when(activityRepository.findByUser(user)).thenReturn(List.of(activity));
        when(activityRepository.getByRoute(route)).thenReturn(List.of(otherAct));
        //then
        List<User> actual = muralService.removeUserFromMural(userOwner, 1L, 1L, null);

        assertEquals(List.of(userOwner), actual);
        assertFalse(mural.getMembers().contains(user));
        assertTrue(route.getVisibility().getAllowedMurals()==null || !route.getVisibility().getAllowedMurals().contains(1L));
        assertTrue(activity.getVisibility().getAllowedMurals()==null || !activity.getVisibility().getAllowedMurals().contains(1L));
        assertNull(otherAct.getRoute());
        assertNotNull(mural.getOwner());

        verify(muralRepository).findById(1L);
        verify(muralRepository, atLeast(1)).save(mural);
        verify(activityRepository, atLeast(1)).save(any(Activity.class));
        verify(routeRepository, atLeast(1)).save(any(Route.class));
    }

    @Test
    void removeUserFromMural_TargetNotMember_ThrowsIncorrectParametersException() {
        //given
        User user = new User();
        User userOwner = new User();
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.removeUserFromMural(userOwner, 1L, 1L, null));

        verify(userRepository).findById(1L);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(mural);
    }

    @Test
    void removeUserFromMural_TargetLastMember_DeletesMural() {
        //given
        User userOwner = new User();
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        mural.addMember(userOwner);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOwner));
        //then
        muralService.removeUserFromMural(userOwner, 1L, 1L, null);

        verify(userRepository).findById(1L);
        verify(muralRepository, atLeast(1)).findById(1L);
        verify(muralRepository, never()).save(mural);
        verify(muralRepository).delete(mural); //could check repercussions, but we leave that to the delete tests
    }

    @Test
    void removeUserFromMural_TargetOwnerNullInheritor_UpdatesInheritor() {
        //given
        User userMember = new User();userMember.setId(2L);
        User userOwner = new User();userOwner.setId(1L);
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        mural.addMember(userOwner);
        mural.addMember(userMember);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOwner));
        //then
        muralService.removeUserFromMural(userOwner, 1L, 1L, null);

        assertNotNull(mural.getOwner());
        assertEquals(userMember, mural.getOwner());
        assertFalse(userOwner.getMemberMurals().contains(mural));

        verify(userRepository).findById(1L);
        verify(muralRepository, atLeast(1)).findById(1L);
        verify(muralRepository).save(mural);
        verify(userRepository).save(userOwner);
    }

    @Test
    void removeUserFromMural_TargetOwnerWithInheritorNotMember_ThrowsIncorrectParametersException() {
        //given
        User userNewOwner = new User();userNewOwner.setId(3L);
        User userMember = new User();userMember.setId(2L);
        User userOwner = new User();userOwner.setId(1L);
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        mural.addMember(userOwner);
        mural.addMember(userMember);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOwner));
        when(userRepository.findById(3L)).thenReturn(Optional.of(userNewOwner));
        //then
        assertThrows(IncorrectParametersException.class, ()->muralService.removeUserFromMural(userOwner, 1L, 1L, 3L));

        assertNotNull(mural.getOwner());

        verify(userRepository).findById(1L);
        verify(userRepository).findById(3L);
        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(mural);
    }

    @Test
    void removeUserFromMural_TargetOwnerWithInheritor_UpdatesInheritor() {
        //given
        User userNewOwner = new User();userNewOwner.setId(3L);
        User userMember = new User();userMember.setId(2L);
        User userOwner = new User();userOwner.setId(1L);
        Mural mural = new Mural();
        mural.setOwner(userOwner);
        mural.addMember(userOwner);
        mural.addMember(userMember);
        mural.addMember(userNewOwner);
        mural.setId(1L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(userOwner));
        when(userRepository.findById(3L)).thenReturn(Optional.of(userNewOwner));
        //then
        muralService.removeUserFromMural(userOwner, 1L, 1L, 3L);

        assertNotNull(mural.getOwner());
        assertEquals(userNewOwner, mural.getOwner());
        assertFalse(userOwner.getMemberMurals().contains(mural));

        verify(userRepository).findById(1L);
        verify(userRepository).findById(3L);
        verify(muralRepository).findById(1L);
        verify(muralRepository).save(mural);
        verify(userRepository).save(userOwner);
    }
    //</editor-fold>

    //<editor-fold desc="unbanUser">
    @Test
    void unbanUser_UserNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.setOwner(user);
        mural.banUser(user);
        //when
        lenient().when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, ()-> muralService.unbanUser(user, 1L, 1L));

        assertTrue(mural.getBannedUsers().contains(user));

        verify(userRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void unbanUser_MuralNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.setOwner(user);
        mural.banUser(user);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(EntityNotFoundException.class, ()-> muralService.unbanUser(user, 1L, 1L));

        assertTrue(mural.getBannedUsers().contains(user));

        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void unbanUser_UserNotOwner_ThrowsPermissionException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.banUser(user);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        assertThrows(PermissionException.class, ()-> muralService.unbanUser(user, 1L, 1L));

        assertTrue(mural.getBannedUsers().contains(user));

        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void unbanUser_Ok() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.setOwner(user);
        mural.banUser(user);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        //then
        muralService.unbanUser(user, 1L, 1L);

        assertFalse(mural.getBannedUsers().contains(user));

        verify(userRepository).findById(1L);
        verify(muralRepository).findById(1L);
        verify(muralRepository).save(any());
    }
    //</editor-fold>

    //<editor-fold desc="deleteMural">
    @Test
    void deleteMural_MuralNotFound_EntityNotFound() {
        //given
        User user = new User();
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, ()->muralService.deleteMural(user, 1L));

        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(activityRepository, never()).save(any());
        verify(routeRepository, never()).save(any());
        verify(muralRepository, never()).delete(any());
    }

    @Test
    void deleteMural_NotOwner_PermissionException() {
        //given
        User user = new User();
        Mural mural = new Mural();

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        assertThrows(PermissionException.class, ()->muralService.deleteMural(user, 1L));

        verify(muralRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(activityRepository, never()).save(any());
        verify(routeRepository, never()).save(any());
        verify(muralRepository, never()).delete(any());
    }

    @Test
    void deleteMural_Ok() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.setId(1L);
        mural.setOwner(user);
        mural.addMember(user);
        user.addMemberMural(mural);

        Activity activity = new Activity();
        activity.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));
        Route route = new Route();
        route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(activityRepository.findVisibleToMural(anyLong(), anyList())).thenReturn(List.of(activity));
        when(routeRepository.findVisibleToMural(anyLong())).thenReturn(List.of(route));
        //then
        muralService.deleteMural(user, 1L);

        assertFalse(activity.getVisibility().getAllowedMurals().contains(1L));
        assertFalse(user.getMemberMurals().contains(mural));

        verify(userRepository, atLeast(1)).save(any());
        verify(activityRepository, atLeast(1)).save(any());
        verify(routeRepository, atLeast(1)).save(any());
        verify(muralRepository).delete(mural);
    }
    //</editor-fold>

    //<editor-fold desc="editMural">
    @Test
    void editMural_MuralNotFound_EntityNotFoundException() {
        //given
        User user = new User();
        MuralEditDTO newMural = new MuralEditDTO("a","b",2L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, ()->muralService.editMural(user, 1L, newMural));

        verify(muralRepository).findById(1L);
        verify(muralRepository, never()).save(any());
    }

    @Test
    void editMural_NotOwner_PermissionException() {
        //given
        User user = new User();
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        MuralEditDTO newMural = new MuralEditDTO("a", "b", 2L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        assertThrows(PermissionException.class, () -> muralService.editMural(user, 1L, newMural));

        assertEquals("pepe", mural.getName());
        assertEquals("desc", mural.getDescription());
        assertNull(mural.getOwner());

        verify(muralRepository, never()).save(any());
        verify(muralRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void editMural_NewOwnerNotFound_EntityNotFoundException_AndNoChanges() {
        //given
        User user = new User(); user.setId(1L);
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        mural.setOwner(user);
        MuralEditDTO newMural = new MuralEditDTO("a", "b", 2L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        //then
        assertThrows(EntityNotFoundException.class, () -> muralService.editMural(user, 1L, newMural));

        assertEquals("pepe", mural.getName());
        assertEquals("desc", mural.getDescription());
        assertNotNull(mural.getOwner());
        assertEquals(user, mural.getOwner());

        verify(muralRepository, never()).save(any());
        verify(muralRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void editMural_NewOwnerNotMember_EntityNotFoundException_AndNoChanges() {
        //given
        User user = new User(); user.setId(1L);
        User newOwner = new User(); newOwner.setId(2L);
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        mural.setOwner(user);
        MuralEditDTO newMural = new MuralEditDTO("a", "b", 2L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        //then
        assertThrows(IncorrectParametersException.class, () -> muralService.editMural(user, 1L, newMural));

        assertEquals("pepe", mural.getName());
        assertEquals("desc", mural.getDescription());
        assertNotNull(mural.getOwner());
        assertEquals(user, mural.getOwner());

        verify(muralRepository, never()).save(any());
        verify(muralRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void editMural_NullValues_DoesNothing() {
        //given
        User user = new User(); user.setId(1L);
        User newOwner = new User(); newOwner.setId(2L);
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        mural.setOwner(user);
        MuralEditDTO newMural = new MuralEditDTO(null, null, null);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        Mural actual = muralService.editMural(user, 1L, newMural);

        assertEquals("pepe", mural.getName());
        assertEquals("desc", mural.getDescription());
        assertNotNull(mural.getOwner());
        assertEquals(user, mural.getOwner());
        assertEquals(mural, actual);

        verify(muralRepository, atMost(1)).save(any());
        verify(muralRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void editMural_EmptyValues_DoesNothing() {
        //given
        User user = new User(); user.setId(1L);
        User newOwner = new User(); newOwner.setId(2L);
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        mural.setOwner(user);
        MuralEditDTO newMural = new MuralEditDTO("", "", null);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        //then
        Mural actual = muralService.editMural(user, 1L, newMural);

        assertEquals("pepe", mural.getName());
        assertEquals("desc", mural.getDescription());
        assertNotNull(mural.getOwner());
        assertEquals(user, mural.getOwner());
        assertEquals(mural, actual);

        verify(muralRepository, atMost(1)).save(any());
        verify(muralRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void editMural_Ok() {
        //given
        User user = new User(); user.setId(1L);
        User newOwner = new User(); newOwner.setId(2L);
        Mural mural = new Mural();
        mural.setName("pepe");
        mural.setDescription("desc");
        mural.setOwner(user);
        mural.addMember(user);
        mural.addMember(newOwner);
        MuralEditDTO newMural = new MuralEditDTO("a", "b", 2L);
        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newOwner));
        //then
        Mural actual = muralService.editMural(user, 1L, newMural);

        assertEquals("a", mural.getName());
        assertEquals("b", mural.getDescription());
        assertEquals(newOwner, mural.getOwner());
        assertEquals(mural, actual);

        verify(muralRepository).save(mural);
        verify(muralRepository).findById(1L);
        verify(userRepository).findById(2L);
    }
    //</editor-fold>

}