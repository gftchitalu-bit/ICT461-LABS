package zm.ac.mu.registry;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.rate-limit.login=100")
@AutoConfigureMockMvc
@ActiveProfiles("mem")
class RegistryApiTest {
    @Autowired private MockMvc mvc;

    @Test
    void noCookieIsUnauthorized() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void graduateCannotUseRegistrarEndpoint() throws Exception {
        mvc.perform(get("/api/v1/credentials").cookie(login("grad@mu.ac.zm")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownFieldIsBadRequestWithFieldName() throws Exception {
        mvc.perform(post("/api/v1/credentials").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"graduateId":3,"programme":"BSc Computing","awardDate":"2020-01-01","extra":"x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("extra"));
    }

    @Test
    void logoutRejectsUnexpectedBodyFields() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unexpected\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("unexpected"));
    }

    @Test
    void scalarTypeMismatchIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/credentials").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"graduateId":"3","programme":"BSc Computing","awardDate":"2020-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("graduateId"));
    }

    @Test
    void invalidDateIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/credentials").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"graduateId":3,"programme":"BSc Computing","awardDate":"yesterday"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankProgrammeIsUnprocessable() throws Exception {
        mvc.perform(post("/api/v1/credentials").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"graduateId":3,"programme":"","awardDate":"2020-01-01"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].field").value("programme"));
    }

    @Test
    void futureAwardDateIsUnprocessable() throws Exception {
        mvc.perform(post("/api/v1/credentials").cookie(login("reg1@mu.ac.zm"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"graduateId":3,"programme":"BSc Computing","awardDate":"2999-01-01"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void textPostIsRejectedBeforeAuthentication() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.TEXT_PLAIN).content("not json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void revocationRequiresDifferentRegistrarAndCannotBeRepeated() throws Exception {
        Cookie firstRegistrar = login("reg1@mu.ac.zm");
        long credentialId = issueCredential(firstRegistrar, 3);
        MvcResult request = mvc.perform(post("/api/v1/credentials/{id}/revocations", credentialId)
                        .cookie(firstRegistrar).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Incorrect award record\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        long revocationId = objectLong(request, "id");
        mvc.perform(post("/api/v1/revocations/{id}/approvals", revocationId)
                        .cookie(firstRegistrar).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        Cookie secondRegistrar = login("reg2@mu.ac.zm");
        mvc.perform(post("/api/v1/revocations/{id}/approvals", revocationId)
                        .cookie(secondRegistrar).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/revocations/{id}/approvals", revocationId)
                        .cookie(secondRegistrar).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/credentials/{id}/revocations", credentialId)
                        .cookie(firstRegistrar).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Repeated request\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void graduateCannotShareAnotherGraduatesCredential() throws Exception {
        Cookie registrar = login("reg1@mu.ac.zm");
        long credentialId = issueCredential(registrar, 3);
        mvc.perform(post("/api/v1/credentials/{id}/share-links", credentialId)
                        .cookie(login("grad2@mu.ac.zm")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresInHours\":24}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutExpiresCookieAndInvalidatesSession() throws Exception {
        Cookie cookie = login("reg1@mu.ac.zm");
        mvc.perform(post("/api/v1/auth/logout").cookie(cookie).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("session_id="),
                                org.hamcrest.Matchers.containsString("Max-Age=0"),
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Strict"))));
        mvc.perform(get("/api/v1/auth/me").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsAuthenticatedUser() throws Exception {
        mvc.perform(get("/api/v1/auth/me").cookie(login("grad@mu.ac.zm")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("grad@mu.ac.zm"))
                .andExpect(jsonPath("$.role").value("GRADUATE"));
    }

    private Cookie login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getCookie("session_id");
    }

    private long issueCredential(Cookie registrar, long graduateId) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/credentials").cookie(registrar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"graduateId\":" + graduateId
                                + ",\"programme\":\"BSc Computing\",\"awardDate\":\"2020-01-01\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectLong(result, "id");
    }

    private long objectLong(MvcResult result, String field) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                result.getResponse().getContentAsString()).get(field).longValue();
    }
}
