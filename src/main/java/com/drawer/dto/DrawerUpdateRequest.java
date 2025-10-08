package com.drawer.dto;

import lombok.*;

/**
 * 그림 수정 요청 DTO (부분 수정용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class DrawerUpdateRequest {

	private String title;

	private Integer width;
	private Integer height;

	private String vectorJson;
}
