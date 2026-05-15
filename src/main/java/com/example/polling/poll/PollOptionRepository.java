package com.example.polling.poll;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
    List<PollOption> findByPollAndIdIn(Poll poll, Collection<Long> ids);
}
