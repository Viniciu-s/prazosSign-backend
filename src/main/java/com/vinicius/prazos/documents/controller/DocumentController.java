package com.vinicius.prazos.documents.controller;

import com.vinicius.prazos.documents.domain.dto.DocumentMoveRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentResponse;
import com.vinicius.prazos.documents.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@GetMapping
	public List<DocumentResponse> list(
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestParam(required = false) String status
	) {
		return documentService.listDocuments(userDetails.getUsername(), status);
	}

	@GetMapping("/{id}")
	public DocumentResponse get(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
		return documentService.getDocument(id, userDetails.getUsername());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public DocumentResponse create(
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody DocumentRequest request
	) {
		return documentService.createDocument(userDetails.getUsername(), request);
	}

	@PutMapping("/{id}")
	public DocumentResponse update(
		@PathVariable Long id,
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody DocumentRequest request
	) {
		return documentService.updateDocument(id, userDetails.getUsername(), request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
		documentService.deleteDocument(id, userDetails.getUsername());
	}

	@PostMapping("/{id}/send")
	public DocumentResponse send(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
		return documentService.sendDocument(id, userDetails.getUsername());
	}

	@PostMapping("/{id}/move")
	public DocumentResponse move(
		@PathVariable Long id,
		@AuthenticationPrincipal UserDetails userDetails,
		@RequestBody DocumentMoveRequest request
	) {
		return documentService.moveDocument(id, userDetails.getUsername(), request);
	}
}
