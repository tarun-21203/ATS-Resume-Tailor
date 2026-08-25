package com.resumetailor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResumeRequestTest {

    @Test
    void recordFieldsAreAssigned() {
        ResumeRequest request = new ResumeRequest("abc", "jd", true);
        assertEquals("abc", request.resumePdfBase64());
        assertEquals("jd", request.jobDescription());
        assertEquals(true, request.includeCoverLetter());
    }
}
