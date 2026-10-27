package com.vida.common;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Contrato do OpenAPI a partir dos records: componente sem {@code @Nullable} é obrigatório;
 * com {@code @Nullable} é opcional e aceita {@code null}. Assim os tipos gerados para o front
 * ({@code npm run api:types}) não ficam com tudo opcional.
 */
@Component
class RecordNullabilityConverter implements ModelConverter {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        Schema resolved = chain.next().resolve(type, context, chain);
        Class<?> cls = Json.mapper().constructType(type.getType()).getRawClass();
        if (resolved == null || !cls.isRecord()) {
            return resolved;
        }

        Schema model = resolved;
        if (resolved.get$ref() != null) {
            String name = resolved.get$ref().substring(resolved.get$ref().lastIndexOf('/') + 1);
            model = context.getDefinedModels().get(name);
        }
        if (model == null || model.getProperties() == null) {
            return resolved;
        }

        Map<String, Schema> properties = model.getProperties();
        List<String> required = new ArrayList<>();
        for (RecordComponent component : cls.getRecordComponents()) {
            Schema property = properties.get(component.getName());
            if (property == null) {
                continue;
            }
            if (component.getAnnotatedType().isAnnotationPresent(Nullable.class)) {
                markNullable(property);
            } else {
                required.add(component.getName());
            }
        }
        model.setRequired(required.isEmpty() ? null : required);
        return resolved;
    }

    @SuppressWarnings("rawtypes")
    private static void markNullable(Schema property) {
        if (property.get$ref() != null) {
            return; // referências a outros schemas: só fica opcional
        }
        Set<String> types = new LinkedHashSet<>();
        if (property.getTypes() != null) {
            types.addAll(property.getTypes());
        } else if (property.getType() != null) {
            types.add(property.getType());
        }
        types.add("null");
        property.setTypes(types);
    }
}
