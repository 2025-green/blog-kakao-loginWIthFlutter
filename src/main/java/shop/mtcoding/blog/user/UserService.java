package shop.mtcoding.blog.user;

import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.mtcoding.blog._core.error.ex.ExceptionApi400;
import shop.mtcoding.blog._core.error.ex.ExceptionApi401;
import shop.mtcoding.blog._core.error.ex.ExceptionApi404;
import shop.mtcoding.blog._core.util.JwtUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// 비지니스로직, 트랜잭션처리, DTO 완료
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    // RestAPI 규칙1 : insert 요청시에 그 행을 dto에 담아서 리턴한다
    @Transactional
    public UserResponse.DTO 회원가입(UserRequest.JoinDTO reqDTO) {
        String encPassword = BCrypt.hashpw(reqDTO.getPassword(), BCrypt.gensalt());
        reqDTO.setPassword(encPassword);

        Optional<User> userOP = userRepository.findByUsername(reqDTO.getUsername());
        if (userOP.isPresent()) throw new ExceptionApi400("중복된 유저네임이 존재합니다");

        User userPS = userRepository.save(reqDTO.toEntity());
        return new UserResponse.DTO(userPS);
    }

    // TODO : A4용지에다가 id, username 적어, A4용지를 서명, A4용지를 돌려주기
    public UserResponse.TokenDTO 로그인(UserRequest.LoginDTO loginDTO) {
        User userPS = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new ExceptionApi401("유저네임 혹은 비밀번호가 틀렸습니다"));

        Boolean isSame = BCrypt.checkpw(loginDTO.getPassword(), userPS.getPassword());

        if (!isSame) throw new ExceptionApi401("유저네임 혹은 비밀번호가 틀렸습니다");

        // 토큰 생성
        String accessToken = JwtUtil.create(userPS);
        String refreshToken = JwtUtil.createRefresh(userPS);

        // TODO: RestAPI 전환 끝나면 수업 해줄께요!!
        // DB에 Device서명값(LoginDTO), IP(request), User-Agent(request), RefreshToken(만든거 사용)

        return UserResponse.TokenDTO.builder().accessToken(accessToken).refreshToken(refreshToken).build();
    }


    public Map<String, Object> 유저네임중복체크(String username) {
        Optional<User> userOP = userRepository.findByUsername(username);
        Map<String, Object> dto = new HashMap<>();

        if (userOP.isPresent()) {
            dto.put("available", false);
        } else {
            dto.put("available", true);
        }
        return dto;
    }


    // TODO: 규칙3 : update된 데이터도 돌려줘야 함 (변경이 된 row를 돌려줘야 함)
    @Transactional
    public UserResponse.DTO 회원정보수정(UserRequest.UpdateDTO updateDTO, Integer userId) {

        User userPS = userRepository.findById(userId)
                .orElseThrow(() -> new ExceptionApi404("자원을 찾을 수 없습니다"));

        userPS.update(updateDTO.getPassword(), updateDTO.getEmail()); // 영속화된 객체의 상태변경
        return new UserResponse.DTO(userPS); // 리턴한 이유는 세션을 동기화해야해서!!
    } // 더티체킹 -> 상태가 변경되면 update을 날려요!!

    @Transactional
    public UserResponse.IdTokenDTO 카카오로그인(String idToken) { // application context 담기
        // 1. 공개키 존재 확인 없으면 다운로드
        // 2. id Token 검증 (base64 디코딩, 서명검증)
        OAuthProfile oAuthProfile = MyRSAUtil.verify(idToken);

        User user = null;

        // 3. 회원가입 유무 확인
        Optional<User> userOP
                = userRepository.findByUsername(ProviderType.KAKAO + "_" + oAuthProfile.getSub());

        if (userOP.isEmpty()) {
            // 4. 안되있으면 강제 회원가입
            user = User.builder()
                    .username(ProviderType.KAKAO + "_" + oAuthProfile.getSub())
                    .password(UUID.randomUUID().toString())
                    .email(null)
                    .provider(ProviderType.KAKAO)
                    .build();
            userRepository.save(user);
        } else {
            user = userOP.get();
        }


        return new UserResponse.IdTokenDTO(user, idToken);
    }
}
