package com.drawer.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 그림 조회/응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Draweresponse {

	private UUID id;
	private String title;
	private Integer width;
	private Integer height;
	private String vectorJson;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
