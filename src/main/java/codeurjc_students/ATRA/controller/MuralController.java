package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<MuralDTO> getMural(Principal principal, @PathVariable Long id){
        User user = principalVerification(principal);

        Mural mural = muralService.findById(id).orElseThrow(()->new HttpException(404, "Mural not found"));
        if (!mural.getMembers().contains(user) || user.hasRole("ADMIN")) throw new HttpException(403, "Only members or admin can fetch a mural. User is not a member of specified mural");

        MuralDTO result = new MuralDTO(mural);
        if (mural.getOwner().equals(user) || mural.getMembers().contains(user))  //delete second clause to only allow user to see the code
            result.setCode(mural.getCode());
        return ResponseEntity.ok(result);
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
            if ("owned".equals(type)) return ResponseEntity.ok(MuralDTO.toDto(muralService.findOwnedBy(user)));
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
                                                @RequestParam("visibility") String visibility,
                                                @RequestParam("thumbnail")MultipartFile thumbnail,
                                                @RequestParam("banner") MultipartFile banner
                                                ) {
        try {
            if (!"PRIVATE".equals(visibility) && !"PUBLIC".equals(visibility)) throw new HttpException(400, "Invalid visibility");
            User owner = principalVerification(principal);
            Mural newMural = new Mural(
                    name,
                    description,
                    owner,
                    VisibilityType.valueOf(visibility),
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
        return ResponseEntity.ok(ActivityDTO.toDto(activityService.findVisibleTo(mural)));
    }

    @PostMapping("/join")
    public ResponseEntity<Integer> joinMural(Principal principal, @RequestBody String muralCodeOrId) {
        User user = principalVerification(principal);
        Mural mural;
        if (muralCodeOrId.contains("-")) {
            mural = muralService.findByCode(muralCodeOrId).orElseThrow(() -> new HttpException(404, "Mural with specified code not found. Cannot join specified mural."));
        } else {
            mural = muralService.findById(Long.parseLong(muralCodeOrId)).orElseThrow(() -> new HttpException(404, "Mural with specified id not found. Cannot join specified mural."));
        }

        if (mural.getMembers().contains(user)) return ResponseEntity.ok(1); //1 for user already in mural
        if (mural.getBannedUsers().contains(user)) return ResponseEntity.ok(2); //2 for user banned from mural
        //join mural, return 0
        mural.addMember(user);
        user.addMemberMural(mural);
        muralService.save(mural);
        userService.save(user);
        return ResponseEntity.ok(0);
    }

    @DeleteMapping("/{id}/users/me")
    public ResponseEntity<List<NamedId>> removeCurrentUserFromMural(Principal principal, @PathVariable Long id, @RequestParam(value="inheritor", required=false) Long inheritorId) {
        Mural mural = muralService.findById(id).orElseThrow(()->new HttpException(404, "Mural not found"));
        User user = principalVerification(principal);
        return removeSpecifiedUserFromMural(mural, user, inheritorId);

    }

    @DeleteMapping("/{muralId}/users/{userId}")
    public ResponseEntity<List<NamedId>> removeSpecifiedUserFromMural(Principal principal, @PathVariable Long muralId, @PathVariable Long userId, @RequestParam(value="inheritor", required=false) Long inheritorId) {
        Mural mural = muralService.findById(muralId).orElseThrow(()->new HttpException(404, "Mural not found"));
        User admin = principalVerification(principal);
        if (!admin.hasRole("ADMIN") && !admin.equals(mural.getOwner())) throw new HttpException(403, "Only admins and the mural owner can remove users from murals");
        User user = userService.findById(userId).orElseThrow(() -> new HttpException(404, "User not found"));
        return removeSpecifiedUserFromMural(mural, user, inheritorId);
    }

    @PostMapping("/{muralId}/users/{userId}/ban")
    public ResponseEntity<List<NamedId>> changeBanner(Principal principal, @PathVariable("muralId") Long muralId, @PathVariable("userId") Long userId){
        List<NamedId> body = removeSpecifiedUserFromMural(principal, muralId, userId, null).getBody();
        Mural mural = muralService.findById(muralId).orElseThrow(()->new HttpException(404, "Mural not found"));
        User user = userService.findById(userId).orElseThrow(() -> new HttpException(404, "User to ban not found"));
        mural.banUser(user);
        muralService.save(mural);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMural(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(id).orElseThrow(() -> new HttpException(404, "Mural not found"));
        if (!user.equals(mural.getOwner()) && !user.hasRole("ADMIN")) throw new HttpException(403, "User is not authorized to delete this mural");
        //checks made, now to actually delete it
        deletionService.deleteMural(id);
        return null;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MuralDTO> editMural(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(id).orElseThrow(()->new HttpException(404, "Mural not found"));
        if (!user.equals(mural.getOwner()) && !user.hasRole("ADMIN")) throw new HttpException(403, "User is not authorized to edit this mural");
        muralService.patch(mural, body);

        return ResponseEntity.ok(new MuralDTO(mural));
    }

    @GetMapping("/{id}/isVisible")
    public ResponseEntity<Boolean> isVisibleByAuthUser(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(id).orElseThrow(() -> new HttpException(404, "Mural not found"));
        return ResponseEntity.ok(user.getMemberMurals().contains(mural));
    }

    private ResponseEntity<List<NamedId>> removeSpecifiedUserFromMural(Mural mural, User user, Long inheritorId) {
        if (!mural.getMembers().contains(user)) throw new HttpException(404, "User is not a member of specified mural");
        if (mural.getMembers().size()==1) {
            deletionService.deleteMural(mural.getId());
            return ResponseEntity.ok().build();
        }

        //handle owner crap
        if (user.equals(mural.getOwner())) {
            User inheritor = null;
            if (inheritorId!=null) {
                User a = userService.findById(inheritorId).orElseThrow(()->new HttpException(404, "Inheriting user not found, cancelling."));
                if (!mural.getMembers().contains(a)) throw new HttpException(422, "Inheriting user is not a member of the mural");
                inheritor = a;

            }
            user.removeMemberMural(mural);
            mural.removeOwner(user, inheritor); //also removes from members
        } else {
            user.removeMemberMural(mural);
            mural.removeMember(user);
        }
        for (Activity a : activityService.findByUser(user)) {
            if (a.getVisibility().isMuralSpecific() && a.getVisibility().getAllowedMurals().contains(mural.getId())) {
                a.getVisibility().removeMural(mural.getId());
                if (a.getVisibility().getAllowedMurals().isEmpty()) a.changeVisibilityTo(VisibilityType.PRIVATE);
            }
        }

        userService.save(user);
        muralService.save(mural);
        return ResponseEntity.ok(BasicNamedId.from(new ArrayList<>(mural.getMembers())));
    }

    @PutMapping("/{id}/thumbnail")
    public ResponseEntity<String> changeThumbnail(Principal principal, @PathVariable Long id, @RequestParam("file") MultipartFile body){
        return changeThumbnailOrBanner(true, principal, id, body);
    }
    @PutMapping("/{id}/banner")
    public ResponseEntity<String> changeBanner(Principal principal, @PathVariable Long id, @RequestParam("file") MultipartFile body){
        return changeThumbnailOrBanner(false, principal, id, body);
    }


    private ResponseEntity<String> changeThumbnailOrBanner(boolean shouldChangeThumbnail, Principal principal, Long id, MultipartFile body) {
        User user = principalVerification(principal);
        Mural mural = muralService.findById(id).orElseThrow(()->new HttpException(404, "Mural not found"));
        if (!user.equals(mural.getOwner()) && !user.hasRole("ADMIN")) throw new HttpException(403, "User is not authorized to edit this mural");

        //ImageIO is used to prevent injection
        try (InputStream is = body.getInputStream()) {
            if (body.getContentType()==null || !body.getContentType().startsWith("image/")) throw new HttpException(422, "Received file is not an image.");
            BufferedImage image = ImageIO.read(is);
            if (image == null) throw new HttpException(422, "File is not a valid image");

            if (shouldChangeThumbnail) mural.setThumbnail(body.getBytes());
            else mural.setBanner(body.getBytes());
            muralService.save(mural);
        } catch (IOException e) {
            throw new HttpException(400, "File could not be opened");
        }
        return ResponseEntity.ok().build();
    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
    }

}

