package com.management.shop.gobalusers.repository;

import com.management.shop.gobalusers.entity.RegisterUserOTPEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserOtpRepo extends JpaRepository<RegisterUserOTPEntity, Integer>{

	RegisterUserOTPEntity getByUsername(String storedUser);

    @Modifying
    @Transactional
    @Query(value = "UPDATE newuo  SET status = ?2 WHERE id = ?1", nativeQuery = true)
    void updateOldOTP(Integer id, String status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE newuo  SET status = ?2 WHERE phone_number = ?1 and event=?3 and source=?4", nativeQuery = true)
    void updateOldOTPWithPhone(String phoneNumber, String status, String event, String source);


    @Transactional
    @Modifying
    @Query(value = "DELETE FROM newuo WHERE username = ?1", nativeQuery = true)
    void removeOldOTP(String username);

    @Query(value = "SELECT * FROM newuo WHERE username = ?1 ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    RegisterUserOTPEntity getLatestOtp(String username);
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM newuo WHERE id = ?1", nativeQuery = true)
    void removeOldOTPById(Integer id);

    List<RegisterUserOTPEntity> getByPhoneNumber(String phoneNumber);

    @Query(
            value = "SELECT * FROM newuo " +
                    "WHERE phone_number = ?1 " +
                    "AND created_date >= CURDATE() " +
                    "AND created_date < CURDATE() + INTERVAL 1 DAY " +
                    "ORDER BY created_date",
            nativeQuery = true
    )
    List<RegisterUserOTPEntity> getByPhoneOtpForToday(String phoneNumber);

    @Query(value="SELECT * FROM newuo WHERE phone_number = ?1 and status =?2 ORDER BY created_date DESC LIMIT 1", nativeQuery = true)
    RegisterUserOTPEntity getLatestByPhone(String phone, String status);
}
