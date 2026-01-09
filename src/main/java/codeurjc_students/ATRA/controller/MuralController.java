package codeurjc_students.atra.controller;

import codeurjc_students.atra.dto.*;
import codeurjc_students.atra.exception.HttpException;
import codeurjc_students.atra.exception.IncorrectParametersException;
import codeurjc_students.atra.model.*;
import codeurjc_students.atra.model.auxiliary.BasicNamedId;
import codeurjc_students.atra.model.auxiliary.NamedId;
import codeurjc_students.atra.model.auxiliary.VisibilityType;
import codeurjc_students.atra.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/murals")
@Tag(name = "Murals", description = "Mural management endpoints")
public class MuralController {

	private final UserService userService;
    private final MuralService muralService;



    @GetMapping("/{id}")
    @Operation(summary = "Get mural by ID", description = "Retrieve a mural's information by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mural found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<MuralDTO> getMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id){
        User user = principalVerification(principal);
        Mural mural = muralService.getMural(user, id);

        MuralDTO result = new MuralDTO(mural);
        if (mural.getOwner().equals(user) || mural.getMembers().contains(user))  //delete second clause to only allow user to see the code
            result.setCode(mural.getCode());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/thumbnail")
    @Operation(summary = "Get mural thumbnail", description = "Retrieve the thumbnail image of a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thumbnail retrieved"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<byte[]> getThumbnail(
        @Parameter(description = "Mural ID") @PathVariable Long id){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralService.getThumbnail(id));
    }

    @GetMapping("/{id}/banner")
    @Operation(summary = "Get mural banner", description = "Retrieve the banner image of a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Banner retrieved"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<byte[]> getBanner(
        @Parameter(description = "Mural ID") @PathVariable Long id){
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(muralService.getBanner(id));
    }

    @GetMapping
    @Operation(summary = "Get murals", description = "Retrieve murals of a specific type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Murals retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<MuralDTO>> getMurals(
        Principal principal, 
        @Parameter(description = "Type of murals to retrieve") @RequestParam(name = "type") String type){
        User user = principalVerification(principal);
        return ResponseEntity.ok(MuralDTO.toDto(muralService.getMurals(user, type)));
    }

    @PostMapping
    @Operation(summary = "Create new mural", description = "Create a new mural with thumbnail and banner images")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mural created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid image format or visibility"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<MuralDTO> createMural(
        Principal principal,
        @Parameter(description = "Mural name") @RequestParam("name") String name,
        @Parameter(description = "Mural description") @RequestParam("description") String description,
        @Parameter(description = "Visibility type: PUBLIC or PRIVATE") @RequestParam("visibility") String visibility,
        @Parameter(description = "Thumbnail image file") @RequestParam("thumbnail")MultipartFile thumbnail,
        @Parameter(description = "Banner image file") @RequestParam("banner") MultipartFile banner
    ) {
        User user = principalVerification(principal);
        Mural newMural;
        try {
            newMural = new Mural(
                    name,
                    description,
                    user,
                    VisibilityType.valueOf(visibility),
                    thumbnail.getBytes(),
                    banner.getBytes()
            );
        } catch (IOException e) {
            throw new IncorrectParametersException("There was an error reading the selected thumbnail or banner. Make sure they are images with the correct format.", e);
        } catch (IllegalArgumentException e) {
            throw new IncorrectParametersException("Selected visibility is not a valid visibility. Valid visibility values are PRIVATE and PUBLIC.", e);
        }

        return ResponseEntity.ok(new MuralDTO(muralService.createMural(newMural)));
    }

    @PostMapping("/join")
    @Operation(summary = "Join mural", description = "Join a mural using a code or mural ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully joined mural"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<Integer> joinMural(
        Principal principal, 
        @RequestBody String muralCodeOrId) {
        User user = principalVerification(principal);

        String muralCode = null;
        Long muralId = null;
        if (muralCodeOrId.contains("-")) muralCode = muralCodeOrId;
        else muralId = Long.parseLong(muralCodeOrId);

        return ResponseEntity.ok(muralService.joinMural(user, muralCode, muralId));
    }

    @DeleteMapping("/{id}/users/me")
    @Operation(summary = "Leave mural", description = "Remove the current user from a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User removed from mural"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<List<? extends NamedId>> removeCurrentUserFromMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id, 
        @Parameter(description = "New owner ID if current owner is leaving") @RequestParam(value="inheritor", required=false) Long inheritorId) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.removeUserFromMural(user, user.getId(), id, inheritorId)));
    }

    @DeleteMapping("/{muralId}/users/{userId}")
    @Operation(summary = "Remove user from mural", description = "Remove a specified user from a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User removed from mural"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Mural or user not found")
    })
    public ResponseEntity<List<NamedId>> removeSpecifiedUserFromMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long muralId, 
        @Parameter(description = "User ID to remove") @PathVariable Long userId, 
        @Parameter(description = "New owner ID if current owner is leaving") @RequestParam(value="inheritor", required=false) Long inheritorId) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.removeUserFromMural(user, userId, muralId, inheritorId)));
    }

    @PostMapping("/{muralId}/users/{userId}/ban")
    @Operation(summary = "Ban user from mural", description = "Ban a user from a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User banned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Mural or user not found")
    })
    public ResponseEntity<List<NamedId>> banUser(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable("muralId") Long muralId, 
        @Parameter(description = "User ID to ban") @PathVariable("userId") Long userId){
        User user = principalVerification(principal);
        return ResponseEntity.ok(BasicNamedId.from(muralService.banUser(user, muralId, userId)));
    }

    @PostMapping("/{muralId}/users/{userId}/unban")
    @Operation(summary = "Unban user from mural", description = "Remove a ban for a user from a mural")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User unbanned successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Mural or user not found")
    })
    public ResponseEntity<List<NamedId>> unbanUser(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable("muralId") Long muralId, 
        @Parameter(description = "User ID to unban") @PathVariable("userId") Long userId){
        User user = principalVerification(principal);
        List<User> bannedUsers = muralService.unbanUser(user, userId, muralId);
        return ResponseEntity.ok(BasicNamedId.from(bannedUsers));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete mural", description = "Delete a mural and all its contents")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mural deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - only owner can delete"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<String> deleteMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id) {
        User user = principalVerification(principal);
        muralService.deleteMural(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit mural", description = "Update mural information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mural updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<MuralDTO> editMural(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id, 
        @RequestBody MuralEditDTO body) {
        User user = principalVerification(principal);
        Mural mural = muralService.editMural(user, id, body);
        return ResponseEntity.ok(new MuralDTO(mural));
    }

    @GetMapping("/{id}/isVisible")
    @Operation(summary = "Check mural visibility", description = "Check if a mural is visible to the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Visibility status retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<Boolean> isVisibleByAuthUser(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id) {
        User user = principalVerification(principal);
        return ResponseEntity.ok(muralService.isVisibleByUser(user, id));
    }

    @PutMapping("/{id}/thumbnail")
    @Operation(summary = "Update mural thumbnail", description = "Replace the mural's thumbnail image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Thumbnail updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid image format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<String> changeThumbnail(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id, 
        @Parameter(description = "Image file") @RequestParam("file") MultipartFile body){
        User user = principalVerification(principal);
        byte[] img = parseImage(body);
        muralService.changeThumbnailOrBanner(true, user, id, img);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/banner")
    @Operation(summary = "Update mural banner", description = "Replace the mural's banner image")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Banner updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid image format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Mural not found")
    })
    public ResponseEntity<String> changeBanner(
        Principal principal, 
        @Parameter(description = "Mural ID") @PathVariable Long id, 
        @Parameter(description = "Image file") @RequestParam("file") MultipartFile body){
        User user = principalVerification(principal);
        byte[] img = parseImage(body);
        muralService.changeThumbnailOrBanner(false, user, id, img);
        return ResponseEntity.noContent().build();
    }


    private byte[] parseImage(MultipartFile body) {
        try (InputStream is = body.getInputStream()) {
            String contentType = body.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) throw new IncorrectParametersException("Received file is not an image.");
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

