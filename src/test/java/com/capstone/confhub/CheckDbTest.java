package com.capstone.confhub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Disabled("Requires external database configuration")
public class CheckDbTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void checkPaper() {
        System.out.println("=========================================");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, title, abstract_field FROM papers ORDER BY id DESC LIMIT 5");
        for (Map<String, Object> row : rows) {
            System.out.println("Paper ID: " + row.get("id"));
            System.out.println("Title: " + row.get("title"));
            System.out.println("Abstract: " + row.get("abstract_field"));
            System.out.println("-----------------------------------------");
        }
        
        List<Map<String, Object>> files = jdbcTemplate.queryForList("SELECT id, paper_id, url FROM paper_files ORDER BY id DESC LIMIT 5");
        for (Map<String, Object> row : files) {
            System.out.println("File ID: " + row.get("id") + ", Paper ID: " + row.get("paper_id") + "\nURL: " + row.get("url"));
            System.out.println("-----------------------------------------");
        }
        System.out.println("=========================================");
    }
}
