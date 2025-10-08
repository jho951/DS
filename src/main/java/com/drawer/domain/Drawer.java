package com.drawer.domain;

import lombok.*;

import java.util.UUID;

import com.drawer.common.BaseEntity;

/**
 * Drawing 도메인 모델
 * - 벡터 JSON만 저장/수정/조회하는 기본 엔티티
 * - MyBatis에서 mapUnderscoreToCamelCase=true 설정 시
 *   DB 컬럼명(snake_case) ↔ 필드명(camelCase) 자동 매핑됩니다.
 *   CREATE TABLE drawing (
 *     id CHAR(36) PRIMARY KEY AUTO_INCREMENT,
 *     title VARCHAR(200) NOT NULL,
 *     width INT NULL,
 *     height INT NULL,
 *     vector_json JSON NOT NULL,
 *     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
 *   );
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Drawer extends BaseEntity {
	private UUID id;

	private String title;

	private Integer width;
	private Integer height;

	private String vectorJson;
}
