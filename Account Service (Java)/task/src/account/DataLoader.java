package account;

import account.model.Group;
import account.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DataLoader {

    private final GroupRepository groupRepository;

    @Autowired
    public DataLoader(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    @PostConstruct
    public void createRoles() {
        // Create roles if they do not exist
        groupRepository.findByName("ROLE_ADMINISTRATOR")
                .orElseGet(() -> groupRepository.save(new Group("ROLE_ADMINISTRATOR")));
        groupRepository.findByName("ROLE_ACCOUNTANT")
                .orElseGet(() -> groupRepository.save(new Group("ROLE_ACCOUNTANT")));
        groupRepository.findByName("ROLE_USER")
                .orElseGet(() -> groupRepository.save(new Group("ROLE_USER")));
    }
}
