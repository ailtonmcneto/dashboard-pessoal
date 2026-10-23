package com.vida;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfig.class)
class VidaApplicationTests {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void sobeComPostgres18ComUuidV7() {
        String version = jdbc.queryForObject("select uuid_extract_version(uuidv7())::text", String.class);
        assertThat(version).isEqualTo("7");
    }
}
