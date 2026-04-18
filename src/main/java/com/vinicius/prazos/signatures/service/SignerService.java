package com.vinicius.prazos.signatures.service;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.signatures.domain.dto.CreateSignerRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitRequest;
import com.vinicius.prazos.signatures.domain.dto.PublicSignatureSubmitResponse;
import com.vinicius.prazos.signatures.domain.dto.PublicSigningViewResponse;
import com.vinicius.prazos.signatures.domain.dto.SignerResponse;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import com.vinicius.prazos.signatures.repository.SignerRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SignerService {

	private static final Duration TOKEN_TTL = Duration.ofDays(7);

	private final DocumentRepository documentRepository;
	private final SignerRepository signerRepository;
	private final CustomUserDetailsService userDetailsService;
	private final SignatureLogService signatureLogService;
	private final SignatureValidator signatureValidator;

	public SignerService(
		DocumentRepository documentRepository,
		SignerRepository signerRepository,
		CustomUserDetailsService userDetailsService,
		SignatureLogService signatureLogService,
		SignatureValidator signatureValidator
	) {
		this.documentRepository = documentRepository;
		this.signerRepository = signerRepository;
		this.userDetailsService = userDetailsService;
		this.signatureLogService = signatureLogService;
		this.signatureValidator = signatureValidator;
	}

	@Transactional
	public SignerResponse addSigner(Long documentId, String email, CreateSignerRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(documentId, user.getId());
		ensureDraft(document);

		String normalizedEmail = normalizeEmail(request.email());
		if (signerRepository.existsByDocumentIdAndEmailIgnoreCase(document.getId(), normalizedEmail)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um signatário com esse e-mail no documento");
		}

		Integer signingOrder = normalizeSigningOrder(request.signingOrder());
		validateSigningOrderConfiguration(document.getId(), signingOrder);

		Signer signer = new Signer();
		signer.setDocument(document);
		signer.setName(normalizeName(request.name()));
		signer.setEmail(normalizedEmail);
		signer.setSigningOrder(signingOrder);
		signer.setStatus(SignerStatus.PENDENTE);
		signer.setToken(UUID.randomUUID().toString());
		signer.setTokenExpiresAt(Instant.now().plus(TOKEN_TTL));

		Signer savedSigner = signerRepository.save(signer);
		signatureLogService.log(document, savedSigner, SignatureLogEvent.SIGNATARIO_ADICIONADO, "Signatário adicionado ao documento", null, null);
		return toResponse(savedSigner);
	}

	@Transactional(readOnly = true)
	public List<SignerResponse> listSigners(Long documentId, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		loadOwnedDocument(documentId, user.getId());

		return signerRepository.findAllByDocumentId(documentId).stream()
			.sorted(signerComparator())
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public PublicSigningViewResponse registerView(String token, String ipAddress, String userAgent) {
		Signer signer = loadActiveSignerByToken(token);

		if (signer.getViewedAt() == null) {
			signer.setViewedAt(Instant.now());
		}

		boolean canSign = canSignerSubmit(signer);
		if (canSign && signer.getStatus() == SignerStatus.PENDENTE) {
			signer.setStatus(SignerStatus.VISUALIZADO);
		}

		Signer savedSigner = signerRepository.save(signer);
		signatureLogService.log(
			savedSigner.getDocument(),
			savedSigner,
			SignatureLogEvent.LINK_VISUALIZADO,
			"Link de assinatura visualizado",
			ipAddress,
			userAgent
		);

		return toPublicViewResponse(savedSigner, canSign);
	}

	@Transactional
	public PublicSignatureSubmitResponse submitSignature(
		String token,
		PublicSignatureSubmitRequest request,
		String ipAddress,
		String userAgent
	) {
		Signer signer = loadActiveSignerByToken(token);

		if (signer.getSignedAt() != null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esse link já foi utilizado para assinar");
		}

		if (!canSignerSubmit(signer)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ainda não é a vez desse signatário assinar");
		}

		signer.setName(normalizeName(request.name()));
		signer.setSignatureValue(normalizeSignature(request.signature()));
		signer.setSignedAt(Instant.now());
		signer.setStatus(SignerStatus.ASSINADO);
		signer.setSignatureHash(signatureValidator.generateSignatureHash(signer.getDocument(), signer));

		Signer savedSigner = signerRepository.save(signer);
		signatureLogService.log(
			savedSigner.getDocument(),
			savedSigner,
			SignatureLogEvent.ASSINATURA_ENVIADA,
			"Assinatura registrada com sucesso",
			ipAddress,
			userAgent
		);

		boolean signatureValid = signatureValidator.isValid(savedSigner.getDocument(), savedSigner);
		signatureLogService.log(
			savedSigner.getDocument(),
			savedSigner,
			SignatureLogEvent.ASSINATURA_VALIDADA,
			signatureValid ? "Assinatura validada com sucesso" : "Falha na validação da assinatura",
			ipAddress,
			userAgent
		);

		Document document = savedSigner.getDocument();
		List<Signer> signers = signerRepository.findAllByDocumentId(document.getId());
		activateNextSigners(signers);
		document.setStatus(resolveDocumentStatus(document, signers));
		documentRepository.save(document);
		signerRepository.saveAll(signers);

		return new PublicSignatureSubmitResponse(
			savedSigner.getId(),
			document.getId(),
			savedSigner.getStatus(),
			document.getStatus(),
			savedSigner.getSignedAt(),
			signatureValid
		);
	}

	private Document loadOwnedDocument(Long documentId, Long userId) {
		return documentRepository.findByIdAndUserId(documentId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
	}

	private Signer loadActiveSignerByToken(String token) {
		Signer signer = signerRepository.findByToken(token)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link de assinatura inválido"));

		if (signer.getTokenExpiresAt().isBefore(Instant.now())) {
			if (signer.getStatus() != SignerStatus.EXPIRADO && signer.getSignedAt() == null) {
				signer.setStatus(SignerStatus.EXPIRADO);
				signerRepository.save(signer);
				signatureLogService.log(
					signer.getDocument(),
					signer,
					SignatureLogEvent.LINK_EXPIRADO,
					"Tentativa de uso de link expirado",
					null,
					null
				);
			}
			throw new ResponseStatusException(HttpStatus.GONE, "Link de assinatura expirado");
		}

		if (signer.getDocument().getStatus() == DocumentStatus.CANCELADO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento cancelado não pode ser assinado");
		}

		if (signer.getDocument().getStatus() == DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento ainda não foi enviado para assinatura");
		}

		return signer;
	}

	private void ensureDraft(Document document) {
		if (document.getStatus() != DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Signatários só podem ser gerenciados enquanto o documento estiver em rascunho");
		}
	}

	private void validateSigningOrderConfiguration(Long documentId, Integer signingOrder) {
		List<Signer> existingSigners = signerRepository.findAllByDocumentId(documentId);
		if (existingSigners.isEmpty()) {
			return;
		}

		boolean hasOrderedSigners = existingSigners.stream().anyMatch(existing -> existing.getSigningOrder() != null);
		boolean hasUnorderedSigners = existingSigners.stream().anyMatch(existing -> existing.getSigningOrder() == null);

		if ((hasOrderedSigners && signingOrder == null) || (hasUnorderedSigners && signingOrder != null)) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Para usar ordem de assinatura, todos os signatários do documento precisam seguir o mesmo padrão"
			);
		}

		if (signingOrder != null && existingSigners.stream().anyMatch(existing -> signingOrder.equals(existing.getSigningOrder()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe um signatário com essa ordem de assinatura");
		}
	}

	private boolean canSignerSubmit(Signer signer) {
		if (signer.getSignedAt() != null || signer.getStatus() == SignerStatus.EXPIRADO) {
			return false;
		}

		List<Signer> signers = signerRepository.findAllByDocumentId(signer.getDocument().getId());
		boolean hasSigningOrder = signers.stream().anyMatch(existing -> existing.getSigningOrder() != null);
		if (!hasSigningOrder) {
			return true;
		}

		Integer currentOrder = signer.getSigningOrder();
		if (currentOrder == null) {
			return true;
		}

		Optional<Integer> nextOrder = signers.stream()
			.filter(existing -> existing.getSignedAt() == null)
			.map(Signer::getSigningOrder)
			.filter(Objects::nonNull)
			.min(Integer::compareTo);

		return nextOrder.map(currentOrder::equals).orElse(true);
	}

	private void activateNextSigners(List<Signer> signers) {
		boolean hasSigningOrder = signers.stream().anyMatch(signer -> signer.getSigningOrder() != null);
		if (!hasSigningOrder) {
			return;
		}

		Optional<Integer> nextOrder = signers.stream()
			.filter(signer -> signer.getSignedAt() == null)
			.map(Signer::getSigningOrder)
			.filter(Objects::nonNull)
			.min(Integer::compareTo);

		for (Signer signer : signers) {
			if (signer.getSignedAt() != null || signer.getStatus() == SignerStatus.EXPIRADO || signer.getSigningOrder() == null) {
				continue;
			}

			if (nextOrder.isPresent() && nextOrder.get().equals(signer.getSigningOrder())) {
				if (signer.getStatus() == SignerStatus.AGUARDANDO_ORDEM) {
					signer.setStatus(SignerStatus.PENDENTE);
				}
			} else {
				signer.setStatus(SignerStatus.AGUARDANDO_ORDEM);
			}
		}
	}

	private DocumentStatus resolveDocumentStatus(Document document, List<Signer> signers) {
		long signedCount = signers.stream()
			.filter(signer -> signer.getSignedAt() != null)
			.count();

		if (signedCount == 0) {
			return DocumentStatus.AGUARDANDO_ASSINATURA;
		}

		if (signedCount < signers.size()) {
			return DocumentStatus.PARCIALMENTE_ASSINADO;
		}

		boolean allValid = signers.stream().allMatch(signer -> signatureValidator.isValid(document, signer));
		return allValid ? DocumentStatus.VALIDADO : DocumentStatus.ASSINADO;
	}

	private Comparator<Signer> signerComparator() {
		return Comparator.comparing(
			(Signer signer) -> signer.getSigningOrder() == null ? Integer.MAX_VALUE : signer.getSigningOrder()
		)
			.thenComparing(Signer::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Signer::getId, Comparator.nullsLast(Comparator.naturalOrder()));
	}

	private String normalizeName(String name) {
		String normalizedName = name.trim();
		if (normalizedName.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do signatário é obrigatório");
		}
		return normalizedName;
	}

	private String normalizeEmail(String email) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		if (normalizedEmail.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail do signatário é obrigatório");
		}
		return normalizedEmail;
	}

	private Integer normalizeSigningOrder(Integer signingOrder) {
		if (signingOrder == null) {
			return null;
		}

		if (signingOrder <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordem de assinatura deve ser maior que zero");
		}

		return signingOrder;
	}

	private String normalizeSignature(String signature) {
		String normalizedSignature = signature.trim();
		if (normalizedSignature.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assinatura é obrigatória");
		}
		return normalizedSignature;
	}

	private SignerResponse toResponse(Signer signer) {
		return new SignerResponse(
			signer.getId(),
			signer.getDocument().getId(),
			signer.getName(),
			signer.getEmail(),
			signer.getSigningOrder(),
			signer.getStatus(),
			signer.getToken(),
			buildPublicLink(signer),
			signer.getTokenExpiresAt(),
			signer.getViewedAt(),
			signer.getSignedAt(),
			signer.getSignedAt() == null ? null : signatureValidator.isValid(signer.getDocument(), signer),
			signer.getCreatedAt()
		);
	}

	private PublicSigningViewResponse toPublicViewResponse(Signer signer, boolean canSign) {
		return new PublicSigningViewResponse(
			signer.getId(),
			signer.getDocument().getId(),
			signer.getDocument().getTitle(),
			signer.getDocument().getContent(),
			signer.getName(),
			signer.getEmail(),
			signer.getSigningOrder(),
			signer.getStatus(),
			signer.getDocument().getStatus(),
			signer.getViewedAt(),
			signer.getSignedAt(),
			signer.getTokenExpiresAt(),
			signer.getSignedAt() == null ? null : signatureValidator.isValid(signer.getDocument(), signer),
			canSign
		);
	}

	private String buildPublicLink(Signer signer) {
		return "/sign/public/" + signer.getToken();
	}
}
