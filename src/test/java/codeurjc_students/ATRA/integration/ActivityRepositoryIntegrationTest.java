package codeurjc_students.ATRA.integration;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ActivityRepositoryIntegrationTest {

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private MuralRepository muralRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private UserRepository userRepository;


    List<Activity> createActivities(User owner, Long muralId, Route route){
        if (owner==null) {
            owner = new User();
            owner.setName("pepe");
        }
        userRepository.save(owner);
        User notMe = new User();
        userRepository.save(notMe);

        Activity activity1 = new Activity();
        Activity activity2 = new Activity();
        Activity activity3 = new Activity();
        Activity activity4 = new Activity();
        Activity activity5 = new Activity();

        activity1.setVisibility(new Visibility(VisibilityType.PUBLIC));
        activity2.setVisibility(new Visibility(VisibilityType.MURAL_PUBLIC));
        activity3.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(muralId)));
        activity4.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC));
        activity5.setVisibility(new Visibility(VisibilityType.PRIVATE));

        activity1.setOwner(owner);
        activity2.setOwner(owner);
        activity3.setOwner(owner);
        activity4.setOwner(owner);
        activity5.setOwner(owner);

        activity1.setRoute(route);
        activity2.setRoute(route);
        activity3.setRoute(route);
        activity4.setRoute(route);
        activity5.setRoute(route);

        activityRepository.save(activity1);
        activityRepository.save(activity2);
        activityRepository.save(activity3);
        activityRepository.save(activity4);
        activityRepository.save(activity5);

        return List.of(activity1,activity2,activity3,activity4,activity5);
    }


    @Test
    @Transactional
    @Rollback
    void testFindById() {
        User someUser = new User();
        userRepository.save(someUser);
        Activity activity = new Activity();
        activity.setOwner(someUser);
        activityRepository.save(activity);

        Optional<Activity> found = activityRepository.findById(activity.getId());
        Optional<Activity> notFound = activityRepository.findById(20L);

        assertEquals(Optional.empty(), notFound);
        assertEquals(Optional.of(activity), found);
    }

    @Test
    @Transactional
    void testFindVisibleToMural() {
        Mural mural = new Mural();
        User member = new User();member.setName("member");
        User notMember = new User();notMember.setName("NotMember");
        mural.getMembers().add(member);

        List<Activity> activitiesMember = createActivities(member, 1L, null);
        List<Activity> activitiesNotMember = createActivities(notMember, 1L, null);

        muralRepository.save(mural);
        Collection<Activity> actual = activityRepository.findVisibleToMural(mural.getId(), mural.getMembers().stream().map(User::getId).toList());

        assertEquals(List.of(activitiesMember.get(0), activitiesMember.get(1), activitiesMember.get(2)), actual);
        activitiesNotMember.forEach(a->assertFalse(actual.contains(a)));
    }

    @Test
    @Transactional
    void testFindVisibleToMuralPageable() {
        Mural mural = new Mural();
        User member = new User();member.setName("member");
        User notMember = new User();notMember.setName("NotMember");
        mural.getMembers().add(member);

        List<Activity> activitiesMember = createActivities(member, 1L, null);
        List<Activity> activitiesNotMember = createActivities(notMember, 1L, null);

        muralRepository.save(mural);
        Page<Activity> page0 = activityRepository.findVisibleToMural(
                mural.getId(),
                mural.getMembers().stream().map(User::getId).toList(),
                PageRequest.of(0,2));
        Page<Activity> pageFull = activityRepository.findVisibleToMural(
                mural.getId(),
                mural.getMembers().stream().map(User::getId).toList(),
                PageRequest.of(0,5));

        assertEquals(2, page0.getContent().size());
        assertFalse(page0.getContent().contains(activitiesMember.get(2)));

        assertTrue(pageFull.getContent().containsAll(List.of(activitiesMember.get(0), activitiesMember.get(1))));

        activitiesNotMember.forEach(activity -> {
            assertFalse(page0.getContent().contains(activity));
            assertFalse(pageFull.getContent().contains(activity));
        });
    }

    @Test
    @Transactional
    void testFindVisibleToMuralAndRouteIsNull() {
        Mural mural = new Mural();
        User member = new User();member.setName("member");
        User notMember = new User();notMember.setName("NotMember");
        mural.getMembers().add(member);
        Route route = new Route();route.setName("My Route");
        route.setCreatedBy(member);
        userRepository.save(member);
        routeRepository.save(route);

        List<Activity> activitiesMemberNull = createActivities(member, 1L, null);
        List<Activity> activitiesNotMemberNull = createActivities(notMember, 1L, null);
        List<Activity> activitiesMember = createActivities(member, 1L, route);
        List<Activity> activitiesNotMember = createActivities(notMember, 1L, route);

        muralRepository.save(mural);
        Collection<Activity> actual = activityRepository.findVisibleToMuralAndRouteIsNull(mural.getId(), mural.getMembers().stream().map(User::getId).toList());

        assertEquals(List.of(activitiesMemberNull.get(0), activitiesMemberNull.get(1), activitiesMemberNull.get(2)), actual);
        activitiesNotMember.forEach(a->assertFalse(actual.contains(a)));
        activitiesNotMemberNull.forEach(a->assertFalse(actual.contains(a)));
        activitiesMember.forEach(a->assertFalse(actual.contains(a)));
    }

    @Test
    @Transactional
    void testFindVisibleToMuralAndRouteIsNullPageable() {
        Mural mural = new Mural();
        User member = new User();member.setName("member");
        User notMember = new User();notMember.setName("NotMember");
        mural.getMembers().add(member);
        Route route = new Route();route.setName("My Route");
        route.setCreatedBy(member);
        userRepository.save(member);
        routeRepository.save(route);

        List<Activity> activitiesMemberNull = createActivities(member, 1L, null);
        List<Activity> activitiesNotMemberNull = createActivities(notMember, 1L, null);
        List<Activity> activitiesMember = createActivities(member, 1L, route);
        List<Activity> activitiesNotMember = createActivities(notMember, 1L, route);

        muralRepository.save(mural);
        Page<Activity> page0 = activityRepository.findVisibleToMuralAndRouteIsNull(
                mural.getId(),
                mural.getMembers().stream().map(User::getId).toList(),
                PageRequest.of(0,2));
        Page<Activity> pageFull = activityRepository.findVisibleToMuralAndRouteIsNull(
                mural.getId(),
                mural.getMembers().stream().map(User::getId).toList(),
                PageRequest.of(0,5));

        assertEquals(2, page0.getContent().size());
        assertFalse(page0.getContent().contains(activitiesMemberNull.get(2)));

        assertTrue(pageFull.getContent().containsAll(List.of(activitiesMemberNull.get(0), activitiesMemberNull.get(1), activitiesMemberNull.get(2))));

        activitiesNotMember.forEach(activity -> {
            assertFalse(page0.getContent().contains(activity));
            assertFalse(pageFull.getContent().contains(activity));
        });
        activitiesMember.forEach(activity -> {
            assertFalse(page0.getContent().contains(activity));
            assertFalse(pageFull.getContent().contains(activity));
        });
        activitiesNotMember.forEach(activity -> {
            assertFalse(page0.getContent().contains(activity));
            assertFalse(pageFull.getContent().contains(activity));
        });
        activitiesNotMemberNull.forEach(activity -> {
            assertFalse(page0.getContent().contains(activity));
            assertFalse(pageFull.getContent().contains(activity));
        });
    }

    @Test
    @Transactional
    void testFindByUserAndVisibleToMural() {
        Mural mural = new Mural();
        User me = new User();me.setName("Me");
        User notMe = new User();notMe.setName("NotMe");
        mural.getMembers().add(me);
        mural.getMembers().add(notMe);
        User outsider = new User();outsider.setName("outsider1");

        List<Activity> activitiesMeInMural = createActivities(me, 1L, null);
        List<Activity> activitiesNotMeInMural = createActivities(notMe, 1L, null);
        List<Activity> activitiesMeOutMural = createActivities(me, 2L, null);
        List<Activity> activitiesNotMeOutMural = createActivities(outsider, 2L, null);

        muralRepository.save(mural);
        Collection<Activity> actual = activityRepository.findByUserAndVisibleToMural(me, mural.getId());

        assertTrue(actual.containsAll(List.of(activitiesMeInMural.get(0), activitiesMeInMural.get(1), activitiesMeInMural.get(2),
                activitiesMeOutMural.get(0), activitiesMeOutMural.get(1))));
        activitiesNotMeInMural.forEach(a->assertFalse(actual.contains(a)));
        activitiesNotMeOutMural.forEach(a->assertFalse(actual.contains(a)));
    }

    @Test
    @Transactional
    void testFindByRouteAndMural() {
        Mural mural = new Mural();
        User member = new User();member.setName("member");
        User notMember = new User();notMember.setName("NotMember");
        mural.getMembers().add(member);

        Route route1 = new Route();route1.setName("Route1");route1.setVisibility(new Visibility(VisibilityType.PUBLIC));route1.setCreatedBy(member);
        Route route2 = new Route();route2.setName("Route2");route2.setVisibility(new Visibility(VisibilityType.PUBLIC));route2.setCreatedBy(notMember);

        userRepository.save(member);
        userRepository.save(notMember);
        routeRepository.save(route1);
        routeRepository.save(route2);

        List<Activity> activitiesMember = createActivities(member, 1L, null);
        List<Activity> activitiesNotMember = createActivities(notMember, 1L, null);
        List<Activity> activitiesMemberR1 = createActivities(member, 1L, route1);
        List<Activity> activitiesNotMemberR1 = createActivities(notMember, 1L, route1);
        List<Activity> activitiesMemberR2 = createActivities(member, 1L, route2);
        List<Activity> activitiesNotMemberR2 = createActivities(notMember, 1L, route2);

        muralRepository.save(mural);
        Collection<Activity> actual = activityRepository.findByRouteAndMural(route1, mural.getId());

        assertTrue(
                actual.containsAll(
                        List.of(activitiesMemberR1.get(0), activitiesMemberR1.get(1), activitiesMemberR1.get(2),
                                activitiesNotMemberR1.get(0), activitiesNotMemberR1.get(1), activitiesNotMemberR1.get(2))
                ));
        activitiesMember.forEach(a->assertFalse(actual.contains(a)));
        activitiesNotMember.forEach(a->assertFalse(actual.contains(a)));
        activitiesMemberR2.forEach(a->assertFalse(actual.contains(a)));
        activitiesNotMemberR2.forEach(a->assertFalse(actual.contains(a)));

    }

}
