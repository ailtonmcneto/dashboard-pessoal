package com.vida.saude;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.common.Strings;
import com.vida.saude.HealthDtos.DayStatus;
import com.vida.saude.HealthDtos.DayValue;
import com.vida.saude.HealthDtos.MeasurementRequest;
import com.vida.saude.HealthDtos.MeasurementResponse;
import com.vida.saude.HealthDtos.MetricRequest;
import com.vida.saude.HealthDtos.MetricResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricService {

    static final int MAX_SERIES_DAYS = 366;

    private record Suggestion(String name, String unit, MetricType.Aggregation aggregation, String target,
            MetricType.TargetKind targetKind, String color) {
    }

    private static final List<Suggestion> DEFAULTS = List.of(
            new Suggestion("Sono", "h", MetricType.Aggregation.SOMA, "8", MetricType.TargetKind.MINIMO, "#6250d6"),
            new Suggestion("Água", "ml", MetricType.Aggregation.SOMA, "2000", MetricType.TargetKind.MINIMO, "#2a78d6"),
            new Suggestion("Treino", "min", MetricType.Aggregation.SOMA, "30", MetricType.TargetKind.MINIMO, "#eb6834"),
            new Suggestion("Peso", "kg", MetricType.Aggregation.ULTIMO, null, null, "#0d9488"),
            new Suggestion("Humor", "1–5", MetricType.Aggregation.MEDIA, null, null, "#e87ba4"));

    private final MetricTypeRepository metrics;
    private final MeasurementRepository measurements;
    private final CurrentUser currentUser;

    MetricService(MetricTypeRepository metrics, MeasurementRepository measurements, CurrentUser currentUser) {
        this.metrics = metrics;
        this.measurements = measurements;
        this.currentUser = currentUser;
    }

    /** Métricas com o valor de hoje, a média dos últimos 7 dias e o último registro. */
    @Transactional(readOnly = true)
    public List<MetricResponse> list() {
        UUID userId = currentUser.id();
        ZoneId zone = currentUser.zone();
        LocalDate today = currentUser.today();
        List<MetricType> list = metrics.findByUserIdOrderByArchivedAscNameAsc(userId);
        if (list.isEmpty()) {
            return List.of();
        }
        Instant weekStart = today.minusDays(6).atStartOfDay(zone).toInstant();
        Map<UUID, List<Measurement>> recent = measurements
                .findByUserIdAndMetricTypeIdInAndMeasuredAtGreaterThanEqual(userId,
                        list.stream().map(MetricType::getId).toList(), weekStart)
                .stream().collect(Collectors.groupingBy(Measurement::getMetricTypeId));
        return list.stream().map(m -> toResponse(m, recent.getOrDefault(m.getId(), List.of()), today, zone)).toList();
    }

    @Transactional
    public List<MetricResponse> createDefaults() {
        UUID userId = currentUser.id();
        if (metrics.existsByUserId(userId)) {
            throw BusinessException.conflict("Você já tem métricas cadastradas");
        }
        for (Suggestion s : DEFAULTS) {
            MetricType m = new MetricType(userId);
            m.update(s.name(), s.unit(), s.aggregation(), s.target() == null ? null : new BigDecimal(s.target()),
                    s.targetKind(), s.color(), false);
            metrics.save(m);
        }
        return list();
    }

    @Transactional
    public MetricResponse create(MetricRequest req) {
        MetricType metric = new MetricType(currentUser.id());
        apply(metric, req);
        metrics.save(metric);
        return toResponse(metric, List.of(), currentUser.today(), currentUser.zone());
    }

    @Transactional
    public MetricResponse update(UUID id, MetricRequest req) {
        MetricType metric = find(id);
        apply(metric, req);
        return list().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    /** Apaga a métrica com todos os registros. Para manter o histórico, arquive. */
    @Transactional
    public void delete(UUID id) {
        metrics.delete(find(id));
    }

    // ---------- registros ----------

    @Transactional
    public MeasurementResponse addMeasurement(UUID metricId, MeasurementRequest req) {
        MetricType metric = find(metricId);
        if (metric.isArchived()) {
            throw BusinessException.badRequest("A métrica " + metric.getName() + " está arquivada");
        }
        Instant at = req.measuredAt() != null ? req.measuredAt() : Instant.now();
        if (at.isAfter(Instant.now().plus(Duration.ofMinutes(5)))) {
            throw BusinessException.badRequest("O registro não pode ser no futuro");
        }
        Measurement m = measurements.save(new Measurement(currentUser.id(), metric.getId(), at, req.value(),
                Strings.clean(req.note())));
        return toResponse(m);
    }

    /** Registros entre os dias {@code from} e {@code to} (inclusive), mais recentes primeiro. */
    @Transactional(readOnly = true)
    public List<MeasurementResponse> measurements(UUID metricId, LocalDate from, LocalDate to) {
        find(metricId);
        checkRange(from, to);
        ZoneId zone = currentUser.zone();
        return measurements.findByUserIdAndMetricTypeIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                        currentUser.id(), metricId, from.atStartOfDay(zone).toInstant(),
                        to.plusDays(1).atStartOfDay(zone).toInstant())
                .stream().map(MetricService::toResponse).toList();
    }

    @Transactional
    public void deleteMeasurement(UUID id) {
        measurements.delete(measurements.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new NotFoundException("Registro")));
    }

    /** Um valor por dia (null sem registro), com a indicação se bateu a meta — para o gráfico. */
    @Transactional(readOnly = true)
    public List<DayValue> series(UUID metricId, LocalDate from, LocalDate to) {
        MetricType metric = find(metricId);
        checkRange(from, to);
        ZoneId zone = currentUser.zone();
        List<DailyValues.Point> points = measurements
                .findByUserIdAndMetricTypeIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                        currentUser.id(), metricId, from.atStartOfDay(zone).toInstant(),
                        to.plusDays(1).atStartOfDay(zone).toInstant())
                .stream().map(m -> new DailyValues.Point(m.getMeasuredAt(), m.getValue())).toList();
        Map<LocalDate, BigDecimal> byDay = DailyValues.byDay(points, metric.getAggregation(), zone);
        List<DayValue> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            BigDecimal v = byDay.get(d);
            result.add(new DayValue(d, v, v == null ? null : DailyValues.meets(v, metric)));
        }
        return result;
    }

    MetricType find(UUID id) {
        return metrics.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Métrica"));
    }

    private void apply(MetricType metric, MetricRequest req) {
        if ((req.target() == null) != (req.targetKind() == null)) {
            throw BusinessException.badRequest("Meta precisa de valor e tipo (mínimo ou máximo) juntos");
        }
        String name = req.name().strip();
        boolean duplicate = metrics.findByUserIdOrderByArchivedAscNameAsc(currentUser.id()).stream()
                .anyMatch(m -> !Objects.equals(m.getId(), metric.getId()) && m.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw BusinessException.conflict("Já existe uma métrica com esse nome");
        }
        metric.update(name, req.unit().strip(),
                req.aggregation() == null ? MetricType.Aggregation.SOMA : req.aggregation(),
                req.target(), req.targetKind(),
                req.color() == null ? "#0d9488" : req.color().toLowerCase(), Boolean.TRUE.equals(req.archived()));
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw BusinessException.badRequest("'to' não pode ser antes de 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_SERIES_DAYS) {
            throw BusinessException.badRequest("Intervalo máximo de 366 dias");
        }
    }

    private MetricResponse toResponse(MetricType m, List<Measurement> recent, LocalDate today, ZoneId zone) {
        Map<LocalDate, BigDecimal> byDay = DailyValues.byDay(
                recent.stream().map(x -> new DailyValues.Point(x.getMeasuredAt(), x.getValue())).toList(),
                m.getAggregation(), zone);
        BigDecimal todayValue = byDay.get(today);
        DayStatus status;
        if (todayValue == null) {
            status = DayStatus.SEM_DADOS;
        } else {
            Boolean met = DailyValues.meets(todayValue, m);
            status = met == null ? DayStatus.SEM_META : met ? DayStatus.ATINGIDA : DayStatus.NAO_ATINGIDA;
        }
        BigDecimal average7 = byDay.isEmpty() ? null
                : byDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(byDay.size()), 2, RoundingMode.HALF_UP);
        Measurement last = measurements.findFirstByUserIdAndMetricTypeIdOrderByMeasuredAtDesc(currentUser.id(), m.getId())
                .orElse(null);
        return new MetricResponse(m.getId(), m.getName(), m.getUnit(), m.getAggregation(), m.getTarget(),
                m.getTargetKind(), m.getColor(), m.isArchived(), todayValue, status, average7,
                last == null ? null : last.getValue(), last == null ? null : last.getMeasuredAt());
    }

    private static MeasurementResponse toResponse(Measurement m) {
        return new MeasurementResponse(m.getId(), m.getMetricTypeId(), m.getMeasuredAt(), m.getValue(), m.getNote());
    }
}
