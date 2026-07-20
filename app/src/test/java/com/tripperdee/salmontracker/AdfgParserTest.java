package com.tripperdee.salmontracker;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class AdfgParserTest {
    private final FishRepository.Project project = FishRepository.PROJECTS.get(0);

    @Test public void parsesObjectRows() throws Exception {
        String json = "{\"data\":[{\"Date\":\"07/17\",\"Daily Count\":\"14,545\",\"Cumulative Count\":\"153,945\",\"Notes\":\"Preliminary\"}]}";
        List<AppDatabase.CountRecord> rows = FishRepository.parseOfficialPayload(json, project, 2026, 1L);
        assertEquals(1, rows.size());
        assertEquals("2026-07-17", rows.get(0).reportDate);
        assertEquals(14545, rows.get(0).dailyCount);
        assertEquals(153945, rows.get(0).cumulativeCount);
        assertEquals("PRELIMINARY", rows.get(0).status);
    }

    @Test public void parsesArrayRows() throws Exception {
        String json = "{\"aaData\":[[\"07/18\",\"18,426\",\"356,214\",\"Left Bank: 9,000\"]]}";
        List<AppDatabase.CountRecord> rows = FishRepository.parseOfficialPayload(json, project, 2026, 1L);
        assertEquals(1, rows.size());
        assertEquals(18426, rows.get(0).dailyCount);
        assertEquals(356214, rows.get(0).cumulativeCount);
    }

    @Test public void formattingOnlyChangesDoNotAffectFingerprint() throws Exception {
        String a = "[[\"07/18\",\"18,426\",\"356,214\"]]";
        String b = "[[\"07/18\",18426,356214]]";
        AppDatabase.CountRecord one = FishRepository.parseOfficialPayload(a, project, 2026, 1L).get(0);
        AppDatabase.CountRecord two = FishRepository.parseOfficialPayload(b, project, 2026, 2L).get(0);
        assertEquals(one.fingerprint, two.fingerprint);
    }

    @Test public void incompleteRowsAreRejected() throws Exception {
        String json = "[[\"07/18\",\"18,426\",\"-\"]]";
        assertTrue(FishRepository.parseOfficialPayload(json, project, 2026, 1L).isEmpty());
    }

    @Test public void htmlFallbackParsesOfficialTableShape() {
        String html = "<table><tr><td>07/18</td><td>18,426</td><td>356,214</td><td>Revised</td></tr></table>";
        List<AppDatabase.CountRecord> rows = FishRepository.parseHtmlPayload(html, project, 2026, 1L);
        assertEquals(1, rows.size());
        assertEquals("REVISED", rows.get(0).status);
    }
}
