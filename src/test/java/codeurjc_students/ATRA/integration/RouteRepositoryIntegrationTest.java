package codeurjc_students.ATRA.integration;

import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.Visibility;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DataJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RouteRepositoryIntegrationTest {

    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private UserRepository userRepository;

    static List<Route> routes = new ArrayList<>();
    static User me = new User();



    @BeforeEach
    void setUp() {
        User notMe = new User();
        userRepository.save(me);
        userRepository.save(notMe);
        Activity act = new Activity();
        act.setOwner(me);
        activityRepository.save(act);

        Route route1 = new Route();
        route1.setId(1L);
        route1.setCreatedBy(me);
        route1.setVisibility(new Visibility(VisibilityType.PUBLIC));
        Route route2 = new Route();
        route2.setId(2L);
        act.setRoute(route2);
        route2.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC));
        Route route3 = new Route();
        route3.setId(3L);
        route3.setVisibility(new Visibility(VisibilityType.MURAL_SPECIFIC, List.of(1L)));
        Route route4 = new Route();
        route4.setId(4L);
        route4.setVisibility(new Visibility(VisibilityType.PRIVATE));
        route2.setCreatedBy(notMe);
        route3.setCreatedBy(notMe);
        route4.setCreatedBy(notMe);
        routes.add(route1);
        routes.add(route2);
        routes.add(route3);
        routes.add(route4);
        routeRepository.saveAll(routes);
    }


    @Test
    @Transactional
    void testFindById() {
        Optional<Route> found = routeRepository.findById(1L);
        Optional<Route> notFound = routeRepository.findById(20L);

        assertEquals(Optional.empty(), notFound);
        assertTrue(found.isPresent());
        assertEquals(routes.get(0), found.get());
    }

    @Test
    @Transactional
    void testFindVisibleToMural() {
        List<Route> visibleToMural = routeRepository.findVisibleToMural(1L);

        assertEquals(List.of(routes.get(0),routes.get(2)), visibleToMural);
    }

    @Test
    @Transactional
    void testFindCreatedByOrOwned() {
        List<Route> visibleToMural = routeRepository.findUsedOrCreatedBy(me);

        assertEquals(List.of(routes.get(0),routes.get(1)), visibleToMural);
    }


}
