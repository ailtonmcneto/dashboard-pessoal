package com.vida.saude;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DailyValuesTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private static DailyValues.Point p(String instant, String value) {
        return new DailyValues.Point(Instant.parse(instant), new BigDecimal(value));
    }

    // 01:30Z do dia 23 ainda é 22:30 do dia 22 em São Paulo
    private final List<DailyValues.Point> points = List.of(
            p("2026-10-22T12:00:00Z", "500"),
            p("2026-10-23T01:30:00Z", "300"),
            p("2026-10-23T12:00:00Z", "1000"));

    @Test
    void agrupaPeloDiaNoFusoDoUsuario() {
        var soma = DailyValues.byDay(points, MetricType.Aggregation.SOMA, SP);
        assertThat(soma.get(LocalDate.of(2026, 10, 22))).isEqualByComparingTo("800");
        assertThat(soma.get(LocalDate.of(2026, 10, 23))).isEqualByComparingTo("1000");
    }

    @Test
    void mediaEUltimo() {
        var media = DailyValues.byDay(points, MetricType.Aggregation.MEDIA, SP);
        assertThat(media.get(LocalDate.of(2026, 10, 22))).isEqualByComparingTo("400");
        var ultimo = DailyValues.byDay(points, MetricType.Aggregation.ULTIMO, SP);
        assertThat(ultimo.get(LocalDate.of(2026, 10, 22))).isEqualByComparingTo("300");
    }

    @Test
    void metaMinimaEMaxima() {
        MetricType agua = new MetricType(null);
        agua.update("Água", "ml", MetricType.Aggregation.SOMA, new BigDecimal("2000"), MetricType.TargetKind.MINIMO,
                "#000000", false);
        assertThat(DailyValues.meets(new BigDecimal("2000"), agua)).isTrue();
        assertThat(DailyValues.meets(new BigDecimal("1999"), agua)).isFalse();

        MetricType tela = new MetricType(null);
        tela.update("Tela", "min", MetricType.Aggregation.SOMA, new BigDecimal("120"), MetricType.TargetKind.MAXIMO,
                "#000000", false);
        assertThat(DailyValues.meets(new BigDecimal("90"), tela)).isTrue();
        assertThat(DailyValues.meets(new BigDecimal("121"), tela)).isFalse();

        MetricType peso = new MetricType(null);
        peso.update("Peso", "kg", MetricType.Aggregation.ULTIMO, null, null, "#000000", false);
        assertThat(DailyValues.meets(new BigDecimal("80"), peso)).isNull();
    }
}
