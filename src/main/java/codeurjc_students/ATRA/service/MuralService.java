package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.dto.MuralEditDTO;
import codeurjc_students.ATRA.exception.EntityNotFoundException;
import codeurjc_students.ATRA.exception.IncorrectParametersException;
import codeurjc_students.ATRA.exception.PermissionException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class MuralService {

	@Autowired
	private MuralRepository muralRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private RouteRepository routeRepository;
	@Autowired
	private ActivityRepository activityRepository;

	public boolean exists(long id) {
		return muralRepository.existsById(id);
	}


	/**
	 * Generates the mural code, and adds the mural to the memberList of all its member users
	 * @param mural
	 */
	protected void newMural(Mural mural) {
		String code;
		do {
			String[] parts = UUID.randomUUID().toString().split("-");
			code = parts[1] + "-" + parts[2] + "-" + parts[3];
		} while (muralRepository.findByCode(code).isPresent()); //repeat until empty, to make sure it's not repeated
		mural.setCode(code);
		muralRepository.save(mural);
		//generate code

		for (var user: mural.getMembers()) {
			user.getMemberMurals().add(mural);
		}
	}

	private Collection<Mural> findOther(List<Mural> memberMurals) {
		Set<Mural> hashSet = new HashSet<>(findByVisibility(VisibilityType.PUBLIC));
		memberMurals.forEach(hashSet::remove);
		return hashSet;
	}

	private Collection<Mural> findByVisibility(VisibilityType visibilityType) {
		return muralRepository.findByVisibility(visibilityType);
	}

	public static byte[] getDefaultThumbnailBytes() {
		try {
			return new ClassPathResource("static/defaultThumbnailImage.png").getInputStream().readAllBytes();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static byte[] getDefaultBannerBytes() {
		try {
			return new ClassPathResource("static/defaultBannerImage.png").getInputStream().readAllBytes();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

    private void patch(Mural mural, MuralEditDTO newMural) {
		String name = newMural.getName();
		String description = newMural.getDescription();
		Long newOwnerLong = newMural.getNewOwner();
		if (newOwnerLong!=null) {
			User newOwner = userRepository.findById(newOwnerLong).orElseThrow(() -> new EntityNotFoundException("User not found, can't change owner"));
			if (!mural.getMembers().contains(newOwner)) throw new IncorrectParametersException("New owner is not a member of the mural. Cannot transfer ownership");
			User previousOwner = mural.getOwner();
			mural.setOwner(newOwner);
			userRepository.save(previousOwner);
			userRepository.save(newOwner);
		}
		if (name!=null && !name.isEmpty()) mural.setName(name);
		if (description!=null && !description.isEmpty()) mural.setDescription(description);
		muralRepository.save(mural);
	}

    private Collection<Mural> findOwnedBy(User user) {
		return muralRepository.findByOwner(user);
    }

	public Mural getMural(User user, Long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		if (!mural.getMembers().contains(user) || user.isAdmin()) throw new PermissionException("User is not a member of specified mural. Only members or admin can fetch a mural");
		return mural;
	}

	public byte[] getThumbnail(Long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		byte[] bytes = mural.getThumbnail();
		if (bytes==null) bytes = MuralService.getDefaultThumbnailBytes();
		return bytes;
	}

	public byte[] getBanner(Long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		byte[] bytes = mural.getBanner();
		if (bytes==null) bytes = MuralService.getDefaultBannerBytes();
		return bytes;
	}

	public Collection<Mural> getMurals(User user, String type) {
		if ("owned".equals(type)) return this.findOwnedBy(user);
		else if ("member".equals(type)) return user.getMemberMurals();
		else if ("other".equals(type)) return this.findOther(user.getMemberMurals());
		else throw new EntityNotFoundException("500 Internal Server Error: Unknown type for GET /api/murals");
	}

	public Mural createMural(Mural mural) {
		if (mural.getVisibility()!=VisibilityType.PRIVATE && mural.getVisibility()!=VisibilityType.PUBLIC) throw new IncorrectParametersException("Invalid visibility");

		this.newMural(mural);
		return mural;

	}

	public Integer joinMural(User user, String muralCode, Long muralId) {
		if (muralCode==null && muralId==null) throw new IncorrectParametersException("Tried to join mural without giving its id or code");
		if (muralCode!=null && muralId!=null) throw new IncorrectParametersException("When joining a mural you need to specify its code or its id, but not both."); //this never happens
		Mural mural = (muralCode!=null? muralRepository.findByCode(muralCode):muralRepository.findById(muralId)).orElseThrow(()->new EntityNotFoundException("Mural not found")) ;

		if (mural.getMembers().contains(user)) return 1; //1 for user already in mural
		if (mural.getBannedUsers().contains(user)) return 2; //2 for user banned from mural
		//join mural, return 0
		mural.addMember(user);
		user.addMemberMural(mural);
		muralRepository.save(mural);
		userRepository.save(user);
		return 0;
	}

	private List<User> removeSpecifiedUserFromMural(Mural mural, User user, Long inheritorId) {
		if (!mural.getMembers().contains(user)) throw new IncorrectParametersException("Target user is not a member of specified mural");
		if (mural.getMembers().size()==1) {
			this.deleteMuralConsequences(mural.getId());
			return new ArrayList<>();
		}

		//handle owner crap
		if (user.equals(mural.getOwner())) {
			User inheritor = null;
			if (inheritorId!=null) {
				User a = userRepository.findById(inheritorId).orElseThrow(()->new EntityNotFoundException("Inheriting user not found, cancelling."));
				if (!mural.getMembers().contains(a)) throw new IncorrectParametersException("Inheriting user is not a member of the mural");
				inheritor = a;

			}
			user.removeMemberMural(mural);
			mural.removeOwner(user, inheritor); //also removes from members
		} else {
			user.removeMemberMural(mural);
			mural.removeMember(user);
		}
		for (Activity a : activityRepository.findByOwner(user)) {
			if (a.getVisibility().isMuralSpecific() && a.getVisibility().getAllowedMuralsNonNull().contains(mural.getId())) {
				a.getVisibility().removeMural(mural.getId());
				if (a.getVisibility().getAllowedMuralsNonNull().isEmpty()) a.changeVisibilityTo(VisibilityType.PRIVATE);
				activityRepository.save(a);
			}
		}

		for (Route r: routeRepository.findAllByCreatedBy(user)) {
			if (r.getVisibility().isMuralSpecific()) {
				r.getVisibility().removeMural(mural.getId());
			}
			activityRepository.getByRoute(r).forEach(activity -> {
				if (!r.getCreatedBy().equals(activity.getOwner()) && r.equals(activity.getRoute())){
					activity.setRoute(null);
					activityRepository.save(activity);
				}
			});
			routeRepository.save(r);
		}
		userRepository.save(user);
		muralRepository.save(mural);
		return mural.getMembers();
	}

	public List<User> removeUserFromMural(User ppal, Long targetUserId, Long muralId, Long inheritorId) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		User targetUser = userRepository.findById(targetUserId).orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (!ppal.equals(targetUser) && !ppal.isAdmin() && !ppal.equals(mural.getOwner())) throw new PermissionException("Only admins and the mural owner can remove users from murals");

		return removeSpecifiedUserFromMural(mural, targetUser, inheritorId);
	}

	public List<User> banUser(User user, Long muralId, Long userId) {
		if (muralId==null || userId==null) throw new IncorrectParametersException("You need to specify a user id and mural id");
		Mural mural = muralRepository.findById(muralId).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		User targetUser = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User to ban not found"));

		if (!user.equals(mural.getOwner())) throw new PermissionException("Only admins and the mural owner can remove users from murals");
		if (targetUser.equals(mural.getOwner())) throw new IncorrectParametersException("The owner of the mural cannot be banned");

		List<User> members = removeSpecifiedUserFromMural(mural, targetUser, null);

		mural.banUser(targetUser);
		muralRepository.save(mural);
		return members;
	}

	public List<User> unbanUser(User ppal, Long targetUserId, Long muralId) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		if (!ppal.equals(mural.getOwner()) && !ppal.isAdmin()) throw new PermissionException("Only admin or mural owner can unban users");
		User targetUser = userRepository.findById(targetUserId).orElseThrow(() -> new EntityNotFoundException("User to unban not found"));
		mural.unbanUser(targetUser);
		muralRepository.save(mural);
		return mural.getBannedUsers();
	}

	public void deleteMural(User user, Long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Mural not found"));
		if (!user.equals(mural.getOwner()) && !user.isAdmin()) throw new PermissionException("User is not authorized to delete this mural");
		//checks made, now to actually delete it
		this.deleteMuralConsequences(id);
	}

	public Mural editMural(User user, Long muralId, MuralEditDTO changes) {
		Mural mural = muralRepository.findById(muralId).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		if (!user.equals(mural.getOwner())) throw new PermissionException("User is not authorized to edit this mural");
		this.patch(mural, changes);
		return mural;
	}

	public Boolean isVisibleByUser(User user, Long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Mural not found"));
		return user.getMemberMurals().contains(mural);
	}

	public void changeThumbnailOrBanner(boolean shouldChangeThumbnail, User user, Long id, byte[] image) {
		Mural mural = muralRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Mural not found"));
		if (!user.equals(mural.getOwner()) && !user.isAdmin()) throw new PermissionException("User is not authorized to edit this mural");
		//ImageIO is used to prevent injection

		if (shouldChangeThumbnail) mural.setThumbnail(image);
		else mural.setBanner(image);
		muralRepository.save(mural);
	}

	private void deleteMuralConsequences(long id) {
		Mural mural = muralRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Mural not found"));

		List<User> members = mural.getMembers();
		members.forEach(user -> {
			user.removeMemberMural(mural);
			userRepository.save(user);
		});
		activityRepository.findVisibleToMural(mural.getId(), members.stream().map(User::getId).toList()).forEach(activity -> {
			Visibility visibility = activity.getVisibility();
			if (visibility.isMuralSpecific()) {
				visibility.removeMural(mural.getId());
				activityRepository.save(activity);
			}
		});
		routeRepository.findVisibleToMural(mural.getId()).forEach(route -> {
			Visibility visibility = route.getVisibility();
			if (visibility.isMuralSpecific()) {
				visibility.removeMural(id);
				routeRepository.save(route);
			}
		});
		muralRepository.delete(mural);
	}
}
