package com.vida.saude;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Transforma registros soltos em um valor por dia (no fuso do usuário), conforme a métrica. */
final class DailyValues {

    record Point(Instant at, BigDecimal value) {
    }

    private DailyValues() {
    }

    static Map<LocalDate, BigDecimal> byDay(List<Point> points, MetricType.Aggregation aggregation, ZoneId zone) {
        Map<LocalDate, List<Point>> grouped = new HashMap<>();
        for (Point p : points) {
            grouped.computeIfAbsent(p.at().atZone(zone).toLocalDate(), d -> new ArrayList<>()).add(p);
        }
        Map<LocalDate, BigDecimal> result = new TreeMap<>();
        grouped.forEach((day, list) -> result.put(day, aggregate(list, aggregation)));
        return result;
    }

    static BigDecimal aggregate(List<Point> list, MetricType.Aggregation aggregation) {
        return switch (aggregation) {
            case SOMA -> list.stream().map(Point::value).reduce(BigDecimal.ZERO, BigDecimal::add);
            case MEDIA -> list.stream().map(Point::value).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
            case ULTIMO -> list.stream().max(Comparator.comparing(Point::at)).orElseThrow().value();
        };
    }

    /** A meta do dia foi cumprida? null quando não há meta. */
    static Boolean meets(BigDecimal value, MetricType metric) {
        if (metric.getTarget() == null) {
            return null;
        }
        int cmp = value.compareTo(metric.getTarget());
        return metric.getTargetKind() == MetricType.TargetKind.MINIMO ? cmp >= 0 : cmp <= 0;
    }
}
