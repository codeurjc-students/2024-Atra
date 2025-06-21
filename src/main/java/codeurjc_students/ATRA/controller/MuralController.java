package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
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
    private DeletionService deletionService;


    @GetMapping("/{id}")
    public ResponseEntity<MuralDTO> getMural(@PathVariable Long id){
        Optional<Mural> muralOpt = muralService.findById(id);
        if (muralOpt.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new MuralDTO(muralOpt.get()));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id){
        Optional<Mural> muralOpt = muralService.findById(id);
        if (muralOpt.isEmpty()) return ResponseEntity.notFound().build();
        byte[] body = muralOpt.get().getThumbnail();
        if (body==null) body = MuralService.getDefaultThumbnailBytes();

        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(body);
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
            if ("owned".equals(type)) return ResponseEntity.ok(MuralDTO.toDto(user.getOwnedMurals()));
            else if ("member".equals(type)) return ResponseEntity.ok(MuralDTO.toDto(user.getMemberMurals()));
            else if ("other".equals(type)) return ResponseEntity.ok(MuralDTO.toDto(muralService.findOther(user.getMemberMurals())));
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
            return ResponseEntity.ok(new MuralDTO(newMural));
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    @GetMapping("/{muralId}/activities")
    public ResponseEntity<List<ActivityDTO>> getActivities(Principal principal, @PathVariable Long muralId){
        //exceptions are handled by HttpExceptionHandler
        User user = principalVerification(principal);
        Mural mural = muralService.findById(muralId).orElseThrow(()->new HttpException(404, "Mural not found"));
        if (!mural.getMembers().contains(user) && !user.hasRole("ADMIN")) throw new HttpException(403, "User is not in Mural, thus, they can't access its activities");
        //user and mural both exists, and user is in the mural
        //now return the mural's activities
        return ResponseEntity.ok(ActivityDTO.toDto(mural.getActivities()));
    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
    }


}

