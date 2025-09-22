package codeurjc_students.ATRA.controller;

import codeurjc_students.ATRA.dto.*;
import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.model.*;
import codeurjc_students.ATRA.model.auxiliary.BasicNamedId;
import codeurjc_students.ATRA.model.auxiliary.NamedId;
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
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/murals")
public class MuralController {

	@Autowired
	private UserService userService;
    @Autowired
    private MuralService muralService;



    @GetMapping("/{id}")
    public ResponseEntity<MuralDTO> getMural(Principal principal, @PathVariable Long id){
        User user = principalVerification(principal);
        Mural mural = muralService.getMural(user, id);

        MuralDTO result = new MuralDTO(mural);
        if (mural.getOwner().equals(user) || mural.getMembers().contains(user))  //delete second clause to only allow user to see the code
            result.setCode(mural.getCode());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralService.getThumbnail(id));
    }
    @GetMapping("/{id}/banner")
    public ResponseEntity<byte[]> getBanner(@PathVariable Long id){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralService.getBanner(id));
    }

    @GetMapping
    public ResponseEntity<List<MuralDTO>> getMurals(Principal principal, @RequestParam(name = "type") String type){
        User user = principalVerification(principal);
        return ResponseEntity.ok(MuralDTO.toDto(muralService.getMurals(user, type)));
    }

    @PostMapping
    public ResponseEntity<MuralDTO> createMural(Principal principal,
                                                @RequestParam("name") String name,
                                                @RequestParam("description") String description,
                                                @RequestParam("visibility") String visibility,
                                                @RequestParam("thumbnail")MultipartFile thumbnail,
                                                @RequestParam("banner") MultipartFile banner
                                                ) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(new MuralDTO(muralService.createMural(user, name, description, visibility, thumbnail, banner)));
    }

    @PostMapping("/join")
    public ResponseEntity<Integer> joinMural(Principal principal, @RequestBody String muralCodeOrId) {
        User user = principalVerification(principal);

        String muralCode = null;
        Long muralId = null;
        if (muralCodeOrId.contains("-")) muralCode = muralCodeOrId;
        else muralId = Long.parseLong(muralCodeOrId);

        return ResponseEntity.ok(muralService.joinMural(user, muralCode, muralId));
    }

    @DeleteMapping("/{id}/users/me")
    public ResponseEntity<List<? extends NamedId>> removeCurrentUserFromMural(Principal principal, @PathVariable Long id, @RequestParam(value="inheritor", required=false) Long inheritorId) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.removeUserFromMural(user, user.getId(), id, inheritorId)));
    }

    @DeleteMapping("/{muralId}/users/{userId}")
    public ResponseEntity<List<NamedId>> removeSpecifiedUserFromMural(Principal principal, @PathVariable Long muralId, @PathVariable Long userId, @RequestParam(value="inheritor", required=false) Long inheritorId) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.removeUserFromMural(user, userId, muralId, inheritorId)));
    }

    @PostMapping("/{muralId}/users/{userId}/ban")
    public ResponseEntity<List<NamedId>> banUser(Principal principal, @PathVariable("muralId") Long muralId, @PathVariable("userId") Long userId){
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.banUser(user, muralId, userId)));
    }
    @PostMapping("/{muralId}/users/{userId}/unban")
    public ResponseEntity<List<NamedId>> unbanUser(Principal principal, @PathVariable("muralId") Long muralId, @PathVariable("userId") Long userId){
        User user = principalVerification(principal);
        List<User> bannedUsers = muralService.unbanUser(user, userId, muralId);
        return ResponseEntity.ok(BasicNamedId.from(bannedUsers));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMural(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        muralService.deleteMural(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MuralDTO> editMural(Principal principal, @PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = principalVerification(principal);
        Mural mural = muralService.editMural(user, id, body);
        return ResponseEntity.ok(new MuralDTO(mural));
    }

    @GetMapping("/{id}/isVisible")
    public ResponseEntity<Boolean> isVisibleByAuthUser(Principal principal, @PathVariable Long id) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(muralService.isVisibleByUser(user, id));
    }

    @PutMapping("/{id}/thumbnail")
    public ResponseEntity<String> changeThumbnail(Principal principal, @PathVariable Long id, @RequestParam("file") MultipartFile body){
        User user = principalVerification(principal);
        byte[] img = parseImage(body);
        muralService.changeThumbnailOrBanner(true, user, id, img);
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/{id}/banner")
    public ResponseEntity<String> changeBanner(Principal principal, @PathVariable Long id, @RequestParam("file") MultipartFile body){
        User user = principalVerification(principal);
        byte[] img = parseImage(body);
        muralService.changeThumbnailOrBanner(false, user, id, img);
        return ResponseEntity.noContent().build();
    }


    private byte[] parseImage(MultipartFile body) {
        try (InputStream is = body.getInputStream()) {
            if (body.getContentType()==null || !body.getContentType().startsWith("image/")) throw new IncorrectParametersException("Received file is not an image.");
            BufferedImage image = ImageIO.read(is);
            if (image == null) throw new IncorrectParametersException("File is not a valid image");
            return body.getBytes();
        } catch (IOException e) {
            throw new IncorrectParametersException("File could not be opened");
        }
    }

    private User principalVerification(Principal principal) throws HttpException {
        if (principal==null) throw new HttpException(401);
        return userService.findByUserName(principal.getName()).orElseThrow(() -> new HttpException(404, "User not found"));
    }

}

