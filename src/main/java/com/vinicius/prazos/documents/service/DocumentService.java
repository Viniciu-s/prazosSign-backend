package com.vinicius.prazos.documents.service;

import com.vinicius.prazos.auth.domain.entity.User;
import com.vinicius.prazos.auth.security.CustomUserDetailsService;
import com.vinicius.prazos.documents.domain.dto.DocumentMoveRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentRequest;
import com.vinicius.prazos.documents.domain.dto.DocumentResponse;
import com.vinicius.prazos.documents.domain.entity.Document;
import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.documents.repository.DocumentRepository;
import com.vinicius.prazos.groups.domain.entity.Group;
import com.vinicius.prazos.groups.repository.GroupRepository;
import com.vinicius.prazos.signatures.domain.entity.Signer;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import com.vinicius.prazos.signatures.repository.SignerRepository;
import com.vinicius.prazos.signatures.repository.SignatureLogRepository;
import com.vinicius.prazos.signatures.service.SignatureLogService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentService {

	private final DocumentRepository documentRepository;
	private final GroupRepository groupRepository;
	private final CustomUserDetailsService userDetailsService;
	private final SignerRepository signerRepository;
	private final SignatureLogRepository signatureLogRepository;
	private final SignatureLogService signatureLogService;

	public DocumentService(
		DocumentRepository documentRepository,
		GroupRepository groupRepository,
		CustomUserDetailsService userDetailsService,
		SignerRepository signerRepository,
		SignatureLogRepository signatureLogRepository,
		SignatureLogService signatureLogService
	) {
		this.documentRepository = documentRepository;
		this.groupRepository = groupRepository;
		this.userDetailsService = userDetailsService;
		this.signerRepository = signerRepository;
		this.signatureLogRepository = signatureLogRepository;
		this.signatureLogService = signatureLogService;
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> listDocuments(String email, String status) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		DocumentStatus filter = parseStatus(status);

		List<Document> documents = filter == null
			? documentRepository.findAllByUserIdOrderByUpdatedAtDesc(user.getId())
			: documentRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), filter);

		return documents.stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public DocumentResponse getDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		return toResponse(loadOwnedDocument(id, user.getId()));
	}

	@Transactional
	public DocumentResponse createDocument(String email, DocumentRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);

		Document document = new Document();
		document.setUser(user);
		document.setTitle(normalizeTitle(request.title()));
		document.setContent(normalizeContent(request.content()));
		document.setStatus(DocumentStatus.RASCUNHO);

		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public DocumentResponse updateDocument(Long id, String email, DocumentRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());

		if (document.getStatus() != DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento enviado não pode ser editado");
		}

		document.setTitle(normalizeTitle(request.title()));
		document.setContent(normalizeContent(request.content()));

		return toResponse(documentRepository.save(document));
	}

	@Transactional
	public void deleteDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());
		signatureLogRepository.deleteByDocumentId(document.getId());
		signerRepository.deleteByDocumentId(document.getId());
		documentRepository.delete(document);
	}

	@Transactional
	public DocumentResponse sendDocument(Long id, String email) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());

		if (document.getStatus() != DocumentStatus.RASCUNHO) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento já foi enviado");
		}

		List<Signer> signers = signerRepository.findAllByDocumentId(document.getId());
		if (signers.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documento precisa ter ao menos um signatário antes do envio");
		}

		prepareSignersForSending(signers);
		document.setStatus(DocumentStatus.AGUARDANDO_ASSINATURA);
		Document savedDocument = documentRepository.save(document);
		signerRepository.saveAll(signers);
		signatureLogService.log(savedDocument, null, SignatureLogEvent.DOCUMENTO_ENVIADO, "Documento enviado para assinatura", null, null);
		return toResponse(savedDocument);
	}

	@Transactional
	public DocumentResponse moveDocument(Long id, String email, DocumentMoveRequest request) {
		User user = userDetailsService.loadDomainUserByEmail(email);
		Document document = loadOwnedDocument(id, user.getId());
		Group group = loadOwnedGroup(request.groupId(), user.getId());

		document.setGroup(group);
		return toResponse(documentRepository.save(document));
	}

	private Document loadOwnedDocument(Long id, Long userId) {
		return documentRepository.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
	}

	private Group loadOwnedGroup(Long groupId, Long userId) {
		if (groupId == null) {
			return null;
		}

		return groupRepository.findByIdAndUserId(groupId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo não encontrado"));
	}

	private DocumentStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}

		try {
			return DocumentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status do documento inválido");
		}
	}

	private void prepareSignersForSending(List<Signer> signers) {
		boolean hasSigningOrder = signers.stream().anyMatch(signer -> signer.getSigningOrder() != null);
		if (!hasSigningOrder) {
			signers.stream()
				.filter(signer -> signer.getSignedAt() == null)
				.forEach(signer -> signer.setStatus(SignerStatus.PENDENTE));
			return;
		}

		Optional<Integer> firstOrder = signers.stream()
			.map(Signer::getSigningOrder)
			.filter(Objects::nonNull)
			.min(Integer::compareTo);

		for (Signer signer : signers) {
			if (signer.getSignedAt() != null || signer.getSigningOrder() == null) {
				continue;
			}

			if (firstOrder.isPresent() && firstOrder.get().equals(signer.getSigningOrder())) {
				signer.setStatus(SignerStatus.PENDENTE);
			} else {
				signer.setStatus(SignerStatus.AGUARDANDO_ORDEM);
			}
		}
	}

	private String normalizeTitle(String title) {
		String normalizedTitle = title.trim();
		if (normalizedTitle.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título do documento é obrigatório");
		}
		return normalizedTitle;
	}

	private String normalizeContent(String content) {
		if (content == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conteúdo do documento é obrigatório");
		}
		return content;
	}

	private DocumentResponse toResponse(Document document) {
		Long groupId = document.getGroup() == null ? null : document.getGroup().getId();

		return new DocumentResponse(
			document.getId(),
			groupId,
			document.getTitle(),
			document.getContent(),
			document.getStatus(),
			document.getCreatedAt(),
			document.getUpdatedAt()
		);
	}
}
