package com.management.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.management.shop.entity.Report;

public interface ReportDetailsRepo extends JpaRepository<Report, Integer>{

	@Query(value="SELECT * FROM reports_details where user_id=?2 ORDER BY created_at  DESC LIMIT ?1", nativeQuery=true)
	List<Report> findByLimit(Integer limit, String userId);

}
