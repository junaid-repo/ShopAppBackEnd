package com.management.shop.repository;

 import com.management.shop.entity.LoginHistory;
 import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
 import java.util.List;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Integer> {

    @Query(value="SELECT * FROM login_history WHERE username = ?1 ORDER BY logged_timing DESC LIMIT 1", nativeQuery = true)
    LoginHistory findByUsername(String username);

    @Query(value="select * from login_history where logged_in_status = ?1 order by logged_timing desc", nativeQuery = true)
    List<LoginHistory> findAllByLogged(Boolean status);


}
