package zm.ac.mu.registry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("stub")
class CredentialControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void validRequest_returns201_withLocationHeader() throws Exception {
        mvc.perform(post("/api/v1/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"graduateId":"G1","programme":"BSc CS","awardDate":"2026-01-01"}"""))
           .andExpect(status().isCreated())
           .andExpect(header().exists("Location"))
           .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void blankProgramme_returns400_withFieldError() throws Exception {
        mvc.perform(post("/api/v1/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"graduateId":"G1","programme":"","awardDate":"2026-01-01"}"""))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.errors[0].field").value("programme"));
    }

    @Test
    void badDate_returns400() throws Exception {
        mvc.perform(post("/api/v1/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"graduateId":"G1","programme":"BSc CS","awardDate":"yesterday"}"""))
           .andExpect(status().isBadRequest())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unknownCredential_returns404_problemJson() throws Exception {
        mvc.perform(get("/api/v1/credentials/404"))
           .andExpect(status().isNotFound())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
           .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void sizeTooBig_returns400() throws Exception {
        mvc.perform(get("/api/v1/credentials?size=1000"))
           .andExpect(status().isBadRequest());
    }
}