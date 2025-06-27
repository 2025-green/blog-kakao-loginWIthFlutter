package shop.mtcoding.blog._core.filter;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import shop.mtcoding.blog._core.util.JwtUtil;
import shop.mtcoding.blog._core.util.Resp;
import shop.mtcoding.blog.user.User;

import java.io.IOException;
import java.io.PrintWriter;

public class AuthorizationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        System.out.println("카카오톡 로그인이면 실행되면 안됨 주소가 /s 일때만 실행됨");
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String accessToken = request.getHeader("Authorization");

        try {
            if (accessToken == null || accessToken.isBlank()) throw new RuntimeException("토큰을 전달해주세요");
            if (!accessToken.startsWith("Bearer ")) throw new RuntimeException("Bearer 프로토콜 지켜야지 짜식아");

            accessToken = accessToken.replace("Bearer ", "");


            User user = JwtUtil.verify(accessToken);

            System.out.println("----------------------------------------");
            // 토큰을 다시 검증하기 귀찮아서, 임시로 세션에 넣어둔거다.
            HttpSession session = request.getSession();
            session.setAttribute("sessionUser", user);
            System.out.println("토큰을 받아서 세션에 넣음!!!!!!!!!!!!!!!!!!");
            System.out.println("----------------------------------------");

            chain.doFilter(request, response);
        } catch (TokenExpiredException e1) {
            e1.printStackTrace();
            exResponse(response, "토큰이 만료되었습니다");
        } catch (JWTDecodeException | SignatureVerificationException e2) {
            e2.printStackTrace();
            exResponse(response, "토큰 검증에 실패했어요");
        } catch (RuntimeException e3) {
            e3.printStackTrace();
            exResponse(response, e3.getMessage());
        }
    }

    private void exResponse(HttpServletResponse response, String msg) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.setStatus(401);
        PrintWriter out = response.getWriter();

        Resp<?> resp = Resp.fail(401, msg);
        String responseBody = new ObjectMapper().writeValueAsString(resp);
        out.println(responseBody);
    }
}
