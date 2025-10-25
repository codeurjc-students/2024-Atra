package codeurjc_students.ATRA.e2e;

import codeurjc_students.ATRA.AtraApplication;
import codeurjc_students.ATRA.model.Activity;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.Route;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.ActivityRepository;
import codeurjc_students.ATRA.repository.RouteRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import codeurjc_students.ATRA.service.ActivityService;
import codeurjc_students.ATRA.service.MuralService;
import codeurjc_students.ATRA.service.RouteService;
import codeurjc_students.ATRA.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Clase de pruebas end-to-end (e2e) para la gestión de películas en la aplicación web.
 * Se realizan pruebas para la creación, edición, eliminación y validación de películas en la aplicación.
 * Utiliza Selenium para interactuar con la interfaz web y verificar su comportamiento.
 */
@SpringBootTest(
        classes = AtraApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@EntityScan(basePackages = "codeurjc_students.ATRA.model")
public class E2eTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private MuralService muralService;
    @Autowired
    private RouteService routeService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    private WebDriver driver;
    private WebDriverWait wait;
    private String BASE_URL;

    @BeforeAll
    void databaseInit() throws IOException {
        //<editor-fold desc="Users">
        User asd = new User("asd", passwordEncoder.encode("asd"));
        asd.setName("asd");
        userService.save(asd);
        User qwe = new User("qwe", passwordEncoder.encode("qwe"));
        qwe.setName("qwe");
        userService.save(qwe);
        User zxc = new User("zxc", passwordEncoder.encode("zxc"));
        zxc.setName("zxc");
        userService.save(zxc);
        //</editor-fold>

        System.out.println("Users initialized");

        //<editor-fold desc="Murals">
        Mural asdMural1 = new Mural("Mural1 asd", asd, new ArrayList<>());
        Mural asdMural2 = new Mural("Mural2 asd", asd, new ArrayList<>());

        for (Mural mural : List.of(asdMural1, asdMural2)) {
            try {
                setThumbnailAndBanner(mural);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            muralService.newMural(mural);
        }
        //</editor-fold>

        System.out.println("Murals initialized");

        //<editor-fold desc="Activities">
        List<Activity> activities = new ArrayList<>();
        for (int i=0;i<8;i++) {//19
            activities.add(activityService.newActivity(new ClassPathResource("static/track" + i + ".gpx").getInputStream(), asd));
        }        //all activities are initially created as property of asd, though asd does not know this

        System.out.println("Activities created");

        //<editor-fold desc="set asd activities">
        activities.get(0).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(1).changeVisibilityTo(VisibilityType.PUBLIC);
        activities.get(2).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(3).changeVisibilityTo(VisibilityType.MURAL_PUBLIC);
        activities.get(4).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, new ArrayList<>());
        activities.get(5).changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of(asdMural2.getId()));//, qweMural.getId(), zxcMural.getId()
        //activity.get(6) //también va para este pero como queda privada nos la ahorramos
        for (int i=0;i<7;i++) {
            Activity activity = activities.get(i);
            activity.setName("act" + i +" "+ asd.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
            activity.setOwner(asd);
            activityService.save(activity);
        }
        System.out.println("asd Activities set");
        //</editor-fold>

        //<editor-fold desc="set qwe and zxc activities">
        System.out.println("qwe Activities set");
        activities.get(7).changeVisibilityTo(VisibilityType.PUBLIC);//13

        Activity activity = activities.get(7);//i
        activity.setName("act " + 7 + zxc.getName() + " ("+activity.getVisibility().getType().getShortName()+")");
        activity.setOwner(zxc);
        activityService.save(activity);

        System.out.println("zxc Activities set");
        //</editor-fold>

        System.out.println("Activities initialized");
        //</editor-fold>

        //<editor-fold desc="Routes">
        Route route = routeService.newRoute(activityService.findByUser(asd).get(0));
        route.changeVisibilityTo(VisibilityType.PUBLIC);
        route.setName("r1 asd (PU)");
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(asd).get(2));
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r2 asd (MS)");
        activity.setRoute(route);
        activityService.save(activity);
        activity = activityService.findByUser(asd).get(1);
        activity.setRoute(route);
        activityService.save(activity);
        routeService.save(route);

        route = routeService.newRoute(activityService.findByUser(asd).get(3));
        route.changeVisibilityTo(VisibilityType.MURAL_SPECIFIC, List.of());
        route.setName("r3 asd (MS)");
        Activity extraActivity = activityService.findByUser(asd).get(4);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);

        extraActivity = activityService.findByUser(asd).get(6);
        extraActivity.setRoute(route);
        activityService.save(extraActivity);
        routeService.save(route);

        System.out.println("Routes initialized");
        //</editor-fold>
        System.out.println("Initialization complete");
    }

    @BeforeEach
    void setUp()  {
        BASE_URL = "http://localhost:4200";

        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless=new");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(BASE_URL+"/");
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }


    @Test
    void checkUserInfo() {
        login("asd");

        WebElement table = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@id='table-Info']")));
        //wait until everything finishes loading
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));

        verifyTable(table, "# of routes", "3");//5
        verifyTable(table, "# of activities", "7");//9
        verifyTable(table, "Total Distance", "42.32km");//48.94km
        verifyTable(table, "Total Time", "6:17:42");//7:06:25
        verifyTable(table, "Avg Pace", "12:41");
        verifyTable(table, "Max Distance", "11.62km");
        verifyTable(table, "Max Time", "2:00:02");
        verifyTable(table, "username", "asd");
        verifyTable(table, "displayname", "asd");
    }

    @Test
    void selectActivityTest() {
        //clicar en selector de actividades
        //seleccionar primera
        //dar submit
        //confirmar datos de overview (nombre, fecha, duration...)

        login("asd");
        //seleccionar actividad
        String name = "act1 asd (MP)";
        selectActivity(name);

        WebElement table = driver.findElement(By.xpath("//table"));
        verifyTable(table,"Name:", name);
        verifyTable(table,"Start time:", "8:01");
        verifyTable(table,"Date:", "2025-02-28");
        verifyTable(table,"Duration:", "46:30");
        verifyTable(table,"Total distance:", "8.63");
        verifyTable(table,"Elevation gain:", "91.91");
        verifyTable(table,"Average altitude:", "697.84");
        verifyTable(table,"Average cadence:", "0.00");
        verifyTable(table,"Average heartrate:", "0.00");
        verifyTable(table,"Average Pace:", "5:23");
        verifyTable(table,"Route:", "r2 asd (MS)");
    }

    @Test
    void deleteActivity() {
        //    seleccionar actividad
        //    dar delete
        //    checkear que se borra
        //    checkear que te lleva al selector
        //    checkear que no está en la lista
        //    intentar ir a /activities/deletedActivityId y confirmar error
        String activity = "act1 asd (MP)";
        login("asd");
        selectActivity(activity);
        //save the id for later
        String[] urlParts = driver.getCurrentUrl().split("/");
        String activityId = urlParts[urlParts.length-1];
        //delete it
        driver.findElement(By.id("btn-delete-activity")).click();
        driver.findElement(By.id("btn-confirm")).click();
        //check we're sent to the list and a notification is shown
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-activity-select")));
        String toastMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("toast-success"))).getText();
        assertTrue(toastMsg.contains("Activity deleted") );
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("toast-success")));
        //check it's not in the list
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));
        int shouldBe0 = driver.findElements(By.xpath("//table/tbody/tr[td[2][text()='"+activity+"']]")).size(); //ngx-toastr toast-success
        assertEquals(0, shouldBe0);
        //check we can't go to it
        driver.get(BASE_URL+"/me/activities/"+activityId);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-dismiss-alert-1"))).click();
        //puede dar problemas porque el alert que quitamos con la línea de arriba también tiene ese id. Aunque al haber hecho click debería no haber problema
        //podría también ser que por alguna razón el click haga click a ambos, habrá que ver cómo va la cosa
        String header404 = driver.findElement(By.id("header-alert")).getText();
        assertEquals("404 Not found", header404);
    }

    @Test
    void createActivity() {
        //3. subir actividad
        //clicar en subir actividad
        //subir actividad
        //darle a sí
        //comprobar datos de overview (nombre, fecha, duration...)
        String name = "Morning Run";

        login("qwe"); //a user with no activities
        Path filePath = Paths.get("src/main/resources/static/test_activity.gpx");
        File file = filePath.toFile();
        driver.findElement(By.id("input-upload-activity")).sendKeys(file.getAbsolutePath());
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm"))).click();
        String actName = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-activity-name"))).getText();
        assertEquals(name, actName);
        WebElement table = driver.findElement(By.xpath("//table"));
        verifyTable(table,"Name:", name);
        verifyTable(table,"Start time:", "9:32");
        verifyTable(table,"Date:", "2024-12-17");
        verifyTable(table,"Duration:", "1:01:16");
        verifyTable(table,"Total distance:", "10.74");
        verifyTable(table,"Elevation gain:", "145.00");
        verifyTable(table,"Average altitude:", "641.05");
        verifyTable(table,"Average cadence:", "86.95");
        verifyTable(table,"Average heartrate:", "155.32");
        verifyTable(table,"Average Pace:", "5:42");

        selectActivity(name); //to check it's in the list

    }

    @Test
    void deleteRoute() throws InterruptedException {
        // clicar en rutas
        // seleccionar r1.
        // confirmar que no hay botón
        // seleccionar r2
        // intentar borrar
        // confirmar que da error
        // seleccionar r3
        // borrar
        // confirmar que la borra
        // confirmar que deselecciona
        // confirmar que no está en la lista

        //check deleting a route that can be deleted deletes the route
        String route = "r3 asd (MS)";
        login("asd");
        selectRoute(route);
            //delete it
        driver.findElement(By.id("btn-delete-route")).click();
        driver.findElement(By.id("btn-confirm")).click();
            //check we're sent to the list and a notification is shown
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-route-name")));
        String toastMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("toast-success"))).getText();
        assertTrue(toastMsg.contains("Route deleted") );
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("toast-success")));
            //check it's not in the list
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));
        int shouldBe0 = driver.findElements(By.xpath("//table/tbody/tr[td[2][text()='"+route+"']]")).size();
        assertEquals(0, shouldBe0);
            //check no route is selected
        boolean noRouteSelected = driver.findElement(By.xpath("//th/input[@type='radio']")).isSelected();
        assertTrue(noRouteSelected);

        //check public can't be deleted
        route = "r1 asd (PU)";
        selectRoute(route);
        shouldBe0 = driver.findElements(By.id("btn-delete-route")).size();
        assertEquals(0, shouldBe0);

        //check used by others can't be deleted
        route = "r2 asd (MS)";
        selectRoute(route);
            //delete it
        driver.findElement(By.id("btn-delete-route")).click();
        driver.findElement(By.id("btn-confirm")).click();
        Thread.sleep(500);

            //check the alert is correct
        String cantDeleteMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-alert"))).getText();
        assertEquals("400 Bad Request", cantDeleteMsg);
            //check stll exists
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));
        driver.findElement(By.xpath("//table/tbody/tr[td[2][text()='"+route+"']]"));

    }

    @Test
    void createRoute() {
        //seleccionar actividad
        //dar a create route
        //cambiar nombre
        //poner desc
        //dar a create
        //aceptar que te lleve
        //confirmar que carga /routes?selected=9
        //confirmar que está seleccionada
        //Confirmar nombre desc y distance
        String routeName = "New Route Test";
        String routeDesc = "New Route Description";
        String routeDist = "31.41";
        String routeEle = "271.82";

        login("asd");
        selectActivity("act5 asd (MS)");
        driver.findElement(By.id("btn-create-route")).click();
        WebElement webElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("input-route-name")));
        webElement.clear();
        webElement.sendKeys(routeName);
        webElement = driver.findElement(By.id("input-route-desc"));
        webElement.clear();
        webElement.sendKeys(routeDesc);
        webElement = driver.findElement(By.id("input-route-dist"));
        webElement.clear();
        webElement.sendKeys(routeDist);
        webElement = driver.findElement(By.id("input-route-ele"));
        webElement.clear();
        webElement.sendKeys(routeEle);

        driver.findElement(By.id("btn-route-create")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("btn-confirm"))).click();

        WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table/tbody/tr[td[2][text()='" + routeName + "']]")));
        List<WebElement> cells = row.findElements(By.tagName("td"));
        assertTrue(cells.get(0).findElement(By.tagName("input")).isSelected()); //Cuidao
        assertEquals(routeName, cells.get(1).getText());
        assertEquals(routeDesc, cells.get(2).getText());
        assertEquals(routeDist+"km", cells.get(3).getText());
        assertEquals(routeEle+"m", cells.get(4).getText());

        String text = driver.findElement(By.id("header-route-name")).getText();
        assertTrue(text.contains(routeName));
        text = driver.findElement(By.id("text-route-description")).getText();
        assertTrue(text.contains(routeDesc));
        text = driver.findElement(By.id("text-route-distance")).getText();
        assertTrue(text.contains(routeDist+"km"));

    }

    @Test
    void selectMuralTest() throws InterruptedException {
        login("asd");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-mural"))).click();
        Thread.sleep(1000L);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='owned-murals-container']/div[contains(@class,'thumbnail-container')][1]"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-mural-Info")));

        WebElement table = driver.findElement(By.id("table-Info"));
        verifyTable(table,"Name", "Mural1 asd");
        verifyTable(table,"Owner", "asd");
        verifyTable(table,"# of members", "1");
        verifyTable(table,"# of routes", "1");
        verifyTable(table,"# of activities", "4");
        verifyTable(table,"Total Distance", "27.28km");
        verifyTable(table,"Total Time", "4:42:13");
        verifyTable(table,"Avg Pace", "16:53");
        verifyTable(table,"Max Distance", "11.62km");
        verifyTable(table,"Max Time", "2:00:02");
        verifyTable(table,"Owner", "asd");
    }
    @Test
    void deleteMural() throws InterruptedException {
        //seleccionar mural1 asd
        //ir a settings
        //pasar gauntlet de delete
        //confirmar que se borra
        //confirmar que no se muestra en el listado
        //intentar ir a /murals/deletedMuralId y comprobar que da error
        String muralName = "Mural1 asd";

        login("asd");
        //Select the mural
        wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-mural"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')][1]")));
        Thread.sleep(1000);
        driver.findElement(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')][1]")).click();
        //save the id for later
        wait.until(ExpectedConditions.urlContains("/dashboard"));
        String[] urlParts = driver.getCurrentUrl().split("/");
        String muralId = urlParts[urlParts.length-2];
        //delete mural
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-mural-settings"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-mural-delete"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("input-alert-text"))).sendKeys(muralName);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-dismiss-alert"))).click();
        //check it's not in the list
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')]")));
        int size = driver.findElements(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')]")).size();
        assertEquals(1, size);

        Thread.sleep(3000);
        driver.get(BASE_URL+"/murals/"+muralId+"/dashboard");
        String alertHeader = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-alert"))).getText();
        assertTrue(alertHeader.contains("404 Not found"));
    }

    @Test
    void createMural() throws IOException {
        //dar al +
        //        introducir datos
        //dar a crear mural
        //confirmar que tiene los datos introducidos (como comprobamos las imagenes?)
        String name = "New Mural Name Test";
        String desc = "New Mural Desc Test";
        Path thumbnailPath = Paths.get("src/main/resources/static/alt_3-2.png");
        File file = thumbnailPath.toFile();
        String thumbnail = file.getAbsolutePath();
        byte[] expectedThumbnailBytes = Files.readAllBytes(thumbnailPath);
        Path bannerPath = Paths.get("src/main/resources/static/altBannerImage.png");
        file = bannerPath.toFile();
        String banner = file.getAbsolutePath();
        byte[] expectedBannerBytes = Files.readAllBytes(bannerPath);

        login("asd");
        //create mural
        wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-mural"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-mural-create"))).click();
            //fill form
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-new-mural")));

        driver.findElement(By.id("input-mural-name")).sendKeys(name);
        //driver.findElement(By.id("input-mural-name")).sendKeys(Keys.TAB);
        driver.findElement(By.id("input-mural-desc")).sendKeys(desc);
        //driver.findElement(By.id("input-mural-desc")).sendKeys(Keys.TAB);
        driver.findElement(By.id("input-mural-banner")).sendKeys(banner);
        driver.findElement(By.id("input-mural-thumbnail")).sendKeys(thumbnail);

        driver.findElement(By.id("header-new-mural")).click();

        driver.findElement(By.id("btn-mural-create-final")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-confirm"))).click();
        //wait for mural to be created and check values in dashboard
        wait.until(ExpectedConditions.urlContains("dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/murals/"));

        WebElement img = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("img-mural-banner")));
        URL imageUrl = new URL(img.getAttribute("src").replace("4200","8080"));
        try (InputStream in = imageUrl.openStream()) {
            byte[] actualBytes = in.readAllBytes();
            assertArrayEquals(expectedBannerBytes, actualBytes);
        }

        WebElement table = driver.findElement(By.id("table-Info"));
        verifyTable(table,"Name", name);
        verifyTable(table,"Owner", "asd");
        verifyTable(table,"# of members", "1");
        //check desc and data in settings
        driver.findElement(By.id("btn-mural-settings")).click();
        String text = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("text-mural-owner"))).getText();
        assertEquals("asd", text);
        text = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("text-mural-name"))).getText();
        assertEquals(name, text);
        text = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("text-mural-desc"))).getText();
        assertEquals(desc, text);

        //check it's in the list and the thumbnail is good. if image checking works badly just remove the image check
        driver.get(BASE_URL+"/murals");
        //wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')]")));
        WebElement container = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@id='owned-murals-container']/div[contains(@class, 'thumbnail-container')][./div/span[text()='"+name+"']]")));
        //String src = container.findElement(By.xpath(".//img")).getAttribute("src");
        img = container.findElement(By.xpath(".//img"));
        imageUrl = new URL(img.getAttribute("src").replace("4200","8080"));
        try (InputStream in = imageUrl.openStream()) {
            byte[] actualBytes = in.readAllBytes();
            assertArrayEquals(expectedThumbnailBytes, actualBytes);
        }
    }


    void verifyTable(WebElement table, String target, String expected) {
        String actual = table.findElement(By.xpath("./tbody/tr[td[1][contains(text(), '"+target+"')]]/td[2]")).getText();
        assertEquals(expected, actual);
    }

    private void login(String username) {
        driver.get(BASE_URL);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("btn-login-modal"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.id("username"))).sendKeys(username);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("password"))).sendKeys(username);
        driver.findElement(By.id("btn-login")).click();
        wait.until(ExpectedConditions.urlToBe(BASE_URL+"/me/home"));
        wait.until(ExpectedConditions.elementToBeClickable(By.id("nav-user")));
    }

    private void selectActivity(String name) {
        //select activity
        driver.findElement(By.id("btn-activity-list")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("loading")));
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table/tbody/tr[td[2]/div[text()='"+name+"']]/td[1]/input[@type='checkbox']"))).click();
        driver.findElement(By.id("btn-submit-activities")).click();

        //wait until it loads
        String header = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-activity-name"))).getText();
        assertEquals(name, header);
    }

    private void selectRoute(String name) {
        //select route
        driver.findElement(By.id("btn-route-list")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//table/tbody/tr[td[2][text()='"+name+"']]/td[1]/input[@type='radio']"))).click();

        //wait until it loads
        String header = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("header-route-name"))).getText();
        assertTrue(header.contains(name));
    }

    private void setThumbnailAndBanner(Mural mural) throws IOException {
        byte[] thumbnailBytes = new ClassPathResource("static/defaultThumbnailImage.png").getInputStream().readAllBytes();
        byte[] bannerBytes = new ClassPathResource("static/altBannerImage.png").getInputStream().readAllBytes();
        mural.setBanner(bannerBytes);
        mural.setThumbnail(thumbnailBytes);
    }
}
