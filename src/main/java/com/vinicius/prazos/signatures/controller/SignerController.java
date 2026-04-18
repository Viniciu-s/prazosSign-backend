package com.vinicius.prazos.signatures.controller;

import com.vinicius.prazos.signatures.domain.dto.CreateSignerRequest;
import com.vinicius.prazos.signatures.domain.dto.SignerResponse;
import com.vinicius.prazos.signatures.service.SignerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/documents/{id}/signers")
public class SignerController {

	private final SignerService signerService;

	public SignerController(SignerService signerService) {
		this.signerService = signerService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SignerResponse create(
		@PathVariable("id") Long documentId,
		@AuthenticationPrincipal UserDetails userDetails,
		@Valid @RequestBody CreateSignerRequest request
	) {
		return signerService.addSigner(documentId, userDetails.getUsername(), request);
	}

	@GetMapping
	public List<SignerResponse> list(
		@PathVariable("id") Long documentId,
		@AuthenticationPrincipal UserDetails userDetails
	) {
		return signerService.listSigners(documentId, userDetails.getUsername());
	}
}
