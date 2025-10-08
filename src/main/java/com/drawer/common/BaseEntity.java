package com.drawer.common;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public abstract class BaseEntity {
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Integer version;
}
