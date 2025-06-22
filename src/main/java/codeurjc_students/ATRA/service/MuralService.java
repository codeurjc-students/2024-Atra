package codeurjc_students.ATRA.service;

import codeurjc_students.ATRA.model.Mural;
import codeurjc_students.ATRA.model.User;
import codeurjc_students.ATRA.model.auxiliary.VisibilityType;
import codeurjc_students.ATRA.repository.MuralRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class MuralService {

	@Autowired
	private MuralRepository repository;

	@Autowired
	private UserService userService; //a bit sketchy, I fear circular dependencies

	public Optional<Mural> findById(long id) {
		return repository.findById(id);
	}

	public boolean exists(long id) {
		return repository.existsById(id);
	}

	public List<Mural> findAll() {
		return repository.findAll();
	}

	public void save(Mural mural) {
		repository.save(mural);
	}

	/**
	 * DeletionService.deleteMural(Long id) should be called instead.
	 * @param id
	 */
	void delete(long id) {
		repository.deleteById(id);
	}

	public void newMural(Mural mural) {
		String code;
		do {
			String[] parts = UUID.randomUUID().toString().split("-");
			code = parts[1] + "-" + parts[2] + "-" + parts[3];
		} while (repository.findByCode(code).isPresent()); //repeat until empty, to make sure it's not repeated
		mural.setCode(code);
		System.out.println(code);
		repository.save(mural);
		//generate code

		repository.save(mural);
		User owner = mural.getOwner();
		owner.getOwnedMurals().add(mural);
		userService.save(owner);
		mural.getMembers().forEach(user -> {
			user.getMemberMurals().add(mural);
			userService.save(user);
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
		return repository.findByCode(muralCode);
	}
}
