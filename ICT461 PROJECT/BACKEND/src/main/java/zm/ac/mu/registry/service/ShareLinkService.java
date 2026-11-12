package zm.ac.mu.registry.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import zm.ac.mu.registry.data.Data.Credential;
import zm.ac.mu.registry.data.Data.CredentialStatus;
import zm.ac.mu.registry.data.Data.ShareLink;
import zm.ac.mu.registry.data.Data.ShareLinkRepository;
import zm.ac.mu.registry.data.Data.User;
import zm.ac.mu.registry.dto.ShareLinkRequest;
import zm.ac.mu.registry.dto.ShareLinkResponse;
import zm.ac.mu.registry.web.ApiFailure;
import zm.ac.mu.registry.web.Hashing;

@Service
public class ShareLinkService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final CredentialService credentials;
    private final ShareLinkRepository links;
    private final AuditService audit;

    public ShareLinkService(CredentialService credentials, ShareLinkRepository links, AuditService audit) {
        this.credentials = credentials;
        this.links = links;
        this.audit = audit;
    }

    public ShareLinkResponse create(long credentialId, ShareLinkRequest request, User graduate, String baseUrl) {
        Credential credential = credentials.require(credentialId);
        if (credential.graduateId() != graduate.id())
            throw new ApiFailure(HttpStatus.FORBIDDEN, "You may only share your own credentials.");
        if (credential.status() == CredentialStatus.REVOKED)
            throw new ApiFailure(HttpStatus.CONFLICT, "A revoked credential cannot be shared.");
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires = Instant.now().plus(request.expiresInHours(), ChronoUnit.HOURS);
        links.saveShareLink(new ShareLink(Hashing.sha256(token), credentialId, expires));
        String url = baseUrl + "/api/v1/verify/" + token;
        audit.record(graduate.id(), "SHARE_CREATED", "CREDENTIAL", credentialId, "Share link created");
        return new ShareLinkResponse(token, url, qrDataUri(url), expires);
    }

    private String qrDataUri(String url) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate the share QR code.", exception);
        }
    }
}
