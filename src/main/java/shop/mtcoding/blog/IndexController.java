package shop.mtcoding.blog;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import shop.mtcoding.blog._core.util.Resp;
import shop.mtcoding.blog.user.UserService;

@RequiredArgsConstructor
@RestController
public class IndexController {

    private final UserService userService;

    // redirect url : /kakao
    @GetMapping("/kakao")
    public String kakao(String code) {
        return "카카오로부터 받은 임시 코드 : " + code;
    }

    @PostMapping("/oauth/login")
    public ResponseEntity<?> login(@RequestBody String idToken) {
        System.out.println("idToken 잘받음 : " + idToken);
        var respDTO = userService.카카오로그인(idToken);
        return Resp.ok(respDTO);
    }
}
