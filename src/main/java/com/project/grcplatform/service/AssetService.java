package com.project.grcplatform.service;

import com.project.grcplatform.constant.*;
import com.project.grcplatform.dto.AssetIncidentRequestDTO;
import com.project.grcplatform.dto.AssetIncidentResponseDTO;
import com.project.grcplatform.dto.AssetMapDTO;
import com.project.grcplatform.dto.AssetRequestDTO;
import com.project.grcplatform.dto.AssetResponseDTO;
import com.project.grcplatform.dto.UserSummaryDTO;
import com.project.grcplatform.exception.AppException;
import com.project.grcplatform.exception.NotFoundException;
import com.project.grcplatform.mapper.AssetMapper;
import com.project.grcplatform.model.Asset;
import com.project.grcplatform.model.AssetCategory;
import com.project.grcplatform.model.AssetHistory;
import com.project.grcplatform.model.AssetIncident;
import com.project.grcplatform.model.AssetType;
import com.project.grcplatform.model.Organisation;
import com.project.grcplatform.repository.AssetCategoryRepository;
import com.project.grcplatform.repository.AssetHistoryRepository;
import com.project.grcplatform.repository.AssetIncidentRepository;
import com.project.grcplatform.repository.AssetRepository;
import com.project.grcplatform.repository.AssetTypeRepository;
import com.project.grcplatform.model.User;
import com.project.grcplatform.repository.OrganisationRepository;
import com.project.grcplatform.repository.UserRepository;
import com.project.grcplatform.security.JwtAuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final AssetTypeRepository assetTypeRepository;
    private final AssetCategoryRepository assetCategoryRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final AssetIncidentRepository assetIncidentRepository;
    private final AuditService auditService;

    // Ref prefix per type name
    private static final Map<String, String> REF_PREFIX = Map.of(
            "PROCESS",     "P",
            "INFORMATION", "I",
            "EQUIPMENT",   "E",
            "HR",          "H",
            "LOCAL",       "L",
            "SUPPLIER",    "S",
            "APPLICATION", "A",
            "DOCUMENT",    "D"
    );

    // Types that require at least 1 PROCESS to exist
    private static final List<String> NEEDS_PROCESS = List.of(
            "INFORMATION", "HR", "LOCAL"
    );

    // Types that require at least 1 INFORMATION to exist
    private static final List<String> NEEDS_INFORMATION = List.of(
            "EQUIPMENT", "SUPPLIER", "APPLICATION", "DOCUMENT"
    );

    // ── Read ──────────────────────────────────────────────────────────────────

    public Page<AssetResponseDTO> findAll(String name, String categoryId, String typeId,
                                          LifecycleStatus lifecycleStatus, String ownerId,
                                          String directionCentraleId, String directionId,
                                          Pageable pageable) {
        return assetRepository
                .findAllWithFilters(name, categoryId, typeId, lifecycleStatus,
                        ownerId, directionCentraleId, directionId, pageable)
                .map(AssetMapper::toDTO);
    }

    public AssetMapDTO getMap(String directionCentraleId, String directionId, String typeId, String categoryId) {
        List<Asset> assets = assetRepository.findAllByDeletedFalse().stream()
                .filter(a -> directionCentraleId == null || (a.getDirectionCentrale() != null
                        && a.getDirectionCentrale().getId().equals(directionCentraleId)))
                .filter(a -> directionId == null || (a.getDirection() != null
                        && a.getDirection().getId().equals(directionId)))
                .filter(a -> typeId == null || (a.getType() != null
                        && a.getType().getId().equals(typeId)))
                .filter(a -> categoryId == null || (a.getCategory() != null
                        && a.getCategory().getId().equals(categoryId)))
                .toList();

        List<AssetMapDTO.AssetMapItemDTO> items = assets.stream()
                .map(this::toMapItem)
                .toList();

        // Summary
        Map<String, Long> byCategory = items.stream()
                .filter(i -> i.getCategoryName() != null)
                .collect(Collectors.groupingBy(AssetMapDTO.AssetMapItemDTO::getCategoryName, Collectors.counting()));

        Map<String, Long> byTypeSummary = items.stream()
                .filter(i -> i.getTypeName() != null)
                .collect(Collectors.groupingBy(AssetMapDTO.AssetMapItemDTO::getTypeName, Collectors.counting()));

        Map<Integer, Long> byCriticality = items.stream()
                .filter(i -> i.getCriticalityScore() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getCriticalityScore().intValue(), Collectors.counting()));

        // Grouped by type
        Map<String, List<AssetMapDTO.AssetMapItemDTO>> byType = items.stream()
                .filter(i -> i.getTypeName() != null)
                .collect(Collectors.groupingBy(AssetMapDTO.AssetMapItemDTO::getTypeName));

        // Grouped by direction (entity)
        Map<String, List<AssetMapDTO.AssetMapItemDTO>> byDirection = items.stream()
                .filter(i -> i.getDirectionName() != null)
                .collect(Collectors.groupingBy(AssetMapDTO.AssetMapItemDTO::getDirectionName));

        return AssetMapDTO.builder()
                .summary(AssetMapDTO.SummaryDTO.builder()
                        .total(items.size())
                        .byCategory(byCategory)
                        .byType(byTypeSummary)
                        .byCriticality(byCriticality)
                        .build())
                .byType(byType)
                .byDirection(byDirection)
                .build();
    }

    private AssetMapDTO.AssetMapItemDTO toMapItem(Asset a) {
        return AssetMapDTO.AssetMapItemDTO.builder()
                .id(a.getId())
                .ref(a.getRef())
                .name(a.getName())
                .typeName(a.getType() != null ? a.getType().getName() : null)
                .categoryName(a.getCategory() != null ? a.getCategory().getName() : null)
                .criticalityScore(a.getCriticalityScore())
                .ownerName(a.getOwner() != null
                        ? a.getOwner().getFirstname() + " " + a.getOwner().getLastname() : null)
                .directionName(a.getDirection() != null ? a.getDirection().getName() : null)
                .directionCentraleName(a.getDirectionCentrale() != null
                        ? a.getDirectionCentrale().getName() : null)
                .build();
    }

    public AssetResponseDTO findById(String id) {
        return AssetMapper.toDTO(getOrThrow(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public AssetResponseDTO create(AssetRequestDTO request) {
        AssetType type = assetTypeRepository.findById(request.getTypeId())
                .orElseThrow(() -> new NotFoundException("Asset type not found: " + request.getTypeId()));

        String typeName = type.getName().toUpperCase();

        // 1 — Dependency validation
        validateDependency(typeName);

        // 2 — Per-type field validation
        validateFieldsForType(typeName, request);

        // 3 — Build entity
        Asset asset = AssetMapper.toEntity(request);
        asset.setType(type);

        if (request.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Asset category not found: " + request.getCategoryId()));
            asset.setCategory(category);
        }

        resolveDirections(request, asset);

        // 4 — Resolve owner
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        asset.setOwner(owner);

        // 6 — Generate ref
        asset.setRef(generateRef(typeName));

        // 7 — Calculate criticality score
        asset.setCriticalityScore(calculateScore(typeName, request.getConfidentiality(),
                request.getIntegrity(), request.getAvailability()));

        Asset saved = assetRepository.saveAndFlush(asset);
        Asset fresh = assetRepository.findByIdAndDeletedFalse(saved.getId()).orElse(saved);
        saveHistory(fresh, "Asset created");

        auditService.log(AuditAction.ASSET_CREATED, AuditEntityType.ASSET,
                fresh.getId(), "Asset created: " + fresh.getName());

        return AssetMapper.toDTO(fresh);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public AssetResponseDTO update(String id, AssetRequestDTO request) {
        Asset asset = getOrThrow(id);

        Map<String, Object> oldValues = snapshot(asset);

        AssetMapper.updateEntity(asset, request);

        if (request.getCategoryId() != null) {
            AssetCategory category = assetCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException("Asset category not found"));
            asset.setCategory(category);
        }
        if (request.getTypeId() != null) {
            AssetType type = assetTypeRepository.findById(request.getTypeId())
                    .orElseThrow(() -> new NotFoundException("Asset type not found"));
            asset.setType(type);
        }

        resolveDirections(request, asset);

        if (request.getOwnerId() != null) {
            User owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            asset.setOwner(owner);
        }

        // Recalculate criticality if any CIA field or type changed
        String typeName = asset.getType() != null ? asset.getType().getName().toUpperCase() : "";
        asset.setCriticalityScore(calculateScore(typeName,
                asset.getConfidentiality(), asset.getIntegrity(), asset.getAvailability()));

        Asset saved = assetRepository.save(asset);
        saveHistory(saved, "Asset updated");

        auditService.log(AuditAction.ASSET_UPDATED, AuditEntityType.ASSET,
                saved.getId(), "Asset updated: " + saved.getName(), oldValues, snapshot(saved));

        return AssetMapper.toDTO(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(String id) {
        Asset asset = getOrThrow(id);
        asset.setDeleted(true);
        assetRepository.save(asset);
        saveHistory(asset, "Asset deleted");
        auditService.log(AuditAction.ASSET_DELETED, AuditEntityType.ASSET,
                id, "Asset deleted: " + asset.getName());
    }

    public List<AssetHistory> getHistory(String id) {
        getOrThrow(id);
        return assetHistoryRepository.findByAsset_IdOrderByCreatedAtDesc(id);
    }

    // ── Lifecycle transition ──────────────────────────────────────────────────

    @Transactional
    public AssetResponseDTO transition(String id, LifecycleStatus newStatus) {
        Asset asset = getOrThrow(id);
        LifecycleStatus current = asset.getLifecycleStatus();

        if (!current.canTransitionTo(newStatus)) {
            throw new AppException(ErrorCode.INVALID_DATA,
                    Map.of("message", "Cannot transition from " + current + " to " + newStatus));
        }

        asset.setLifecycleStatus(newStatus);
        assetRepository.save(asset);
        saveHistory(asset, "Lifecycle changed: " + current + " → " + newStatus);

        auditService.log(AuditAction.ASSET_LIFECYCLE_CHANGED, AuditEntityType.ASSET,
                asset.getId(), "Lifecycle changed from " + current + " to " + newStatus + " for: " + asset.getName());

        return AssetMapper.toDTO(asset);
    }

    // ── Expiring assets ───────────────────────────────────────────────────────

    public List<AssetResponseDTO> getExpiring(int withinDays) {
        LocalDate from = LocalDate.now();
        LocalDate to   = from.plusDays(withinDays);
        return assetRepository.findAllByEndOfLifeDateBetweenAndDeletedFalse(from, to)
                .stream().map(AssetMapper::toDTO).toList();
    }

    public List<AssetResponseDTO> getOverdue() {
        return assetRepository.findAllByEndOfLifeDateBeforeAndDeletedFalse(LocalDate.now())
                .stream().map(AssetMapper::toDTO).toList();
    }

    // ── Incidents ─────────────────────────────────────────────────────────────

    public List<AssetIncidentResponseDTO> getIncidents(String assetId) {
        getOrThrow(assetId);
        return assetIncidentRepository.findAllByAsset_IdOrderByOccurredAtDesc(assetId)
                .stream().map(this::toIncidentDTO).toList();
    }

    @Transactional
    public AssetIncidentResponseDTO createIncident(String assetId, AssetIncidentRequestDTO dto) {
        Asset asset = getOrThrow(assetId);

        User reporter = userRepository.findById(getCurrentUserId()).orElse(null);

        AssetIncident incident = AssetIncident.builder()
                .asset(asset)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .severity(dto.getSeverity())
                .occurredAt(dto.getOccurredAt())
                .resolvedAt(dto.getResolvedAt())
                .reportedBy(reporter)
                .build();

        AssetIncident saved = assetIncidentRepository.save(incident);

        auditService.log(AuditAction.ASSET_INCIDENT_CREATED, AuditEntityType.ASSET,
                assetId, "Incident reported on asset: " + asset.getName() + " — " + dto.getTitle());

        return toIncidentDTO(saved);
    }

    @Transactional
    public AssetIncidentResponseDTO updateIncident(String assetId, String incidentId,
                                                    AssetIncidentRequestDTO dto) {
        getOrThrow(assetId);
        AssetIncident incident = assetIncidentRepository.findByIdAndAsset_Id(incidentId, assetId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));

        if (dto.getTitle()       != null) incident.setTitle(dto.getTitle());
        if (dto.getDescription() != null) incident.setDescription(dto.getDescription());
        if (dto.getSeverity()    != null) incident.setSeverity(dto.getSeverity());
        if (dto.getOccurredAt()  != null) incident.setOccurredAt(dto.getOccurredAt());
        if (dto.getResolvedAt()  != null) incident.setResolvedAt(dto.getResolvedAt());

        AssetIncident saved = assetIncidentRepository.save(incident);

        auditService.log(AuditAction.ASSET_INCIDENT_UPDATED, AuditEntityType.ASSET,
                assetId, "Incident updated on asset id: " + assetId);

        return toIncidentDTO(saved);
    }

    @Transactional
    public void deleteIncident(String assetId, String incidentId) {
        getOrThrow(assetId);
        AssetIncident incident = assetIncidentRepository.findByIdAndAsset_Id(incidentId, assetId)
                .orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));
        assetIncidentRepository.delete(incident);

        auditService.log(AuditAction.ASSET_INCIDENT_DELETED, AuditEntityType.ASSET,
                assetId, "Incident deleted on asset id: " + assetId);
    }

    private AssetIncidentResponseDTO toIncidentDTO(AssetIncident i) {
        AssetIncidentResponseDTO dto = new AssetIncidentResponseDTO();
        dto.setId(i.getId());
        dto.setAssetId(i.getAsset().getId());
        dto.setAssetName(i.getAsset().getName());
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setSeverity(i.getSeverity());
        dto.setOccurredAt(i.getOccurredAt());
        dto.setResolvedAt(i.getResolvedAt());
        dto.setReportedBy(UserSummaryDTO.of(i.getReportedBy()));
        dto.setCreatedAt(i.getCreatedAt());
        return dto;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateDependency(String typeName) {
        if (NEEDS_PROCESS.contains(typeName) && !assetRepository.existsByType_NameAndDeletedFalse("PROCESS")) {
            throw new AppException(ErrorCode.INVALID_DATA,
                    Map.of("message", "A PROCESS asset must exist before creating a " + typeName + " asset"));
        }
        if (NEEDS_INFORMATION.contains(typeName) && !assetRepository.existsByType_NameAndDeletedFalse("INFORMATION")) {
            throw new AppException(ErrorCode.INVALID_DATA,
                    Map.of("message", "An INFORMATION asset must exist before creating a " + typeName + " asset"));
        }
    }

    private void validateFieldsForType(String typeName, AssetRequestDTO request) {
        if ("PROCESS".equals(typeName)) {
            // PROCESS must NOT have C/I
            if (request.getConfidentiality() != null || request.getIntegrity() != null) {
                throw new AppException(ErrorCode.INVALID_DATA,
                        Map.of("message", "PROCESS assets do not use confidentiality or integrity fields"));
            }
        } else {
            // All other types MUST have C and I
            if (request.getConfidentiality() == null || request.getIntegrity() == null) {
                throw new AppException(ErrorCode.INVALID_DATA,
                        Map.of("message", "Confidentiality and integrity are required for " + typeName + " assets"));
            }
        }
    }

    private String generateRef(String typeName) {
        String prefix = REF_PREFIX.getOrDefault(typeName, typeName.substring(0, 1));
        int max = assetRepository.findMaxRefNumberByTypeName(typeName);
        return prefix + (max + 1);
    }

    private short calculateScore(String typeName, Short c, Short i, Short d) {
        if ("PROCESS".equals(typeName)) {
            return d != null ? d : 1;
        }
        short sc = c != null ? c : 1;
        short si = i != null ? i : 1;
        short sd = d != null ? d : 1;
        return (short) Math.max(Math.max(sc, si), sd);
    }

    private void resolveDirections(AssetRequestDTO request, Asset asset) {
        if (request.getDirectionCentraleId() != null) {
            Organisation dc = organisationRepository.findById(request.getDirectionCentraleId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
            asset.setDirectionCentrale(dc);
        }
        if (request.getDirectionId() != null) {
            Organisation dir = organisationRepository.findById(request.getDirectionId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORGANISATION_NOT_FOUND));
            asset.setDirection(dir);
        }
    }

    private Asset getOrThrow(String id) {
        return assetRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.ASSET_NOT_FOUND));
    }

    private Map<String, Object> snapshot(Asset asset) {
        Map<String, Object> s = new HashMap<>();
        s.put("name",            asset.getName());
        s.put("ref",             asset.getRef());
        s.put("owner",           asset.getOwner() != null ? asset.getOwner().getEmail() : null);
        s.put("category",        asset.getCategory()        != null ? asset.getCategory().getName()        : null);
        s.put("type",            asset.getType()            != null ? asset.getType().getName()            : null);
        s.put("confidentiality", asset.getConfidentiality());
        s.put("integrity",       asset.getIntegrity());
        s.put("availability",    asset.getAvailability());
        s.put("criticalityScore",asset.getCriticalityScore());
        s.put("lifecycleStatus", asset.getLifecycleStatus() != null ? asset.getLifecycleStatus().name()   : null);
        return s;
    }

    private void saveHistory(Asset asset, String summary) {
        AssetHistory history = AssetHistory.builder()
                .asset(asset)
                .changedBy(getCurrentUserId())
                .changeSummary(summary)
                .snapshot(snapshot(asset))
                .build();
        assetHistoryRepository.save(history);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuthToken) return jwtAuthToken.getUserId();
        return "system";
    }
}
