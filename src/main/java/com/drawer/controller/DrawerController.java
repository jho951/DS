package com.drawer.controller;


import com.drawer.domain.Drawer;
import com.drawer.dto.*;
import com.drawer.service.DrawerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Drawing", description = "그림판 벡터 JSON 저장/조회/수정/삭제")
@RestController
@RequestMapping("/api/drawings")
@RequiredArgsConstructor
public class DrawerController {

	private final DrawerService service;

	@Operation(summary = "그림 생성")
	@PostMapping
	public ResponseEntity<Draweresponse> create(@Valid @RequestBody DrawerCreateRequest req) {
		Drawer d = Drawer.builder()
			.title(req.getTitle())
			.width(req.getWidth())
			.height(req.getHeight())
			.vectorJson(req.getVectorJson())
			.build();

		UUID id = service.create(d);
		Draweresponse body = Draweresponse.builder()
			.id(id)
			.title(d.getTitle())
			.width(d.getWidth())
			.height(d.getHeight())
			.vectorJson(d.getVectorJson())
			.build();

		return ResponseEntity.created(URI.create("/api/drawings/" + id)).body(body);
	}


	@Operation(summary = "단건 조회")
	@GetMapping("/{id}")
	public ResponseEntity<Draweresponse> get(@PathVariable UUID id) {
		Drawer row = service.get(id);
		return (row == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(toResponse(row));
	}

	@Operation(summary = "부분 수정(PATCH)")
	@PatchMapping("/{id}")
	public ResponseEntity<Draweresponse> patch(@PathVariable UUID id,
		@RequestBody DrawerUpdateRequest req) {
		if (service.get(id) == null) return ResponseEntity.notFound().build();

		Drawer patch = Drawer.builder()
			.title(req.getTitle())
			.width(req.getWidth())
			.height(req.getHeight())
			.vectorJson(req.getVectorJson())
			.build();

		service.updatePartial(id, patch);
		return ResponseEntity.ok(toResponse(service.get(id)));
	}

	@Operation(summary = "삭제")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		if (service.get(id) == null) return ResponseEntity.notFound().build();
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "목록 조회(페이징)")
	@GetMapping
	public ResponseEntity<List<Draweresponse>> list(@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "20") int size) {
		List<Drawer> rows = service.list(page, size);
		return ResponseEntity.ok(rows.stream().map(this::toResponse).toList());
	}

	private Draweresponse toResponse(Drawer d) {
		return Draweresponse.builder()
			.id(d.getId())
			.title(d.getTitle())
			.width(d.getWidth())
			.height(d.getHeight())
			.vectorJson(d.getVectorJson())
			.createdAt(d.getCreatedAt())
			.updatedAt(d.getUpdatedAt())
			.build();
	}
}
