package com.vinicius.prazos.signatures.controller;

import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitResponse;
import com.vinicius.prazos.signatures.domain.dto.PublicSigningViewResponse;
import com.vinicius.prazos.signatures.service.SignerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sign/public/{token}")
public class PublicSigningController {

	private final SignerService signerService;

	public PublicSigningController(SignerService signerService) {
		this.signerService = signerService;
	}

	@PostMapping("/view")
	public PublicSigningViewResponse view(@PathVariable String token, HttpServletRequest request) {
		return signerService.registerView(token, request.getRemoteAddr(), request.getHeader("User-Agent"));
	}

	@PostMapping("/submit")
	public PublicSignatureSubmitResponse submit(
		@PathVariable String token,
		@Valid @RequestBody PublicSignatureSubmitRequest request,
		HttpServletRequest servletRequest
	) {
		return signerService.submitSignature(token, request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
	}
}
