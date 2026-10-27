package com.vida.saude;

import com.vida.common.CurrentUser;
import com.vida.saude.HealthDtos.DayValue;
import com.vida.saude.HealthDtos.HabitRequest;
import com.vida.saude.HealthDtos.HabitResponse;
import com.vida.saude.HealthDtos.MeasurementRequest;
import com.vida.saude.HealthDtos.MeasurementResponse;
import com.vida.saude.HealthDtos.MetricRequest;
import com.vida.saude.HealthDtos.MetricResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Saúde: métricas (com registros e série diária) e hábitos (com marcação por dia). */
@RestController
class HealthController {

    private final MetricService metrics;
    private final HabitService habits;
    private final CurrentUser currentUser;

    HealthController(MetricService metrics, HabitService habits, CurrentUser currentUser) {
        this.metrics = metrics;
        this.habits = habits;
        this.currentUser = currentUser;
    }

    // ---------- Métricas ----------

    @GetMapping("/metrics")
    List<MetricResponse> metrics() {
        return metrics.list();
    }

    @PostMapping("/metrics")
    @ResponseStatus(HttpStatus.CREATED)
    MetricResponse createMetric(@Valid @RequestBody MetricRequest req) {
        return metrics.create(req);
    }

    /** Cria as métricas sugeridas (sono, água, treino, peso, humor) — só para quem não tem nenhuma. */
    @PostMapping("/metrics/defaults")
    @ResponseStatus(HttpStatus.CREATED)
    List<MetricResponse> createDefaultMetrics() {
        return metrics.createDefaults();
    }

    @PutMapping("/metrics/{id}")
    MetricResponse updateMetric(@PathVariable UUID id, @Valid @RequestBody MetricRequest req) {
        return metrics.update(id, req);
    }

    @DeleteMapping("/metrics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMetric(@PathVariable UUID id) {
        metrics.delete(id);
    }

    /** Sem datas: últimos 30 dias. */
    @GetMapping("/metrics/{id}/series")
    List<DayValue> series(@PathVariable UUID id, @RequestParam(required = false) @Nullable LocalDate from,
            @RequestParam(required = false) @Nullable LocalDate to) {
        LocalDate end = to != null ? to : currentUser.today();
        return metrics.series(id, from != null ? from : end.minusDays(29), end);
    }

    @GetMapping("/metrics/{id}/measurements")
    List<MeasurementResponse> measurements(@PathVariable UUID id,
            @RequestParam(required = false) @Nullable LocalDate from,
            @RequestParam(required = false) @Nullable LocalDate to) {
        LocalDate end = to != null ? to : currentUser.today();
        return metrics.measurements(id, from != null ? from : end.minusDays(29), end);
    }

    @PostMapping("/metrics/{id}/measurements")
    @ResponseStatus(HttpStatus.CREATED)
    MeasurementResponse addMeasurement(@PathVariable UUID id, @Valid @RequestBody MeasurementRequest req) {
        return metrics.addMeasurement(id, req);
    }

    @DeleteMapping("/measurements/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMeasurement(@PathVariable UUID id) {
        metrics.deleteMeasurement(id);
    }

    // ---------- Hábitos ----------

    @GetMapping("/habits")
    List<HabitResponse> habits() {
        return habits.list();
    }

    @PostMapping("/habits")
    @ResponseStatus(HttpStatus.CREATED)
    HabitResponse createHabit(@Valid @RequestBody HabitRequest req) {
        return habits.create(req);
    }

    @PutMapping("/habits/{id}")
    HabitResponse updateHabit(@PathVariable UUID id, @Valid @RequestBody HabitRequest req) {
        return habits.update(id, req);
    }

    @DeleteMapping("/habits/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteHabit(@PathVariable UUID id) {
        habits.delete(id);
    }

    /** Marca o dia como cumprido (idempotente). */
    @PutMapping("/habits/{id}/checks/{date}")
    HabitResponse check(@PathVariable UUID id, @PathVariable LocalDate date) {
        return habits.check(id, date);
    }

    @DeleteMapping("/habits/{id}/checks/{date}")
    HabitResponse uncheck(@PathVariable UUID id, @PathVariable LocalDate date) {
        return habits.uncheck(id, date);
    }

    /** Sem datas: últimas 12 semanas. */
    @GetMapping("/habits/{id}/history")
    List<LocalDate> history(@PathVariable UUID id, @RequestParam(required = false) @Nullable LocalDate from,
            @RequestParam(required = false) @Nullable LocalDate to) {
        LocalDate end = to != null ? to : currentUser.today();
        return habits.history(id, from != null ? from : end.minusWeeks(12).plusDays(1), end);
    }
}
