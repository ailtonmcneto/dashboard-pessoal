package com.vida.financas;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.financas.FinanceDtos.CategoryRequest;
import com.vida.financas.FinanceDtos.CategoryResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    static final String DEFAULT_COLOR = "#78716c";

    /** Sugestão inicial; o usuário edita, arquiva ou cria outras. */
    private record Suggestion(Category.Kind kind, String name, String color) {
    }

    private static final List<Suggestion> DEFAULTS = List.of(
            new Suggestion(Category.Kind.DESPESA, "Moradia", "#2563eb"),
            new Suggestion(Category.Kind.DESPESA, "Alimentação", "#ea580c"),
            new Suggestion(Category.Kind.DESPESA, "Transporte", "#ca8a04"),
            new Suggestion(Category.Kind.DESPESA, "Saúde", "#16a34a"),
            new Suggestion(Category.Kind.DESPESA, "Educação", "#9333ea"),
            new Suggestion(Category.Kind.DESPESA, "Lazer", "#db2777"),
            new Suggestion(Category.Kind.DESPESA, "Assinaturas", "#0891b2"),
            new Suggestion(Category.Kind.DESPESA, "Compras", "#4f46e5"),
            new Suggestion(Category.Kind.DESPESA, "Outros", "#78716c"),
            new Suggestion(Category.Kind.RECEITA, "Salário", "#0f766e"),
            new Suggestion(Category.Kind.RECEITA, "Freelance", "#65a30d"),
            new Suggestion(Category.Kind.RECEITA, "Rendimentos", "#0284c7"),
            new Suggestion(Category.Kind.RECEITA, "Outras receitas", "#78716c"));

    private final CategoryRepository categories;
    private final CurrentUser currentUser;

    CategoryService(CategoryRepository categories, CurrentUser currentUser) {
        this.categories = categories;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return all().stream().map(CategoryService::toResponse).toList();
    }

    List<Category> all() {
        return categories.findByUserIdOrderByKindAscNameAsc(currentUser.id());
    }

    Map<UUID, Category> byId() {
        return all().stream().collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    /** Cria as categorias sugeridas — só se o usuário ainda não tiver nenhuma. */
    @Transactional
    public List<CategoryResponse> createDefaults() {
        UUID userId = currentUser.id();
        if (categories.existsByUserId(userId)) {
            throw BusinessException.conflict("Você já tem categorias cadastradas");
        }
        for (Suggestion d : DEFAULTS) {
            Category c = new Category(userId, d.kind());
            c.update(d.name(), null, d.color(), false);
            categories.save(c);
        }
        return list();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        Category category = new Category(currentUser.id(), req.kind());
        apply(category, req);
        return toResponse(categories.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        Category category = find(id);
        if (category.getKind() != req.kind()) {
            throw BusinessException.badRequest("Não dá para trocar o tipo de uma categoria");
        }
        apply(category, req);
        return toResponse(category);
    }

    /** Apaga a categoria (e subcategorias); os lançamentos ficam sem categoria. Prefira arquivar. */
    @Transactional
    public void delete(UUID id) {
        categories.delete(find(id));
    }

    Category find(UUID id) {
        return categories.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Categoria"));
    }

    private void apply(Category category, CategoryRequest req) {
        UUID parentId = req.parentId();
        if (parentId != null) {
            Category parent = find(parentId);
            if (parent.getParentId() != null) {
                throw BusinessException.badRequest("Só existe um nível de subcategoria");
            }
            if (parent.getKind() != req.kind()) {
                throw BusinessException.badRequest("A subcategoria precisa ser do mesmo tipo da categoria");
            }
            if (category.getId() != null && (parent.getId().equals(category.getId())
                    || categories.existsByParentId(category.getId()))) {
                throw BusinessException.badRequest("Uma categoria com subcategorias não pode virar subcategoria");
            }
        }
        boolean duplicate = all().stream().anyMatch(c -> !c.getId().equals(category.getId())
                && c.getKind() == req.kind()
                && Objects.equals(c.getParentId(), parentId)
                && c.getName().equalsIgnoreCase(req.name().strip()));
        if (duplicate) {
            throw BusinessException.conflict("Já existe uma categoria com esse nome");
        }
        category.update(req.name().strip(), parentId,
                req.color() == null ? DEFAULT_COLOR : req.color().toLowerCase(),
                Boolean.TRUE.equals(req.archived()));
    }

    static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getKind(), c.getParentId(), c.getColor(), c.isArchived());
    }
}
