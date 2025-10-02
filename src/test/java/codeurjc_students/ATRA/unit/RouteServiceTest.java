package codeurjc_students.ATRA.unit;

import codeurjc_students.ATRA.exception.EntityNotFoundException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.exception.PermissionException;
import codeurjc_students.ATRA.exception.VisibilityException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.DataPoint;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import codeurjc_students.ATRA.service.RouteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.parameters.P;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)

public class RouteServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private MuralRepository muralRepository;

    @InjectMocks
    private RouteService routeService;

    //<editor-fold desc="changeVisibility">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void changeVisibility_RouteNotFound_ThrowsEntityNotFoundException(VisibilityType visibilityType) {
        //given

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.changeVisibility(1L, visibilityType, null));
        verify(routeRepository, never()).save(any(Route.class));
        verify(routeRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "MURAL_PUBLIC", "PUBLIC", "PRIVATE" })
    void changeVisibility_Ok(VisibilityType visibilityType) {
        //given
        Route route = new Route();
        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        routeService.changeVisibility(1L, visibilityType, null);
        assertEquals(visibilityType, route.getVisibility().getType());
        assertNull(route.getVisibility().getAllowedMurals());

        verify(routeRepository).save(route);
        verify(routeRepository).findById(1L);
    }

    @Test
    void changeVisibility_ToMuralSpecific_Ok() {
        //given
        Route route = new Route();
        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        routeService.changeVisibility(1L, VisibilityType.MURAL_SPECIFIC, null);
        assertEquals(VisibilityType.MURAL_SPECIFIC, route.getVisibility().getType());
        assertNotNull(route.getVisibility().getAllowedMurals());

        verify(routeRepository).save(route);
        verify(routeRepository).findById(1L);
    }

    @Test
    void changeVisibility_FromPublic_ThrowsIncorrectParametersException() {
        //given
        Route route = new Route();
        route.setVisibility(new Visibility(VisibilityType.PUBLIC));
        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.changeVisibility(1L, VisibilityType.PRIVATE, null));
        assertEquals(VisibilityType.PUBLIC, route.getVisibility().getType());
        assertNull(route.getVisibility().getAllowedMurals());

        verify(routeRepository, never()).save(route);
        verify(routeRepository).findById(1L);
    }

    @Test
    void changeVisibility_ToPrivate_Ok() {
        //given
        User routeOwner = new User();routeOwner.setId(1L);
        User activityOwner = new User();routeOwner.setId(2L);
        Route route = new Route();route.setCreatedBy(routeOwner);
        Activity activity  = new Activity();activity.setOwner(activityOwner);
        route.setVisibility(new Visibility(VisibilityType.MURAL_PUBLIC));
        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findByRoute(route)).thenReturn(List.of(activity));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.changeVisibility(1L, VisibilityType.PRIVATE, null));
        assertEquals(VisibilityType.MURAL_PUBLIC, route.getVisibility().getType());
        assertNull(route.getVisibility().getAllowedMurals());

        verify(routeRepository, never()).save(route);
        verify(routeRepository).findById(1L);
        verify(activityRepository).findByRoute(route);
    }

    //</editor-fold>

    //<editor-fold desc="getActivitiesAssignedToRoute">
    @Test
    void getActivitiesAssignedToRoute_RouteNotFound_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.getActivitiesAssignedToRoute(1L, user, null));
        verify(routeRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC" })
    void getActivitiesAssignedToRoute_NotVisible_ThrowsVisibilityException(VisibilityType visibilityType) {
        //given
        User user = new User();user.setId(1L);
        User userOwner = new User();userOwner.setId(2L);
        Route route = new Route();
        route.setVisibility(new Visibility(visibilityType));
        route.setCreatedBy(userOwner);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(VisibilityException.class, ()->routeService.getActivitiesAssignedToRoute(1L, user, null));
        verify(routeRepository).findById(1L);
    }

    @Test
    void getActivitiesAssignedToRoute_MuralSpecifiedButNotFound_ThrowsEntityNotFoundException() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route();
        route.setCreatedBy(user);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.getActivitiesAssignedToRoute(1L, user, 1L));
        verify(routeRepository).findById(1L);
        verify(muralRepository).findById(1L);
    }

    @Test
    void getActivitiesAssignedToRoute_UserNotMember_ThrowsPermissionException() {
        //given
        Mural mural = new Mural();mural.setId(1L);
        User user = new User();user.setId(1L);
        Route route = new Route();
        route.setCreatedBy(user);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(PermissionException.class, ()->routeService.getActivitiesAssignedToRoute(1L, user, 1L));
        verify(routeRepository).findById(1L);
        verify(muralRepository).findById(1L);
    }

    @Test
    void getActivitiesAssignedToRoute_Ok_user() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setId(1L);
        route.setCreatedBy(user);
        Activity activity = new Activity(); activity.setId(1L); activity.setOwner(user);  activity.setRoute(route);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findByRouteAndOwner(route, user)).thenReturn(List.of(activity));

        //then
        Collection<Activity> activitiesAssignedToRoute = routeService.getActivitiesAssignedToRoute(1L, user, null);

        assertEquals(List.of(activity), activitiesAssignedToRoute);

        verify(routeRepository).findById(1L);
        verify(activityRepository).findByRouteAndOwner(route, user);
        verify(activityRepository, never()).findByRouteAndMural(any(), anyLong());

    }

    @Test
    void getActivitiesAssignedToRoute_Ok_mural() {
        //given
        Mural mural = new Mural();mural.setId(1L);
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setId(1L);
        route.setCreatedBy(user);

        Activity act1 = new Activity(); act1.setId(1L); act1.setOwner(user);  act1.setRoute(route);

        mural.addMember(user);
        user.getMemberMurals().add(mural);


        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(activityRepository.findByRouteAndMural(route, 1L)).thenReturn(List.of(act1));

        //then
        Collection<Activity> activitiesAssignedToRoute = routeService.getActivitiesAssignedToRoute(1L, user, 1L);

        assertEquals(List.of(act1), activitiesAssignedToRoute);

        verify(routeRepository).findById(1L);
        verify(muralRepository).findById(1L);
        verify(activityRepository, never()).findByRouteAndOwner(route, user);
        verify(activityRepository).findByRouteAndMural(route, 1L);

    }

    //</editor-fold>

    //<editor-fold desc="getAllRoutes">
    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void getAllRoutes_FromAuthUser_ok(VisibilityType visibilityType) {
        //given
        String from = "authUser";
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user);

        //when
        when(routeRepository.findUsedOrCreatedBy(user)).thenReturn(List.of(route,route2));

        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, null, visibilityType);
        List<Route> routes2 = routeService.getAllRoutes(user, null, null, null, visibilityType);

        assertEquals(List.of(route, route2), routes);
        assertEquals(List.of(route, route2), routes2);

        verify(routeRepository, times(2)).findUsedOrCreatedBy(user);
    }

    @Test
    void getAllRoutes_FromUserOrMural_NullId_ThrowsIncorrectParameters() {
        //given
        String from = "user";
        String from2 = "user";
        User user = new User();user.setId(1L);

        //when
        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.getAllRoutes(user, null, from, null, null));
        assertThrows(IncorrectParametersException.class, ()->routeService.getAllRoutes(user, null, from2, null, null));

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getAllRoutes_FromUser_NotFound_ThrowsEntityNotFound() {
        //given
        String from = "user";
        User user = new User();user.setId(1L);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.getAllRoutes(user, null, from, 2L, null));

        verify(userRepository).findById(2L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "MURAL_PUBLIC", "MURAL_SPECIFIC", "PRIVATE" })
    void getAllRoutes_FromUser_NotOwnerNotAdmin_RequestingVisibility_ThrowsVisibilityException(VisibilityType visibilityType) {
        //given
        String from = "user";
        User user = new User();user.setId(1L);
        User user2 = new User();user.setId(2L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        //then
        assertThrows(VisibilityException.class, ()->routeService.getAllRoutes(user, null, from, 2L, visibilityType));

        verify(userRepository).findById(2L);
    }

    @Test
    void getAllRoutes_FromUser_NotOwnerNotAdmin_Ok() {
        //given
        String from = "user";
        User user = new User();user.setId(1L);
        User user2 = new User();user.setId(2L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user2);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user2);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(routeRepository.findByCreatedByAndVisibilityTypeIn(user2, List.of(VisibilityType.PUBLIC))).thenReturn(List.of(route,route2));
        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 2L, null);
        List<Route> routes2 = routeService.getAllRoutes(user, null, from, 2L, VisibilityType.PUBLIC);

        assertEquals(List.of(route, route2), routes);
        assertEquals(List.of(route, route2), routes2);

        verify(userRepository, times(2)).findById(2L);
    }

    @Test
    void getAllRoutes_FromUser_Owner_NullVis_Ok() {
        //given
        String from = "user";
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user);

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.findUsedOrCreatedBy(user)).thenReturn(List.of(route,route2));
        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 1L, null);

        assertEquals(List.of(route, route2), routes);

        verify(userRepository).findById(1L);
        verify(routeRepository).findUsedOrCreatedBy(user);

    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void getAllRoutes_FromUser_Owner_RequestingVis_Ok(VisibilityType visibilityType) {
        //given
        String from = "user";
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user);

        //when
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(routeRepository.findByCreatedByAndVisibilityTypeIn(user, List.of(visibilityType))).thenReturn(List.of(route,route2));
        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 1L, visibilityType);

        assertEquals(List.of(route, route2), routes);

        verify(userRepository).findById(1L);
        verify(routeRepository).findByCreatedByAndVisibilityTypeIn(user, List.of(visibilityType));
    }

    @Test
    void getAllRoutes_FromUser_Admin_NullVis_Ok() {
        //given
        String from = "user";
        User user = new User();user.setId(1L);user.setRoles(List.of("ADMIN"));
        User user2 = new User();user2.setId(2L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user2);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user2);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(routeRepository.findByCreatedByAndVisibilityTypeIn(user2, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC))).
                thenReturn(List.of(route,route2));
        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 2L, null);

        assertEquals(List.of(route, route2), routes);

        verify(userRepository).findById(2L);
        verify(routeRepository).findByCreatedByAndVisibilityTypeIn(user2, List.of(VisibilityType.PUBLIC, VisibilityType.MURAL_PUBLIC, VisibilityType.MURAL_SPECIFIC));
    }

    @Test
    void getAllRoutes_FromUser_Admin_RequestingPrivate_ThrowsVisibilityException() {
        //given
        String from = "user";
        User user = new User();user.setId(1L);user.setRoles(List.of("ADMIN"));
        User user2 = new User();user2.setId(2L);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        //then
        assertThrows(VisibilityException.class, ()->routeService.getAllRoutes(user, null, from, 2L, VisibilityType.PRIVATE));

        verify(userRepository).findById(2L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "PUBLIC", "MURAL_PUBLIC", "MURAL_SPECIFIC" })
    void getAllRoutes_FromUser_Admin_RequestingNonPrivate_Ok(VisibilityType visibilityType) {
        //given
        String from = "user";
        User user = new User();user.setId(1L);user.setRoles(List.of("ADMIN"));
        User user2 = new User();user2.setId(2L);
        Route route = new Route(); route.setId(1L);route.setCreatedBy(user2);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user2);

        //when
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(routeRepository.findByCreatedByAndVisibilityTypeIn(user2, List.of(visibilityType))).thenReturn(List.of(route,route2));
        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 2L, visibilityType);

        assertEquals(List.of(route, route2), routes);

        verify(userRepository).findById(2L);
        verify(routeRepository).findByCreatedByAndVisibilityTypeIn(user2, List.of(visibilityType));
    }

    @Test
    void getAllRoutes_FromMural_NotFound_ThrowsEntityNotFound() {
        //given
        String from = "mural";
        User user = new User();user.setId(1L);

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.getAllRoutes(user, null, from, 1L, null));

        verify(muralRepository).findById(1L);
    }

    @Test
    void getAllRoutes_FromMural_UserNotMember_ThrowsVisibilityException() {
        //given
        String from = "mural";
        User user = new User();user.setId(1L);
        Mural mural = new Mural();mural.setId(1L);

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(VisibilityException.class, ()->routeService.getAllRoutes(user, null, from, 1L, null));

        verify(muralRepository).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void getAllRoutes_FromMural_RequestingVisibility_ThrowsIncorrectParameters(VisibilityType visibilityType) {
        //given
        String from = "mural";
        User user = new User();user.setId(1L);
        Mural mural = new Mural();mural.setId(1L);
        mural.getMembers().add(user);
        user.getMemberMurals().add(mural);

        //when
        lenient().when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.getAllRoutes(user, null, from, 1L, visibilityType));

        verify(muralRepository, atMost(1)).findById(1L);
    }

    @Test
    void getAllRoutes_FromMural_Ok() {
        //given
        String from = "mural";
        User user = new User();user.setId(1L);
        Mural mural = new Mural();mural.setId(1L);
        mural.getMembers().add(user);
        user.getMemberMurals().add(mural);

        Route route = new Route(); route.setId(1L);route.setCreatedBy(user);
        Route route2 = new Route(); route2.setId(2L);route2.setCreatedBy(user);

        //when
        when(muralRepository.findById(1L)).thenReturn(Optional.of(mural));
        when(routeRepository.findVisibleToMural(1L)).thenReturn(List.of(route, route2));

        //then
        List<Route> routes = routeService.getAllRoutes(user, null, from, 1L, null);

        assertEquals(List.of(route, route2), routes);

        verify(muralRepository).findById(1L);
        verify(routeRepository).findVisibleToMural(1L);
    }

    @Test
    void getAllRoutes_FromIncorrect_ThrowsIncorrectParameters() {
        //given
        String from = "pepe";
        User user = new User();user.setId(1L);

        //when
        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.getAllRoutes(user, null, from, 1L, null));
    }

    //</editor-fold>

    //<editor-fold desc="createRoute">
    @Test
    void createRoute_ActNotFound_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.createRoute(user,1L, null));

        verify(activityRepository).findById(1L);
    }

    @Test
    void createRoute_NullAct_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);

        //when
        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.createRoute(user,null, null));

        verify(activityRepository, never()).findById(any());
    }

    @Test
    void createRoute_UserNotActOwner_ThrowsIncorrectParameters() {
        //given
        User user = new User();user.setId(1L);
        Activity activity = new Activity();

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.createRoute(user,1L, null));

        verify(activityRepository).findById(1L);
    }

    @Test
    void createRoute_NullRoute_UsesDefaults() {
        //given
        User user = new User();user.setId(1L);
        Activity activity = new Activity();
        activity.setOwner(user);
        activity.setId(1L);

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Route r = routeService.createRoute(user,1L, null);

        assertEquals("Route from Activity 1", r.getName());
        assertEquals("This route has no description. Feel free to add one!", r.getDescription());
        assertEquals(user, r.getCreatedBy());
        assertNull(r.getTotalDistance());
        assertNull(r.getElevationGain());
        assertEquals(r, activity.getRoute());

        verify(activityRepository).findById(1L);
        verify(routeRepository).save(r);
        verify(activityRepository).save(activity);
    }

    @Test
    void createRoute_EmptyRoute_UsesDefaults() {
        //given
        User user = new User();user.setId(1L);
        Activity activity = new Activity();
        activity.setOwner(user);
        activity.setId(1L);
        //activity needs to have
        //  datapoints
        //  summary.totalDistance
        //  summary.elevationGain

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Route r = routeService.createRoute(user,1L, new Route());

        assertEquals("Route from Activity 1", r.getName());
        assertEquals("This route has no description. Feel free to add one!", r.getDescription());
        assertEquals(user, r.getCreatedBy());
        assertNull(r.getTotalDistance());
        assertNull(r.getElevationGain());
        assertEquals(r, activity.getRoute());

        verify(activityRepository).findById(1L);
        verify(routeRepository).save(r);
        verify(activityRepository).save(activity);
    }

    @Test
    void createRoute_EmptyFields_UsesDefaults() {
        //given
        User user = new User();user.setId(1L);
        Activity activity = new Activity();
        activity.setOwner(user);
        activity.setId(1L);
        Route ro = new Route();
        ro.setName("");
        ro.setDescription("");
        ro.setTotalDistance(0.0);
        ro.setElevationGain(0.0);
        //activity needs to have
        //  datapoints
        //  summary.totalDistance
        //  summary.elevationGain

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Route r = routeService.createRoute(user,1L, new Route());

        assertEquals("Route from Activity 1", r.getName());
        assertEquals("This route has no description. Feel free to add one!", r.getDescription());
        assertEquals(user, r.getCreatedBy());
        assertNull(r.getTotalDistance());
        assertNull(r.getElevationGain());
        assertEquals(r, activity.getRoute());

        verify(activityRepository).findById(1L);
        verify(routeRepository).save(r);
        verify(activityRepository).save(activity);
    }

    @Test
    void createRoute_Ok() {
        //given
        User user = new User();user.setId(1L);
        Activity activity = new Activity();
        activity.setOwner(user);
        activity.setId(1L);
        activity.setDataPoints(List.of(
                new DataPoint(1.0,1.0),
                new DataPoint(2.0,2.0),
                new DataPoint(3.0,3.0)
        ));
        Route ro = new Route();
        ro.setName("pepe");
        ro.setDescription("Ruta creada por pepe, que es un guasón");
        ro.setTotalDistance(12.98);
        ro.setElevationGain(124.3);

        //when
        when(activityRepository.findById(1L)).thenReturn(Optional.of(activity));

        //then
        Route r = routeService.createRoute(user,1L, ro);

        assertEquals("pepe", r.getName());
        assertEquals("Ruta creada por pepe, que es un guasón", r.getDescription());
        assertEquals(user, r.getCreatedBy());
        assertEquals(12.98, r.getTotalDistance());
        assertEquals(124.3, r.getElevationGain());
        assertEquals(List.of(
                        new Coordinates(1.0,1.0),
                        new Coordinates(2.0,2.0),
                        new Coordinates(3.0,3.0)
                )
                , r.getCoordinates());

        assertEquals(r, activity.getRoute());

        verify(activityRepository).findById(1L);
        verify(routeRepository).save(r);
        verify(activityRepository).save(activity);
    }
    //</editor-fold>

    //<editor-fold desc="addActivitiesToRoute">
    @Test
    void addActivitiesToRoute_NullRouteOrNullActivityIds_ThrowsIncorrectParameters() {
        //given
        User user = new User();user.setId(1L);

        //when
        lenient().when(activityRepository.findAllById(any())).thenReturn(List.of(new Activity()));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.addActivitiesToRoute(user,null, new ArrayList<>()));
        assertThrows(IncorrectParametersException.class, ()->routeService.addActivitiesToRoute(user,1L, null));

        verify(routeRepository, never()).findById(1L);
    }

    @ParameterizedTest
    @EnumSource(value = VisibilityType.class, names = { "PRIVATE", "MURAL_SPECIFIC", "MURAL_PUBLIC" })
    void addActivitiesToRoute_RouteNotVisible_ThrowsVisibilityException(VisibilityType visibilityType) {
        //given
        User user = new User();user.setId(1L);
        User user2 = new User();user.setId(2L);
        Route route = new Route();route.setId(1L);route.setCreatedBy(user2);route.setVisibility(new Visibility(visibilityType));

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        lenient().when(activityRepository.findAllById(any())).thenReturn(List.of(new Activity()));


        //then
        assertThrows(VisibilityException.class, ()->routeService.addActivitiesToRoute(user,1L, List.of(1L)));

        verify(routeRepository).findById(1L);
        verify(activityRepository, never()).save(any());
    }

    @Test
    void addActivitiesToRoute_NoActivitiesFound_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route();route.setId(1L);route.setCreatedBy(user);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findAllById(anyIterable())).thenReturn(List.of());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.addActivitiesToRoute(user,1L, List.of(1L)));

        verify(routeRepository).findById(1L);
        verify(activityRepository).findAllById(anyIterable());
        verify(activityRepository, never()).save(any());
    }

    @Test
    void addActivitiesToRoute_ActNotOwned_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route();route.setId(1L);route.setCreatedBy(user);
        Activity activity = new Activity();activity.setOwner(user);
        Activity activity2 = new Activity();

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findAllById(List.of(1L,2L))).thenReturn(List.of(activity,activity2));

        //then
        assertThrows(PermissionException.class, ()->routeService.addActivitiesToRoute(user,1L, List.of(1L,2L)));

        verify(routeRepository).findById(1L);
        verify(activityRepository).findAllById(List.of(1L,2L));
        verify(activityRepository, never()).save(any());
    }

    @Test
    void addActivitiesToRoute_RouteOwned_Ok() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route();route.setId(1L);route.setCreatedBy(user);
        Activity activity = new Activity();activity.setOwner(user); activity.setId(1L);
        Activity activity2 = new Activity();activity2.setOwner(user); activity2.setId(2L);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findAllById(List.of(1L,2L))).thenReturn(List.of(activity,activity2));

        //then
        routeService.addActivitiesToRoute(user,1L, List.of(1L,2L));

        assertEquals(route, activity.getRoute());
        assertEquals(route, activity2.getRoute());

        verify(routeRepository).findById(1L);
        verify(activityRepository).findAllById(List.of(1L,2L));
        verify(activityRepository).save(activity);
        verify(activityRepository).save(activity2);
    }

    //</editor-fold>

    //<editor-fold desc="deleteRoute">
    @Test
    void deleteRoute_RouteNotFound_ThrowsEntityNotFound() {
        //given
        User user = new User();user.setId(1L);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        //then
        assertThrows(EntityNotFoundException.class, ()->routeService.deleteRoute(user,1L));
        assertThrows(EntityNotFoundException.class, ()->routeService.deleteRoute(user,null));

        verify(routeRepository).findById(1L);
        verify(routeRepository, times(2)).findById(any());
        verify(routeRepository, never()).delete(any());
        verify(routeRepository, never()).deleteById(any());
    }

    @ParameterizedTest
    @EnumSource(VisibilityType.class)
    void deleteRoute_NotOwnerNotAdmin_ThrowsPermissionException(VisibilityType visibilityType) {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setVisibility(new Visibility(visibilityType));

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(PermissionException.class, ()->routeService.deleteRoute(user,1L));

        verify(routeRepository).findById(1L);
        verify(routeRepository, never()).delete(any());
        verify(routeRepository, never()).deleteById(any());
    }

    @Test
    void deleteRoute_PublicRoute_NotAdmin_ThrowsPermissionException() {
        //given
        User user = new User();user.setId(1L);
        Route route = new Route(); route.setCreatedBy(user);route.setVisibility(new Visibility(VisibilityType.PUBLIC));

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));

        //then
        assertThrows(PermissionException.class, ()->routeService.deleteRoute(user,1L));

        verify(routeRepository).findById(1L);
        verify(routeRepository, never()).delete(any());
        verify(routeRepository, never()).deleteById(any());
    }

    @Test
    void deleteRoute_MuralSpecific_UsedByOthers_ThrowsIncorrectParametersException() {
        //given
        User user = new User();user.setId(1L);
        User user2 = new User();user2.setId(2L);
        Route route = new Route(); route.setCreatedBy(user);route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC));
        Activity activity = new Activity();activity.setId(1L); activity.setRoute(route); activity.setOwner(user2);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findByRoute(route)).thenReturn(List.of(activity));

        //then
        assertThrows(IncorrectParametersException.class, ()->routeService.deleteRoute(user,1L));

        verify(routeRepository).findById(1L);
        verify(routeRepository, never()).delete(any());
        verify(routeRepository, never()).deleteById(any());
    }

    @Test
    void deleteRoute_PublicRoute_Admin_Ok() {
        //given
        User user = new User();user.setId(1L);user.setRoles(List.of("ADMIN"));
        User user2 = new User();user2.setId(2L);
        Route route = new Route(); route.setCreatedBy(user2);route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC));
        Activity activity = new Activity();activity.setId(1L); activity.setRoute(route); activity.setOwner(user2);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findByRoute(route)).thenReturn(List.of(activity));

        //then
        routeService.deleteRoute(user,1L);

        assertNull(activity.getRoute());

        verify(routeRepository).findById(1L);
        verify(activityRepository).findByRoute(route);
        verify(activityRepository).save(activity);
        verify(routeRepository).delete(route);
    }

    @Test
    void deleteRoute_PublicRoute_Owner_Ok() {
        //given
        User user = new User();user.setId(1L);user.setRoles(List.of("ADMIN"));
        User user2 = new User();user2.setId(2L);
        Route route = new Route(); route.setCreatedBy(user);route.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC));
        Activity activity = new Activity();activity.setId(1L); activity.setRoute(route); activity.setOwner(user);

        //when
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(activityRepository.findByRoute(route)).thenReturn(List.of(activity));

        //then
        routeService.deleteRoute(user,1L);

        assertNull(activity.getRoute());

        verify(routeRepository).findById(1L);
        verify(activityRepository).findByRoute(route);
        verify(activityRepository).save(activity);
        verify(routeRepository).delete(route);
    }


    //</editor-fold>
}