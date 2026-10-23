package com.vida.academico;

import com.vida.academico.AcademicDtos.ResourceRequest;
import com.vida.academico.AcademicDtos.ResourceResponse;
import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cursos livres, livros, vídeos… */
@Service
public class ResourceService {

    private final ResourceRepository resources;
    private final CurrentUser currentUser;

    ResourceService(ResourceRepository resources, CurrentUser currentUser) {
        this.resources = resources;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> list() {
        return resources.findByUserIdOrderByStatusAscTitleAsc(currentUser.id()).stream()
                .map(ResourceService::toResponse)
                .toList();
    }

    @Transactional
    public ResourceResponse create(ResourceRequest req) {
        Resource resource = new Resource(currentUser.id());
        apply(resource, req);
        return toResponse(resources.save(resource));
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest req) {
        Resource resource = find(id);
        apply(resource, req);
        return toResponse(resource);
    }

    @Transactional
    public void delete(UUID id) {
        resources.delete(find(id));
    }

    Resource find(UUID id) {
        return resources.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Recurso"));
    }

    Map<UUID, Resource> findAll(Collection<UUID> ids) {
        return resources.findByUserIdAndIdIn(currentUser.id(), ids).stream()
                .collect(Collectors.toMap(Resource::getId, Function.identity()));
    }

    private void apply(Resource resource, ResourceRequest req) {
        int done = req.doneUnits() == null ? 0 : req.doneUnits();
        if (req.totalUnits() != null && done > req.totalUnits()) {
            throw BusinessException.badRequest("O progresso não pode passar do total");
        }
        if (req.startedOn() != null && req.finishedOn() != null && req.finishedOn().isBefore(req.startedOn())) {
            throw BusinessException.badRequest("A conclusão não pode ser antes do início");
        }
        Resource.Status status = req.status() == null ? Resource.Status.ATIVO : req.status();
        LocalDate today = currentUser.today();
        LocalDate startedOn = req.startedOn();
        LocalDate finishedOn = req.finishedOn();
        if (startedOn == null && status != Resource.Status.PLANEJADO) {
            startedOn = finishedOn != null && finishedOn.isBefore(today) ? finishedOn : today;
        }
        if (status == Resource.Status.CONCLUIDO) {
            finishedOn = finishedOn == null ? today : finishedOn;
            done = req.totalUnits() != null ? req.totalUnits() : done;
        }
        resource.update(req.title().strip(), req.kind() == null ? Resource.Kind.CURSO : req.kind(),
                Strings.clean(req.provider()), Strings.clean(req.url()), req.totalUnits(), done, status,
                startedOn, finishedOn);
    }

    static ResourceResponse toResponse(Resource r) {
        Integer percent = r.getTotalUnits() == null ? null : r.getDoneUnits() * 100 / r.getTotalUnits();
        return new ResourceResponse(r.getId(), r.getTitle(), r.getKind(), r.getProvider(), r.getUrl(),
                r.getTotalUnits(), r.getDoneUnits(), percent, r.getStatus(), r.getStartedOn(), r.getFinishedOn());
    }
}
