package codeurjc_students.ATRA.unit;

import codeurjc_students.ATRA.dto.ActivityEditDTO;
import codeurjc_students.ATRA.exception.EntityNotFoundException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.exception.PermissionException;
import codeurjc_students.ATRA.exception.VisibilityException;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.service.ActivityService;
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
public class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private MuralRepository muralRepository;
    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private ActivityService activityService;

    //<editor-fold desc="changeVisibility">
    @Test
    void changeVisibilityOk() {
        //given
        Activity activity = new Activity();
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any())).thenAnswer(iom->iom.getArgument(0));

        //then
        activityService.changeVisibility(1L, VisibilityType.PUBLIC, null);
        verify(activityRepository).findById(1L);
        verify(activityRepository, times(1)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isPublic());
        assertNull(activity.getVisibility().getAllowedMurals());


        activityService.changeVisibility(1L, VisibilityType.MURAL_PUBLIC, null);
        verify(activityRepository, times(2)).findById(1L);
        verify(activityRepository, times(2)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isMuralPublic());
        assertNull(activity.getVisibility().getAllowedMurals());


        activityService.changeVisibility(1L, VisibilityType.MURAL_SPECIFIC, null);
        verify(activityRepository, times(3)).findById(1L);
        verify(activityRepository, times(3)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isMuralSpecific());
        assertNotNull(activity.getVisibility().getAllowedMurals());

        activityService.changeVisibility(1L, VisibilityType.PRIVATE, null);
        verify(activityRepository, times(4)).findById(1L);
        verify(activityRepository, times(4)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isPrivate());
        assertNull(activity.getVisibility().getAllowedMurals());

        activityService.changeVisibility(1L, VisibilityType.MURAL_SPECIFIC, List.of(1L,2L,3L));
        verify(activityRepository, times(5)).findById(1L);
        verify(activityRepository, times(5)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isMuralSpecific());
        assertEquals(activity.getVisibility().getAllowedMurals(), new HashSet<>(List.of(1L,2L,3L)));

        activityService.changeVisibility(1L, VisibilityType.MURAL_PUBLIC, List.of(1L,2L,3L));
        verify(activityRepository, times(6)).findById(1L);
        verify(activityRepository, times(6)).save(any(Activity.class));
        assertTrue(activity.getVisibility().isMuralPublic());
        assertNull(activity.getVisibility().getAllowedMurals());
    }
    @Test
    void changeVisibility_withInexistentActivity_throwsEntityNotFoundException() {
        //given
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->activityService.changeVisibility(1L, VisibilityType.PUBLIC, null));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).save(any(Activity.class));
    }
    //</editor-fold>

    //<editor-fold desc="removeActivityFromRoute">
    @Test
    void removeActivityFromRouteOk() {
        //given
        User user = mock(User.class);
        Route route = mock(Route.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setRoute(route);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        activityService.removeActivityFromRoute(user,1L,1L);

        verify(activityRepository).findById(1L);
        verify(routeRepository).findById(1L);
        verify(activityRepository).save(activity);
        verify(routeRepository, never()).save(any(Route.class));

        assertNull(activity.getRoute());
    }

    @Test
    void removeRoute_WithNoRoute_Ok() {
        //given
        User user = mock(User.class);
        Route route = mock(Route.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setRoute(route);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        activityService.removeRoute(user,1L);

        verify(activityRepository).findById(1L);
        verify(routeRepository, never()).findById(1L);
        verify(activityRepository).save(activity);
        verify(routeRepository, never()).save(any(Route.class));

        assertNull(activity.getRoute());
    }

    @Test
    void removeActivityFromRoute_withInexistentRoute_ThrowsEntityNotFoundException() {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        //when
        lenient().when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class,()->activityService.removeActivityFromRoute(user,1L,1L));

        verify(routeRepository).findById(1L);
        verify(activityRepository, never()).save(any(Activity.class));
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void removeActivityFromRoute_withInexistentActivity_ThrowsEntityNotFoundException() {
        //given
        User user = mock(User.class);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(routeRepository.findById(1L)).thenReturn(Optional.of(mock(Route.class)));


        //then
        assertThrows(EntityNotFoundException.class,()->activityService.removeActivityFromRoute(user,1L,1L));

        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).save(any(Activity.class));
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void removeActivityFromRoute_withActivityNotPartOfRoute_throwsIncorrectParameterException() {
        //given
        User user = mock(User.class);
        Route route = mock(Route.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setRoute(null);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(IncorrectParametersException.class, ()->activityService.removeActivityFromRoute(user,1L,1L));

        verify(activityRepository).findById(1L);
        verify(routeRepository).findById(1L);
        verify(activityRepository, never()).save(activity);
        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void removeActivityFromRoute_withActivityNotOwned_throwsPermissionException() {
        //given
        User juan = mock(User.class);
        juan.setName("juan");
        User pepe = mock(User.class);
        pepe.setName("pepe");
        Route route = mock(Route.class);
        Activity activity = new Activity();
        activity.setUser(pepe);
        activity.setRoute(route);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        lenient().when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(PermissionException.class, ()->activityService.removeActivityFromRoute(juan,1L,1L));

        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).save(activity);
        verify(routeRepository, never()).save(any(Route.class));
    }

    //</editor-fold>

    //<editor-fold desc="getActivity">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void getActivityOk_ActivityOwned(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Activity actual = activityService.getActivity(user,1L, null);
        verify(activityRepository).findById(1L);
        assertEquals(activity, actual);
    }

    @Test
    void getActivityOk_ActivityNotOwned() {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(VisibilityType.PUBLIC));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Activity actual = activityService.getActivity(user,1L, null);
        verify(activityRepository).findById(1L);
        assertEquals(activity, actual);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC"})
    void getActivity_NonPublicNotOwned_ThrowsVisibilityException(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        assertThrows(VisibilityException.class, ()->activityService.getActivity(user,1L, null));
        verify(activityRepository).findById(1L);
    }

    //do tests from mural perspective
    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PUBLIC", "MURAL_PUBLIC"})
    void getActivity_fromMural_GenericOk(VisibilityType visibilityType) {
        //given
        Mural mural = new Mural();
        User user = new User();
        mural.addMember(user);
        user.addMemberMural(mural);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        Activity actual = activityService.getActivity(user,1L, 1L);
        verify(activityRepository).findById(1L);
        verify(muralRepository).findById(1L);
        assertEquals(activity, actual);
    }

    @Test
    void getActivity_fromMural_MuralSpecificOk() {
        //given
        Mural mural = new Mural();
        mural.setId(1L);
        User user = new User();
        mural.addMember(user);
        user.addMemberMural(mural);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(List.of(1L)));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        Activity actual = activityService.getActivity(user,1L, 1L);
        verify(activityRepository).findById(1L);
        verify(muralRepository).findById(1L);
        assertEquals(activity, actual);
    }

    @Test
    void getActivity_fromMural_PrivateKO() {
        //given
        Mural mural = new Mural();
        mural.setId(1L);
        User user = new User();
        mural.addMember(user);
        user.addMemberMural(mural);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(VisibilityType.PRIVATE));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(VisibilityException.class, ()->activityService.getActivity(user,1L, 1L));
        verify(activityRepository).findById(1L);
        verify(muralRepository).findById(1L);
    }

    @Test
    void getActivity_fromMural_MuralSpecificKO() {
        //given
        Mural mural = new Mural();
        mural.setId(1L);
        User user = new User();
        mural.addMember(user);
        user.addMemberMural(mural);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(List.of(2L)));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(VisibilityException.class, ()->activityService.getActivity(user,1L, 1L));
        verify(activityRepository).findById(1L);
        verify(muralRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC", "PUBLIC"})
    void getActivity_fromMural_NotMemberKO() {
        //given
        Mural mural = new Mural();
        mural.setId(1L);
        User user = new User();
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(List.of(2L)));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(IncorrectParametersException.class, ()->activityService.getActivity(user,1L, 1L));
        verify(activityRepository).findById(1L);
        verify(muralRepository).findById(1L);
    }
    //</editor-fold>

    //<editor-fold desc="addRoute">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void addRoute_OwnedRoute_OK(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        Route route = new Route();
        route.setCreatedBy(user);
        route.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        Activity actual = activityService.addRoute(user,1L, 1L);
        verify(activityRepository).findById(1L);
        verify(routeRepository).findById(1L);
        assertEquals(route, actual.getRoute());
    }

    @Test
    void addRoute_PublicNotOwnedRoute_OK() {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        Route route = new Route();
        route.setVisibility(new Visibility(VisibilityType.PUBLIC));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        Activity actual = activityService.addRoute(user,1L, 1L);
        verify(activityRepository).findById(1L);
        verify(routeRepository).findById(1L);
        assertEquals(route, actual.getRoute());
    }

    @Test
    void addRoute_ActNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        Route route = new Route();
        route.setCreatedBy(user);

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());
        lenient().when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(EntityNotFoundException.class, ()->activityService.addRoute(user,1L, 1L));
        verify(activityRepository).findById(1L);
    }

    @Test
    void addRoute_RouteNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        Route route = new Route();
        route.setCreatedBy(user);

        //when
        lenient().when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->activityService.addRoute(user,1L, 1L));
        verify(routeRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC"})
    void addRoute_ActNotOwned_PermissionException(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        Route route = new Route();
        route.setCreatedBy(user);
        route.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        lenient().when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(PermissionException.class, ()->activityService.addRoute(user,1L, 1L));
        verify(activityRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC"})
    void addRoute_RouteNotVisible_ThrowsVisibilityException(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        User routeOwner = new User();
        routeOwner.setName("pepe");
        Activity activity = new Activity();
        activity.setUser(user);
        Route route = new Route();
        route.setCreatedBy(routeOwner);
        route.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(VisibilityException.class, ()->activityService.addRoute(user,1L, 1L));
        verify(activityRepository).findById(1L);
        verify(routeRepository).findById(1L);
    }
    //</editor-fold>

    //<editor-fold desc="deleteActivity">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void deleteActivity_Owned_Ok(VisibilityType visibilityType) {
        //given
        User user = mock(User.class);
        Activity activity = new Activity();
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Activity actual = activityService.deleteActivity(user,1L);
        verify(activityRepository).findById(1L);
        verify(activityRepository).deleteById(1L);
        assertEquals(activity, actual);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = {"PUBLIC", "MURAL_SPECIFIC", "MURAL_PUBLIC"})
    void deleteActivity_NonPrivateNotOwned_ButAdmin_Ok(VisibilityType visibilityType) {
        //given
        User user = new User();
        user.setRoles(List.of("ADMIN"));
        Activity activity = new Activity();
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Activity actual = activityService.deleteActivity(user,1L);
        verify(activityRepository).findById(1L);
        verify(activityRepository).deleteById(1L);
        assertEquals(activity, actual);
    }

    @Test
    void deleteActivity_NotFound_ThrowsEntityNotFoundException() {
        //given
        User user = mock(User.class);
        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->activityService.deleteActivity(user,1L));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).deleteById(1L);
    }

    @Test
    void deleteActivity_PrivateNotOwned_ButAdmin_ThrowsPermissionException() {
        //given
        User user = new User();
        user.setRoles(List.of("ADMIN"));
        Activity activity = new Activity();
        activity.setVisibility(new Visibility(VisibilityType.PRIVATE));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        assertThrows(PermissionException.class, ()->activityService.deleteActivity(user,1L));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).deleteById(1L);
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void deleteActivity_NotOwned_NotAdmin_ThrowsPermissionException(VisibilityType visibilityType) {
        //given
        User user = new User();
        Activity activity = new Activity();
        activity.setVisibility(new Visibility(visibilityType));

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        assertThrows(PermissionException.class, ()->activityService.deleteActivity(user,1L));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).deleteById(1L);
    }
    //</editor-fold>

    //<editor-fold desc="editActivity">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void editActivity(VisibilityType visibilityType) {
        //given
        User user = new User();
        Activity activity = new Activity();
        activity.setName("someOtherThing");
        activity.setType("Walking");
        activity.setId(1L);
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));
        ActivityEditDTO newAct = new ActivityEditDTO("actPepe", "Running");

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(iom->iom.getArgument(0));

        //then
        Activity actual = activityService.editActivity(user,1L, newAct);
        verify(activityRepository).findById(1L);
        verify(activityRepository).save(activity);
        assertEquals(newAct.getName(), actual.getName());
        assertEquals(newAct.getType(), actual.getType());
        assertEquals(1L, actual.getId());
        assertEquals(user, actual.getUser());
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void editActivity_NullFields(VisibilityType visibilityType) {
        //given
        User user = new User();
        Activity activity = new Activity();
        activity.setName("someOtherThing");
        activity.setType("Walking");
        activity.setId(1L);
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));
        ActivityEditDTO newAct = new ActivityEditDTO(null, null);

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(iom->iom.getArgument(0));

        //then
        Activity actual = activityService.editActivity(user,1L, newAct);
        verify(activityRepository).findById(1L);
        verify(activityRepository).save(activity);
        assertEquals("someOtherThing", actual.getName());
        assertEquals("Walking", actual.getType());
        assertEquals(1L, actual.getId());
        assertEquals(user, actual.getUser());
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void editActivity_EmptyFields(VisibilityType visibilityType) {
        //given
        User user = new User();
        Activity activity = new Activity();
        activity.setName("someOtherThing");
        activity.setType("Walking");
        activity.setId(1L);
        activity.setUser(user);
        activity.setVisibility(new Visibility(visibilityType));
        ActivityEditDTO newAct = new ActivityEditDTO("", "");

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(any(Activity.class))).thenAnswer(iom->iom.getArgument(0));

        //then
        Activity actual = activityService.editActivity(user,1L, newAct);
        verify(activityRepository).findById(1L);
        verify(activityRepository).save(activity);
        assertEquals("someOtherThing", actual.getName());
        assertEquals("Walking", actual.getType());
        assertEquals(1L, actual.getId());
        assertEquals(user, actual.getUser());
    }

    @Test
    void editActivity_NotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();
        ActivityEditDTO newAct = new ActivityEditDTO("actPepe", "Running");

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->activityService.editActivity(user,1L, newAct));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void editActivity_NotOwned_ThrowsPermissionException(VisibilityType visibilityType) {
        //given
        User user = new User();
        user.setName("pepe");
        User otherUser = new User();
        otherUser.setName("juan");
        Activity activity = new Activity();
        activity.setUser(otherUser);
        activity.setVisibility(new Visibility(visibilityType));
        ActivityEditDTO newAct = new ActivityEditDTO("asd", "asd");

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        assertThrows(PermissionException.class, ()->activityService.editActivity(user,1L, newAct));
        verify(activityRepository).findById(1L);
        verify(activityRepository, never()).save(any(Activity.class));
    }

    //</editor-fold>




}

