package shop.mtcoding.blog.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.util.Base64;

public class MyRSAUtil {

    public static JwtKeySet downloadRSAKey() {
        // 만약에 있으면 혹은 없으면
        String jwtUrl = "https://kauth.kakao.com/.well-known/jwks.json"; // ← 실제 주소로 교체

        RestTemplate restTemplate = new RestTemplate();
        JwtKeySet keySet = restTemplate.getForObject(jwtUrl, JwtKeySet.class);

        return keySet;
    }

    public static OAuthProfile verify(String idToken) {
        JwtKeySet keySet = downloadRSAKey();

        String n = keySet.getKeys().get(1).getN();
        String e = keySet.getKeys().get(1).getE();
        System.out.println("n : " + n);
        System.out.println("e : " + e);

        BigInteger bin = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger bie = new BigInteger(1, Base64.getUrlDecoder().decode(e));

        RSAKey rsaKey = new RSAKey.Builder(Base64URL.encode(bin), Base64URL.encode(bie)).build();
        try {
            // 1. 파싱
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            // 2. 검증
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());

            if (signedJWT.verify(verifier)) {
                System.out.println("ID Token을 검증하였습니다");
                String payload = signedJWT.getPayload().toString();

                System.out.println("페이로드");
                System.out.println(payload);
                ObjectMapper objectMapper = new ObjectMapper();
                OAuthProfile profile = objectMapper.readValue(payload, OAuthProfile.class);
                return profile;

            } else {
                throw new RuntimeException("id토큰 검증 실패");
            }
        } catch (Exception ex) { // 공개키가 카카오에서 변경됐을때!!
            throw new RuntimeException(ex.getMessage());
        }
    }
}
