package account.service;

import account.model.SecurityEvent;
import account.repository.SecurityEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityEventServiceImpl implements SecurityEventService {

    private final SecurityEventRepository securityEventRepository;

    @Autowired
    public SecurityEventServiceImpl(SecurityEventRepository securityEventRepository) {
        this.securityEventRepository = securityEventRepository;
    }

    @Override
    public void logEvent(String action, String subject, String object, String path) {
        SecurityEvent event = new SecurityEvent(LocalDateTime.now(), action, subject, object, path);
        securityEventRepository.save(event);
    }

    @Override
    public List<SecurityEvent> getAllEvents() {
        return securityEventRepository.findAll();
    }
}
