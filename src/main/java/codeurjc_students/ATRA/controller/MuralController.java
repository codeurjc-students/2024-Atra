package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;


@RestController
@RequestMapping("/api/murals")
public class MuralController {

	@Autowired
	private UserService userService;
    @Autowired
	private RouteService routeService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private MuralService muralService;
    @Autowired
    private DtoService dtoService;
    @Autowired
    private DeletionService deletionService;


    @GetMapping("/{id}")
    public ResponseEntity<MuralDTO> getMural(@PathVariable Long id){
        Optional<Mural> muralOpt = muralService.findById(id);
        if (muralOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dtoService.toDto(muralOpt.get()));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id){
        Optional<Mural> muralOpt = muralService.findById(id);
        if (muralOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralOpt.get().getThumbnail());
    }
    @GetMapping("/{id}/banner")
    public ResponseEntity<byte[]> getBanner(@PathVariable Long id){
        Optional<Mural> muralOpt = muralService.findById(id);
        if (muralOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralOpt.get().getBanner());
    }

    @GetMapping
    public ResponseEntity<List<MuralDTO>> getMurals(Principal principal, @RequestParam(name = "type") String type){
        try {
            User user = principalVerification(principal);
            if ("owned".equals(type)) return ResponseEntity.ok(dtoService.toDto(user.getOwnedMurals()));
            else if ("member".equals(type)) return ResponseEntity.ok(dtoService.toDto(user.getMemberMurals()));
            else if ("other".equals(type)) return ResponseEntity.ok(dtoService.toDto(muralService.findOther(user.getMemberMurals())));
            else throw new HttpException(500, "500 Internal Server Error: Unknown type for GET /api/murals");

        } catch (HttpException e) {
            if (e.getStatus()==500) throw new RuntimeException(e.getMessage());
            return ResponseEntity.status(e.getStatus()).build();
        }
    }

    @PostMapping
    public ResponseEntity<MuralDTO> createMural(Principal principal,
                                                @RequestParam("name") String name,
                                                @RequestParam("description") String description,
                                                @RequestParam("thumbnail")MultipartFile thumbnail,
                                                @RequestParam("banner") MultipartFile banner
                                                ) {
        try {
            User owner = principalVerification(principal);
            Mural newMural = new Mural(
                    name,
                    description,
                    owner,
                    thumbnail.getBytes(),
                    banner.getBytes()
            );
            this.muralService.newMural(newMural);
            return ResponseEntity.ok(dtoService.toDto(newMural));
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        User user = userService.findByUserName(principal.getName()).orElse(null);
        if (user == null) throw new HttpException(404);
        else return user;
    }


}

