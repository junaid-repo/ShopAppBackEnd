package com.management.shop.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SalesListResponse {

	private List<SalesResponseDTO> content;
	private Integer page;
	private Integer size;
	private Integer totalPages;
	private Integer totalElements;
	
}
