package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.exception.HttpException;
import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.MuralRepository;
import codeurjc_students.ATRA.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class MuralService {

	@Autowired
	private MuralRepository muralRepository;

	@Autowired
	private UserRepository userRepository; //a bit sketchy, I fear circular dependencies

	public Optional<Mural> findById(long id) {
		return muralRepository.findById(id);
	}

	public boolean exists(long id) {
		return muralRepository.existsById(id);
	}

	public List<Mural> findAll() {
		return muralRepository.findAll();
	}

	public void save(Mural mural) {
		muralRepository.save(mural);
	}

	/**
	 * DeletionService.deleteMural(Long id) should be called instead.
	 * @param id
	 */
	void delete(long id) {
		muralRepository.deleteById(id);
	}

	public void newMural(Mural mural) {
		String code;
		do {
			String[] parts = UUID.randomUUID().toString().split("-");
			code = parts[1] + "-" + parts[2] + "-" + parts[3];
		} while (muralRepository.findByCode(code).isPresent()); //repeat until empty, to make sure it's not repeated
		mural.setCode(code);
		System.out.println(code);
		muralRepository.save(mural);
		//generate code

		muralRepository.save(mural);
		mural.getMembers().forEach(user -> {
			user.getMemberMurals().add(mural);
			userRepository.save(user);
		});
	}

	public Collection<Mural> findOther(List<Mural> memberMurals) {
		Set<Mural> hashSet = new HashSet<>(findAll());
		memberMurals.forEach(hashSet::remove);
		return hashSet.stream().filter(mural -> mural.getVisibility() == VisibilityType.PUBLIC).toList();
	}

	public static byte[] getDefaultThumbnailBytes() {
		File file = new File("target/classes/static/defaultThumbnailImage.png");
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static byte[] getDefaultBannerBytes() {
		File file = new File("target/classes/static/defaultBannerImage.png");
		try {
			return Files.readAllBytes(file.toPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Optional<Mural> findByCode(String muralCode) {
		return muralRepository.findByCode(muralCode);
	}

    public void patch(Mural mural, Map<String, String> newMural) {
		String name = newMural.get("name");
		String description = newMural.get("description");
		String newOwnerString = newMural.get("owner");
		if (name!=null) mural.setName(name);
		if (description!=null) mural.setDescription(description);
		if (newOwnerString!=null) {
			User newOwner = userRepository.findById(Long.parseLong(newOwnerString)).orElseThrow(() -> new HttpException(404, "User not found, can't change owner"));
			if (!mural.getMembers().contains(newOwner)) throw new HttpException(422, "New owner is not a member of the mural. Cannot transfer ownership");
			User previousOwner = mural.getOwner();
			mural.setOwner(newOwner);
			userRepository.save(previousOwner);
			userRepository.save(newOwner);
		}
		muralRepository.save(mural);


	}

    public Collection<Mural> findOwnedBy(User user) {
		return muralRepository.findByOwner(user);
    }
}
