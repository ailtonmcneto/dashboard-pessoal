package com.vida.saude;

import com.vida.common.BusinessException;
import com.vida.common.CurrentUser;
import com.vida.common.NotFoundException;
import com.vida.saude.HealthDtos.DayCell;
import com.vida.saude.HealthDtos.HabitRequest;
import com.vida.saude.HealthDtos.HabitResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HabitService {

    static final int LAST_DAYS = 7;

    private final HabitRepository habits;
    private final HabitCheckRepository checks;
    private final CurrentUser currentUser;

    HabitService(HabitRepository habits, HabitCheckRepository checks, CurrentUser currentUser) {
        this.habits = habits;
        this.checks = checks;
        this.currentUser = currentUser;
    }

    /** Hábitos com sequência, progresso da semana e os últimos 7 dias. */
    @Transactional(readOnly = true)
    public List<HabitResponse> list() {
        UUID userId = currentUser.id();
        LocalDate today = currentUser.today();
        List<Habit> list = habits.findByUserIdOrderByArchivedAscNameAsc(userId);
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, Set<LocalDate>> done = checks
                .findByUserIdAndHabitIdInAndDateGreaterThanEqual(userId, list.stream().map(Habit::getId).toList(),
                        today.minusDays(HabitStats.HISTORY_DAYS))
                .stream().collect(Collectors.groupingBy(HabitCheck::getHabitId,
                        Collectors.mapping(HabitCheck::getDate, Collectors.toSet())));
        return list.stream().map(h -> toResponse(h, done.getOrDefault(h.getId(), Set.of()), today)).toList();
    }

    @Transactional
    public HabitResponse create(HabitRequest req) {
        Habit habit = new Habit(currentUser.id());
        apply(habit, req);
        habits.save(habit);
        return toResponse(habit, Set.of(), currentUser.today());
    }

    @Transactional
    public HabitResponse update(UUID id, HabitRequest req) {
        apply(find(id), req);
        return get(id);
    }

    @Transactional
    public void delete(UUID id) {
        habits.delete(find(id));
    }

    /** Marca o dia como cumprido (idempotente). */
    @Transactional
    public HabitResponse check(UUID id, LocalDate date) {
        Habit habit = find(id);
        if (date.isAfter(currentUser.today())) {
            throw BusinessException.badRequest("Não dá para marcar um dia que ainda não chegou");
        }
        if (date.isBefore(habit.getStartsOn())) {
            throw BusinessException.badRequest("O hábito começou depois desse dia");
        }
        if (checks.findByUserIdAndHabitIdAndDate(currentUser.id(), id, date).isEmpty()) {
            checks.save(new HabitCheck(currentUser.id(), id, date));
        }
        return get(id);
    }

    @Transactional
    public HabitResponse uncheck(UUID id, LocalDate date) {
        find(id);
        checks.findByUserIdAndHabitIdAndDate(currentUser.id(), id, date).ifPresent(checks::delete);
        return get(id);
    }

    /** Dias cumpridos num intervalo (para o calendário). */
    @Transactional(readOnly = true)
    public List<LocalDate> history(UUID id, LocalDate from, LocalDate to) {
        find(id);
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw BusinessException.badRequest("Intervalo inválido (máximo de 366 dias)");
        }
        return checks.findByUserIdAndHabitIdAndDateBetweenOrderByDate(currentUser.id(), id, from, to).stream()
                .map(HabitCheck::getDate).toList();
    }

    private HabitResponse get(UUID id) {
        return list().stream().filter(h -> h.id().equals(id)).findFirst().orElseThrow();
    }

    private Habit find(UUID id) {
        return habits.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Hábito"));
    }

    private void apply(Habit habit, HabitRequest req) {
        Habit.Schedule schedule = req.schedule() == null ? Habit.Schedule.DIARIO : req.schedule();
        Set<DayOfWeek> weekdays = new HashSet<>();
        if (req.weekdays() != null) {
            req.weekdays().forEach(d -> weekdays.add(DayOfWeek.of(d)));
        }
        if (schedule == Habit.Schedule.DIAS_SEMANA && weekdays.isEmpty()) {
            throw BusinessException.badRequest("Escolha pelo menos um dia da semana");
        }
        if (schedule == Habit.Schedule.X_POR_SEMANA && req.timesPerWeek() == null) {
            throw BusinessException.badRequest("Informe quantas vezes por semana");
        }
        String name = req.name().strip();
        boolean duplicate = habits.findByUserIdOrderByArchivedAscNameAsc(currentUser.id()).stream()
                .anyMatch(h -> !Objects.equals(h.getId(), habit.getId()) && h.getName().equalsIgnoreCase(name));
        if (duplicate) {
            throw BusinessException.conflict("Já existe um hábito com esse nome");
        }
        LocalDate startsOn = req.startsOn() != null ? req.startsOn()
                : habit.getStartsOn() != null ? habit.getStartsOn() : currentUser.today();
        habit.update(name, schedule, weekdays, req.timesPerWeek(), startsOn,
                req.color() == null ? "#0d9488" : req.color().toLowerCase(), Boolean.TRUE.equals(req.archived()));
    }

    private static HabitResponse toResponse(Habit h, Set<LocalDate> done, LocalDate today) {
        HabitStats.Rule rule = h.rule();
        HabitStats.Stats s = HabitStats.compute(rule, done, today);
        List<DayCell> lastDays = new ArrayList<>();
        for (LocalDate d = today.minusDays(LAST_DAYS - 1L); !d.isAfter(today); d = d.plusDays(1)) {
            lastDays.add(new DayCell(d, HabitStats.isDue(rule, d), done.contains(d)));
        }
        List<Integer> weekdays = HabitStats.weekdays(rule.weekdaysMask()).stream()
                .map(DayOfWeek::getValue).sorted().toList();
        return new HabitResponse(h.getId(), h.getName(), h.getSchedule(), weekdays,
                h.getTimesPerWeek() == null ? null : h.getTimesPerWeek().intValue(), h.getStartsOn(), h.getColor(),
                h.isArchived(), s.currentStreak(), s.bestStreak(), s.streakUnit(), s.dueToday(), s.doneToday(),
                s.doneThisWeek(), s.targetThisWeek(), s.rate30(), lastDays);
    }
}
